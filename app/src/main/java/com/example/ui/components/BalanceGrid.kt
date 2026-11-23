package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BucketStats
import com.example.model.CashBucket
import com.example.model.NumberFormatters
import com.example.model.PaymentMethod
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.tabular

@Composable
fun BalanceBucketsSection(
    bucketStats: List<BucketStats>,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current
    val cashStats = bucketStats.filter { it.bucket.method == PaymentMethod.CASH }
    val shamCashStats = bucketStats.filter { it.bucket.method == PaymentMethod.SHAM_CASH }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Group 1: Cash Box (3 buckets: SYP, TRY, USD)
        SectionGroupHeader(
            title = "الصندوق (كاش)",
            subtitle = "3 عملات",
            icon = Icons.Rounded.AccountBalanceWallet,
            accentColor = financial.cashBadge,
            accentContainer = financial.cashBadgeContainer,
        )

        // Featured primary Cash SYP card full-width, followed by Cash TRY & Cash USD side by side
        val cashSyp = cashStats.firstOrNull { it.bucket == CashBucket.CASH_SYP }
        val cashTry = cashStats.firstOrNull { it.bucket == CashBucket.CASH_TRY }
        val cashUsd = cashStats.firstOrNull { it.bucket == CashBucket.CASH_USD }

        if (cashSyp != null) {
            BucketBalanceCard(
                stats = cashSyp,
                featured = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bucket_card_${cashSyp.bucket.name.lowercase()}"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (cashTry != null) {
                BucketBalanceCard(
                    stats = cashTry,
                    featured = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bucket_card_${cashTry.bucket.name.lowercase()}"),
                )
            }
            if (cashUsd != null) {
                BucketBalanceCard(
                    stats = cashUsd,
                    featured = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bucket_card_${cashUsd.bucket.name.lowercase()}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Group 2: Sham Cash (2 buckets: SYP, USD)
        SectionGroupHeader(
            title = "شام كاش",
            subtitle = "ليرة سورية ودولار",
            icon = Icons.Rounded.Smartphone,
            accentColor = financial.shamCashBadge,
            accentContainer = financial.shamCashBadgeContainer,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            shamCashStats.forEach { stat ->
                BucketBalanceCard(
                    stats = stat,
                    featured = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bucket_card_${stat.bucket.name.lowercase()}"),
                )
            }
        }
    }
}

@Composable
private fun SectionGroupHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    accentContainer: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BucketBalanceCard(
    stats: BucketStats,
    featured: Boolean,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current
    val isCash = stats.bucket.method == PaymentMethod.CASH
    val methodColor = if (isCash) financial.cashBadge else financial.shamCashBadge
    val methodContainer = if (isCash) financial.cashBadgeContainer else financial.shamCashBadgeContainer
    val isNegative = stats.netBalance < -0.0001

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isNegative) financial.outflow.copy(alpha = 0.45f) else financial.cardBorder,
        ),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stats.bucket.shortTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = methodContainer,
                ) {
                    Text(
                        text = stats.bucket.currency.symbolArabic,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = methodColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            AnimatedContent(
                targetState = NumberFormatters.formatAmount(stats.netBalance),
                label = "balance_value",
            ) { formattedBalance ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formattedBalance,
                        style = (if (featured) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
                            .tabular()
                            .copy(fontWeight = FontWeight.Bold),
                        color = if (isNegative) financial.outflow else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stats.bucket.currency.symbolArabic,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = if (featured) 3.dp else 2.dp),
                    )
                }
            }

            // Micro inflow / outflow & opening indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MicroFlowTag(
                    icon = Icons.Rounded.ArrowDownward,
                    amount = stats.totalInflow,
                    tint = financial.inflow,
                    modifier = Modifier.weight(1f, fill = false),
                )
                MicroFlowTag(
                    icon = Icons.Rounded.ArrowUpward,
                    amount = stats.totalOutflow,
                    tint = financial.outflow,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (featured && stats.opening != 0.0) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "افتتاحي: ${NumberFormatters.formatAmount(stats.opening)}",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroFlowTag(
    icon: ImageVector,
    amount: Double,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            text = NumberFormatters.formatAmount(amount),
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = if (amount > 0.0) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
