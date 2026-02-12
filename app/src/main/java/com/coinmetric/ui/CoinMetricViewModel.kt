package com.coinmetric.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SampleTransaction(
    val title: String,
    val amount: Int,
    val date: String,
    val category: String,
    val income: Boolean,
)

data class DashboardState(
    val isLoading: Boolean = true,
    val balance: Int = 0,
    val income: Int = 0,
    val expense: Int = 0,
    val limitsUsedPercent: Int = 0,
    val avgDailyExpense: Int = 0,
    val expenseTrendText: String = "",
    val expenseTrend: List<Int> = emptyList(),
    val allTransactions: List<SampleTransaction> = emptyList(),
    val latestTransactions: List<String> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
)

data class TransactionUiModel(
    val title: String,
    val amount: Int,
    val date: String,
    val category: String,
    val income: Boolean,
)

data class AddTransactionState(
    val id: String? = null,
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val categories: List<String> = emptyList(),
    val newCategoryName: String = "",
    val isIncome: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
)

data class ActivityLogUiModel(
    val actor: String,
    val action: String,
    val target: String,
    val createdAtLabel: String,
)

data class SettingsState(
    val darkThemeEnabled: Boolean = false,
    val googleSyncEnabled: Boolean = true,
    val showOnboarding: Boolean = true,
    val isOfflineMode: Boolean = false,
    val pendingSyncItems: Int = 0,
    val isSyncInProgress: Boolean = false,
    val syncError: String? = null,
    val lastSyncTimeLabel: String? = null,
    val inviteEmail: String = "",
    val inviteRole: String = "editor",
    val inviteError: String? = null,
    val inviteSuccessMessage: String? = null,
    val pendingInvites: List<FamilyInviteUiModel> = emptyList(),
    val currentUserRole: String = "owner",
    val activityLog: List<ActivityLogUiModel> = emptyList(),
)

data class FamilyInviteUiModel(
    val email: String,
    val role: String,
    val status: String,
)

class CoinMetricViewModel : ViewModel() {
    private val transactions = mutableListOf(
        SampleTransaction("Продукты", -1800, "2023-10-27", "Еда", false),
        SampleTransaction("Кафе", -560, "2023-10-26", "Досуг", false),
        SampleTransaction("Зарплата", 85000, "2023-10-25", "Доход", true),
    )

    private val categories = mutableListOf("Еда", "Транспорт", "Досуг", "Коммунальные", "Доход")

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _addState = MutableStateFlow(AddTransactionState())
    val addState: StateFlow<AddTransactionState> = _addState.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    init {
        syncCategoriesWithTransactions()
        _addState.value = _addState.value.copy(categories = categories.toList())
        viewModelScope.launch {
            delay(800)
            _dashboard.value = buildDashboardState(isLoading = false)
            _settings.value = _settings.value.copy(
                activityLog = listOf(
                    ActivityLogUiModel("Вы", "Добавили операцию", "Продукты: -1800₽", "Сегодня, 14:20"),
                    ActivityLogUiModel("Анна (Editor)", "Изменила лимит", "Еда: 15 000₽", "Вчера, 18:45")
                )
            )
        }
    }

    fun updateAmount(value: String) {
        _addState.value = _addState.value.copy(
            amount = value,
            amountError = null,
            error = null,
            successMessage = null,
        )
    }

    fun updateCategory(value: String) {
        _addState.value = _addState.value.copy(
            category = value,
            categoryError = null,
            error = null,
            successMessage = null,
        )
    }

    fun updateNewCategoryName(value: String) {
        _addState.value = _addState.value.copy(newCategoryName = value, error = null)
    }

    fun addNewCategory() {
        val state = _addState.value
        val categoryName = state.newCategoryName.trim()
        if (categoryName.isBlank()) {
            _addState.value = state.copy(error = "Введите название категории")
            return
        }
        val exists = categories.any { it.equals(categoryName, ignoreCase = true) }
        if (exists) {
            _addState.value = state.copy(error = "Такая категория уже существует")
            return
        }

        categories.add(categoryName)
        categories.sortBy { it.lowercase(Locale.getDefault()) }
        _addState.value = state.copy(
            categories = categories.toList(),
            category = categoryName,
            newCategoryName = "",
            error = null,
        )
    }

    fun updateNote(value: String) {
        _addState.value = _addState.value.copy(note = value)
    }

    fun updateIncomeFlag(isIncome: Boolean) {
        _addState.value = _addState.value.copy(isIncome = isIncome)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _addState.value
        val amountValue = state.amount.replace(",", ".").toDoubleOrNull()?.toInt()
        val amountError = if (amountValue == null || amountValue <= 0) "Введите корректную сумму" else null
        val categoryError = if (state.category.isBlank()) "Укажите категорию" else null
        
        if (amountError != null || categoryError != null) {
            _addState.value = state.copy(
                amountError = amountError,
                categoryError = categoryError,
                error = "Проверьте обязательные поля",
                successMessage = null,
            )
            return
        }

        val validAmount = requireNotNull(amountValue)
        val signedAmount = if (state.isIncome) validAmount else -validAmount
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        if (state.id != null) {
            val index = transactions.indexOfFirst { it.hashCode().toString() == state.id }
            if (index != -1) {
                transactions[index] = SampleTransaction(
                    title = state.note.ifBlank { if (state.isIncome) "Доход" else "Расход" },
                    amount = signedAmount,
                    date = transactions[index].date,
                    category = state.category,
                    income = state.isIncome,
                )
            }
        } else {
            transactions.add(0, SampleTransaction(
                title = state.note.ifBlank { if (state.isIncome) "Доход" else "Расход" },
                amount = signedAmount,
                date = currentDate,
                category = state.category,
                income = state.isIncome,
            ))
        }

        if (categories.none { it.equals(state.category, ignoreCase = true) }) {
            categories.add(state.category)
            categories.sortBy { it.lowercase(Locale.getDefault()) }
        }

        _dashboard.value = buildDashboardState(isLoading = false)
        enqueueSyncChanges(1)
        _addState.value = AddTransactionState(
            categories = categories.toList(),
            successMessage = "Операция сохранена",
        )
        onSuccess()
    }
    
