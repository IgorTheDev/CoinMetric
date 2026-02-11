package com.coinmetric.data.repository

import com.coinmetric.data.local.CategoryMonthSpendRow
import com.coinmetric.data.local.CategorySpendRow
import com.coinmetric.data.local.CoinMetricDao
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: CoinMetricDao) {
    fun categories(): Flow<List<Category>> = dao.observeCategories()
    fun members(): Flow<List<FamilyMember>> = dao.observeMembers()
    fun invites(): Flow<List<CollaborationInvite>> = dao.observeInvites()
    fun limits(): Flow<List<CategoryLimit>> = dao.observeLimits()
    fun recurringPayments(): Flow<List<RecurringPayment>> = dao.observeRecurringPayments()
    fun transactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun categorySpend(): Flow<List<CategorySpendRow>> = dao.observeCategorySpend()
    fun monthCategorySpend(monthKey: String): Flow<List<CategoryMonthSpendRow>> = dao.observeMonthCategorySpend(monthKey)
    fun income(): Flow<Double> = dao.observeTotalIncome()
    fun expenses(): Flow<Double> = dao.observeTotalExpense()

    suspend fun addCategory(name: String, colorHex: String = "#4CAF50") {
        dao.upsertCategory(Category(name = name, colorHex = colorHex))
    }

    suspend fun addMember(name: String, email: String, role: String) {
        dao.upsertMember(FamilyMember(name = name, email = email, role = role))
    }

    suspend fun sendInvite(email: String, inviterName: String, role: String = "editor") {
        dao.upsertInvite(CollaborationInvite(email = email, inviterName = inviterName, role = role))
    }

    suspend fun setInviteStatus(invite: CollaborationInvite, status: String) {
        dao.upsertInvite(invite.copy(status = status, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun addCategoryLimit(categoryId: Long, monthlyLimit: Double, monthKey: String) {
        dao.upsertLimit(CategoryLimit(categoryId = categoryId, monthlyLimit = monthlyLimit, monthKey = monthKey))
    }

    suspend fun getMonthlyCategoryExpense(categoryId: Long, monthKey: String): Double =
        dao.getMonthlyCategoryExpense(categoryId, monthKey)

    suspend fun addRecurring(title: String, amount: Double, dayOfMonth: Int, categoryId: Long? = null) {
        dao.upsertRecurringPayment(
            RecurringPayment(title = title, amount = amount, dayOfMonth = dayOfMonth, categoryId = categoryId),
        )
    }

    suspend fun addTransaction(
        amount: Double,
        note: String,
        categoryId: Long?,
        memberId: Long?,
        dateEpochMillis: Long,
        isIncome: Boolean,
    ) {
        dao.upsertTransaction(
            TransactionEntity(
                amount = amount,
                note = note,
                categoryId = categoryId,
                memberId = memberId,
                dateEpochMillis = dateEpochMillis,
                isIncome = isIncome,
            ),
        )
    }

    suspend fun exportSnapshot(): SyncSnapshot =
        SyncSnapshot(
            categories = dao.getAllCategories(),
            members = dao.getAllMembers(),
            transactions = dao.getAllTransactions(),
            recurringPayments = dao.getAllRecurringPayments(),
            invites = dao.getAllInvites(),
            limits = dao.getAllLimits(),
        )

    suspend fun importSnapshot(snapshot: SyncSnapshot) {
        dao.upsertCategories(snapshot.categories)
        dao.upsertMembers(snapshot.members)
        dao.upsertTransactions(snapshot.transactions)
        dao.upsertRecurringPayments(snapshot.recurringPayments)
        dao.upsertInvites(snapshot.invites)
        dao.upsertLimits(snapshot.limits)
    }
}

data class SyncSnapshot(
    val categories: List<Category> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val invites: List<CollaborationInvite> = emptyList(),
    val limits: List<CategoryLimit> = emptyList(),
)
