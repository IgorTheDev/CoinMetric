package com.coinmetric.sync

import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.SyncChangeLog
import com.coinmetric.data.model.TransactionEntity
import com.coinmetric.data.repository.SyncEntityType
import com.coinmetric.data.repository.SyncSnapshot
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun pushSnapshot(accountEmail: String, snapshot: SyncSnapshot) {
        val root = firestore.collection("budgets").document(accountEmail)
        uploadCollection(root.collection("categories"), snapshot.categories.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))
        uploadCollection(root.collection("members"), snapshot.members.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))
        uploadCollection(root.collection("transactions"), snapshot.transactions.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))
        uploadCollection(root.collection("recurring"), snapshot.recurringPayments.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))
        uploadCollection(root.collection("invites"), snapshot.invites.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))
        uploadCollection(root.collection("limits"), snapshot.limits.associateBy({ it.id.toString() }, { it.toFirestoreMap() }))

        val changeLog = if (snapshot.changeLog.isEmpty()) {
            snapshot.toSyntheticChangeLog(source = "local")
        } else {
            snapshot.changeLog
        }
        uploadChangeLog(root.collection("changes"), changeLog)
    }

    suspend fun pullSnapshot(accountEmail: String): SyncSnapshot {
        val root = firestore.collection("budgets").document(accountEmail)
        return SyncSnapshot(
            categories = root.collection("categories").get().await().documents.mapNotNull { it.toCategory() },
            members = root.collection("members").get().await().documents.mapNotNull { it.toMember() },
            transactions = root.collection("transactions").get().await().documents.mapNotNull { it.toTransaction() },
            recurringPayments = root.collection("recurring").get().await().documents.mapNotNull { it.toRecurring() },
            invites = root.collection("invites").get().await().documents.mapNotNull { it.toInvite() },
            limits = root.collection("limits").get().await().documents.mapNotNull { it.toLimit() },
            changeLog = root.collection("changes").get().await().documents.mapNotNull { it.toChangeLog() },
        )
    }

    suspend fun pullChanges(accountEmail: String, sinceEpochMillis: Long): SyncSnapshot {
        val root = firestore.collection("budgets").document(accountEmail)
        val changedDocs = root.collection("changes")
            .whereGreaterThan("updatedAtEpochMillis", sinceEpochMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.toChangeLog() }

        if (changedDocs.isEmpty()) return SyncSnapshot()

        return SyncSnapshot(
            categories = fetchChangedEntities(changedDocs, SyncEntityType.CATEGORY) { id ->
                root.collection("categories").document(id.toString()).get().await().toCategory()
            },
            members = fetchChangedEntities(changedDocs, SyncEntityType.MEMBER) { id ->
                root.collection("members").document(id.toString()).get().await().toMember()
            },
            transactions = fetchChangedEntities(changedDocs, SyncEntityType.TRANSACTION) { id ->
                root.collection("transactions").document(id.toString()).get().await().toTransaction()
            },
            recurringPayments = fetchChangedEntities(changedDocs, SyncEntityType.RECURRING) { id ->
                root.collection("recurring").document(id.toString()).get().await().toRecurring()
            },
            invites = fetchChangedEntities(changedDocs, SyncEntityType.INVITE) { id ->
                root.collection("invites").document(id.toString()).get().await().toInvite()
            },
            limits = fetchChangedEntities(changedDocs, SyncEntityType.LIMIT) { id ->
                root.collection("limits").document(id.toString()).get().await().toLimit()
            },
            changeLog = changedDocs,
        )
    }

    private suspend fun uploadCollection(collection: CollectionReference, docs: Map<String, Map<String, Any?>>) {
        docs.forEach { (id, body) -> collection.document(id).set(body).await() }
    }

    private suspend fun uploadChangeLog(collection: CollectionReference, changes: List<SyncChangeLog>) {
        changes.forEach { change ->
            val docId = "${change.entityType}_${change.entityId}_${change.updatedAtEpochMillis}"
            collection.document(docId).set(
                mapOf(
                    "entityType" to change.entityType,
                    "entityId" to change.entityId,
                    "updatedAtEpochMillis" to change.updatedAtEpochMillis,
                    "action" to change.action,
                    "source" to change.source,
                ),
            ).await()
        }
    }

    private suspend fun <T> fetchChangedEntities(
        changes: List<SyncChangeLog>,
        entityType: String,
        loader: suspend (Long) -> T?,
    ): List<T> {
        return changes.filter { it.entityType == entityType }
            .distinctBy { it.entityId }
            .mapNotNull { loader(it.entityId) }
    }
}

private fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "colorHex" to colorHex,
    "updatedAt" to updatedAtEpochMillis,
)

private fun FamilyMember.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "email" to email,
    "role" to role,
    "updatedAt" to updatedAtEpochMillis,
)

