package com.coinmetric.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coinmetric.data.local.CoinMetricDatabase
import com.coinmetric.data.repository.BudgetRepository
import com.coinmetric.data.repository.SyncSnapshot
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
            val syncState = repository.getSyncState()

            val remoteChanges = service.pullChanges(email, syncState.lastPulledAtEpochMillis)
            repository.importSnapshot(remoteChanges)

            val localChanges = repository.exportChangesSince(syncState.lastPushedAtEpochMillis)
            if (!localChanges.isEmpty()) {
                service.pushSnapshot(email, localChanges)
            }

            repository.saveSyncState(
                syncState.copy(
                    lastPulledAtEpochMillis = maxOf(syncState.lastPulledAtEpochMillis, remoteChanges.lastChangeTimestamp()),
                    lastPushedAtEpochMillis = maxOf(syncState.lastPushedAtEpochMillis, localChanges.lastChangeTimestamp()),
                ),
            )
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

private fun SyncSnapshot.isEmpty(): Boolean =
    categories.isEmpty() &&
        members.isEmpty() &&
        transactions.isEmpty() &&
        recurringPayments.isEmpty() &&
        invites.isEmpty() &&
        limits.isEmpty() &&
        changeLog.isEmpty()

private fun SyncSnapshot.lastChangeTimestamp(): Long =
    changeLog.maxOfOrNull { it.updatedAtEpochMillis }
        ?: listOf(
            categories.maxOfOrNull { it.updatedAtEpochMillis },
            members.maxOfOrNull { it.updatedAtEpochMillis },
            transactions.maxOfOrNull { it.updatedAtEpochMillis },
            recurringPayments.maxOfOrNull { it.updatedAtEpochMillis },
            invites.maxOfOrNull { it.updatedAtEpochMillis },
            limits.maxOfOrNull { it.updatedAtEpochMillis },
        ).filterNotNull().maxOrNull()
        ?: 0
