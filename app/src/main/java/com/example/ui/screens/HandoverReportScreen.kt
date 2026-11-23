package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.HandoverReportData
import com.example.model.NumberFormatters
import com.example.model.PaymentMethod
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.tabular

@Composable
fun HandoverReportScreen(
    report: HandoverReportData,
    onCopyWhatsApp: (HandoverReportData) -> Unit,
    onShareWhatsApp: (HandoverReportData) -> Unit,
    onPrintPdf: (HandoverReportData) -> Unit,
    onRequestCloseDay: (() -> Unit)?,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current

    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header Banner
        item(key = "report_header") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, financial.cardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("report_back_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "رجوع",
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تقرير تسليم الغلة للمسؤول المالي",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = report.dateLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (report.isArchived) {
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = "مؤرشف",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (report.openingBalances.hasAnyNonZero()) {
                        HorizontalDivider(color = financial.cardBorder)
                        Text(
                            text = "الرصيد الافتتاحي المضاف لبداية اليوم:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            report.bucketStats.filter { it.opening != 0.0 }.forEach { stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stat.bucket.arabicTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = NumberFormatters.formatWithCurrency(stat.opening, stat.bucket.currency),
                                        style = MaterialTheme.typography.labelMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section A: إجمالي المقبوضات
        item(key = "section_a") {
            ReportSectionCard(
                title = "القسم أ: إجمالي المقبوضات",
                accentColor = financial.inflow,
                accentContainer = financial.inflowContainer,
                icon = Icons.Rounded.ArrowDownward,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.bucketStats.forEachIndexed { index, stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stat.bucket.arabicTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = NumberFormatters.formatWithCurrency(stat.totalInflow, stat.bucket.currency),
                                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                color = if (stat.totalInflow > 0.0) financial.inflow else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index < report.bucketStats.lastIndex) {
                            HorizontalDivider(color = financial.cardBorder.copy(alpha = 0.65f))
                        }
                    }
                }
            }
        }

        // Section B: كشف الفواتير والنثريات المدفوعة
        item(key = "section_b") {
            ReportSectionCard(
                title = "القسم ب: كشف الفواتير والنثريات المدفوعة",
                accentColor = financial.outflow,
                accentContainer = financial.outflowContainer,
                icon = Icons.Rounded.ArrowUpward,
            ) {
                val outflows = report.outflows
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (outflows.isEmpty()) {
                        Text(
                            text = "لا توجد فواتير أو نثريات مدفوعة خلال هذا اليوم.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        )
                    } else {
                        // 4-Column Header: (البيان | المبلغ | العملة | وسيلة الدفع)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "البيان",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(2.2f),
                                )
                                Text(
                                    text = "المبلغ",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.3f),
                                )
                                Text(
                                    text = "العملة",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f),
                                )
                                Text(
                                    text = "الوسيلة",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.1f),
                                )
                            }
                        }

                        outflows.forEachIndexed { idx, tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(2.2f)) {
                                    Text(
                                        text = tx.displayStatement,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = NumberFormatters.formatTimeArabic(tx.timestampMillis),
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = NumberFormatters.formatAmount(tx.amount),
                                    style = MaterialTheme.typography.labelLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                    color = financial.outflow,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.3f),
                                )
                                Text(
                                    text = tx.bucket.currency.symbolArabic,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f),
                                )
                                Text(
                                    text = tx.bucket.method.shortArabic,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.1f),
                                )
                            }
                            if (idx < outflows.lastIndex) {
                                HorizontalDivider(color = financial.cardBorder.copy(alpha = 0.55f))
                            }
                        }
                    }

                    HorizontalDivider(color = financial.cardBorder)
                    Text(
                        text = "إجمالي المصروفات حسب الصندوق",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.bucketStats.forEach { stat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stat.bucket.arabicTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = NumberFormatters.formatWithCurrency(stat.totalOutflow, stat.bucket.currency),
                                    style = MaterialTheme.typography.labelLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                    color = if (stat.totalOutflow > 0.0) financial.outflow else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section C: صافي الغلة المطلوب تسليمها (5 Exact Buckets)
        item(key = "section_c") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "القسم ج: صافي الغلة المطلوب تسليمها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "الصافي = الرصيد الافتتاحي + المقبوضات - المصروفات لكل صندوق بشكل مستقل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    report.bucketStats.forEach { stat ->
                        val isCash = stat.bucket.method == PaymentMethod.CASH
                        val badgeColor = if (isCash) financial.cashBadge else financial.shamCashBadge
                        val badgeBg = if (isCash) financial.cashBadgeContainer else financial.shamCashBadgeContainer
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, financial.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("net_handover_${stat.bucket.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(badgeBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isCash) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.Smartphone,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stat.bucket.arabicTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = stat.bucket.currency.isoCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Text(
                                    text = NumberFormatters.formatWithCurrency(stat.netBalance, stat.bucket.currency),
                                    style = MaterialTheme.typography.titleLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                    color = if (stat.netBalance < -0.0001) {
                                        financial.outflow
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                            }
                        }
                    }

                    // Equivalent Totals (USD & SYP) + Applied Exchange Rates
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_equivalent_totals_card"),
                    ) {
                        val eq = report.equivalentTotals
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "إجمالي الغلة المعادل (جميع الصناديق الـ 5)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text = "أسعار الصرف المعتمدة: 1$ = ${NumberFormatters.formatAmount(report.exchangeRates.usdToSypRate)} ل.س  |  1$ = ${NumberFormatters.formatAmount(report.exchangeRates.usdToTryRate)} ل.ت",
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = "المعادل بالدولار ($)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                        )
                                        Text(
                                            text = "${NumberFormatters.formatAmount(eq.totalUsd)} $",
                                            style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = "المعادل بالليرة السورية",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                        )
                                        Text(
                                            text = "${NumberFormatters.formatAmount(eq.totalSyp)} ل.س",
                                            style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Report Actions: Copy for WhatsApp, Share, Print/Save PDF, Close & Archive Day
        item(key = "report_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onCopyWhatsApp(report) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = financial.inflow,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("copy_whatsapp_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نسخ للواتساب",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    OutlinedButton(
                        onClick = { onShareWhatsApp(report) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, financial.inflow),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("share_report_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "مشاركة التقرير",
                            tint = financial.inflow,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onPrintPdf(report) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("print_pdf_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Print,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "طباعة / حفظ PDF (مع خانات التوقيع)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }

                if (!report.isArchived && onRequestCloseDay != null) {
                    Button(
                        onClick = onRequestCloseDay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = financial.outflow,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("close_archive_day_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إغلاق وتصفير اليوم",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    accentColor: Color,
    accentContainer: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    val financial = LocalFinancialColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financial.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}
