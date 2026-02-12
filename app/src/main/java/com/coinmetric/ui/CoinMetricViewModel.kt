package com.coinmetric.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coinmetric.CoinMetricApp
import com.coinmetric.sync.FirestoreSyncService
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
    val isRecurring: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
)

data class CategoriesState(
    val categories: List<String> = emptyList(),
    val monthlyLimits: Map<String, Int> = emptyMap(),
    val selectedCategory: String = "",
    val monthlyLimitInput: String = "",
    val newCategoryName: String = "",
    val error: String? = null,
    val successMessage: String? = null,
)

data class ActivityLogUiModel(
    val actor: String,
    val action: String,
    val target: String,
    val createdAtLabel: String,
)

data class LimitAlertUiModel(
    val category: String,
    val spent: Double,
    val limit: Double,
    val isExceeded: Boolean,
)

data class SettingsState(
    val darkThemeEnabled: Boolean = false,
    val recurringRemindersEnabled: Boolean = true,
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
    val currentUserEmail: String = "",
    val activityLog: List<ActivityLogUiModel> = emptyList(),
    val subscriptionPlan: String = "free",
    val pinProtectionEnabled: Boolean = false,
    val biometricProtectionEnabled: Boolean = false,
    val securitySetupCompleted: Boolean = false,
    val googleSyncEnabled: Boolean = false,
)

data class FamilyInviteUiModel(
    val email: String,
    val role: String,
    val status: String,
)

class CoinMetricViewModel : ViewModel() {
    private val allowedRoles = setOf("owner", "editor", "viewer")
    private val allowedInviteRoles = setOf("editor", "viewer")
    private val allowedSubscriptionPlans = setOf("free", "pro")
    private val allowedInviteStatuses = setOf("Ожидает принятия", "Принято", "Отклонено")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    private var syncService: FirestoreSyncService
        get() = CoinMetricApp.syncService
        private set(value) { /* no-op */ }

    private var syncNotificationHelper: SyncNotificationHelper
        get() = CoinMetricApp.syncNotificationHelper
        private set(value) { /* no-op */ }

    private val transactions = mutableListOf(
        SampleTransaction("Продукты", -1800, "2023-10-27", "Еда", false),
        SampleTransaction("Кафе", -560, "2023-10-26", "Досуг", false),
        SampleTransaction("Зарплата", 85000, "2023-10-25", "Доход", true),
    )

    private val categories = mutableListOf("Еда", "Транспорт", "Досуг", "Коммунальные", "Доход")
    private val categoryMonthlyLimits = mutableMapOf(
        "Еда" to 15_000,
        "Транспорт" to 6_000,
        "Развлечения" to 7_500,
        "Досуг" to 5_000,
    )

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _addState = MutableStateFlow(AddTransactionState())
    val addState: StateFlow<AddTransactionState> = _addState.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _categoriesState = MutableStateFlow(CategoriesState())
    val categoriesState: StateFlow<CategoriesState> = _categoriesState.asStateFlow()

    private val _limitAlertEvent = MutableStateFlow<LimitAlertUiModel?>(null)
    val limitAlertEvent: StateFlow<LimitAlertUiModel?> = _limitAlertEvent.asStateFlow()

