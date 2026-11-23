package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.model.CashBucket
import com.example.model.NumberFormatters
import com.example.model.OpeningBalances
import com.example.model.ThousandsSeparatorTransformation
import com.example.ui.theme.tabular

@Composable
fun OpeningBalanceDialog(
    currentBalances: OpeningBalances,
    onDismiss: () -> Unit,
    onSave: (OpeningBalances) -> Unit,
) {
    var cashSyp by remember(currentBalances) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentBalances.cashSyp))
    }
    var cashTry by remember(currentBalances) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentBalances.cashTry))
    }
    var cashUsd by remember(currentBalances) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentBalances.cashUsd))
    }
    var shamCashSyp by remember(currentBalances) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentBalances.shamCashSyp))
    }
    var shamCashUsd by remember(currentBalances) {
        mutableStateOf(NumberFormatters.amountToEditableString(currentBalances.shamCashUsd))
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "الرصيد الافتتاحي لليوم",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = {
                            cashSyp = ""
                            cashTry = ""
                            cashUsd = ""
                            shamCashSyp = ""
                            shamCashUsd = ""
                        },
                        modifier = Modifier.testTag("reset_opening_inputs_button"),
                    ) {
                        Text("تصفير الكل")
                    }
                }

                Text(
                    text = "أدخل الرصيد الموجود في الصندوق بداية اليوم (اختياري، الافتراضي 0).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OpeningInputField(
                    bucket = CashBucket.CASH_SYP,
                    value = cashSyp,
                    onValueChange = { cashSyp = NumberFormatters.sanitizeNumericInput(it) },
                )
                OpeningInputField(
                    bucket = CashBucket.CASH_TRY,
                    value = cashTry,
                    onValueChange = { cashTry = NumberFormatters.sanitizeNumericInput(it) },
                )
                OpeningInputField(
                    bucket = CashBucket.CASH_USD,
                    value = cashUsd,
                    onValueChange = { cashUsd = NumberFormatters.sanitizeNumericInput(it) },
                )
                OpeningInputField(
                    bucket = CashBucket.SHAM_CASH_SYP,
                    value = shamCashSyp,
                    onValueChange = { shamCashSyp = NumberFormatters.sanitizeNumericInput(it) },
                )
                OpeningInputField(
                    bucket = CashBucket.SHAM_CASH_USD,
                    value = shamCashUsd,
                    onValueChange = { shamCashUsd = NumberFormatters.sanitizeNumericInput(it) },
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                            .testTag("cancel_opening_balance_button"),
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onSave(
                                OpeningBalances(
                                    cashSyp = NumberFormatters.parseAmount(cashSyp) ?: 0.0,
                                    cashTry = NumberFormatters.parseAmount(cashTry) ?: 0.0,
                                    cashUsd = NumberFormatters.parseAmount(cashUsd) ?: 0.0,
                                    shamCashSyp = NumberFormatters.parseAmount(shamCashSyp) ?: 0.0,
                                    shamCashUsd = NumberFormatters.parseAmount(shamCashUsd) ?: 0.0,
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_opening_balance_button"),
                    ) {
                        Text("حفظ الأرصدة")
                    }
                }
            }
        }
    }
}

@Composable
private fun OpeningInputField(
    bucket: CashBucket,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(bucket.arabicTitle) },
        placeholder = { Text("0") },
        suffix = {
            Text(
                text = bucket.currency.symbolArabic,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
        visualTransformation = ThousandsSeparatorTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("opening_input_${bucket.name.lowercase()}"),
    )
}
