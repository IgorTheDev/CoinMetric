package com.coinmetric.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.SyncChangeLog
import com.coinmetric.data.model.SyncState
import com.coinmetric.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<Category>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: FamilyMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<FamilyMember>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: CategoryLimit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimits(limits: List<CategoryLimit>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringPayment(payment: RecurringPayment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringPayments(payments: List<RecurringPayment>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvite(invite: CollaborationInvite): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvites(invites: List<CollaborationInvite>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChangeLogs(logs: List<SyncChangeLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: SyncState)

    @Query("SELECT * FROM category ORDER BY name")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM familymember ORDER BY name")
    fun observeMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM collaborationinvite ORDER BY createdAtEpochMillis DESC")
    fun observeInvites(): Flow<List<CollaborationInvite>>

    @Query("SELECT * FROM recurringpayment WHERE active = 1 ORDER BY dayOfMonth")
    fun observeRecurringPayments(): Flow<List<RecurringPayment>>

    @Query("SELECT * FROM transactionentity ORDER BY dateEpochMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM categorylimit ORDER BY monthKey DESC")
    fun observeLimits(): Flow<List<CategoryLimit>>

    @Query("SELECT * FROM category")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM category WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM familymember")
    suspend fun getAllMembers(): List<FamilyMember>

    @Query("SELECT * FROM familymember WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): FamilyMember?

    @Query("SELECT * FROM transactionentity")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactionentity WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM recurringpayment")
    suspend fun getAllRecurringPayments(): List<RecurringPayment>

    @Query("SELECT * FROM recurringpayment WHERE id = :id LIMIT 1")
    suspend fun getRecurringPaymentById(id: Long): RecurringPayment?

    @Query("SELECT * FROM collaborationinvite")
    suspend fun getAllInvites(): List<CollaborationInvite>

    @Query("SELECT * FROM collaborationinvite WHERE id = :id LIMIT 1")
    suspend fun getInviteById(id: Long): CollaborationInvite?

    @Query("SELECT * FROM categorylimit")
    suspend fun getAllLimits(): List<CategoryLimit>

    @Query("SELECT * FROM categorylimit WHERE id = :id LIMIT 1")
    suspend fun getLimitById(id: Long): CategoryLimit?

    @Query("SELECT * FROM syncchangelog WHERE updatedAtEpochMillis > :since ORDER BY updatedAtEpochMillis ASC")
    suspend fun getChangeLogsSince(since: Long): List<SyncChangeLog>

    @Query("SELECT * FROM syncstate WHERE id = 1 LIMIT 1")
    suspend fun getSyncState(): SyncState?

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

    @Query(
        """
        SELECT c.id as categoryId, c.name as category, IFNULL(SUM(t.amount), 0) as spent
        FROM category c
        LEFT JOIN transactionentity t ON t.categoryId = c.id
            AND t.isIncome = 0
            AND strftime('%Y-%m', t.dateEpochMillis / 1000, 'unixepoch', 'localtime') = :monthKey
        GROUP BY c.id
        ORDER BY c.name
        """,
    )
    fun observeMonthCategorySpend(monthKey: String): Flow<List<CategoryMonthSpendRow>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactionentity WHERE isIncome = 1")
    fun observeTotalIncome(): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactionentity WHERE isIncome = 0")
    fun observeTotalExpense(): Flow<Double>

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) FROM transactionentity
        WHERE categoryId = :categoryId
          AND isIncome = 0
          AND strftime('%Y-%m', dateEpochMillis / 1000, 'unixepoch', 'localtime') = :monthKey
        """,
    )
    suspend fun getMonthlyCategoryExpense(categoryId: Long, monthKey: String): Double
}

data class CategorySpendRow(
    val category: String,
    val spent: Double,
)

data class CategoryMonthSpendRow(
    val categoryId: Long,
    val category: String,
    val spent: Double,
)
