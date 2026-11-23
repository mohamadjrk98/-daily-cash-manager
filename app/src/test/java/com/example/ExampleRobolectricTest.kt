package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExchangeRateFetcher
import com.example.export.ReportExporter
import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.Currency
import com.example.model.ExchangeRates
import com.example.model.HandoverReportData
import com.example.model.NumberFormatters
import com.example.model.OpeningBalances
import com.example.model.PaymentMethod
import com.example.model.TransactionType
import com.example.model.computeBucketStats
import com.example.model.computeEquivalentTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مدير الصندوق والغلة اليومية", appName)
    }

    @Test
    fun `smart currency selector restricts sham cash to SYP and USD only`() {
        assertEquals(
            listOf(Currency.SYP, Currency.TRY, Currency.USD),
            CashBucket.allowedCurrencies(PaymentMethod.CASH),
        )
        assertEquals(
            listOf(Currency.SYP, Currency.USD),
            CashBucket.allowedCurrencies(PaymentMethod.SHAM_CASH),
        )
        assertEquals(
            Currency.SYP,
            CashBucket.coerceCurrency(PaymentMethod.SHAM_CASH, Currency.TRY),
        )
        assertEquals(5, CashBucket.entries.size)
    }

    @Test
    fun `bucket math and whatsapp report cover all five buckets independently`() {
        val opening = OpeningBalances(
            cashSyp = 500_000.0,
            cashTry = 1_000.0,
            cashUsd = 100.0,
            shamCashSyp = 250_000.0,
            shamCashUsd = 50.0,
        )
        val txs = listOf(
            CashTransaction(1, TransactionType.INFLOW, 1_500_000.0, CashBucket.CASH_SYP, "مبيعات", "مبيعات صباحية", 1_700_000_000_000L),
            CashTransaction(2, TransactionType.OUTFLOW, 200_000.0, CashBucket.CASH_SYP, "فاتورة", "كهرباء", 1_700_000_100_000L),
            CashTransaction(3, TransactionType.INFLOW, 300.0, CashBucket.SHAM_CASH_USD, "دفعة زبون", "حوالة زبون", 1_700_000_200_000L),
            CashTransaction(4, TransactionType.OUTFLOW, 250.0, CashBucket.CASH_TRY, "نقل", "شحن", 1_700_000_300_000L),
        )
        val stats = computeBucketStats(opening, txs)
        assertEquals(1_800_000.0, stats.first { it.bucket == CashBucket.CASH_SYP }.netBalance, 0.001)
        assertEquals(750.0, stats.first { it.bucket == CashBucket.CASH_TRY }.netBalance, 0.001)
        assertEquals(100.0, stats.first { it.bucket == CashBucket.CASH_USD }.netBalance, 0.001)
        assertEquals(250_000.0, stats.first { it.bucket == CashBucket.SHAM_CASH_SYP }.netBalance, 0.001)
        assertEquals(350.0, stats.first { it.bucket == CashBucket.SHAM_CASH_USD }.netBalance, 0.001)

        val rates = ExchangeRates(
            usdToSypRate = 14_500.0,
            usdToTryRate = 35.0,
            lastUpdatedLabel = "تعديل يدوي",
        )
        val report = HandoverReportData(
            dateLabel = "الاثنين، 2026/11/23",
            timestampMillis = 1_700_000_300_000L,
            openingBalances = opening,
            bucketStats = stats,
            transactions = txs,
            exchangeRates = rates,
        )
        val whatsappText = ReportExporter.formatForWhatsApp(report)
        assertTrue(whatsappText.contains("كاش - ليرة سورية"))
        assertTrue(whatsappText.contains("كاش - ليرة تركية"))
        assertTrue(whatsappText.contains("كاش - دولار أمريكي"))
        assertTrue(whatsappText.contains("شام كاش - ليرة سورية"))
        assertTrue(whatsappText.contains("شام كاش - دولار أمريكي"))
        assertTrue(whatsappText.contains("إجمالي الصندوق المعادل بالدولار"))
        assertTrue(whatsappText.contains("إجمالي الصندوق المعادل بالليرة السورية"))
        assertTrue(whatsappText.contains("14,500"))
        assertFalse(whatsappText.contains("شام كاش - ليرة تركية"))

        val html = ReportExporter.buildPrintableHtml(report)
        assertTrue(html.contains("إجمالي الصندوق المعادل بالدولار"))
        assertTrue(html.contains("إجمالي الصندوق المعادل بالليرة السورية"))
        assertTrue(html.contains("14,500"))
    }

    @Test
    fun `equivalent totals calculate combined 5-bucket net accurately`() {
        val opening = OpeningBalances(
            cashSyp = 1_450_000.0, // = 100 USD
            cashTry = 350.0,       // = 10 USD
            cashUsd = 100.0,       // = 100 USD
            shamCashSyp = 725_000.0, // = 50 USD
            shamCashUsd = 40.0,    // = 40 USD
        )
        val stats = computeBucketStats(opening, emptyList())
        val rates = ExchangeRates(usdToSypRate = 14_500.0, usdToTryRate = 35.0)
        val eq = computeEquivalentTotals(stats, rates)

        assertEquals(300.0, eq.totalUsd, 0.001)
        assertEquals(4_350_000.0, eq.totalSyp, 0.001)
    }

    @Test
    fun `exchange rate json parser parses valid er-api payload and rejects malformed input`() {
        val sampleJson = """{"result":"success","base_code":"USD","rates":{"USD":1,"SYP":14750.256,"TRY":34.892}}"""
        val parsed = ExchangeRateFetcher.parseRatesFromJson(sampleJson, 1_700_000_000_000L)
        assertNotNull(parsed)
        assertEquals(14750.26, parsed!!.usdToSypRate, 0.001)
        assertEquals(34.89, parsed.usdToTryRate, 0.001)

        assertNull(ExchangeRateFetcher.parseRatesFromJson("""{"rates":{"SYP":0,"TRY":35}}"""))
        assertNull(ExchangeRateFetcher.parseRatesFromJson("invalid json"))
    }

    @Test
    fun `arabic digits normalize and format cleanly`() {
        assertEquals("1250000.5", NumberFormatters.sanitizeNumericInput("١٢٥٠٠٠٠٫٥"))
        assertEquals("1,250,000.5", NumberFormatters.formatAmount(1_250_000.5))
    }
}
