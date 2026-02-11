package com.coinmetric.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.delay

class GoogleSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.retry()
        // Заглушка синхронизации: здесь можно отправлять локальные изменения в Drive/Sheets/Firestore.
        if (account.email.isNullOrBlank()) return Result.retry()
        delay(500)
        return Result.success()
    }
}
