package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val amount: Double,
    val bucket: String,
    val category: String,
    val note: String,
    val timestampMillis: Long,
) {
    fun toDomain(): CashTransaction = CashTransaction(
        id = id,
        type = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.INFLOW,
        amount = amount,
        bucket = CashBucket.fromNameOrNull(bucket) ?: CashBucket.CASH_SYP,
        category = category,
        note = note,
        timestampMillis = timestampMillis,
    )

    companion object {
        fun fromDomain(tx: CashTransaction): TransactionEntity = TransactionEntity(
            id = tx.id,
            type = tx.type.name,
            amount = tx.amount,
            bucket = tx.bucket.name,
            category = tx.category,
            note = tx.note,
            timestampMillis = tx.timestampMillis,
        )
    }
}

@Entity(tableName = "archived_days")
data class ArchivedDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateLabel: String,
    val closedAtMillis: Long,
    val snapshotJson: String,
)