    init {
        syncCategoriesWithTransactions()
        _addState.value = _addState.value.copy(categories = categories.toList())
        val firstCategory = categories.firstOrNull().orEmpty()
        _categoriesState.value = CategoriesState(
            categories = categories.toList(),
            monthlyLimits = categoryMonthlyLimits.toMap(),
            selectedCategory = firstCategory,
            monthlyLimitInput = categoryMonthlyLimits[firstCategory]?.toString().orEmpty(),
        )
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

    fun initializeSyncService(context: Context) {
        // Service is now initialized in the application class
        // This method kept for compatibility but does nothing
    }

    fun updateCurrentUserEmail(email: String) {
        _settings.value = _settings.value.copy(currentUserEmail = email)
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
        val state = _categoriesState.value
        if (_settings.value.currentUserRole == "viewer") {
            _categoriesState.value = state.copy(error = "Роль просмотра не позволяет добавлять категории", successMessage = null)
            return
        }
        val categoryName = state.newCategoryName.trim()
        if (categoryName.isBlank()) {
            _categoriesState.value = state.copy(error = "Введите название категории", successMessage = null)
            return
        }
        val exists = categories.any { it.equals(categoryName, ignoreCase = true) }
        if (exists) {
            _categoriesState.value = state.copy(error = "Такая категория уже существует", successMessage = null)
            return
        }

        categories.add(categoryName)
        categories.sortBy { it.lowercase(Locale.getDefault()) }
        _addState.value = _addState.value.copy(
            categories = categories.toList(),
        )
        _categoriesState.value = state.copy(
            categories = categories.toList(),
            selectedCategory = categoryName,
            monthlyLimitInput = "",
            newCategoryName = "",
            error = null,
            successMessage = "Категория добавлена",
        )
        appendActivityLog(
            action = "Создана категория",
            target = categoryName,
        )
    }

    fun updateCategoriesNewCategoryName(value: String) {
        _categoriesState.value = _categoriesState.value.copy(newCategoryName = value, error = null, successMessage = null)
    }

    fun updateSelectedLimitCategory(value: String) {
        _categoriesState.value = _categoriesState.value.copy(
            selectedCategory = value,
            monthlyLimitInput = categoryMonthlyLimits[value]?.toString().orEmpty(),
            error = null,
            successMessage = null,
        )
    }

    fun updateMonthlyLimitInput(value: String) {
        val sanitized = value.filter { it.isDigit() }
        _categoriesState.value = _categoriesState.value.copy(monthlyLimitInput = sanitized, error = null, successMessage = null)
    }

    fun saveMonthlyLimit() {
        val state = _categoriesState.value
        if (_settings.value.currentUserRole == "viewer") {
            _categoriesState.value = state.copy(error = "Роль просмотра не позволяет изменять лимиты", successMessage = null)
            return
        }
        if (state.selectedCategory.isBlank()) {
            _categoriesState.value = state.copy(error = "Выберите категорию", successMessage = null)
            return
        }

        val limit = state.monthlyLimitInput.toIntOrNull()
        if (limit == null || limit <= 0) {
            _categoriesState.value = state.copy(error = "Введите корректный лимит", successMessage = null)
            return
        }

        categoryMonthlyLimits[state.selectedCategory] = limit
        _categoriesState.value = state.copy(
            monthlyLimits = categoryMonthlyLimits.toMap(),
            error = null,
            successMessage = "Лимит на месяц сохранён",
        )
        appendActivityLog(
            action = "Обновление лимита",
            target = "${state.selectedCategory}: ${limit.toRubCurrency()}",
        )
        evaluateLimitAlert(state.selectedCategory)
    }

    fun updateNote(value: String) {
        _addState.value = _addState.value.copy(note = value)
    }

    fun updateIncomeFlag(isIncome: Boolean) {
        _addState.value = _addState.value.copy(isIncome = isIncome)
    }

    fun updateRecurringFlag(isRecurring: Boolean) {
        _addState.value = _addState.value.copy(isRecurring = isRecurring)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _addState.value
        if (_settings.value.currentUserRole == "viewer") {
            _addState.value = state.copy(
                error = "Роль просмотра не позволяет добавлять или редактировать операции",
                successMessage = null,
            )
            return
        }
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
        
        val action = if (state.id != null) "Редактирование операции" else "Создание операции"

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
            _categoriesState.value = _categoriesState.value.copy(
                categories = categories.toList(),
            )
        }

        _dashboard.value = buildDashboardState(isLoading = false)
        evaluateLimitAlert(state.category)
        enqueueSyncChanges(1)
        appendActivityLog(
            action = action,
            target = "${state.category}: ${signedAmount.toRubCurrency()}",
        )
        if (state.isRecurring) {
            appendActivityLog(
                action = "Добавлен постоянный платёж",
                target = "${state.category}: ${signedAmount.toRubCurrency()}",
            )
        }
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
        if (_settings.value.currentUserRole == "viewer") {
            _addState.value = _addState.value.copy(
                error = "Роль просмотра не позволяет удалять операции",
                successMessage = null,
            )
            return
        }
        val index = transactions.indexOf(transaction)
        if (index == -1) return
        transactions.removeAt(index)
        _dashboard.value = buildDashboardState(isLoading = false)
        evaluateLimitAlert(transaction.category)
        enqueueSyncChanges(1)
        appendActivityLog(
            action = "Удаление операции",
            target = "${transaction.category}: ${transaction.amount.toRubCurrency()}",
        )
    }
    
    fun resetAddState() {
        _addState.value = AddTransactionState(categories = categories.toList())
    }

    fun setDarkTheme(enabled: Boolean) {
        if (_settings.value.darkThemeEnabled == enabled) return
        _settings.value = _settings.value.copy(darkThemeEnabled = enabled)
        appendActivityLog(
            action = "Тема приложения",
            target = if (enabled) "Тёмная" else "Светлая",
        )
    }



