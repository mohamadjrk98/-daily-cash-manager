package com.example.model

enum class PaymentMethod(
    val shortArabic: String,
    val fullArabic: String,
) {
    CASH(shortArabic = "كاش", fullArabic = "كاش (الصندوق)"),
    SHAM_CASH(shortArabic = "شام كاش", fullArabic = "شام كاش"),
}

enum class Currency(
    val symbolArabic: String,
    val fullArabic: String,
    val isoCode: String,
) {
    SYP(symbolArabic = "ل.س", fullArabic = "ليرة سورية", isoCode = "SYP"),
    TRY(symbolArabic = "ل.ت", fullArabic = "ليرة تركية", isoCode = "TRY"),
    USD(symbolArabic = "$", fullArabic = "دولار أمريكي", isoCode = "USD"),
}

/**
 * Closed 5-bucket domain model enforcing the exact supported Method × Currency combinations:
 * - Cash: SYP, TRY, USD
 * - Sham Cash: SYP, USD (never TRY)
 */
enum class CashBucket(
    val method: PaymentMethod,
    val currency: Currency,
    val arabicTitle: String,
    val shortTitle: String,
) {
    CASH_SYP(
        method = PaymentMethod.CASH,
        currency = Currency.SYP,
        arabicTitle = "كاش - ليرة سورية",
        shortTitle = "ليرة سورية",
    ),
    CASH_TRY(
        method = PaymentMethod.CASH,
        currency = Currency.TRY,
        arabicTitle = "كاش - ليرة تركية",
        shortTitle = "ليرة تركية",
    ),
    CASH_USD(
        method = PaymentMethod.CASH,
        currency = Currency.USD,
        arabicTitle = "كاش - دولار أمريكي",
        shortTitle = "دولار أمريكي",
    ),
    SHAM_CASH_SYP(
        method = PaymentMethod.SHAM_CASH,
        currency = Currency.SYP,
        arabicTitle = "شام كاش - ليرة سورية",
        shortTitle = "ليرة سورية",
    ),
    SHAM_CASH_USD(
        method = PaymentMethod.SHAM_CASH,
        currency = Currency.USD,
        arabicTitle = "شام كاش - دولار أمريكي",
        shortTitle = "دولار أمريكي",
    );

    companion object {
        val cashBuckets: List<CashBucket> = listOf(CASH_SYP, CASH_TRY, CASH_USD)
        val shamCashBuckets: List<CashBucket> = listOf(SHAM_CASH_SYP, SHAM_CASH_USD)

        fun allowedCurrencies(method: PaymentMethod): List<Currency> = when (method) {
            PaymentMethod.CASH -> listOf(Currency.SYP, Currency.TRY, Currency.USD)
            PaymentMethod.SHAM_CASH -> listOf(Currency.SYP, Currency.USD)
        }

        fun coerceCurrency(method: PaymentMethod, current: Currency): Currency {
            val allowed = allowedCurrencies(method)
            return if (current in allowed) current else allowed.first()
        }

        fun from(method: PaymentMethod, currency: Currency): CashBucket {
            val safeCurrency = coerceCurrency(method, currency)
            return entries.first { it.method == method && it.currency == safeCurrency }
        }

        fun fromNameOrNull(name: String): CashBucket? =
            entries.firstOrNull { it.name == name }
    }
}

enum class TransactionType(val arabicLabel: String, val fullArabicLabel: String) {
    INFLOW(arabicLabel = "قبض", fullArabicLabel = "قبض (مبيعات / دفعة)"),
    OUTFLOW(arabicLabel = "صرف", fullArabicLabel = "صرف (فاتورة / نثرية)"),
}

enum class LedgerFilter(val arabicLabel: String) {
    ALL("الكل"),
    INFLOWS("المقبوضات"),
    OUTFLOWS("المدفوعات والفواتير"),
}

data class CashTransaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Double,
    val bucket: CashBucket,
    val category: String,
    val note: String,
    val timestampMillis: Long,
) {
    val displayStatement: String
        get() = when {
            note.isNotBlank() && category.isNotBlank() -> "$category — $note"
            note.isNotBlank() -> note
            category.isNotBlank() -> category
            else -> if (type == TransactionType.INFLOW) "قبض نقدي" else "مصروف عام"
        }
}

