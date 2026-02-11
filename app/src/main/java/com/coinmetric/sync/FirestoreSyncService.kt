package com.coinmetric.sync

import com.coinmetric.data.model.Category
import com.coinmetric.data.model.CategoryLimit
import com.coinmetric.data.model.CollaborationInvite
import com.coinmetric.data.model.FamilyMember
import com.coinmetric.data.model.RecurringPayment
import com.coinmetric.data.model.TransactionEntity
import com.coinmetric.data.repository.SyncSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun pushSnapshot(accountEmail: String, snapshot: SyncSnapshot) {
        val root = firestore.collection("budgets").document(accountEmail)
        uploadCollection(root.collection("categories"), snapshot.categories.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf("id" to v.id, "name" to v.name, "colorHex" to v.colorHex, "updatedAt" to v.updatedAtEpochMillis)
        })
        uploadCollection(root.collection("members"), snapshot.members.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf("id" to v.id, "name" to v.name, "email" to v.email, "role" to v.role, "updatedAt" to v.updatedAtEpochMillis)
        })
        uploadCollection(root.collection("transactions"), snapshot.transactions.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf(
                "id" to v.id,
                "amount" to v.amount,
                "note" to v.note,
                "categoryId" to v.categoryId,
                "memberId" to v.memberId,
                "dateEpochMillis" to v.dateEpochMillis,
                "isIncome" to v.isIncome,
                "updatedAt" to v.updatedAtEpochMillis,
            )
        })
        uploadCollection(root.collection("recurring"), snapshot.recurringPayments.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf(
                "id" to v.id,
                "title" to v.title,
                "amount" to v.amount,
                "dayOfMonth" to v.dayOfMonth,
                "categoryId" to v.categoryId,
                "active" to v.active,
                "updatedAt" to v.updatedAtEpochMillis,
            )
        })
        uploadCollection(root.collection("invites"), snapshot.invites.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf(
                "id" to v.id,
                "email" to v.email,
                "inviterName" to v.inviterName,
                "role" to v.role,
                "status" to v.status,
                "createdAt" to v.createdAtEpochMillis,
                "updatedAt" to v.updatedAtEpochMillis,
            )
        })
        uploadCollection(root.collection("limits"), snapshot.limits.associateBy { it.id.toString() }.mapValues { (_, v) ->
            mapOf(
                "id" to v.id,
                "categoryId" to v.categoryId,
                "monthlyLimit" to v.monthlyLimit,
                "monthKey" to v.monthKey,
                "updatedAt" to v.updatedAtEpochMillis,
            )
        })
    }

    suspend fun pullSnapshot(accountEmail: String): SyncSnapshot {
        val root = firestore.collection("budgets").document(accountEmail)
        val categories = root.collection("categories").get().await().documents.mapNotNull { doc ->
            Category(
                id = doc.getLong("id") ?: return@mapNotNull null,
                name = doc.getString("name") ?: return@mapNotNull null,
                colorHex = doc.getString("colorHex") ?: "#4CAF50",
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }
        val members = root.collection("members").get().await().documents.mapNotNull { doc ->
            FamilyMember(
                id = doc.getLong("id") ?: return@mapNotNull null,
                name = doc.getString("name") ?: return@mapNotNull null,
                email = doc.getString("email") ?: return@mapNotNull null,
                role = doc.getString("role") ?: "member",
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }
        val transactions = root.collection("transactions").get().await().documents.mapNotNull { doc ->
            TransactionEntity(
                id = doc.getLong("id") ?: return@mapNotNull null,
                amount = doc.getDouble("amount") ?: return@mapNotNull null,
                note = doc.getString("note") ?: "",
                categoryId = doc.getLong("categoryId"),
                memberId = doc.getLong("memberId"),
                dateEpochMillis = doc.getLong("dateEpochMillis") ?: System.currentTimeMillis(),
                isIncome = doc.getBoolean("isIncome") ?: false,
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }
        val recurring = root.collection("recurring").get().await().documents.mapNotNull { doc ->
            RecurringPayment(
                id = doc.getLong("id") ?: return@mapNotNull null,
                title = doc.getString("title") ?: return@mapNotNull null,
                amount = doc.getDouble("amount") ?: return@mapNotNull null,
                dayOfMonth = (doc.getLong("dayOfMonth") ?: 1).toInt(),
                categoryId = doc.getLong("categoryId"),
                active = doc.getBoolean("active") ?: true,
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }
        val invites = root.collection("invites").get().await().documents.mapNotNull { doc ->
            CollaborationInvite(
                id = doc.getLong("id") ?: return@mapNotNull null,
                email = doc.getString("email") ?: return@mapNotNull null,
                inviterName = doc.getString("inviterName") ?: return@mapNotNull null,
                role = doc.getString("role") ?: "editor",
                status = doc.getString("status") ?: "pending",
                createdAtEpochMillis = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }
        val limits = root.collection("limits").get().await().documents.mapNotNull { doc ->
            CategoryLimit(
                id = doc.getLong("id") ?: return@mapNotNull null,
                categoryId = doc.getLong("categoryId") ?: return@mapNotNull null,
                monthlyLimit = doc.getDouble("monthlyLimit") ?: return@mapNotNull null,
                monthKey = doc.getString("monthKey") ?: return@mapNotNull null,
                updatedAtEpochMillis = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
            )
        }

        return SyncSnapshot(categories, members, transactions, recurring, invites, limits)
    }

    private suspend fun uploadCollection(
        collection: com.google.firebase.firestore.CollectionReference,
        docs: Map<String, Map<String, Any?>>,
    ) {
        docs.forEach { (id, body) ->
            collection.document(id).set(body).await()
        }
    }
}