    fun setRecurringReminders(enabled: Boolean) {
        if (_settings.value.recurringRemindersEnabled == enabled) return
        _settings.value = _settings.value.copy(recurringRemindersEnabled = enabled)
        appendActivityLog(
            action = "Напоминания о платежах",
            target = if (enabled) "Включены" else "Отключены",
        )
    }

    fun setSubscriptionPlan(plan: String) {
        val normalizedPlan = plan.trim().lowercase(Locale.getDefault())
        if (!allowedSubscriptionPlans.contains(normalizedPlan)) return
        if (_settings.value.subscriptionPlan == normalizedPlan) return
        _settings.value = _settings.value.copy(subscriptionPlan = normalizedPlan)
        appendActivityLog(
            action = "План подписки",
            target = when (normalizedPlan) {
                "pro" -> "CoinMetric Pro"
                else -> "CoinMetric Free"
            },
        )
    }

    fun setGoogleSync(enabled: Boolean) {
        _settings.value = _settings.value.copy(googleSyncEnabled = enabled)
        if (!enabled) {
            _settings.value = _settings.value.copy(syncError = "Синхронизация отключена пользователем")
        }
        appendActivityLog(
            action = "Google Sync",
            target = if (enabled) "Включена" else "Отключена",
        )
    }

    fun setPinProtectionEnabled(enabled: Boolean) {
        if (_settings.value.pinProtectionEnabled == enabled) return
        _settings.value = _settings.value.copy(pinProtectionEnabled = enabled, syncError = null)
        appendActivityLog(
            action = "PIN-защита",
            target = if (enabled) "Включена" else "Отключена",
        )
        if (!enabled && _settings.value.biometricProtectionEnabled) {
            _settings.value = _settings.value.copy(biometricProtectionEnabled = false)
            appendActivityLog(
                action = "Биометрия",
                target = "Отключена",
            )
        }
    }

    fun setBiometricProtectionEnabled(enabled: Boolean) {
        val current = _settings.value
        if (enabled && !current.pinProtectionEnabled) {
            _settings.value = current.copy(syncError = "Сначала включите PIN-защиту")
            return
        }
        if (current.biometricProtectionEnabled == enabled) return
        _settings.value = current.copy(biometricProtectionEnabled = enabled, syncError = null)
        appendActivityLog(
            action = "Биометрия",
            target = if (enabled) "Включена" else "Отключена",
        )
    }

    fun completeSecuritySetup() {
        if (!_settings.value.pinProtectionEnabled) {
            _settings.value = _settings.value.copy(syncError = "Для завершения включите PIN-защиту")
            return
        }
        if (_settings.value.securitySetupCompleted) return
        _settings.value = _settings.value.copy(securitySetupCompleted = true)
        appendActivityLog(
            action = "Мастер безопасности",
            target = "Первичная настройка завершена",
        )
    }

    fun consumeLimitAlertEvent() {
        _limitAlertEvent.value = null
    }

    fun setOfflineMode(enabled: Boolean) {
        if (_settings.value.isOfflineMode == enabled) return
        _settings.value = _settings.value.copy(
            isOfflineMode = enabled,
            syncError = if (enabled) "Автономный режим активен" else null,
        )
        appendActivityLog(
            action = "Автономный режим",
            target = if (enabled) "Включен" else "Отключен",
        )
        if (!enabled) processSyncQueue()
    }

    fun retrySync() {
        val current = _settings.value
        if (current.isOfflineMode) {
            _settings.value = current.copy(syncError = "Отключите автономный режим для синхронизации")
            return
        }
        processSyncQueue()
    }

    fun dismissOnboarding() {
        if (!_settings.value.showOnboarding) return
        _settings.value = _settings.value.copy(showOnboarding = false)
        appendActivityLog(
            action = "Onboarding",
            target = "Подсказки скрыты",
        )
    }

    fun setOnboardingVisible(enabled: Boolean) {
        if (_settings.value.showOnboarding == enabled) return
        _settings.value = _settings.value.copy(showOnboarding = enabled)
        appendActivityLog(
            action = "Onboarding",
            target = if (enabled) "Подсказки включены" else "Подсказки отключены",
        )
    }

    fun updateInviteEmail(value: String) {
        _settings.value = _settings.value.copy(
            inviteEmail = value,
            inviteError = null,
            inviteSuccessMessage = null,
        )
    }

