package com.coinmetric.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private data class SampleTransaction(
    val title: String,
    val amount: Int,
    val date: String,
    val category: String,
    val income: Boolean,
)

data class DashboardState(
    val balance: Int = 125_600,
    val income: Int = 178_500,
    val expense: Int = 52_900,
    val limitsUsedPercent: Int = 67,
    val avgDailyExpense: Int = 2_940,
    val expenseTrendText: String = "-12% к прошлой неделе",
    val latestTransactions: List<String> = emptyList(),
)

data class AddTransactionState(
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val isIncome: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

data class SettingsState(
    val darkThemeEnabled: Boolean = false,
    val googleSyncEnabled: Boolean = true,
)

class CoinMetricViewModel : ViewModel() {
    private val transactions = mutableListOf(
        SampleTransaction("Продукты", -1800, "Сегодня", "Еда", false),
        SampleTransaction("Кафе", -560, "Вчера", "Досуг", false),
        SampleTransaction("Зарплата", 85000, "2 дня назад", "Доход", true),
    )

    private val _dashboard = MutableStateFlow(buildDashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _addState = MutableStateFlow(AddTransactionState())
    val addState: StateFlow<AddTransactionState> = _addState.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    fun updateAmount(value: String) {
        _addState.value = _addState.value.copy(amount = value, error = null, successMessage = null)
    }

    fun updateCategory(value: String) {
        _addState.value = _addState.value.copy(category = value, error = null, successMessage = null)
    }

    fun updateNote(value: String) {
        _addState.value = _addState.value.copy(note = value)
    }

    fun updateIncomeFlag(isIncome: Boolean) {
        _addState.value = _addState.value.copy(isIncome = isIncome)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _addState.value
        val amountValue = state.amount.toIntOrNull()
        if (amountValue == null || amountValue <= 0 || state.category.isBlank()) {
            _addState.value = state.copy(error = "Заполните сумму и категорию", successMessage = null)
            return
        }

        val signedAmount = if (state.isIncome) amountValue else -amountValue
        transactions.add(
            0,
            SampleTransaction(
                title = state.note.ifBlank { if (state.isIncome) "Доход" else "Расход" },
                amount = signedAmount,
                date = "Сегодня",
                category = state.category,
                income = state.isIncome,
            ),
        )

        _dashboard.value = buildDashboardState()
        _addState.value = AddTransactionState(successMessage = "Операция сохранена")
        onSuccess()
    }

    fun setDarkTheme(enabled: Boolean) {
        _settings.value = _settings.value.copy(darkThemeEnabled = enabled)
    }

    fun setGoogleSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(googleSyncEnabled = enabled)
    }

    private fun buildDashboardState(): DashboardState {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        return DashboardState(
            balance = totalIncome - totalExpense,
            income = totalIncome,
            expense = totalExpense,
            latestTransactions = transactions.take(5).map { tx ->
                val sign = if (tx.amount >= 0) "+" else "-"
                "$sign${kotlin.math.abs(tx.amount)} ₽ · ${tx.title} · ${tx.date}"
            },
        )
    }
}
