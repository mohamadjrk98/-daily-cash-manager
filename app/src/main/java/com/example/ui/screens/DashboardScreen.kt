package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CashTransaction
import com.example.model.LedgerFilter
import com.example.model.NumberFormatters
import com.example.model.PaymentMethod
import com.example.model.TransactionType
import com.example.ui.components.BalanceBucketsSection
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.tabular
import com.example.viewmodel.CashUiState

@Composable
fun DashboardScreen(
    uiState: CashUiState,
    onToggleDarkTheme: () -> Unit,
    onOpenOpeningBalances: () -> Unit,
    onOpenHandoverReport: () -> Unit,
    onSelectFilter: (LedgerFilter) -> Unit,
    onEditTransaction: (CashTransaction) -> Unit,
    onDeleteTransaction: (CashTransaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Authoritative Hero Header
        item(key = "hero_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(financial.heroGradientStart, financial.heroGradientEnd)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مدير الصندوق والغلة اليومية",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = uiState.todayDateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        IconButton(
                            onClick = onToggleDarkTheme,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("dashboard_theme_toggle"),
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (uiState.isDarkTheme) "تفعيل الوضع الفاتح" else "تفعيل الوضع الداكن",
                                tint = Color.White,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onOpenOpeningBalances,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("opening_balance_button"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.openingBalances.hasAnyNonZero()) {
                                        "الرصيد الافتتاحي (مُعدّل)"
                                    } else {
                                        "تحديد الرصيد الافتتاحي"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.activeTransactions.size} حركة",
                                    style = MaterialTheme.typography.labelLarge.tabular(),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Five Live Balance Buckets Grid
        item(key = "balance_grid") {
            BalanceBucketsSection(bucketStats = uiState.bucketStats)
        }

        // 3. Prominent Daily Handover Report Button
        item(key = "handover_cta") {
            Button(
                onClick = onOpenHandoverReport,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prepare_handover_report_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AssignmentTurnedIn,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تجهيز تقرير تسليم الغلة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 4. Today's Ledger Header & Filter Tabs
        item(key = "ledger_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "سجل حركات اليوم",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${uiState.filteredTransactions.size} من ${uiState.activeTransactions.size}",
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LedgerFilter.entries.forEach { filter ->
                        val selected = uiState.selectedFilter == filter
                        Surface(
                            onClick = { onSelectFilter(filter) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else financial.cardBorder,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("filter_${filter.name.lowercase()}_tab"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            ) {
                                Text(
                                    text = filter.arabicLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Today's Ledger Entries or Empty State
        if (uiState.filteredTransactions.isEmpty()) {
            item(key = "ledger_empty") {
                EmptyLedgerCard(
                    hasAnyTransactions = uiState.activeTransactions.isNotEmpty(),
                )
            }
        } else {
            items(
                items = uiState.filteredTransactions,
                key = { it.id },
            ) { tx ->
                TransactionRowCard(
                    tx = tx,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx) },
                )
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    tx: CashTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val financial = LocalFinancialColors.current
    val isInflow = tx.type == TransactionType.INFLOW
    val flowColor = if (isInflow) financial.inflow else financial.outflow
    val flowContainer = if (isInflow) financial.inflowContainer else financial.outflowContainer
    val isCash = tx.bucket.method == PaymentMethod.CASH
    val methodColor = if (isCash) financial.cashBadge else financial.shamCashBadge
    val methodBg = if (isCash) financial.cashBadgeContainer else financial.shamCashBadgeContainer

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financial.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("transaction_card_${tx.id}"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(flowContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isInflow) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = tx.type.arabicLabel,
                        tint = flowColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = tx.displayStatement,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BadgePill(
                            text = tx.bucket.method.shortArabic,
                            textColor = methodColor,
                            backgroundColor = methodBg,
                        )
                        BadgePill(
                            text = tx.bucket.currency.symbolArabic,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = NumberFormatters.formatTimeArabic(tx.timestampMillis),
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isInflow) "+" else "-") + NumberFormatters.formatAmount(tx.amount),
                        style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                        color = flowColor,
                    )
                    Text(
                        text = tx.bucket.currency.symbolArabic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Action strip: Edit & Delete (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("edit_transaction_${tx.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعديل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    color = financial.outflowContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("delete_transaction_${tx.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف",
                            tint = financial.outflow,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حذف",
                            style = MaterialTheme.typography.labelMedium,
                            color = financial.outflow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    textColor: Color,
    backgroundColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyLedgerCard(hasAnyTransactions: Boolean) {
    val financial = LocalFinancialColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financial.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = if (hasAnyTransactions) "لا توجد حركات ضمن هذا الفلتر" else "لا توجد حركات مسجلة اليوم",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (hasAnyTransactions) {
                    "جرّب اختيار تبويب آخر أو عرض الكل"
                } else {
                    "اضغط على زر «حركة جديدة» بالأسفل لتسجيل أول قبض أو صرف خلال 3 ثوانٍ"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
