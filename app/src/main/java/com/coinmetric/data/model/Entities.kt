package com.coinmetric.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val role: String,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class CategoryLimit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Double,
    val monthKey: String,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("memberId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val note: String,
    val categoryId: Long?,
    val memberId: Long?,
    val dateEpochMillis: Long,
    val isIncome: Boolean,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity
data class RecurringPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val dayOfMonth: Int,
    val categoryId: Long?,
    val active: Boolean = true,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity
data class CollaborationInvite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val inviterName: String,
    val role: String = "editor",
    val status: String = "pending",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)
