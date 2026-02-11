package com.coinmetric.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: FamilyMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: CategoryLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringPayment(payment: RecurringPayment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM category ORDER BY name")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM familymember ORDER BY name")
    fun observeMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM recurringpayment WHERE active = 1 ORDER BY dayOfMonth")
    fun observeRecurringPayments(): Flow<List<RecurringPayment>>

    @Query("SELECT * FROM transactionentity ORDER BY dateEpochMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT c.name as category, IFNULL(SUM(t.amount), 0) as spent
        FROM category c
        LEFT JOIN transactionentity t ON t.categoryId = c.id AND t.isIncome = 0
        GROUP BY c.id
        ORDER BY spent DESC
        """,
    )
    fun observeCategorySpend(): Flow<List<CategorySpendRow>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactionentity WHERE isIncome = 1")
    fun observeTotalIncome(): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactionentity WHERE isIncome = 0")
    fun observeTotalExpense(): Flow<Double>
}

data class CategorySpendRow(
    val category: String,
    val spent: Double,
)
