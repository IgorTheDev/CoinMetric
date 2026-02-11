package com.coinmetric.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coinmetric.data.local.CoinMetricDatabase
import com.coinmetric.data.repository.BudgetRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn

class GoogleSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.retry()
        val email = account.email ?: return Result.retry()
        val repository = BudgetRepository(CoinMetricDatabase.get(applicationContext).dao())
        val service = FirestoreSyncService()

        return runCatching {
            val remoteSnapshot = service.pullSnapshot(email)
            repository.importSnapshot(remoteSnapshot)
            val localSnapshot = repository.exportSnapshot()
            service.pushSnapshot(email, localSnapshot)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