    fun startEditingTransaction(transaction: SampleTransaction) {
        val id = transaction.hashCode().toString()
        _addState.value = AddTransactionState(
            id = id,
            amount = kotlin.math.abs(transaction.amount).toString(),
            categories = categories.toList(),
            category = transaction.category,
            note = transaction.title,
            isIncome = transaction.income,
        )
    }

    fun deleteTransaction(transaction: SampleTransaction) {
        val index = transactions.indexOf(transaction)
        if (index == -1) return
        transactions.removeAt(index)
        _dashboard.value = buildDashboardState(isLoading = false)
        enqueueSyncChanges(1)
    }
    
    fun resetAddState() {
        _addState.value = AddTransactionState(categories = categories.toList())
    }

    fun setDarkTheme(enabled: Boolean) {
        _settings.value = _settings.value.copy(darkThemeEnabled = enabled)
    }

    fun setGoogleSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(googleSyncEnabled = enabled)
        if (enabled) processSyncQueue()
    }

    fun setOfflineMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isOfflineMode = enabled,
            syncError = if (enabled) "Автономный режим активен" else null,
        )
        if (!enabled) processSyncQueue()
    }

    fun retrySync() {
        processSyncQueue()
    }

    fun dismissOnboarding() {
        _settings.value = _settings.value.copy(showOnboarding = false)
    }

    fun setOnboardingVisible(enabled: Boolean) {
        _settings.value = _settings.value.copy(showOnboarding = enabled)
    }

    fun updateInviteEmail(value: String) {
        _settings.value = _settings.value.copy(inviteEmail = value, inviteError = null)
    }

    fun updateInviteRole(role: String) {
        _settings.value = _settings.value.copy(inviteRole = role)
    }

    fun sendFamilyInvite() {
        val current = _settings.value
        val email = current.inviteEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _settings.value = current.copy(inviteError = "Некорректный email")
            return
        }

        _settings.value = current.copy(
            inviteEmail = "",
            inviteSuccessMessage = "Приглашение отправлено $email",
            pendingInvites = listOf(FamilyInviteUiModel(email, current.inviteRole, "Ожидает")) + current.pendingInvites
        )
    }

    fun updateInviteStatus(email: String, newStatus: String) {
        val updated = _settings.value.pendingInvites.map {
            if (it.email == email) it.copy(status = newStatus) else it
        }
        _settings.value = _settings.value.copy(pendingInvites = updated)
    }

    fun setCurrentUserRole(role: String) {
        _settings.value = _settings.value.copy(currentUserRole = role)
    }

    private fun enqueueSyncChanges(itemsCount: Int) {
        _settings.value = _settings.value.copy(
            pendingSyncItems = _settings.value.pendingSyncItems + itemsCount
        )
        processSyncQueue()
    }

    private fun processSyncQueue() {
        val current = _settings.value
        if (!current.googleSyncEnabled || current.pendingSyncItems == 0 || current.isSyncInProgress || current.isOfflineMode) return

        viewModelScope.launch {
            _settings.value = _settings.value.copy(isSyncInProgress = true)
            delay(1000)
            _settings.value = _settings.value.copy(
                isSyncInProgress = false,
                pendingSyncItems = 0,
                lastSyncTimeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        }
    }

    private fun buildDashboardState(isLoading: Boolean = true): DashboardState {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        
        val avgDaily = if (transactions.any { it.amount < 0 }) totalExpense / 30 else 0
        val limitPercent = if (totalExpense > 0) (totalExpense.toFloat() / 100000 * 100).toInt().coerceIn(0, 100) else 0

        val trend = listOf(1200, 4500, 3200, 8000, 5600, 9100, 7200)

        return DashboardState(
            isLoading = isLoading,
            balance = totalIncome - totalExpense,
            income = totalIncome,
            expense = totalExpense,
            limitsUsedPercent = limitPercent,
            avgDailyExpense = avgDaily,
            expenseTrendText = if (totalExpense > 0) "-5% к прошлому периоду" else "Нет данных",
            expenseTrend = trend,
            allTransactions = transactions.toList(),
            recentTransactions = transactions.map { tx ->
                TransactionUiModel(tx.title, tx.amount, tx.date, tx.category, tx.income)
            },
            latestTransactions = transactions.take(10).map { tx ->
                val sign = if (tx.amount >= 0) "+" else "-"
                "$sign${kotlin.math.abs(tx.amount).toRubCurrency()} · ${tx.title} · ${tx.date}"
            },
        )
    }

    private fun syncCategoriesWithTransactions() {
        val txCategories = transactions.map { it.category }.distinct()
        txCategories.forEach { categoryName ->
            if (categories.none { it.equals(categoryName, ignoreCase = true) }) {
                categories.add(categoryName)
            }
        }
        categories.sortBy { it.lowercase(Locale.getDefault()) }
    }
}