data class OpeningBalances(
    val cashSyp: Double = 0.0,
    val cashTry: Double = 0.0,
    val cashUsd: Double = 0.0,
    val shamCashSyp: Double = 0.0,
    val shamCashUsd: Double = 0.0,
) {
    fun get(bucket: CashBucket): Double = when (bucket) {
        CashBucket.CASH_SYP -> cashSyp
        CashBucket.CASH_TRY -> cashTry
        CashBucket.CASH_USD -> cashUsd
        CashBucket.SHAM_CASH_SYP -> shamCashSyp
        CashBucket.SHAM_CASH_USD -> shamCashUsd
    }

    fun hasAnyNonZero(): Boolean =
        cashSyp != 0.0 || cashTry != 0.0 || cashUsd != 0.0 || shamCashSyp != 0.0 || shamCashUsd != 0.0

    fun toMap(): Map<CashBucket, Double> = CashBucket.entries.associateWith { get(it) }

    companion object {
        val Zero = OpeningBalances()
    }
}

data class BucketStats(
    val bucket: CashBucket,
    val opening: Double,
    val totalInflow: Double,
    val totalOutflow: Double,
) {
    val netBalance: Double
        get() = opening + totalInflow - totalOutflow
}

data class ExchangeRates(
    val usdToSypRate: Double = DEFAULT_USD_TO_SYP,
    val usdToTryRate: Double = DEFAULT_USD_TO_TRY,
    val lastUpdatedLabel: String = DEFAULT_LABEL,
) {
    companion object {
        const val DEFAULT_USD_TO_SYP = 14500.0
        const val DEFAULT_USD_TO_TRY = 35.0
        const val DEFAULT_LABEL = "الأسعار الافتراضية"
        val Default = ExchangeRates()
    }
}

data class EquivalentTotals(
    val totalUsd: Double,
    val totalSyp: Double,
)

data class HandoverReportData(
    val dateLabel: String,
    val timestampMillis: Long,
    val openingBalances: OpeningBalances,
    val bucketStats: List<BucketStats>,
    val transactions: List<CashTransaction>,
    val exchangeRates: ExchangeRates = ExchangeRates.Default,
    val isArchived: Boolean = false,
    val archivedDayId: Long? = null,
) {
    val outflows: List<CashTransaction>
        get() = transactions.filter { it.type == TransactionType.OUTFLOW }

    val inflows: List<CashTransaction>
        get() = transactions.filter { it.type == TransactionType.INFLOW }

    val equivalentTotals: EquivalentTotals
        get() = computeEquivalentTotals(bucketStats, exchangeRates)

    fun statsFor(bucket: CashBucket): BucketStats =
        bucketStats.first { it.bucket == bucket }
}

data class ArchivedDay(
    val id: Long,
    val dateLabel: String,
    val closedAtMillis: Long,
    val report: HandoverReportData,
)

object QuickCategories {
    val outflowCategories: List<String> = listOf("نثريات", "فاتورة", "نقل", "ضيافة", "سلفة")
    val inflowCategories: List<String> = listOf("مبيعات", "دفعة زبون", "تحصيل", "مرتجع")
}

fun computeBucketStats(
    openingBalances: OpeningBalances,
    transactions: List<CashTransaction>,
): List<BucketStats> {
    return CashBucket.entries.map { bucket ->
        val bucketTx = transactions.filter { it.bucket == bucket }
        val totalIn = bucketTx.filter { it.type == TransactionType.INFLOW }.sumOf { it.amount }
        val totalOut = bucketTx.filter { it.type == TransactionType.OUTFLOW }.sumOf { it.amount }
        BucketStats(
            bucket = bucket,
            opening = openingBalances.get(bucket),
            totalInflow = totalIn,
            totalOutflow = totalOut,
        )
    }
}

fun computeEquivalentTotals(
    bucketStats: List<BucketStats>,
    rates: ExchangeRates,
): EquivalentTotals {
    val totalSypBuckets = bucketStats
        .filter { it.bucket.currency == Currency.SYP }
        .sumOf { it.netBalance }
    val totalTryBuckets = bucketStats
        .filter { it.bucket.currency == Currency.TRY }
        .sumOf { it.netBalance }
    val totalUsdBuckets = bucketStats
        .filter { it.bucket.currency == Currency.USD }
        .sumOf { it.netBalance }

    val safeSypRate = rates.usdToSypRate.takeIf { it > 0.0 } ?: ExchangeRates.DEFAULT_USD_TO_SYP
    val safeTryRate = rates.usdToTryRate.takeIf { it > 0.0 } ?: ExchangeRates.DEFAULT_USD_TO_TRY

    val totalUsd = totalUsdBuckets + (totalSypBuckets / safeSypRate) + (totalTryBuckets / safeTryRate)
    val totalSyp = totalSypBuckets + (totalUsdBuckets * safeSypRate) + ((totalTryBuckets / safeTryRate) * safeSypRate)

    return EquivalentTotals(
        totalUsd = totalUsd,
        totalSyp = totalSyp,
    )
}
