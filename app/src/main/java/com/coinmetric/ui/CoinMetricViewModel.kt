package com.coinmetric.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coinmetric.data.local.CategoryMonthSpendRow
import com.coinmetric.data.local.CategorySpendRow
import com.coinmetric.data.local.CoinMetricDatabase
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.TransactionEntity
import com.coinmetric.data.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CoinMetricViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = BudgetRepository(CoinMetricDatabase.get(app).dao())
    private val notificationHelper = LimitNotificationHelper(app)
    private val notifiedThresholdByCategory = mutableMapOf<Long, LimitThreshold>()

    private data class CoreState(
        val categories: List<Category>,
        val members: List<FamilyMember>,
        val invites: List<CollaborationInvite>,
        val limits: List<CategoryLimit>,
        val recurringPayments: List<RecurringPayment>,
    )

    private data class FinanceState(
        val transactions: List<TransactionEntity>,
        val categorySpend: List<CategorySpendRow>,
        val monthCategorySpend: List<CategoryMonthSpendRow>,
        val totalIncome: Double,
        val totalExpense: Double,
    )

    val state: StateFlow<UiState> = combine(
        combine(
            repository.categories(),
            repository.members(),
            repository.invites(),
            repository.limits(),
            repository.recurringPayments(),
        ) { categories, members, invites, limits, recurring ->
            CoreState(categories, members, invites, limits, recurring)
        },
        combine(
            repository.transactions(),
            repository.categorySpend(),
            repository.monthCategorySpend(currentMonthKey()),
            repository.income(),
            repository.expenses(),
        ) { transactions, spend, monthSpend, income, expense ->
            FinanceState(transactions, spend, monthSpend, income, expense)
        },
    ) { core, finance ->
        UiState(
            categories = core.categories,
            members = core.members,
            invites = core.invites,
            limits = core.limits,
            recurringPayments = core.recurringPayments,
            transactions = finance.transactions,
            categorySpend = finance.categorySpend,
            monthCategorySpend = finance.monthCategorySpend,
            limitProgress = buildLimitProgress(core, finance),
            weeklyReport = buildWeeklyReport(finance.transactions, core.categories),
            monthlyReport = buildMonthlyReport(finance.transactions, core.categories),
            totalIncome = finance.totalIncome,
            totalExpense = finance.totalExpense,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init {
        seedData()
    }

    private fun seedData() = viewModelScope.launch {
        if (state.value.categories.isEmpty()) {
            repository.addCategory("Еда", "#FF7043")
            repository.addCategory("Транспорт", "#29B6F6")
            repository.addCategory("Коммунальные", "#AB47BC")
        }
    }

    fun addFamilyMember(name: String, email: String, role: String = "member") = viewModelScope.launch {
        repository.addMember(name, email, role)
    }

    fun sendInvite(email: String, inviterName: String) = viewModelScope.launch {
        repository.sendInvite(email, inviterName)
    }

    fun acceptInvite(invite: CollaborationInvite) = viewModelScope.launch {
        repository.setInviteStatus(invite, "accepted")
        repository.addMember(invite.email.substringBefore('@'), invite.email, invite.role)
    }

    fun declineInvite(invite: CollaborationInvite) = viewModelScope.launch {
        repository.setInviteStatus(invite, "declined")
    }

    fun addCategoryLimit(categoryId: Long, monthlyLimit: Double) = viewModelScope.launch {
        repository.addCategoryLimit(categoryId, monthlyLimit, currentMonthKey())
    }

    fun addRecurring(title: String, amount: Double, day: Int) = viewModelScope.launch {
        repository.addRecurring(title, amount, day)
    }

    fun addTransaction(expression: String, note: String, categoryId: Long?, memberId: Long?, isIncome: Boolean) =
        viewModelScope.launch {
            val amount = evaluateExpression(expression)
            if (amount > 0.0) {
                repository.addTransaction(amount, note, categoryId, memberId, System.currentTimeMillis(), isIncome)
                if (!isIncome && categoryId != null) {
                    checkCategoryLimit(categoryId)
                }
            }
        }

    private suspend fun checkCategoryLimit(categoryId: Long) {
        val monthKey = currentMonthKey()
        val limit = state.value.limits.firstOrNull { it.categoryId == categoryId && it.monthKey == monthKey } ?: return
        val spent = repository.getMonthlyCategoryExpense(categoryId, monthKey)
        val categoryName = state.value.categories.firstOrNull { it.id == categoryId }?.name ?: "Категория"

        val threshold = when {
            spent > limit.monthlyLimit -> LimitThreshold.EXCEEDED
            spent >= limit.monthlyLimit * 0.8 -> LimitThreshold.WARNING
            else -> LimitThreshold.NONE
        }

        val previousThreshold = notifiedThresholdByCategory[categoryId] ?: LimitThreshold.NONE
        if (threshold <= previousThreshold) return

        when (threshold) {
            LimitThreshold.EXCEEDED -> notificationHelper.notifyLimitExceeded(categoryName, spent, limit.monthlyLimit)
            LimitThreshold.WARNING -> notificationHelper.notifyLimitAlmostReached(categoryName, spent, limit.monthlyLimit)
            LimitThreshold.NONE -> Unit
        }
        notifiedThresholdByCategory[categoryId] = threshold
    }

    private fun buildLimitProgress(core: CoreState, finance: FinanceState): List<CategoryLimitProgress> {
        val monthKey = currentMonthKey()
        val monthLimits = core.limits.filter { it.monthKey == monthKey }
        val spendsByCategory = finance.monthCategorySpend.associateBy { it.categoryId }

        return monthLimits.mapNotNull { limit ->
            val category = core.categories.firstOrNull { it.id == limit.categoryId } ?: return@mapNotNull null
            val spent = spendsByCategory[limit.categoryId]?.spent ?: 0.0
            CategoryLimitProgress(
                categoryId = category.id,
                categoryName = category.name,
                spent = spent,
                limit = limit.monthlyLimit,
            )
        }.sortedByDescending { it.progress }
    }

    private fun buildWeeklyReport(
        transactions: List<TransactionEntity>,
        categories: List<Category>,
    ): BudgetPeriodReport {
        val now = System.currentTimeMillis()
        val startCurrent = startOfCurrentWeek(now)
        val duration = now - startCurrent
        val startPrevious = startCurrent - duration
        val previousStats = calculateStats(transactions, categories, startPrevious, startCurrent)
        val currentStats = calculateStats(transactions, categories, startCurrent, now)
        return BudgetPeriodReport(
            title = "Неделя",
            income = currentStats.income,
            expense = currentStats.expense,
            topExpenseCategory = currentStats.topCategory,
            expenseTrendPercent = calculateTrend(currentStats.expense, previousStats.expense),
        )
    }

    private fun buildMonthlyReport(
        transactions: List<TransactionEntity>,
        categories: List<Category>,
    ): BudgetPeriodReport {
        val now = System.currentTimeMillis()
        val startCurrent = startOfCurrentMonth(now)
        val duration = now - startCurrent
        val startPrevious = startCurrent - duration
        val previousStats = calculateStats(transactions, categories, startPrevious, startCurrent)
        val currentStats = calculateStats(transactions, categories, startCurrent, now)
        return BudgetPeriodReport(
            title = "Месяц",
            income = currentStats.income,
            expense = currentStats.expense,
            topExpenseCategory = currentStats.topCategory,
            expenseTrendPercent = calculateTrend(currentStats.expense, previousStats.expense),
        )
    }

    private fun calculateStats(
        transactions: List<TransactionEntity>,
        categories: List<Category>,
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): PeriodStats {
        val periodTransactions = transactions.filter { it.dateEpochMillis in startEpochMillis until endEpochMillis }
        val income = periodTransactions.filter { it.isIncome }.sumOf { it.amount }
        val expenses = periodTransactions.filterNot { it.isIncome }
        val expense = expenses.sumOf { it.amount }
        val categoryNamesById = categories.associate { it.id to it.name }
        val topCategory = expenses
            .groupBy { it.categoryId }
            .maxByOrNull { (_, txs) -> txs.sumOf { it.amount } }
            ?.let { (categoryId, _) ->
                categoryId?.let { categoryNamesById[it] } ?: "Без категории"
            } ?: "Нет расходов"

        return PeriodStats(income = income, expense = expense, topCategory = topCategory)
    }

    private fun calculateTrend(currentExpense: Double, previousExpense: Double): Double? {
        if (previousExpense <= 0.0) return null
        return ((currentExpense - previousExpense) / previousExpense) * 100
    }

    private fun startOfCurrentWeek(now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val shift = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            add(Calendar.DAY_OF_MONTH, -shift)
        }
        return calendar.timeInMillis
    }

    private fun startOfCurrentMonth(now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private fun evaluateExpression(expression: String): Double {
        return runCatching { ExpressionCalculator.eval(expression) }.getOrDefault(0.0)
    }

    fun formatDate(epoch: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epoch))
}

