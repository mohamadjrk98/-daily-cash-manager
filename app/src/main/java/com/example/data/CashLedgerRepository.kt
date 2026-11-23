package com.example.data

import com.example.model.ArchivedDay
import com.example.model.CashTransaction
import com.example.model.ExchangeRates
import com.example.model.HandoverReportData
import com.example.model.NumberFormatters
import com.example.model.OpeningBalances
import com.example.model.computeBucketStats
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CashLedgerRepository(
    private val cashDao: CashDao,
    private val preferencesStore: CashPreferencesStore,
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val snapshotAdapter = moshi.adapter(ArchivedDaySnapshotDto::class.java)
    private val backupAdapter = moshi.adapter(CashBackupPayload::class.java).indent("  ")

    val isDarkTheme: Flow<Boolean> = preferencesStore.isDarkTheme

    val openingBalances: Flow<OpeningBalances> = preferencesStore.openingBalances

    val exchangeRates: Flow<ExchangeRates> = preferencesStore.exchangeRates

    val activeTransactions: Flow<List<CashTransaction>> =
        cashDao.observeActiveTransactions().map { list -> list.map { it.toDomain() } }

    val archivedDays: Flow<List<ArchivedDay>> =
        cashDao.observeArchivedDays().map { entities ->
            entities.mapNotNull { entity -> entity.toArchivedDayOrNull() }
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        preferencesStore.setDarkTheme(enabled)
    }

    suspend fun saveOpeningBalances(balances: OpeningBalances) {
        preferencesStore.saveOpeningBalances(balances)
    }

    suspend fun saveExchangeRates(rates: ExchangeRates) {
        preferencesStore.saveExchangeRates(rates)
    }

    suspend fun addTransaction(tx: CashTransaction) {
        cashDao.insertTransaction(TransactionEntity.fromDomain(tx.copy(id = 0L)))
    }

    suspend fun updateTransaction(tx: CashTransaction) {
        cashDao.updateTransaction(TransactionEntity.fromDomain(tx))
    }

    suspend fun deleteTransaction(id: Long) {
        cashDao.deleteTransactionById(id)
    }

    suspend fun closeAndArchiveDay(nowMillis: Long = System.currentTimeMillis()): Long {
        val opening = preferencesStore.getOpeningBalancesOnce()
        val rates = preferencesStore.getExchangeRatesOnce()
        val transactions = cashDao.getActiveTransactionsOnce().map { it.toDomain() }
        val dateLabel = NumberFormatters.formatArabicDate(nowMillis)
        val snapshot = ArchivedDaySnapshotDto(
            dateLabel = dateLabel,
            closedAtMillis = nowMillis,
            openingBalances = OpeningBalancesDto.fromDomain(opening),
            transactions = transactions.map { TransactionDto.fromDomain(it) },
            exchangeRates = ExchangeRatesDto.fromDomain(rates),
        )
        val json = withContext(Dispatchers.Default) { snapshotAdapter.toJson(snapshot) }
        val entity = ArchivedDayEntity(
            dateLabel = dateLabel,
            closedAtMillis = nowMillis,
            snapshotJson = json,
        )
        val archivedId = cashDao.archiveDayAndClearLedger(entity)
        preferencesStore.resetOpeningBalances()
        return archivedId
    }

    suspend fun deleteArchivedDay(id: Long) {
        cashDao.deleteArchivedDayById(id)
    }

    suspend fun exportBackupJson(nowMillis: Long = System.currentTimeMillis()): String =
        withContext(Dispatchers.IO) {
            val opening = preferencesStore.getOpeningBalancesOnce()
            val rates = preferencesStore.getExchangeRatesOnce()
            val active = cashDao.getActiveTransactionsOnce().map {
                TransactionDto.fromDomain(it.toDomain())
            }
            val archived = cashDao.getArchivedDaysOnce().mapNotNull { entity ->
                runCatching { snapshotAdapter.fromJson(entity.snapshotJson) }.getOrNull()
            }
            val payload = CashBackupPayload(
                schemaVersion = 1,
                exportedAtMillis = nowMillis,
                openingBalances = OpeningBalancesDto.fromDomain(opening),
                activeTransactions = active,
                archivedDays = archived,
                exchangeRates = ExchangeRatesDto.fromDomain(rates),
            )
            backupAdapter.toJson(payload)
        }

    suspend fun importBackupJson(rawJson: String): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = backupAdapter.fromJson(rawJson)
                    ?: throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو فارغ")
                if (payload.schemaVersion < 1) {
                    throw IllegalArgumentException("إصدار النسخة الاحتياطية غير مدعوم")
                }

                val validActive = payload.activeTransactions.mapNotNull { dto ->
                    dto.toDomainOrNull()?.let { TransactionEntity.fromDomain(it.copy(id = 0L)) }
                }

                val validArchived = payload.archivedDays.map { snapshot ->
                    val cleanTransactions = snapshot.transactions.mapNotNull { it.toDomainOrNull() }
                    val cleanSnapshot = snapshot.copy(
                        transactions = cleanTransactions.map { TransactionDto.fromDomain(it) }
                    )
                    ArchivedDayEntity(
                        dateLabel = cleanSnapshot.dateLabel.ifBlank {
                            NumberFormatters.formatArabicDate(cleanSnapshot.closedAtMillis)
                        },
                        closedAtMillis = cleanSnapshot.closedAtMillis,
                        snapshotJson = snapshotAdapter.toJson(cleanSnapshot),
                    )
                }

                cashDao.replaceAllData(
                    transactions = validActive,
                    archivedDays = validArchived,
                )
                preferencesStore.saveOpeningBalances(payload.openingBalances.toDomain())
                payload.exchangeRates?.let { ratesDto ->
                    preferencesStore.saveExchangeRates(ratesDto.toDomain())
                }

                ImportSummary(
                    activeTransactionsCount = validActive.size,
                    archivedDaysCount = validArchived.size,
                )
            }
        }

    private fun ArchivedDayEntity.toArchivedDayOrNull(): ArchivedDay? {
        val snapshot = runCatching { snapshotAdapter.fromJson(snapshotJson) }.getOrNull()
            ?: return null
        val opening = snapshot.openingBalances.toDomain()
        val txs = snapshot.transactions.mapNotNull { it.toDomainOrNull() }
        val stats = computeBucketStats(opening, txs)
        val rates = snapshot.exchangeRates?.toDomain() ?: ExchangeRates.Default
        return ArchivedDay(
            id = id,
            dateLabel = dateLabel,
            closedAtMillis = closedAtMillis,
            report = HandoverReportData(
                dateLabel = dateLabel,
                timestampMillis = closedAtMillis,
                openingBalances = opening,
                bucketStats = stats,
                transactions = txs,
                exchangeRates = rates,
                isArchived = true,
                archivedDayId = id,
            ),
        )
    }
}

data class ImportSummary(
    val activeTransactionsCount: Int,
    val archivedDaysCount: Int,
)
