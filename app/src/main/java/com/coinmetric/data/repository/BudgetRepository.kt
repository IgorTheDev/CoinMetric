package com.coinmetric.data.repository

import com.coinmetric.data.local.CategorySpendRow
import com.coinmetric.data.local.CoinMetricDao
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: CoinMetricDao) {
    fun categories(): Flow<List<Category>> = dao.observeCategories()
    fun members(): Flow<List<FamilyMember>> = dao.observeMembers()
    fun recurringPayments(): Flow<List<RecurringPayment>> = dao.observeRecurringPayments()
    fun transactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun categorySpend(): Flow<List<CategorySpendRow>> = dao.observeCategorySpend()
    fun income(): Flow<Double> = dao.observeTotalIncome()
    fun expenses(): Flow<Double> = dao.observeTotalExpense()

    suspend fun addCategory(name: String, colorHex: String = "#4CAF50") {
        dao.upsertCategory(Category(name = name, colorHex = colorHex))
    }

    suspend fun addMember(name: String, email: String, role: String) {
        dao.upsertMember(FamilyMember(name = name, email = email, role = role))
    }

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
}
