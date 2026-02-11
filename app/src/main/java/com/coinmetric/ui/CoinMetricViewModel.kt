package com.coinmetric.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.Date
import java.util.Locale

class CoinMetricViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = BudgetRepository(CoinMetricDatabase.get(app).dao())
    private val notificationHelper = LimitNotificationHelper(app)

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
            repository.income(),
            repository.expenses(),
        ) { transactions, spend, income, expense ->
            FinanceState(transactions, spend, income, expense)
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
        if (spent > limit.monthlyLimit) {
            val categoryName = state.value.categories.firstOrNull { it.id == categoryId }?.name ?: "Категория"
            notificationHelper.notifyLimitExceeded(categoryName, spent, limit.monthlyLimit)
        }
    }

    private fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private fun evaluateExpression(expression: String): Double {
        return runCatching { ExpressionCalculator.eval(expression) }.getOrDefault(0.0)
    }

    fun formatDate(epoch: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epoch))
}

data class UiState(
    val categories: List<Category> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val invites: List<CollaborationInvite> = emptyList(),
    val limits: List<CategoryLimit> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categorySpend: List<CategorySpendRow> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
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
