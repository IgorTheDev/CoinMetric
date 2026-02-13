package com.coinmetric.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.coinmetric.ui.CoinMetricViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(
    private val activity: AppCompatActivity,
    private var viewModel: CoinMetricViewModel?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : DefaultLifecycleObserver {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    init {
        // Initialize Google Sign In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1047100894264-kc7untqduu5cbqad0a7ra9q05j84hr94.apps.googleusercontent.com") // Web client ID from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        // Register the activity result launcher
        signInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "Firebase Auth with Google account: ${account.email}")
                    firebaseAuthWithGoogle(account.idToken!!, account.email)
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    // Update UI to reflect sign-in failure
                    viewModel?.setGoogleSync(false)
                    viewModel?.updateCurrentUserEmail("")
                }
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        // This ensures the activity result launcher is properly registered
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String, email: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                
                val userEmail = email // Use the email passed from Google Sign-In
                
                if (!userEmail.isNullOrBlank()) {
                    // Update the ViewModel with the user's email
                    viewModel?.updateCurrentUserEmail(userEmail)
                    
                    // Enable Google sync in settings
                    viewModel?.setGoogleSync(true)
                    
                    Log.d(TAG, "Authentication successful with email: $userEmail")
                } else {
                    Log.w(TAG, "User email is null or blank after Google authentication")
                    // Update UI to reflect sign-in failure
                    viewModel?.setGoogleSync(false)
                    viewModel?.updateCurrentUserEmail("")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase authentication with Google failed", e)
                // Update UI to reflect authentication failure
                viewModel?.setGoogleSync(false)
                viewModel?.updateCurrentUserEmail("")
            }
        }
    }

    fun signOut() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                googleSignInClient.signOut().await()
                auth.signOut()
                
                // Reset user email in ViewModel
                viewModel?.updateCurrentUserEmail("")
                
                // Disable Google sync in settings
                viewModel?.setGoogleSync(false)
                
                Log.d(TAG, "Successfully signed out")
            } catch (e: Exception) {
                Log.w(TAG, "Sign out failed", e)
            }
        }
    }

    fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    fun setViewModel(viewModel: CoinMetricViewModel) {
        this.viewModel = viewModel
    }
}