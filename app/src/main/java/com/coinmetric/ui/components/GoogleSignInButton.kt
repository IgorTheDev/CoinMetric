package com.coinmetric.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.coinmetric.R

@Composable
fun GoogleSignInButton(
    onSignInSuccess: (GoogleSignInAccount) -> Unit,
    onSignInFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val googleSignInClient = rememberGoogleSignInClient(context)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                signInWithFirebase(auth, account, onSignInSuccess, onSignInFailed)
            } catch (e: ApiException) {
                onSignInFailed()
            }
        } else {
            onSignInFailed()
        }
    }

    OutlinedButton(
        onClick = {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        },
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Войти через Google")
        }
    }
}

@Composable
private fun rememberGoogleSignInClient(context: Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(com.coinmetric.R.string.default_web_client_id))
        .requestEmail()
        .build()
    
    return GoogleSignIn.getClient(context, gso)
}

private fun signInWithFirebase(
    auth: FirebaseAuth,
    account: GoogleSignInAccount,
    onSuccess: (GoogleSignInAccount) -> Unit,
    onFailure: () -> Unit
) {
    val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess(account)
            } else {
                onFailure()
            }
        }
}