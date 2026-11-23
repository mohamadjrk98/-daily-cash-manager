package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CashLedgerRepository
import com.example.data.ExchangeRateFetcher
import com.example.data.FetchedExchangeRates
import com.example.model.ArchivedDay
import com.example.model.BucketStats
import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.EquivalentTotals
import com.example.model.ExchangeRates
import com.example.model.HandoverReportData
import com.example.model.LedgerFilter
import com.example.model.NumberFormatters
import com.example.model.OpeningBalances
import com.example.model.TransactionType
import com.example.model.computeBucketStats
import com.example.model.computeEquivalentTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
    TODAY,
    REPORT,
    HISTORY,
    SETTINGS,
}

data class ExchangeRateDialogFeedback(
    val message: String,
    val isError: Boolean,
)

data class CashUiState(
    val isDarkTheme: Boolean = false,
    val todayDateLabel: String = NumberFormatters.formatArabicDate(),
    val openingBalances: OpeningBalances = OpeningBalances.Zero,
    val exchangeRates: ExchangeRates = ExchangeRates.Default,
    val activeTransactions: List<CashTransaction> = emptyList(),
    val bucketStats: List<BucketStats> = computeBucketStats(OpeningBalances.Zero, emptyList()),
    val equivalentTotals: EquivalentTotals = EquivalentTotals(0.0, 0.0),
    val selectedFilter: LedgerFilter = LedgerFilter.ALL,
    val archivedDays: List<ArchivedDay> = emptyList(),
    val currentDestination: AppDestination = AppDestination.TODAY,
    val viewingArchivedDay: ArchivedDay? = null,
    val isOpeningBalanceDialogOpen: Boolean = false,
    val isExchangeRateDialogOpen: Boolean = false,
    val isFetchingRates: Boolean = false,
    val fetchedRatesPreview: FetchedExchangeRates? = null,
    val rateDialogFeedback: ExchangeRateDialogFeedback? = null,
    val isTransactionSheetOpen: Boolean = false,
    val editingTransaction: CashTransaction? = null,
    val pendingDeleteTransaction: CashTransaction? = null,
    val isCloseDayDialogOpen: Boolean = false,
    val snackbarMessage: String? = null,
) {
    val filteredTransactions: List<CashTransaction>
        get() = when (selectedFilter) {
            LedgerFilter.ALL -> activeTransactions
            LedgerFilter.INFLOWS -> activeTransactions.filter { it.type == TransactionType.INFLOW }
            LedgerFilter.OUTFLOWS -> activeTransactions.filter { it.type == TransactionType.OUTFLOW }
        }

    val liveHandoverReport: HandoverReportData
        get() = HandoverReportData(
            dateLabel = todayDateLabel,
            timestampMillis = System.currentTimeMillis(),
            openingBalances = openingBalances,
            bucketStats = bucketStats,
            transactions = activeTransactions,
            exchangeRates = exchangeRates,
            isArchived = false,
            archivedDayId = null,
        )
}

private data class LocalOverlayState(
    val selectedFilter: LedgerFilter = LedgerFilter.ALL,
    val currentDestination: AppDestination = AppDestination.TODAY,
    val viewingArchivedDayId: Long? = null,
    val isOpeningBalanceDialogOpen: Boolean = false,
    val isExchangeRateDialogOpen: Boolean = false,
    val isFetchingRates: Boolean = false,
    val fetchedRatesPreview: FetchedExchangeRates? = null,
    val rateDialogFeedback: ExchangeRateDialogFeedback? = null,
    val isTransactionSheetOpen: Boolean = false,
    val editingTransaction: CashTransaction? = null,
    val pendingDeleteTransaction: CashTransaction? = null,
    val isCloseDayDialogOpen: Boolean = false,
    val snackbarMessage: String? = null,
)

private data class PersistedPrefs(
    val isDarkTheme: Boolean,
    val openingBalances: OpeningBalances,
    val exchangeRates: ExchangeRates,
)

