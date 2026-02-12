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
        vm.updateInviteEmail("family@example.com")
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

}
