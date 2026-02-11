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
}
