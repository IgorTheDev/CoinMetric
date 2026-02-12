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

private data class SampleTransaction(
    val title: String,
    val amount: Int,
    val date: String,
    val category: String,
    val income: Boolean,
)

data class DashboardState(
    val isLoading: Boolean = true,
    val balance: Int = 125_600,
    val income: Int = 178_500,
    val expense: Int = 52_900,
    val limitsUsedPercent: Int = 67,
    val avgDailyExpense: Int = 2_940,
    val expenseTrendText: String = "-12% к прошлой неделе",
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
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val isIncome: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
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
)

data class FamilyInviteUiModel(
    val email: String,
    val role: String,
    val status: String,
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

    init {
        viewModelScope.launch {
            delay(500)
            _dashboard.value = buildDashboardState(isLoading = false)
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

    fun updateNote(value: String) {
        _addState.value = _addState.value.copy(note = value)
    }

    fun updateIncomeFlag(isIncome: Boolean) {
        _addState.value = _addState.value.copy(isIncome = isIncome)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _addState.value
        val amountValue = state.amount.toIntOrNull()
        val amountError = if (amountValue == null || amountValue <= 0) "Введите сумму больше 0" else null
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

        _dashboard.value = buildDashboardState(isLoading = false)
        enqueueSyncChanges(1)
        _addState.value = AddTransactionState(successMessage = "Операция сохранена")
        onSuccess()
    }

    fun setDarkTheme(enabled: Boolean) {
        _settings.value = _settings.value.copy(darkThemeEnabled = enabled)
    }

    fun setGoogleSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            googleSyncEnabled = enabled,
            syncError = if (enabled) _settings.value.syncError else null,
        )
        if (enabled) {
            processSyncQueue()
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isOfflineMode = enabled,
            syncError = if (enabled) "Нет подключения. Данные сохранены локально и ждут синхронизации." else null,
        )
        if (!enabled) {
            processSyncQueue()
        }
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
        _settings.value = _settings.value.copy(
            inviteEmail = value,
            inviteError = null,
            inviteSuccessMessage = null,
        )
    }

    fun updateInviteRole(role: String) {
        _settings.value = _settings.value.copy(
            inviteRole = role,
            inviteError = null,
            inviteSuccessMessage = null,
        )
    }

    fun sendFamilyInvite() {
        val current = _settings.value
        val email = current.inviteEmail.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

        if (email.isBlank() || !email.matches(emailRegex)) {
            _settings.value = current.copy(
                inviteError = "Введите корректный email участника",
                inviteSuccessMessage = null,
            )
            return
        }

        if (current.pendingInvites.any { it.email.equals(email, ignoreCase = true) }) {
            _settings.value = current.copy(
                inviteError = "Приглашение для этого email уже отправлено",
                inviteSuccessMessage = null,
            )
            return
        }

        _settings.value = current.copy(
            inviteEmail = "",
            inviteError = null,
            inviteSuccessMessage = "Приглашение отправлено: $email",
            pendingInvites = listOf(
                FamilyInviteUiModel(
                    email = email,
                    role = current.inviteRole,
                    status = "Ожидает принятия",
                ),
            ) + current.pendingInvites,
        )
        enqueueSyncChanges(1)
    }

    fun updateInviteStatus(email: String, newStatus: String) {
        val current = _settings.value
        val currentInvite = current.pendingInvites.firstOrNull { it.email.equals(email, ignoreCase = true) }

        if (currentInvite == null) {
            _settings.value = current.copy(
                inviteError = "Приглашение не найдено",
                inviteSuccessMessage = null,
            )
            return
        }

        if (currentInvite.status != "Ожидает принятия") {
            _settings.value = current.copy(
                inviteError = "Статус уже обновлён",
                inviteSuccessMessage = null,
            )
            return
        }

        val updatedInvites = current.pendingInvites.map { invite ->
            if (invite.email.equals(email, ignoreCase = true)) {
                invite.copy(status = newStatus)
            } else {
                invite
            }
        }

        _settings.value = current.copy(
            pendingInvites = updatedInvites,
            inviteError = null,
            inviteSuccessMessage = "Статус приглашения обновлён: $newStatus",
        )
        enqueueSyncChanges(1)
    }

    private fun enqueueSyncChanges(itemsCount: Int) {
        val current = _settings.value
        _settings.value = current.copy(
            pendingSyncItems = current.pendingSyncItems + itemsCount,
            syncError = if (current.googleSyncEnabled && current.isOfflineMode) {
                "Нет подключения. Данные сохранены локально и ждут синхронизации."
            } else {
                current.syncError
            },
        )
        processSyncQueue()
    }

    private fun processSyncQueue() {
        val current = _settings.value
        if (!current.googleSyncEnabled || current.pendingSyncItems == 0 || current.isSyncInProgress) {
            return
        }
        if (current.isOfflineMode) {
            _settings.value = current.copy(
                syncError = "Нет подключения. Данные сохранены локально и ждут синхронизации.",
            )
            return
        }

        viewModelScope.launch {
            _settings.value = _settings.value.copy(isSyncInProgress = true, syncError = null)
            delay(450)
            _settings.value = _settings.value.copy(
                isSyncInProgress = false,
                pendingSyncItems = 0,
                lastSyncTimeLabel = formatSyncTime(System.currentTimeMillis()),
                syncError = null,
            )
        }
    }

    private fun formatSyncTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru", "RU"))
        return formatter.format(Date(epochMillis))
    }

    private fun buildDashboardState(isLoading: Boolean = true): DashboardState {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        return DashboardState(
            isLoading = isLoading,
            balance = totalIncome - totalExpense,
            income = totalIncome,
            expense = totalExpense,
            recentTransactions = transactions.take(5).map { tx ->
                TransactionUiModel(
                    title = tx.title,
                    amount = tx.amount,
                    date = tx.date,
                    category = tx.category,
                    income = tx.income,
                )
            },
            latestTransactions = transactions.take(5).map { tx ->
                val sign = if (tx.amount >= 0) "+" else "-"
                "$sign${kotlin.math.abs(tx.amount).toRubCurrency()} · ${tx.title} · ${tx.date}"
            },
        )
    }
}