    fun updateInviteRole(role: String) {
        val normalizedRole = role.trim().lowercase(Locale.getDefault())
        _settings.value = _settings.value.copy(
            inviteRole = if (allowedInviteRoles.contains(normalizedRole)) normalizedRole else "editor",
            inviteError = if (allowedInviteRoles.contains(normalizedRole)) null else "Некорректная роль приглашения",
            inviteSuccessMessage = null,
        )
    }

    fun clearInviteFeedback() {
        _settings.value = _settings.value.copy(inviteError = null, inviteSuccessMessage = null)
    }

    fun sendFamilyInvite() {
        val current = _settings.value
        if (current.currentUserRole != "owner") {
            _settings.value = current.copy(inviteError = "Только владелец может отправлять приглашения")
            appendActivityLog(
                action = "Отказ в отправке приглашения",
                target = mapRoleLabel(current.currentUserRole),
            )
            return
        }
        val email = current.inviteEmail.trim().lowercase(Locale.getDefault())
        if (email.isBlank() || !emailRegex.matches(email)) {
            _settings.value = current.copy(inviteError = "Некорректный email")
            return
        }
        if (!allowedInviteRoles.contains(current.inviteRole)) {
            _settings.value = current.copy(inviteError = "Некорректная роль приглашения")
            return
        }
        val duplicateInvite = current.pendingInvites.any {
            it.email.equals(email, ignoreCase = true) && it.status == "Ожидает принятия"
        }
        if (duplicateInvite) {
            _settings.value = current.copy(inviteError = "Для этого email уже есть активное приглашение")
            return
        }

        _settings.value = current.copy(
            inviteEmail = "",
            inviteSuccessMessage = "Приглашение отправлено $email",
            pendingInvites = listOf(FamilyInviteUiModel(email, current.inviteRole, "Ожидает принятия")) + current.pendingInvites
        )
        appendActivityLog(
            action = "Отправка приглашения",
            target = "$email (${current.inviteRole})",
        )
    }

    fun revokeFamilyInvite(email: String) {
        val normalizedEmail = email.trim().lowercase(Locale.getDefault())
        val current = _settings.value
        if (current.currentUserRole != "owner") {
            _settings.value = current.copy(inviteError = "Только владелец может отзывать приглашения")
            appendActivityLog(
                action = "Отказ в отзыве приглашения",
                target = mapRoleLabel(current.currentUserRole),
            )
            return
        }
        val invite = current.pendingInvites.firstOrNull { it.email.equals(normalizedEmail, ignoreCase = true) }
        if (invite == null) {
            _settings.value = current.copy(inviteError = "Приглашение не найдено")
            return
        }
        if (invite.status != "Ожидает принятия") {
            _settings.value = current.copy(inviteError = "Можно отозвать только ожидающее приглашение")
            return
        }
        val updated = current.pendingInvites.filterNot { it.email.equals(normalizedEmail, ignoreCase = true) }
        _settings.value = current.copy(
            pendingInvites = updated,
            inviteError = null,
            inviteSuccessMessage = "Приглашение для $normalizedEmail отозвано",
        )
        appendActivityLog(
            action = "Отзыв приглашения",
            target = normalizedEmail,
        )
    }

    fun updateInviteStatus(email: String, newStatus: String) {
        val normalizedEmail = email.trim().lowercase(Locale.getDefault())
        val current = _settings.value
        if (current.currentUserRole == "viewer") {
            _settings.value = current.copy(inviteError = "Роль просмотра не позволяет менять статус приглашений")
            appendActivityLog(
                action = "Отказ в изменении статуса приглашения",
                target = mapRoleLabel(current.currentUserRole),
            )
            return
        }
        if (!allowedInviteStatuses.contains(newStatus) || newStatus == "Ожидает принятия") {
            _settings.value = _settings.value.copy(inviteError = "Некорректный статус приглашения")
            return
        }
        val invite = current.pendingInvites.firstOrNull { it.email.equals(normalizedEmail, ignoreCase = true) }
        if (invite == null) {
            _settings.value = current.copy(inviteError = "Приглашение не найдено")
            return
        }
        if (invite.status != "Ожидает принятия") {
            _settings.value = current.copy(inviteError = "Статус уже обновлён")
            return
        }

        val updated = current.pendingInvites.map {
            if (it.email.equals(normalizedEmail, ignoreCase = true)) it.copy(status = newStatus) else it
        }
        _settings.value = current.copy(
            pendingInvites = updated,
            inviteError = null,
            inviteSuccessMessage = "Статус приглашения обновлён",
        )
        appendActivityLog(
            action = "Обновление статуса приглашения",
            target = "$normalizedEmail → $newStatus",
        )
    }