class CashManagerViewModel(
    private val repository: CashLedgerRepository,
) : ViewModel() {

    private val overlay = MutableStateFlow(LocalOverlayState())

    private val persistedPrefsFlow = combine(
        repository.isDarkTheme,
        repository.openingBalances,
        repository.exchangeRates,
    ) { isDark, opening, rates ->
        PersistedPrefs(isDark, opening, rates)
    }

    val uiState: StateFlow<CashUiState> = combine(
        persistedPrefsFlow,
        repository.activeTransactions,
        repository.archivedDays,
        overlay,
    ) { prefs, transactions, archived, local ->
        val stats = computeBucketStats(prefs.openingBalances, transactions)
        val equivalents = computeEquivalentTotals(stats, prefs.exchangeRates)
        val viewingDay = local.viewingArchivedDayId?.let { id ->
            archived.firstOrNull { it.id == id }
        }
        CashUiState(
            isDarkTheme = prefs.isDarkTheme,
            todayDateLabel = NumberFormatters.formatArabicDate(),
            openingBalances = prefs.openingBalances,
            exchangeRates = prefs.exchangeRates,
            activeTransactions = transactions,
            bucketStats = stats,
            equivalentTotals = equivalents,
            selectedFilter = local.selectedFilter,
            archivedDays = archived,
            currentDestination = local.currentDestination,
            viewingArchivedDay = viewingDay,
            isOpeningBalanceDialogOpen = local.isOpeningBalanceDialogOpen,
            isExchangeRateDialogOpen = local.isExchangeRateDialogOpen,
            isFetchingRates = local.isFetchingRates,
            fetchedRatesPreview = local.fetchedRatesPreview,
            rateDialogFeedback = local.rateDialogFeedback,
            isTransactionSheetOpen = local.isTransactionSheetOpen,
            editingTransaction = local.editingTransaction,
            pendingDeleteTransaction = local.pendingDeleteTransaction,
            isCloseDayDialogOpen = local.isCloseDayDialogOpen,
            snackbarMessage = local.snackbarMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CashUiState(),
    )

    fun openExchangeRateDialog() {
        overlay.value = overlay.value.copy(
            isExchangeRateDialogOpen = true,
            isFetchingRates = false,
            fetchedRatesPreview = null,
            rateDialogFeedback = null,
        )
    }

    fun dismissExchangeRateDialog() {
        overlay.value = overlay.value.copy(
            isExchangeRateDialogOpen = false,
            isFetchingRates = false,
            fetchedRatesPreview = null,
            rateDialogFeedback = null,
        )
    }

    fun fetchLiveExchangeRates() {
        if (overlay.value.isFetchingRates) return
        overlay.value = overlay.value.copy(
            isFetchingRates = true,
            rateDialogFeedback = null,
        )
        viewModelScope.launch {
            val result = ExchangeRateFetcher.fetchLatestUsdRates()
            result.fold(
                onSuccess = { fetched ->
                    overlay.value = overlay.value.copy(
                        isFetchingRates = false,
                        fetchedRatesPreview = fetched,
                        rateDialogFeedback = ExchangeRateDialogFeedback(
                            message = "تم تحديث الأسعار من الإنترنت — اضغط «حفظ الأسعار» للاعتماد",
                            isError = false,
                        ),
                        snackbarMessage = "تم جلب أسعار الصرف بنجاح",
                    )
                },
                onFailure = { error ->
                    val msg = error.message ?: ExchangeRateFetcher.OFFLINE_ERROR_MESSAGE
                    overlay.value = overlay.value.copy(
                        isFetchingRates = false,
                        rateDialogFeedback = ExchangeRateDialogFeedback(
                            message = msg,
                            isError = true,
                        ),
                        snackbarMessage = msg,
                    )
                },
            )
        }
    }

    fun saveExchangeRates(
        usdToSyp: Double,
        usdToTry: Double,
        updatedLabel: String,
    ) {
        viewModelScope.launch {
            repository.saveExchangeRates(
                ExchangeRates(
                    usdToSypRate = usdToSyp,
                    usdToTryRate = usdToTry,
                    lastUpdatedLabel = updatedLabel,
                )
            )
            overlay.value = overlay.value.copy(
                isExchangeRateDialogOpen = false,
                isFetchingRates = false,
                fetchedRatesPreview = null,
                rateDialogFeedback = null,
                snackbarMessage = "تم حفظ أسعار الصرف وتحديث الإجمالي المعادل",
            )
        }
    }

    fun selectDestination(destination: AppDestination) {
        overlay.value = overlay.value.copy(
            currentDestination = destination,
            viewingArchivedDayId = null,
        )
    }

    fun selectFilter(filter: LedgerFilter) {
        overlay.value = overlay.value.copy(selectedFilter = filter)
    }

    fun toggleDarkTheme() {
        val next = !uiState.value.isDarkTheme
        viewModelScope.launch {
            repository.setDarkTheme(next)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(enabled)
        }
    }

    fun openOpeningBalanceDialog() {
        overlay.value = overlay.value.copy(isOpeningBalanceDialogOpen = true)
    }

    fun dismissOpeningBalanceDialog() {
        overlay.value = overlay.value.copy(isOpeningBalanceDialogOpen = false)
    }

    fun saveOpeningBalances(balances: OpeningBalances) {
        viewModelScope.launch {
            repository.saveOpeningBalances(balances)
            overlay.value = overlay.value.copy(
                isOpeningBalanceDialogOpen = false,
                snackbarMessage = "تم حفظ الأرصدة الافتتاحية بنجاح",
            )
        }
    }

    fun openNewTransactionSheet() {
        overlay.value = overlay.value.copy(
            isTransactionSheetOpen = true,
            editingTransaction = null,
        )
    }

    fun openEditTransactionSheet(tx: CashTransaction) {
        overlay.value = overlay.value.copy(
            isTransactionSheetOpen = true,
            editingTransaction = tx,
        )
    }

    fun dismissTransactionSheet() {
        overlay.value = overlay.value.copy(
            isTransactionSheetOpen = false,
            editingTransaction = null,
        )
    }

    fun saveTransaction(
        existingId: Long?,
        type: TransactionType,
        amount: Double,
        bucket: CashBucket,
        category: String,
        note: String,
        existingTimestamp: Long?,
    ) {
        viewModelScope.launch {
            if (existingId != null && existingId > 0L) {
                repository.updateTransaction(
                    CashTransaction(
                        id = existingId,
                        type = type,
                        amount = amount,
                        bucket = bucket,
                        category = category.trim(),
                        note = note.trim(),
                        timestampMillis = existingTimestamp ?: System.currentTimeMillis(),
                    )
                )
                overlay.value = overlay.value.copy(
                    isTransactionSheetOpen = false,
                    editingTransaction = null,
                    snackbarMessage = "تم تحديث الحركة بنجاح",
                )
            } else {
                repository.addTransaction(
                    CashTransaction(
                        id = 0L,
                        type = type,
                        amount = amount,
                        bucket = bucket,
                        category = category.trim(),
                        note = note.trim(),
                        timestampMillis = System.currentTimeMillis(),
                    )
                )
                overlay.value = overlay.value.copy(
                    isTransactionSheetOpen = false,
                    editingTransaction = null,
                    snackbarMessage = if (type == TransactionType.INFLOW) {
                        "تم تسجيل حركة القبض بنجاح"
                    } else {
                        "تم تسجيل حركة الصرف بنجاح"
                    },
                )
            }
        }
    }

    fun requestDeleteTransaction(tx: CashTransaction) {
        overlay.value = overlay.value.copy(pendingDeleteTransaction = tx)
    }

    fun dismissDeleteTransaction() {
        overlay.value = overlay.value.copy(pendingDeleteTransaction = null)
    }

    fun confirmDeleteTransaction() {
        val tx = overlay.value.pendingDeleteTransaction ?: return
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            overlay.value = overlay.value.copy(
                pendingDeleteTransaction = null,
                snackbarMessage = "تم حذف الحركة وتحديث الأرصدة",
            )
        }
    }

    fun requestCloseDay() {
        overlay.value = overlay.value.copy(isCloseDayDialogOpen = true)
    }

    fun dismissCloseDay() {
        overlay.value = overlay.value.copy(isCloseDayDialogOpen = false)
    }

    fun confirmCloseAndArchiveDay() {
        viewModelScope.launch {
            repository.closeAndArchiveDay()
            overlay.value = overlay.value.copy(
                isCloseDayDialogOpen = false,
                currentDestination = AppDestination.HISTORY,
                viewingArchivedDayId = null,
                selectedFilter = LedgerFilter.ALL,
                snackbarMessage = "تم إغلاق اليوم وأرشفته في سجل الأيام السابقة وتصفير الصندوق",
            )
        }
    }

    fun openArchivedDay(day: ArchivedDay) {
        overlay.value = overlay.value.copy(viewingArchivedDayId = day.id)
    }

    fun closeArchivedDayDetail() {
        overlay.value = overlay.value.copy(viewingArchivedDayId = null)
    }

    fun deleteArchivedDay(dayId: Long) {
        viewModelScope.launch {
            repository.deleteArchivedDay(dayId)
            overlay.value = overlay.value.copy(
                viewingArchivedDayId = if (overlay.value.viewingArchivedDayId == dayId) null else overlay.value.viewingArchivedDayId,
                snackbarMessage = "تم حذف التقرير المؤرشف",
            )
        }
    }

    suspend fun generateBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(rawJson)
            result.fold(
                onSuccess = { summary ->
                    overlay.value = overlay.value.copy(
                        snackbarMessage = "تم الاستيراد بنجاح (${summary.activeTransactionsCount} حركة نشطة و ${summary.archivedDaysCount} يوم مؤرشف)",
                    )
                },
                onFailure = { error ->
                    overlay.value = overlay.value.copy(
                        snackbarMessage = error.message ?: "تعذر قراءة ملف النسخة الاحتياطية",
                    )
                },
            )
        }
    }

    fun showMessage(message: String) {
        overlay.value = overlay.value.copy(snackbarMessage = message)
    }

    fun consumeSnackbar() {
        overlay.value = overlay.value.copy(snackbarMessage = null)
    }

    class Factory(private val repository: CashLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashManagerViewModel(repository) as T
        }
    }
}