enum class LimitThreshold {
    NONE,
    WARNING,
    EXCEEDED,
}

data class CategoryLimitProgress(
    val categoryId: Long,
    val categoryName: String,
    val spent: Double,
    val limit: Double,
) {
    val progress: Float = if (limit > 0) (spent / limit).toFloat() else 0f
    val status: String = when {
        spent > limit -> "Превышен"
        spent >= limit * 0.8 -> "Близко к лимиту"
        else -> "В пределах лимита"
    }
}

data class UiState(
    val categories: List<Category> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val invites: List<CollaborationInvite> = emptyList(),
    val limits: List<CategoryLimit> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categorySpend: List<CategorySpendRow> = emptyList(),
    val monthCategorySpend: List<CategoryMonthSpendRow> = emptyList(),
    val limitProgress: List<CategoryLimitProgress> = emptyList(),
    val weeklyReport: BudgetPeriodReport = BudgetPeriodReport(title = "Неделя"),
    val monthlyReport: BudgetPeriodReport = BudgetPeriodReport(title = "Месяц"),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
)

data class BudgetPeriodReport(
    val title: String,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val topExpenseCategory: String = "Нет расходов",
    val expenseTrendPercent: Double? = null,
)

private data class PeriodStats(
    val income: Double,
    val expense: Double,
    val topCategory: String,
)

