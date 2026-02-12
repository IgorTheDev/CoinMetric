package com.coinmetric.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoinMetricViewModelInviteTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendFamilyInvite_addsPendingInvite() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("Family@Example.com ")
        vm.updateInviteRole("viewer")

        vm.sendFamilyInvite()
        runCurrent()

        val settings = vm.settings.value
        assertEquals(1, settings.pendingInvites.size)
        assertEquals("family@example.com", settings.pendingInvites.first().email)
        assertEquals("viewer", settings.pendingInvites.first().role)
        assertEquals("Ожидает принятия", settings.pendingInvites.first().status)
    }

    @Test
    fun updateInviteStatus_movesFromPendingToAccepted() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("editor@example.com")
        vm.sendFamilyInvite()

        vm.updateInviteStatus("editor@example.com", "Принято")
        runCurrent()

        val updated = vm.settings.value.pendingInvites.first()
        assertEquals("Принято", updated.status)
        assertTrue(vm.settings.value.inviteError == null)
    }

    @Test
    fun updateInviteStatus_forResolvedInvite_returnsError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("viewer@example.com")
        vm.sendFamilyInvite()
        vm.updateInviteStatus("viewer@example.com", "Отклонено")

        vm.updateInviteStatus("viewer@example.com", "Принято")

        assertEquals("Статус уже обновлён", vm.settings.value.inviteError)
    }

    @Test
    fun sendFamilyInvite_asEditor_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.setCurrentUserRole("editor")
        vm.updateInviteEmail("editor@example.com")

        vm.sendFamilyInvite()

        assertEquals("Только владелец может отправлять приглашения", vm.settings.value.inviteError)
        assertTrue(vm.settings.value.pendingInvites.isEmpty())
    }

    @Test
    fun saveTransaction_asViewer_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.setCurrentUserRole("viewer")
        vm.updateAmount("1000")
        vm.updateCategory("Еда")

        vm.saveTransaction(onSuccess = {})

        assertEquals(
            "Роль просмотра не позволяет добавлять или редактировать операции",
            vm.addState.value.error,
        )
    }

    @Test
    fun sendFamilyInvite_writesActivityLog() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("audit@example.com")

        vm.sendFamilyInvite()

        val log = vm.settings.value.activityLog.first()
        assertEquals("Владелец", log.actor)
        assertEquals("Отправка приглашения", log.action)
        assertTrue(log.target.contains("audit@example.com"))
    }

    @Test
    fun saveTransaction_writesCreateActivityLog() {
        val vm = CoinMetricViewModel()
        vm.updateAmount("2100")
        vm.updateCategory("Транспорт")
        vm.updateNote("Такси")

        vm.saveTransaction(onSuccess = {})

        val log = vm.settings.value.activityLog.first()
        assertEquals("Создание операции", log.action)
        assertTrue(log.target.contains("Транспорт"))
    }

    @Test
    fun saveTransaction_withRecurringFlag_writesRecurringActivityLog() {
        val vm = CoinMetricViewModel()
        vm.updateAmount("900")
        vm.updateCategory("Коммунальные")
        vm.updateRecurringFlag(true)

        vm.saveTransaction(onSuccess = {})

        val logActions = vm.settings.value.activityLog.map { it.action }
        assertTrue(logActions.contains("Создание операции"))
        assertTrue(logActions.contains("Добавлен постоянный платёж"))
    }

    @Test
    fun setRecurringReminders_updatesSettingsAndWritesLog() {
        val vm = CoinMetricViewModel()

        vm.setRecurringReminders(false)

        assertEquals(false, vm.settings.value.recurringRemindersEnabled)
        assertEquals("Напоминания о платежах", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun setSubscriptionPlan_updatesPlanAndWritesLog() {
        val vm = CoinMetricViewModel()

        vm.setSubscriptionPlan("pro")

        assertEquals("pro", vm.settings.value.subscriptionPlan)
        assertEquals("План подписки", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun setBiometricProtection_withoutPin_returnsError() {
        val vm = CoinMetricViewModel()

        vm.setBiometricProtectionEnabled(true)

        assertEquals("Сначала включите PIN-защиту", vm.settings.value.syncError)
        assertEquals(false, vm.settings.value.biometricProtectionEnabled)
    }

    @Test
    fun setPinProtection_disablingPinTurnsOffBiometric() {
        val vm = CoinMetricViewModel()

        vm.setPinProtectionEnabled(true)
        vm.setBiometricProtectionEnabled(true)
        vm.setPinProtectionEnabled(false)

        assertEquals(false, vm.settings.value.pinProtectionEnabled)
        assertEquals(false, vm.settings.value.biometricProtectionEnabled)
    }

    @Test
    fun deleteTransaction_asViewer_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        val transaction = vm.dashboard.value.allTransactions.first()
        vm.setCurrentUserRole("viewer")

        vm.deleteTransaction(transaction)

        assertEquals("Роль просмотра не позволяет удалять операции", vm.addState.value.error)
        assertEquals(3, vm.dashboard.value.allTransactions.size)
    }

    @Test
    fun saveMonthlyLimit_writesActivityLog() {
        val vm = CoinMetricViewModel()
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("17000")

        vm.saveMonthlyLimit()

        val log = vm.settings.value.activityLog.first()
        assertEquals("Обновление лимита", log.action)
        assertTrue(log.target.contains("Еда"))
    }

    @Test
    fun addNewCategory_writesActivityLog() {
        val vm = CoinMetricViewModel()
        vm.updateCategoriesNewCategoryName("Здоровье")

        vm.addNewCategory()

        val log = vm.settings.value.activityLog.first()
        assertEquals("Создана категория", log.action)
        assertEquals("Здоровье", log.target)
    }

    @Test
    fun completeSecuritySetup_marksSetupAsCompleted() {
        val vm = CoinMetricViewModel()
        vm.setPinProtectionEnabled(true)

        vm.completeSecuritySetup()

        assertTrue(vm.settings.value.securitySetupCompleted)
        assertEquals("Мастер безопасности", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun completeSecuritySetup_withoutPin_returnsValidationError() {
        val vm = CoinMetricViewModel()

        vm.completeSecuritySetup()

        assertEquals("Для завершения включите PIN-защиту", vm.settings.value.syncError)
        assertEquals(false, vm.settings.value.securitySetupCompleted)
    }

    @Test
    fun sendFamilyInvite_duplicatePendingInvite_returnsError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("family@example.com")
        vm.sendFamilyInvite()
        vm.updateInviteEmail("family@example.com")

        vm.sendFamilyInvite()

        assertEquals("Для этого email уже есть активное приглашение", vm.settings.value.inviteError)
        assertEquals(1, vm.settings.value.pendingInvites.size)
    }

    @Test
    fun revokeFamilyInvite_removesPendingInviteAndWritesLog() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("pending@example.com")
        vm.sendFamilyInvite()

        vm.revokeFamilyInvite("pending@example.com")

        assertEquals(0, vm.settings.value.pendingInvites.size)
        assertEquals("Отзыв приглашения", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun revokeFamilyInvite_asEditor_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("pending@example.com")
        vm.sendFamilyInvite()
        vm.setCurrentUserRole("editor")

        vm.revokeFamilyInvite("pending@example.com")

        assertEquals("Только владелец может отзывать приглашения", vm.settings.value.inviteError)
        assertEquals(1, vm.settings.value.pendingInvites.size)
    }

    @Test
    fun addNewCategory_asViewer_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.setCurrentUserRole("viewer")
        vm.updateCategoriesNewCategoryName("Питомцы")

        vm.addNewCategory()

        assertEquals("Роль просмотра не позволяет добавлять категории", vm.categoriesState.value.error)
    }

    @Test
    fun saveMonthlyLimit_asViewer_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.setCurrentUserRole("viewer")
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("10000")

        vm.saveMonthlyLimit()

        assertEquals("Роль просмотра не позволяет изменять лимиты", vm.categoriesState.value.error)
    }

    @Test
    fun setCurrentUserRole_invalidRole_returnsErrorAndDoesNotChangeRole() {
        val vm = CoinMetricViewModel()

        vm.setCurrentUserRole("admin")

        assertEquals("owner", vm.settings.value.currentUserRole)
        assertEquals("Некорректная роль пользователя", vm.settings.value.inviteError)
    }

    @Test
    fun setOfflineMode_writesActivityLog() {
        val vm = CoinMetricViewModel()

        vm.setOfflineMode(true)

        assertEquals("Автономный режим", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun clearInviteFeedback_clearsErrorAndSuccessMessage() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("bad-email")
        vm.sendFamilyInvite()

        vm.clearInviteFeedback()

        assertEquals(null, vm.settings.value.inviteError)
        assertEquals(null, vm.settings.value.inviteSuccessMessage)
    }

    @Test
    fun setSubscriptionPlan_invalidPlan_isIgnored() {
        val vm = CoinMetricViewModel()

        vm.setSubscriptionPlan("enterprise")

        assertEquals("free", vm.settings.value.subscriptionPlan)
    }

    @Test
    fun saveTransaction_emitsLimitAlertWhenLimitAlmostReached() {
        val vm = CoinMetricViewModel()
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("2000")
        vm.saveMonthlyLimit()
        vm.consumeLimitAlertEvent()

        vm.updateAmount("1800")
        vm.updateCategory("Еда")
        vm.saveTransaction(onSuccess = {})

        val alert = vm.limitAlertEvent.value
        assertTrue(alert != null)
        assertEquals(false, alert?.isExceeded)
        assertEquals("Еда", alert?.category)
    }

    @Test
    fun consumeLimitAlertEvent_clearsEvent() {
        val vm = CoinMetricViewModel()
        vm.updateSelectedLimitCategory("Еда")
        vm.updateMonthlyLimitInput("1000")
        vm.saveMonthlyLimit()

        vm.consumeLimitAlertEvent()

        assertEquals(null, vm.limitAlertEvent.value)
    }

    @Test
    fun updateInviteStatus_withUppercaseEmail_updatesInvite() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("Case@Test.com")
        vm.sendFamilyInvite()

        vm.updateInviteStatus("CASE@test.com", "Принято")

        assertEquals("Принято", vm.settings.value.pendingInvites.first().status)
    }

    @Test
    fun updateInviteStatus_withInvalidStatus_returnsError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("state@example.com")
        vm.sendFamilyInvite()

        vm.updateInviteStatus("state@example.com", "В обработке")

        assertEquals("Некорректный статус приглашения", vm.settings.value.inviteError)
    }

    @Test
    fun updateInviteStatus_toPendingStatus_returnsError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("state@example.com")
        vm.sendFamilyInvite()

        vm.updateInviteStatus("state@example.com", "Ожидает принятия")

        assertEquals("Некорректный статус приглашения", vm.settings.value.inviteError)
    }

    @Test
    fun revokeFamilyInvite_normalizesEmailBeforeLookup() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("normalize@example.com")
        vm.sendFamilyInvite()

        vm.revokeFamilyInvite(" NORMALIZE@EXAMPLE.COM ")

        assertTrue(vm.settings.value.pendingInvites.isEmpty())
    }

    @Test
    fun updateInviteEmail_clearsInviteFeedback() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("bad-email")
        vm.sendFamilyInvite()
        vm.updateInviteEmail("good@example.com")

        assertEquals(null, vm.settings.value.inviteError)
        assertEquals(null, vm.settings.value.inviteSuccessMessage)
    }

    @Test
    fun updateInviteRole_clearsInviteFeedback() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("member@example.com")
        vm.sendFamilyInvite()
        vm.updateInviteRole("viewer")

        assertEquals(null, vm.settings.value.inviteError)
        assertEquals(null, vm.settings.value.inviteSuccessMessage)
    }

    @Test
    fun updateInviteRole_withInvalidValue_setsErrorAndFallsBackToEditor() {
        val vm = CoinMetricViewModel()

        vm.updateInviteRole(" manager ")

        assertEquals("editor", vm.settings.value.inviteRole)
        assertEquals("Некорректная роль приглашения", vm.settings.value.inviteError)
    }

    @Test
    fun setDarkTheme_writesActivityLog() {
        val vm = CoinMetricViewModel()

        vm.setDarkTheme(true)

        assertEquals(true, vm.settings.value.darkThemeEnabled)
        assertEquals("Тема приложения", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun setDarkTheme_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setDarkTheme(false)

        assertTrue(vm.settings.value.activityLog.none { it.action == "Тема приложения" })
    }

    @Test
    fun setGoogleSync_writesActivityLogAndSetsErrorWhenDisabled() {
        val vm = CoinMetricViewModel()

        vm.setGoogleSync(false)

        assertEquals(false, vm.settings.value.googleSyncEnabled)
        assertEquals("Синхронизация отключена пользователем", vm.settings.value.syncError)
        assertEquals("Google Sync", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun retrySync_whenGoogleSyncDisabled_setsError() {
        val vm = CoinMetricViewModel()
        vm.setGoogleSync(false)

        vm.retrySync()

        assertEquals("Включите Google Sync для повторной отправки", vm.settings.value.syncError)
    }

    @Test
    fun retrySync_whenOfflineModeEnabled_setsError() {
        val vm = CoinMetricViewModel()
        vm.setOfflineMode(true)

        vm.retrySync()

        assertEquals("Отключите автономный режим для синхронизации", vm.settings.value.syncError)
    }

    @Test
    fun setCurrentUserRole_withSameRole_doesNotWriteDuplicateLog() {
        val vm = CoinMetricViewModel()

        vm.setCurrentUserRole("owner")

        assertTrue(vm.settings.value.activityLog.none { it.action == "Смена роли" })
    }

    @Test
    fun setSubscriptionPlan_withSamePlan_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setSubscriptionPlan("free")

        assertTrue(vm.settings.value.activityLog.none { it.action == "План подписки" })
    }

    @Test
    fun setSubscriptionPlan_normalizesInput() {
        val vm = CoinMetricViewModel()

        vm.setSubscriptionPlan(" PRO ")

        assertEquals("pro", vm.settings.value.subscriptionPlan)
    }

    @Test
    fun updateInviteStatus_setsSuccessMessage() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("accepted@example.com")
        vm.sendFamilyInvite()

        vm.updateInviteStatus("accepted@example.com", "Принято")

        assertEquals("Статус приглашения обновлён", vm.settings.value.inviteSuccessMessage)
    }

    @Test
    fun sendFamilyInvite_withMalformedEmail_returnsError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("user@")

        vm.sendFamilyInvite()

        assertEquals("Некорректный email", vm.settings.value.inviteError)
        assertTrue(vm.settings.value.pendingInvites.isEmpty())
    }

    @Test
    fun setCurrentUserRole_normalizesInput() {
        val vm = CoinMetricViewModel()

        vm.setCurrentUserRole(" VIEWER ")

        assertEquals("viewer", vm.settings.value.currentUserRole)
    }

    @Test
    fun updateInviteStatus_asViewer_returnsPermissionError() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("viewer-status@example.com")
        vm.sendFamilyInvite()
        vm.setCurrentUserRole("viewer")

        vm.updateInviteStatus("viewer-status@example.com", "Принято")

        assertEquals("Роль просмотра не позволяет менять статус приглашений", vm.settings.value.inviteError)
        assertEquals("Ожидает принятия", vm.settings.value.pendingInvites.first().status)
    }

    @Test
    fun sendFamilyInvite_asEditor_writesDeniedLog() {
        val vm = CoinMetricViewModel()
        vm.setCurrentUserRole("editor")
        vm.updateInviteEmail("no-permission@example.com")

        vm.sendFamilyInvite()

        assertEquals("Отказ в отправке приглашения", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun revokeFamilyInvite_asEditor_writesDeniedLog() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("pending@example.com")
        vm.sendFamilyInvite()
        vm.setCurrentUserRole("editor")

        vm.revokeFamilyInvite("pending@example.com")

        assertEquals("Отказ в отзыве приглашения", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun updateInviteStatus_asViewer_writesDeniedLog() {
        val vm = CoinMetricViewModel()
        vm.updateInviteEmail("pending@example.com")
        vm.sendFamilyInvite()
        vm.setCurrentUserRole("viewer")

        vm.updateInviteStatus("pending@example.com", "Принято")

        assertEquals("Отказ в изменении статуса приглашения", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun dismissOnboarding_writesLogAndHidesHints() {
        val vm = CoinMetricViewModel()

        vm.dismissOnboarding()

        assertEquals(false, vm.settings.value.showOnboarding)
        assertEquals("Onboarding", vm.settings.value.activityLog.first().action)
    }

    @Test
    fun setOnboardingVisible_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setOnboardingVisible(true)

        assertTrue(vm.settings.value.activityLog.none { it.action == "Onboarding" })
    }

    @Test
    fun setRecurringReminders_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setRecurringReminders(true)

        assertTrue(vm.settings.value.activityLog.none { it.action == "Напоминания о платежах" })
    }

    @Test
    fun setOfflineMode_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setOfflineMode(false)

        assertTrue(vm.settings.value.activityLog.none { it.action == "Автономный режим" })
    }

    @Test
    fun completeSecuritySetup_twice_writesOnlyOneLog() {
        val vm = CoinMetricViewModel()
        vm.setPinProtectionEnabled(true)

        vm.completeSecuritySetup()
        vm.completeSecuritySetup()

        assertEquals(1, vm.settings.value.activityLog.count { it.action == "Мастер безопасности" })
    }

    @Test
    fun setPinProtection_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()

        vm.setPinProtectionEnabled(false)

        assertTrue(vm.settings.value.activityLog.none { it.action == "PIN-защита" })
    }

    @Test
    fun setBiometricProtection_withSameValue_doesNotWriteLog() {
        val vm = CoinMetricViewModel()
        vm.setPinProtectionEnabled(true)
        vm.setBiometricProtectionEnabled(true)

        vm.setBiometricProtectionEnabled(true)

        assertEquals(1, vm.settings.value.activityLog.count { it.action == "Биометрия" })
    }

}
