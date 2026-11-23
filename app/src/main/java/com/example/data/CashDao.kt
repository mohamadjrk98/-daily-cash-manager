package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CashDao {
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC, id DESC")
    fun observeActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC, id DESC")
    suspend fun getActiveTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(entities: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM archived_days ORDER BY closedAtMillis DESC, id DESC")
    fun observeArchivedDays(): Flow<List<ArchivedDayEntity>>

    @Query("SELECT * FROM archived_days ORDER BY closedAtMillis DESC, id DESC")
    suspend fun getArchivedDaysOnce(): List<ArchivedDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedDay(entity: ArchivedDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedDays(entities: List<ArchivedDayEntity>)

    @Query("DELETE FROM archived_days WHERE id = :id")
    suspend fun deleteArchivedDayById(id: Long)

    @Query("DELETE FROM archived_days")
    suspend fun clearAllArchivedDays()

    @Transaction
    suspend fun archiveDayAndClearLedger(archivedDay: ArchivedDayEntity): Long {
        val archivedId = insertArchivedDay(archivedDay)
        clearAllTransactions()
        return archivedId
    }

    @Transaction
    suspend fun replaceAllData(
        transactions: List<TransactionEntity>,
        archivedDays: List<ArchivedDayEntity>,
    ) {
        clearAllTransactions()
        clearAllArchivedDays()
        if (transactions.isNotEmpty()) {
            insertTransactions(transactions)
        }
        if (archivedDays.isNotEmpty()) {
            insertArchivedDays(archivedDays)
        }
    }
}
