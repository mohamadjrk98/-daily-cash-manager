package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.ReportExporter
import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.Currency
import com.example.model.HandoverReportData
import com.example.model.NumberFormatters
import com.example.model.OpeningBalances
import com.example.model.PaymentMethod
import com.example.model.TransactionType
import com.example.model.computeBucketStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val report = HandoverReportData(
            dateLabel = "الاثنين، 2026/11/23",
            timestampMillis = 1_700_000_300_000L,
            openingBalances = opening,
            bucketStats = stats,
            transactions = txs,
        )
        val whatsappText = ReportExporter.formatForWhatsApp(report)
        assertTrue(whatsappText.contains("كاش - ليرة سورية"))
        assertTrue(whatsappText.contains("كاش - ليرة تركية"))
        assertTrue(whatsappText.contains("كاش - دولار أمريكي"))
        assertTrue(whatsappText.contains("شام كاش - ليرة سورية"))
        assertTrue(whatsappText.contains("شام كاش - دولار أمريكي"))
        assertFalse(whatsappText.contains("شام كاش - ليرة تركية"))
    }

    @Test
    fun `arabic digits normalize and format cleanly`() {
        assertEquals("1250000.5", NumberFormatters.sanitizeNumericInput("١٢٥٠٠٠٠٫٥"))
        assertEquals("1,250,000.5", NumberFormatters.formatAmount(1_250_000.5))
    }
}