private fun TransactionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "amount" to amount,
    "note" to note,
    "categoryId" to categoryId,
    "memberId" to memberId,
    "dateEpochMillis" to dateEpochMillis,
    "isIncome" to isIncome,
    "updatedAt" to updatedAtEpochMillis,
)

private fun RecurringPayment.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "amount" to amount,
    "dayOfMonth" to dayOfMonth,
    "categoryId" to categoryId,
    "active" to active,
    "updatedAt" to updatedAtEpochMillis,
)

private fun CollaborationInvite.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "email" to email,
    "inviterName" to inviterName,
    "role" to role,
    "status" to status,
    "createdAt" to createdAtEpochMillis,
    "updatedAt" to updatedAtEpochMillis,
)

private fun CategoryLimit.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "categoryId" to categoryId,
    "monthlyLimit" to monthlyLimit,
    "monthKey" to monthKey,
    "updatedAt" to updatedAtEpochMillis,
)

private fun SyncSnapshot.toSyntheticChangeLog(source: String): List<SyncChangeLog> =
    buildList {
        addAll(categories.map { SyncChangeLog(SyncEntityType.CATEGORY, it.id, it.updatedAtEpochMillis, "upsert", source) })
        addAll(members.map { SyncChangeLog(SyncEntityType.MEMBER, it.id, it.updatedAtEpochMillis, "upsert", source) })
        addAll(transactions.map { SyncChangeLog(SyncEntityType.TRANSACTION, it.id, it.updatedAtEpochMillis, "upsert", source) })
        addAll(recurringPayments.map { SyncChangeLog(SyncEntityType.RECURRING, it.id, it.updatedAtEpochMillis, "upsert", source) })
        addAll(invites.map { SyncChangeLog(SyncEntityType.INVITE, it.id, it.updatedAtEpochMillis, "upsert", source) })
        addAll(limits.map { SyncChangeLog(SyncEntityType.LIMIT, it.id, it.updatedAtEpochMillis, "upsert", source) })
    }

private fun com.google.firebase.firestore.DocumentSnapshot.toCategory(): Category? {
    val id = getLong("id") ?: return null
    val name = getString("name") ?: return null
    return Category(
        id = id,
        name = name,
        colorHex = getString("colorHex") ?: "#4CAF50",
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toMember(): FamilyMember? {
    val id = getLong("id") ?: return null
    val name = getString("name") ?: return null
    val email = getString("email") ?: return null
    return FamilyMember(
        id = id,
        name = name,
        email = email,
        role = getString("role") ?: "member",
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toTransaction(): TransactionEntity? {
    val id = getLong("id") ?: return null
    val amount = getDouble("amount") ?: return null
    return TransactionEntity(
        id = id,
        amount = amount,
        note = getString("note") ?: "",
        categoryId = getLong("categoryId"),
        memberId = getLong("memberId"),
        dateEpochMillis = getLong("dateEpochMillis") ?: System.currentTimeMillis(),
        isIncome = getBoolean("isIncome") ?: false,
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toRecurring(): RecurringPayment? {
    val id = getLong("id") ?: return null
    val title = getString("title") ?: return null
    val amount = getDouble("amount") ?: return null
    return RecurringPayment(
        id = id,
        title = title,
        amount = amount,
        dayOfMonth = (getLong("dayOfMonth") ?: 1).toInt(),
        categoryId = getLong("categoryId"),
        active = getBoolean("active") ?: true,
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toInvite(): CollaborationInvite? {
    val id = getLong("id") ?: return null
    val email = getString("email") ?: return null
    val inviterName = getString("inviterName") ?: return null
    return CollaborationInvite(
        id = id,
        email = email,
        inviterName = inviterName,
        role = getString("role") ?: "editor",
        status = getString("status") ?: "pending",
        createdAtEpochMillis = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toLimit(): CategoryLimit? {
    val id = getLong("id") ?: return null
    val categoryId = getLong("categoryId") ?: return null
    val monthlyLimit = getDouble("monthlyLimit") ?: return null
    val monthKey = getString("monthKey") ?: return null
    return CategoryLimit(
        id = id,
        categoryId = categoryId,
        monthlyLimit = monthlyLimit,
        monthKey = monthKey,
        updatedAtEpochMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toChangeLog(): SyncChangeLog? {
    val entityType = getString("entityType") ?: return null
    val entityId = getLong("entityId") ?: return null
    val updatedAtEpochMillis = getLong("updatedAtEpochMillis") ?: return null
    return SyncChangeLog(
        entityType = entityType,
        entityId = entityId,
        updatedAtEpochMillis = updatedAtEpochMillis,
        action = getString("action") ?: "upsert",
        source = getString("source") ?: "remote",
    )
}
