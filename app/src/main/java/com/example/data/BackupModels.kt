package com.example.data

import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.OpeningBalances
import com.example.model.TransactionType
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpeningBalancesDto(
    val cashSyp: Double = 0.0,
    val cashTry: Double = 0.0,
    val cashUsd: Double = 0.0,
    val shamCashSyp: Double = 0.0,
    val shamCashUsd: Double = 0.0,
) {
    fun toDomain(): OpeningBalances = OpeningBalances(
        cashSyp = cashSyp,
        cashTry = cashTry,
        cashUsd = cashUsd,
        shamCashSyp = shamCashSyp,
        shamCashUsd = shamCashUsd,
    )

    companion object {
        fun fromDomain(balances: OpeningBalances): OpeningBalancesDto = OpeningBalancesDto(
            cashSyp = balances.cashSyp,
            cashTry = balances.cashTry,
            cashUsd = balances.cashUsd,
            shamCashSyp = balances.shamCashSyp,
            shamCashUsd = balances.shamCashUsd,
        )
    }
}

@JsonClass(generateAdapter = true)
data class TransactionDto(
    val id: Long = 0L,
    val type: String,
    val amount: Double,
    val bucket: String,
    val category: String = "",
    val note: String = "",
    val timestampMillis: Long,
) {
    fun toDomainOrNull(): CashTransaction? {
        val parsedType = TransactionType.entries.firstOrNull { it.name == type } ?: return null
        val parsedBucket = CashBucket.fromNameOrNull(bucket) ?: return null
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) return null
        return CashTransaction(
            id = id,
            type = parsedType,
            amount = amount,
            bucket = parsedBucket,
            category = category,
            note = note,
            timestampMillis = timestampMillis,
        )
    }

    companion object {
        fun fromDomain(tx: CashTransaction): TransactionDto = TransactionDto(
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

@JsonClass(generateAdapter = true)
data class ArchivedDaySnapshotDto(
    val dateLabel: String,
    val closedAtMillis: Long,
    val openingBalances: OpeningBalancesDto,
    val transactions: List<TransactionDto>,
)

@JsonClass(generateAdapter = true)
data class CashBackupPayload(
    val schemaVersion: Int = 1,
    val appName: String = "مدير الصندوق والغلة اليومية",
    val exportedAtMillis: Long,
    val openingBalances: OpeningBalancesDto,
    val activeTransactions: List<TransactionDto>,
    val archivedDays: List<ArchivedDaySnapshotDto>,
)
