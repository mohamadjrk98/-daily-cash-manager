package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.CashBucket
import com.example.model.HandoverReportData
import com.example.model.NumberFormatters

object ReportExporter {

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("تقرير تسليم الغلة اليومية", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تقرير تسليم الغلة اليومية")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير تسليم الغلة"))
    }

    fun formatForWhatsApp(report: HandoverReportData): String {
        val sb = StringBuilder()
        sb.appendLine("📊 *تقرير تسليم الغلة اليومية*")
        sb.appendLine("📅 *التاريخ:* ${report.dateLabel}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        if (report.openingBalances.hasAnyNonZero()) {
            sb.appendLine("🏁 *الرصيد الافتتاحي:*")
            for (bucket in CashBucket.entries) {
                val opening = report.openingBalances.get(bucket)
                if (opening != 0.0) {
                    sb.appendLine("▫️ ${bucket.arabicTitle}: ${NumberFormatters.formatWithCurrency(opening, bucket.currency)}")
                }
            }
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        }

        // Section A: Total Inflows
        sb.appendLine("🟢 *القسم أ: إجمالي المقبوضات*")
        var anyInflow = false
        for (stat in report.bucketStats) {
            if (stat.totalInflow > 0.0) {
                anyInflow = true
                sb.appendLine("• ${stat.bucket.arabicTitle}: *${NumberFormatters.formatWithCurrency(stat.totalInflow, stat.bucket.currency)}*")
            }
        }
        if (!anyInflow) {
            sb.appendLine("• لا توجد مقبوضات مسجلة")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        // Section B: Paid Bills & Petty Cash
        sb.appendLine("🔴 *القسم ب: الفواتير والنثريات المدفوعة*")
        val outflows = report.outflows
        if (outflows.isEmpty()) {
            sb.appendLine("• لا توجد مصروفات أو فواتير مسجلة")
        } else {
            outflows.forEachIndexed { idx, tx ->
                val amountText = NumberFormatters.formatWithCurrency(tx.amount, tx.bucket.currency)
                sb.appendLine("${idx + 1}. ${tx.displayStatement} — $amountText (${tx.bucket.method.shortArabic})")
            }
            sb.appendLine()
            sb.appendLine("📌 *إجمالي المصروفات حسب الصندوق:*")
            for (stat in report.bucketStats) {
                if (stat.totalOutflow > 0.0) {
                    sb.appendLine("• ${stat.bucket.arabicTitle}: ${NumberFormatters.formatWithCurrency(stat.totalOutflow, stat.bucket.currency)}")
                }
            }
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        // Section C: Net Cash to Hand Over (all 5 buckets explicitly highlighted)
        sb.appendLine("💰 *القسم ج: صافي الغلة المطلوب تسليمها*")
        for (stat in report.bucketStats) {
            val icon = if (stat.bucket.method == com.example.model.PaymentMethod.CASH) "💵" else "📱"
            sb.appendLine("$icon ${stat.bucket.arabicTitle}: *${NumberFormatters.formatWithCurrency(stat.netBalance, stat.bucket.currency)}*")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.append("✍️ *تم الإعداد عبر مدير الصندوق والغلة اليومية*")
        return sb.toString()
    }

    fun printOrSavePdf(context: Context, report: HandoverReportData) {
        val html = buildPrintableHtml(report)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean = true

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    ?: return
                val jobName = "تقرير_تسليم_الغلة_${NumberFormatters.formatFileDate(report.timestampMillis)}"
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build(),
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "UTF-8", null)
    }

    fun buildPrintableHtml(report: HandoverReportData): String {
        val escapedDate = escapeHtml(report.dateLabel)
        val hasOpening = report.openingBalances.hasAnyNonZero()

        val openingRows = if (hasOpening) {
            buildString {
                append("""<section class="card"><h3>الرصيد الافتتاحي لليوم</h3><div class="grid-5">""")
                for (stat in report.bucketStats) {
                    append(
                        """<div class="mini-box">
                            <span class="label">${escapeHtml(stat.bucket.arabicTitle)}</span>
                            <strong class="num">${escapeHtml(NumberFormatters.formatWithCurrency(stat.opening, stat.bucket.currency))}</strong>
                        </div>"""
                    )
                }
                append("</div></section>")
            }
        } else {
            ""
        }

        val inflowRows = buildString {
            for (stat in report.bucketStats) {
                append(
                    """<tr>
                        <td>${escapeHtml(stat.bucket.arabicTitle)}</td>
                        <td>${escapeHtml(stat.bucket.method.shortArabic)}</td>
                        <td>${escapeHtml(stat.bucket.currency.fullArabic)}</td>
                        <td class="num inflow">${escapeHtml(NumberFormatters.formatWithCurrency(stat.totalInflow, stat.bucket.currency))}</td>
                    </tr>"""
                )
            }
        }

        val outflowItemRows = if (report.outflows.isEmpty()) {
            """<tr><td colspan="4" class="empty">لا توجد فواتير أو نثريات مدفوعة خلال هذا اليوم</td></tr>"""
        } else {
            buildString {
                for (tx in report.outflows) {
                    append(
                        """<tr>
                            <td>${escapeHtml(tx.displayStatement)} <span class="time">(${escapeHtml(NumberFormatters.formatTimeArabic(tx.timestampMillis))})</span></td>
                            <td class="num outflow">${escapeHtml(NumberFormatters.formatAmount(tx.amount))}</td>
                            <td>${escapeHtml(tx.bucket.currency.symbolArabic)}</td>
                            <td>${escapeHtml(tx.bucket.method.shortArabic)}</td>
                        </tr>"""
                    )
                }
            }
        }

        val outflowSummaryPills = buildString {
            for (stat in report.bucketStats) {
                append(
                    """<div class="mini-box">
                        <span class="label">${escapeHtml(stat.bucket.arabicTitle)}</span>
                        <strong class="num outflow">${escapeHtml(NumberFormatters.formatWithCurrency(stat.totalOutflow, stat.bucket.currency))}</strong>
                    </div>"""
                )
            }
        }

        val netCards = buildString {
            for (stat in report.bucketStats) {
                val badgeClass = if (stat.bucket.method == com.example.model.PaymentMethod.CASH) "badge-cash" else "badge-sham"
                append(
                    """<div class="net-box">
                        <span class="badge $badgeClass">${escapeHtml(stat.bucket.method.shortArabic)}</span>
                        <div class="net-title">${escapeHtml(stat.bucket.arabicTitle)}</div>
                        <div class="net-amount num">${escapeHtml(NumberFormatters.formatWithCurrency(stat.netBalance, stat.bucket.currency))}</div>
                    </div>"""
                )
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8" />
                <title>تقرير تسليم الغلة اليومية</title>
                <style>
                    @page { size: A4; margin: 14mm; }
                    * { box-sizing: border-box; }
                    body {
                        font-family: 'Cairo', 'Tajawal', 'Segoe UI', Tahoma, sans-serif;
                        color: #0F172A;
                        background: #FFFFFF;
                        margin: 0;
                        padding: 16px;
                        direction: rtl;
                        line-height: 1.5;
                    }
                    header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid #0F2942;
                        padding-bottom: 12px;
                        margin-bottom: 18px;
                    }
                    header h1 {
                        margin: 0;
                        font-size: 20px;
                        color: #0F2942;
                    }
                    header .sub {
                        color: #475569;
                        font-size: 13px;
                        margin-top: 2px;
                    }
                    header .date-box {
                        background: #F1F5F9;
                        border: 1px solid #CBD5E1;
                        border-radius: 8px;
                        padding: 8px 14px;
                        font-weight: 700;
                        font-size: 13px;
                        color: #0F2942;
                    }
                    section.card {
                        border: 1px solid #E2E8F0;
                        border-radius: 10px;
                        padding: 14px;
                        margin-bottom: 16px;
                        page-break-inside: avoid;
                    }
                    h2, h3 {
                        margin: 0 0 10px 0;
                        font-size: 15px;
                        color: #0F2942;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 12.5px;
                    }
                    th, td {
                        border: 1px solid #CBD5E1;
                        padding: 7px 10px;
                        text-align: right;
                    }
                    th {
                        background: #F8FAFC;
                        color: #334155;
                        font-weight: 700;
                    }
                    .num {
                        font-variant-numeric: tabular-nums;
                        direction: ltr;
                        unicode-bidi: isolate;
                        font-weight: 700;
                    }
                    .inflow { color: #059669; }
                    .outflow { color: #E11D48; }
                    .empty { text-align: center; color: #64748B; padding: 14px; }
                    .time { color: #64748B; font-size: 11px; }
                    .grid-5 {
                        display: grid;
                        grid-template-columns: repeat(3, 1fr);
                        gap: 8px;
                        margin-top: 8px;
                    }
                    .mini-box {
                        background: #F8FAFC;
                        border: 1px solid #E2E8F0;
                        border-radius: 8px;
                        padding: 8px 10px;
                        display: flex;
                        flex-direction: column;
                        gap: 4px;
                    }
                    .mini-box .label { font-size: 11.5px; color: #475569; }
                    .net-grid {
                        display: grid;
                        grid-template-columns: repeat(2, 1fr);
                        gap: 10px;
                    }
                    .net-box {
                        border: 1.5px solid #0F2942;
                        background: #F8FAFC;
                        border-radius: 10px;
                        padding: 11px 13px;
                    }
                    .net-title { font-size: 12.5px; color: #334155; margin-top: 4px; }
                    .net-amount { font-size: 18px; color: #0F2942; margin-top: 4px; }
                    .badge {
                        display: inline-block;
                        font-size: 10.5px;
                        font-weight: 700;
                        padding: 2px 8px;
                        border-radius: 99px;
                    }
                    .badge-cash { background: #FEF3C7; color: #92400E; }
                    .badge-sham { background: #DBEAFE; color: #1E40AF; }
                    .signatures {
                        display: flex;
                        justify-content: space-between;
                        gap: 32px;
                        margin-top: 34px;
                        page-break-inside: avoid;
                    }
                    .sig-box {
                        flex: 1;
                        text-align: center;
                    }
                    .sig-line {
                        border-bottom: 1.5px dashed #475569;
                        height: 46px;
                        margin-bottom: 8px;
                    }
                    .sig-label {
                        font-weight: 700;
                        font-size: 13px;
                        color: #0F2942;
                    }
                    @media print {
                        body { padding: 0; }
                    }
                </style>
            </head>
            <body>
                <header>
                    <div>
                        <h1>تقرير تسليم الغلة اليومية</h1>
                        <div class="sub">مدير الصندوق والغلة اليومية — كشف تسليم للمسؤول المالي</div>
                    </div>
                    <div class="date-box">$escapedDate</div>
                </header>

                $openingRows

                <section class="card">
                    <h2>القسم أ: إجمالي المقبوضات</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>الصندوق / الحساب</th>
                                <th>وسيلة الدفع</th>
                                <th>العملة</th>
                                <th>إجمالي المقبوضات</th>
                            </tr>
                        </thead>
                        <tbody>$inflowRows</tbody>
                    </table>
                </section>

                <section class="card">
                    <h2>القسم ب: كشف الفواتير والنثريات المدفوعة</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>البيان</th>
                                <th>المبلغ</th>
                                <th>العملة</th>
                                <th>وسيلة الدفع</th>
                            </tr>
                        </thead>
                        <tbody>$outflowItemRows</tbody>
                    </table>
                    <h3 style="margin-top: 12px;">إجمالي المصروفات حسب الصندوق</h3>
                    <div class="grid-5">$outflowSummaryPills</div>
                </section>

                <section class="card">
                    <h2>القسم ج: صافي الغلة المطلوب تسليمها</h2>
                    <div class="net-grid">$netCards</div>
                </section>

                <div class="signatures">
                    <div class="sig-box">
                        <div class="sig-line"></div>
                        <div class="sig-label">توقيع المحاسب</div>
                    </div>
                    <div class="sig-box">
                        <div class="sig-line"></div>
                        <div class="sig-label">توقيع المسؤول المالي</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }
}
