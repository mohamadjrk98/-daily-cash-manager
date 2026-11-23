package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.FetchedExchangeRates
import com.example.model.ExchangeRates
import com.example.model.NumberFormatters
import com.example.model.ThousandsSeparatorTransformation
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.tabular
import com.example.viewmodel.ExchangeRateDialogFeedback

@Composable
fun ExchangeRateDialog(
    currentRates: ExchangeRates,
    isFetching: Boolean,
    fetchedPreview: FetchedExchangeRates?,
    feedback: ExchangeRateDialogFeedback?,
    onFetchFromInternet: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (usdToSyp: Double, usdToTry: Double, updatedLabel: String) -> Unit,
) {
    val financial = LocalFinancialColors.current

    var sypRateText by remember(currentRates) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentRates.usdToSypRate))
    }
    var tryRateText by remember(currentRates) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentRates.usdToTryRate))
    }
    var pendingSourceLabel by remember(currentRates) {
        mutableStateOf<String?>(null)
    }
    var sypError by remember { mutableStateOf(false) }
    var tryError by remember { mutableStateOf(false) }

    // Apply live API fetched values into the editable inputs automatically
    LaunchedEffect(fetchedPreview) {
        if (fetchedPreview != null) {
            sypRateText = NumberFormatters.amountToEditableString(fetchedPreview.usdToSypRate)
            tryRateText = NumberFormatters.amountToEditableString(fetchedPreview.usdToTryRate)
            pendingSourceLabel = fetchedPreview.fetchedLabel
            sypError = false
            tryError = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyExchange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أسعار الصرف",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "آخر تحديث: ${currentRates.lastUpdatedLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 1. Auto-Fetch Button (Free API: open.er-api.com)
                FilledTonalButton(
                    onClick = onFetchFromInternet,
                    enabled = !isFetching,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("fetch_live_rates_button"),
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري جلب أسعار الصرف...",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحديث تلقائي من الإنترنت",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

                // Inline feedback banner (success or offline fallback)
                if (feedback != null) {
                    val container = if (feedback.isError) financial.outflowContainer else financial.inflowContainer
                    val tint = if (feedback.isError) financial.outflow else financial.inflow
                    val contentColor = if (feedback.isError) financial.onOutflowContainer else financial.onInflowContainer
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = container,
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_fetch_feedback_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (feedback.isError) Icons.Rounded.WifiOff else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = feedback.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor,
                            )
                        }
                    }
                }

                HorizontalDivider(color = financial.cardBorder)

                // 2. Manual Edit Section
                Text(
                    text = "تعديل يدوي حسب سعر الشركة",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                OutlinedTextField(
                    value = sypRateText,
                    onValueChange = { input ->
                        sypRateText = NumberFormatters.sanitizeNumericInput(input)
                        pendingSourceLabel = null
                        sypError = false
                    },
                    label = { Text("سعر صرف الدولار مقابل الليرة السورية") },
                    prefix = {
                        Text(
                            text = "1$ = ",
                            style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    suffix = {
                        Text(
                            text = "ل.س",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = sypError,
                    supportingText = if (sypError) {
                        { Text("يرجى إدخال سعر صرف صحيح أكبر من الصفر") }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_usd_syp_input"),
                )

                OutlinedTextField(
                    value = tryRateText,
                    onValueChange = { input ->
                        tryRateText = NumberFormatters.sanitizeNumericInput(input)
                        pendingSourceLabel = null
                        tryError = false
                    },
                    label = { Text("سعر صرف الدولار مقابل الليرة التركية") },
                    prefix = {
                        Text(
                            text = "1$ = ",
                            style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    suffix = {
                        Text(
                            text = "ل.ت",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = tryError,
                    supportingText = if (tryError) {
                        { Text("يرجى إدخال سعر صرف صحيح أكبر من الصفر") }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_usd_try_input"),
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_rates_button"),
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val parsedSyp = NumberFormatters.parseAmount(sypRateText)
                            val parsedTry = NumberFormatters.parseAmount(tryRateText)
                            sypError = parsedSyp == null || parsedSyp <= 0.0
                            tryError = parsedTry == null || parsedTry <= 0.0
                            if (!sypError && !tryError && parsedSyp != null && parsedTry != null) {
                                val finalLabel = pendingSourceLabel
                                    ?: "تعديل يدوي • ${NumberFormatters.formatTimeArabic(System.currentTimeMillis())}"
                                onSave(parsedSyp, parsedTry, finalLabel)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_rates_button"),
                    ) {
                        Text("حفظ الأسعار")
                    }
                }
            }
        }
    }
}