    fun setCurrentUserRole(role: String) {
        val normalizedRole = role.trim().lowercase(Locale.getDefault())
        if (!allowedRoles.contains(normalizedRole)) {
            _settings.value = _settings.value.copy(inviteError = "Некорректная роль пользователя")
            return
        }
        if (_settings.value.currentUserRole == normalizedRole) return
        _settings.value = _settings.value.copy(currentUserRole = normalizedRole, inviteError = null)
        appendActivityLog(
            action = "Смена роли",
            target = mapRoleLabel(normalizedRole),
        )
    }

    private val limitAlertStateByCategory = mutableMapOf<String, String>()

    private fun evaluateLimitAlert(category: String) {
        val trimmedCategory = category.trim()
        if (trimmedCategory.isBlank()) return
        val limit = categoryMonthlyLimits[trimmedCategory]?.toDouble() ?: return
        if (limit <= 0.0) return
        val spent = transactions
            .filter { !it.income && it.category.equals(trimmedCategory, ignoreCase = true) }
            .sumOf { kotlin.math.abs(it.amount) }
            .toDouble()
        val ratio = if (limit == 0.0) 0.0 else spent / limit
        val previousState = limitAlertStateByCategory[trimmedCategory]
        val newState = when {
            ratio >= 1.0 -> "exceeded"
            ratio >= 0.8 -> "almost"
            else -> "normal"
        }
        if (newState != previousState && newState != "normal") {
            _limitAlertEvent.value = LimitAlertUiModel(
                category = trimmedCategory,
                spent = spent,
                limit = limit,
                isExceeded = newState == "exceeded",
            )
        }
        limitAlertStateByCategory[trimmedCategory] = newState
    }

    private fun enqueueSyncChanges(itemsCount: Int) {
        _settings.value = _settings.value.copy(
            pendingSyncItems = _settings.value.pendingSyncItems + itemsCount
        )
        processSyncQueue()
    }

    private fun processSyncQueue() {
        val current = _settings.value
        if (current.pendingSyncItems == 0 || current.isSyncInProgress || current.isOfflineMode) return

        viewModelScope.launch {
            _settings.value = _settings.value.copy(isSyncInProgress = true)
            
            try {
                // Attempt to perform actual sync with Firestore
                val service = syncService
                val notificationHelper = syncNotificationHelper
                
                if (service != null && notificationHelper != null) {
                    // Create a mock snapshot for demonstration
                    // In real implementation, this would come from your local database
                    val snapshot = com.coinmetric.data.repository.SyncSnapshot(
                        categories = emptyList(), // Replace with actual data from your local DB
                        members = emptyList(),
                        transactions = emptyList(),
                        recurringPayments = emptyList(),
                        invites = emptyList(),
                        limits = emptyList(),
                        changeLog = emptyList()
                    )
                    
                    service.pushSnapshot(current.currentUserEmail, snapshot)
                    notificationHelper.notifySyncSuccess("Синхронизация с облаком завершена успешно")
                } else {
                    throw Exception("Sync service not initialized")
                }
                
                _settings.value = _settings.value.copy(
                    isSyncInProgress = false,
                    pendingSyncItems = 0,
                    syncError = null,
                    lastSyncTimeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                )
            } catch (e: Exception) {
                Log.w("CoinMetricViewModel", "Warning: Sync operation failed with error: ${e.message}", e)
                _settings.value = _settings.value.copy(
                    isSyncInProgress = false,
                    syncError = "Ошибка синхронизации: ${e.message}"
                )
                
                val notificationHelper = syncNotificationHelper
                notificationHelper?.notifySyncError("Ошибка синхронизации: ${e.message}")
            }
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

    private fun appendActivityLog(action: String, target: String) {
        val settings = _settings.value
        val actor = mapRoleLabel(settings.currentUserRole)
        val createdAt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        _settings.value = settings.copy(
            activityLog = listOf(
                ActivityLogUiModel(
                    actor = actor,
                    action = action,
                    target = target,
                    createdAtLabel = createdAt,
                )
            ) + settings.activityLog,
            inviteError = null,
        )
    }

    private fun mapRoleLabel(role: String): String = when (role) {
        "owner" -> "Владелец"
        "editor" -> "Редактор"
        "viewer" -> "Наблюдатель"
        else -> role
    }
}