object ExpressionCalculator {
    fun eval(expr: String): Double {
        val tokens = expr.replace(" ", "")
        if (tokens.isBlank()) return 0.0
        val nums = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var i = 0
        while (i < tokens.length) {
            val ch = tokens[i]
            if (ch.isDigit() || ch == '.') {
                var j = i
                while (j < tokens.length && (tokens[j].isDigit() || tokens[j] == '.')) j++
                nums += tokens.substring(i, j).toDouble()
                i = j
                continue
            }
            if (ch in charArrayOf('+', '-', '*', '/')) {
                while (ops.isNotEmpty() && priority(ops.last()) >= priority(ch)) {
                    applyOp(nums, ops.removeAt(ops.lastIndex))
                }
                ops += ch
            }
            i++
        }
        while (ops.isNotEmpty()) applyOp(nums, ops.removeAt(ops.lastIndex))
        return nums.firstOrNull() ?: 0.0
    }

    private fun priority(op: Char): Int = if (op == '+' || op == '-') 1 else 2

    private fun applyOp(nums: MutableList<Double>, op: Char) {
        if (nums.size < 2) return
        val b = nums.removeAt(nums.lastIndex)
        val a = nums.removeAt(nums.lastIndex)
        val result = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) 0.0 else a / b
            else -> 0.0
        }
        nums += result
    }
}
