package com.coinmetric.data.repository

import com.coinmetric.data.local.CategoryMonthSpendRow
import com.coinmetric.data.local.CategorySpendRow
import com.coinmetric.data.local.CoinMetricDao
import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.SyncChangeLog
import com.coinmetric.data.model.SyncState
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
        val entity = Category(name = name, colorHex = colorHex)
        val id = dao.upsertCategory(entity)
        trackChange(SyncEntityType.CATEGORY, id, entity.updatedAtEpochMillis)
    }

    suspend fun addMember(name: String, email: String, role: String) {
        val entity = FamilyMember(name = name, email = email, role = role)
        val id = dao.upsertMember(entity)
        trackChange(SyncEntityType.MEMBER, id, entity.updatedAtEpochMillis)
    }

    suspend fun sendInvite(email: String, inviterName: String, role: String = "editor") {
        val entity = CollaborationInvite(email = email, inviterName = inviterName, role = role)
        val id = dao.upsertInvite(entity)
        trackChange(SyncEntityType.INVITE, id, entity.updatedAtEpochMillis)
    }

    suspend fun setInviteStatus(invite: CollaborationInvite, status: String) {
        val updated = invite.copy(status = status, updatedAtEpochMillis = System.currentTimeMillis())
        val id = dao.upsertInvite(updated)
        trackChange(SyncEntityType.INVITE, id, updated.updatedAtEpochMillis)
    }

    suspend fun addCategoryLimit(categoryId: Long, monthlyLimit: Double, monthKey: String) {
        val entity = CategoryLimit(categoryId = categoryId, monthlyLimit = monthlyLimit, monthKey = monthKey)
        val id = dao.upsertLimit(entity)
        trackChange(SyncEntityType.LIMIT, id, entity.updatedAtEpochMillis)
    }

    suspend fun getMonthlyCategoryExpense(categoryId: Long, monthKey: String): Double =
        dao.getMonthlyCategoryExpense(categoryId, monthKey)

    suspend fun addRecurring(title: String, amount: Double, dayOfMonth: Int, categoryId: Long? = null) {
        val entity = RecurringPayment(title = title, amount = amount, dayOfMonth = dayOfMonth, categoryId = categoryId)
        val id = dao.upsertRecurringPayment(entity)
        trackChange(SyncEntityType.RECURRING, id, entity.updatedAtEpochMillis)
    }

    suspend fun addTransaction(
        amount: Double,
        note: String,
        categoryId: Long?,
        memberId: Long?,
        dateEpochMillis: Long,
        isIncome: Boolean,
    ) {
        val entity = TransactionEntity(
            amount = amount,
            note = note,
            categoryId = categoryId,
            memberId = memberId,
            dateEpochMillis = dateEpochMillis,
            isIncome = isIncome,
        )
        val id = dao.upsertTransaction(entity)
        trackChange(SyncEntityType.TRANSACTION, id, entity.updatedAtEpochMillis)
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

    suspend fun exportChangesSince(sinceEpochMillis: Long): SyncSnapshot {
        val changes = dao.getChangeLogsSince(sinceEpochMillis).filter { it.source == LOCAL_SOURCE }
        if (changes.isEmpty()) return SyncSnapshot()

        val categories = changes.filterByType(SyncEntityType.CATEGORY)
            .mapNotNull { dao.getCategoryById(it.entityId) }
        val members = changes.filterByType(SyncEntityType.MEMBER)
            .mapNotNull { dao.getMemberById(it.entityId) }
        val transactions = changes.filterByType(SyncEntityType.TRANSACTION)
            .mapNotNull { dao.getTransactionById(it.entityId) }
        val recurring = changes.filterByType(SyncEntityType.RECURRING)
            .mapNotNull { dao.getRecurringPaymentById(it.entityId) }
        val invites = changes.filterByType(SyncEntityType.INVITE)
            .mapNotNull { dao.getInviteById(it.entityId) }
        val limits = changes.filterByType(SyncEntityType.LIMIT)
            .mapNotNull { dao.getLimitById(it.entityId) }

        return SyncSnapshot(
            categories = categories,
            members = members,
            transactions = transactions,
            recurringPayments = recurring,
            invites = invites,
            limits = limits,
            changeLog = changes,
        )
    }

    suspend fun importSnapshot(snapshot: SyncSnapshot, source: String = REMOTE_SOURCE) {
        mergeEntities(
            incoming = snapshot.categories,
            current = dao.getAllCategories().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertCategories(it) },
        ) { record -> trackChange(SyncEntityType.CATEGORY, record.id, record.updatedAtEpochMillis, source) }

        mergeEntities(
            incoming = snapshot.members,
            current = dao.getAllMembers().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertMembers(it) },
        ) { record -> trackChange(SyncEntityType.MEMBER, record.id, record.updatedAtEpochMillis, source) }

        mergeEntities(
            incoming = snapshot.transactions,
            current = dao.getAllTransactions().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertTransactions(it) },
        ) { record -> trackChange(SyncEntityType.TRANSACTION, record.id, record.updatedAtEpochMillis, source) }

        mergeEntities(
            incoming = snapshot.recurringPayments,
            current = dao.getAllRecurringPayments().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertRecurringPayments(it) },
        ) { record -> trackChange(SyncEntityType.RECURRING, record.id, record.updatedAtEpochMillis, source) }

        mergeEntities(
            incoming = snapshot.invites,
            current = dao.getAllInvites().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertInvites(it) },
        ) { record -> trackChange(SyncEntityType.INVITE, record.id, record.updatedAtEpochMillis, source) }

        mergeEntities(
            incoming = snapshot.limits,
            current = dao.getAllLimits().associateBy { it.id },
            updatedAtOf = { it.updatedAtEpochMillis },
            upsert = { dao.upsertLimits(it) },
        ) { record -> trackChange(SyncEntityType.LIMIT, record.id, record.updatedAtEpochMillis, source) }

        if (snapshot.changeLog.isNotEmpty()) {
            dao.upsertChangeLogs(snapshot.changeLog)
        }
    }

    suspend fun getSyncState(): SyncState = dao.getSyncState() ?: SyncState()

    suspend fun saveSyncState(state: SyncState) {
        dao.upsertSyncState(state)
    }

    private suspend fun <T> mergeEntities(
        incoming: List<T>,
        current: Map<Long, T>,
        updatedAtOf: (T) -> Long,
        upsert: suspend (List<T>) -> Unit,
        onMerged: suspend (T) -> Unit,
    ) where T : Any {
        val toUpsert = incoming.filter { incomingItem ->
            val id = incomingItem.idValue()
            val localItem = current[id]
            localItem == null || updatedAtOf(incomingItem) >= updatedAtOf(localItem)
        }

        if (toUpsert.isNotEmpty()) {
            upsert(toUpsert)
            toUpsert.forEach { onMerged(it) }
        }
    }

    private suspend fun trackChange(
        entityType: String,
        entityId: Long,
        updatedAtEpochMillis: Long,
        source: String = LOCAL_SOURCE,
    ) {
        dao.upsertChangeLogs(
            listOf(
                SyncChangeLog(
                    entityType = entityType,
                    entityId = entityId,
                    updatedAtEpochMillis = updatedAtEpochMillis,
                    action = ACTION_UPSERT,
                    source = source,
                ),
            ),
        )
    }

    private fun List<SyncChangeLog>.filterByType(entityType: String): List<SyncChangeLog> =
        filter { it.entityType == entityType }.distinctBy { it.entityId }

    private fun Any.idValue(): Long = when (this) {
        is Category -> id
        is FamilyMember -> id
        is TransactionEntity -> id
        is RecurringPayment -> id
        is CollaborationInvite -> id
        is CategoryLimit -> id
        else -> error("Unsupported entity type for sync: ${this::class.simpleName}")
    }

    companion object {
        const val LOCAL_SOURCE = "local"
        const val REMOTE_SOURCE = "remote"
        const val ACTION_UPSERT = "upsert"
    }
}

object SyncEntityType {
    const val CATEGORY = "category"
    const val MEMBER = "member"
    const val TRANSACTION = "transaction"
    const val RECURRING = "recurring"
    const val INVITE = "invite"
    const val LIMIT = "limit"
}

data class SyncSnapshot(
    val categories: List<Category> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val invites: List<CollaborationInvite> = emptyList(),
    val limits: List<CategoryLimit> = emptyList(),
    val changeLog: List<SyncChangeLog> = emptyList(),
)
