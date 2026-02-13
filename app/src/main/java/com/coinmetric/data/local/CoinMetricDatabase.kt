package com.coinmetric.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.SyncChangeLog
import com.coinmetric.data.model.SyncState
import com.coinmetric.data.model.TransactionEntity

@Database(
    entities = [
        FamilyMember::class,
        Category::class,
        CategoryLimit::class,
        TransactionEntity::class,
        RecurringPayment::class,
        CollaborationInvite::class,
        SyncChangeLog::class,
        SyncState::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class CoinMetricDatabase : RoomDatabase() {
    abstract fun dao(): CoinMetricDao

    companion object {
        @Volatile
        private var instance: CoinMetricDatabase? = null

        fun get(context: Context): CoinMetricDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    CoinMetricDatabase::class.java,
                    "coinmetric.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
