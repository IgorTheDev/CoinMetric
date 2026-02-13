package com.coinmetric.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

object GoogleAuthConfig {
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    
    fun getGoogleSignInClient(context: Context) = GoogleSignIn.getClient(context, getGoogleSignInOptions())
}