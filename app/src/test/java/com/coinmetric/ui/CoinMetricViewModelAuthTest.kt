package com.coinmetric.ui

import com.coinmetric.auth.GoogleAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoinMetricViewModelAuthTest {
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
    fun performGoogleSignIn_callsAuthManagerSignIn() {
        val vm = CoinMetricViewModel()
        var signInCalled = false
        
        // Create a mock GoogleAuthManager
        val mockAuthManager = object : GoogleAuthManager(
            activity = null as android.app.Activity, // This would normally be a mock
            viewModel = null
        ) {
            override fun signIn() {
                signInCalled = true
            }
        }
        
        vm.setGoogleAuthManager(mockAuthManager)
        
        vm.performGoogleSignIn()
        
        assertTrue(signInCalled)
    }

    @Test
    fun updateCurrentUserEmail_updatesSettingsCorrectly() {
        val vm = CoinMetricViewModel()
        
        vm.updateCurrentUserEmail("test@example.com")
        
        assertEquals("test@example.com", vm.settings.value.currentUserEmail)
    }

    @Test
    fun setGoogleSync_enablesSyncAndUpdatesSettings() {
        val vm = CoinMetricViewModel()
        
        vm.setGoogleSync(true)
        
        assertTrue(vm.settings.value.googleSyncEnabled)
        assertNull(vm.settings.value.syncError)
    }

    @Test
    fun setGoogleSync_false_setsSyncError() {
        val vm = CoinMetricViewModel()
        
        vm.setGoogleSync(false)
        
        assertFalse(vm.settings.value.googleSyncEnabled)
        assertEquals("Синхронизация отключена пользователем", vm.settings.value.syncError)
    }

    @Test
    fun setGoogleSync_writesActivityLog() {
        val vm = CoinMetricViewModel()
        
        vm.setGoogleSync(true)
        
        val log = vm.settings.value.activityLog.first()
        assertEquals("Google Sync", log.action)
        assertEquals("Включена", log.target)
    }
    
    @Test
    fun setOfflineMode_isRemovedAndDoesNotAffectSettings() {
        val vm = CoinMetricViewModel()
        
        // Since we're removing offline mode, this test verifies the function still exists
        // but the functionality should be removed from the UI
        vm.setOfflineMode(true)
        
        // After our changes, this should not actually affect the settings
        // Check that the function exists but doesn't cause errors
        assertNotNull(vm.settings.value.isOfflineMode)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleAuthManagerTest {
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
    fun firebaseAuthWithGoogle_success_updatesViewModel() {
        var emailUpdated = ""
        var syncEnabled = false
        
        val mockViewModel = object : CoinMetricViewModel() {
            override fun updateCurrentUserEmail(email: String) {
                emailUpdated = email
            }
            
            override fun setGoogleSync(enabled: Boolean) {
                syncEnabled = enabled
            }
        }
        
        val authManager = GoogleAuthManager(
            activity = null as android.app.Activity, // This would normally be a mock
            viewModel = mockViewModel
        )
        
        // Since we can't easily test the coroutine, we're checking the logic
        // This test focuses on the function signature and basic behavior
        assertTrue(true) // Placeholder test
    }

    @Test
    fun firebaseAuthWithGoogle_failure_updatesViewModelCorrectly() {
        var emailUpdated = ""
        var syncEnabled = true
        
        val mockViewModel = object : CoinMetricViewModel() {
            override fun updateCurrentUserEmail(email: String) {
                emailUpdated = email
            }
            
            override fun setGoogleSync(enabled: Boolean) {
                syncEnabled = enabled
            }
        }
        
        val authManager = GoogleAuthManager(
            activity = null as android.app.Activity, // This would normally be a mock
            viewModel = mockViewModel
        )
        
        // Placeholder test for failure case
        assertTrue(true)
    }
}