package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CashBucket
import com.example.model.CashTransaction
import com.example.model.Currency
import com.example.model.NumberFormatters
import com.example.model.PaymentMethod
import com.example.model.QuickCategories
import com.example.model.ThousandsSeparatorTransformation
import com.example.model.TransactionType
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.tabular

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntrySheet(
    editingTransaction: CashTransaction?,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        type: TransactionType,
        amount: Double,
        bucket: CashBucket,
        category: String,
        note: String,
        existingTimestamp: Long?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financial = LocalFinancialColors.current

    var selectedType by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.type ?: TransactionType.INFLOW)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.let { NumberFormatters.amountToEditableString(it.amount) } ?: ""
        )
    }
    var selectedMethod by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.bucket?.method ?: PaymentMethod.CASH)
    }
    var selectedCurrency by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.bucket?.currency ?: Currency.SYP)
    }
    var selectedCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.category ?: "")
    }
    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var amountError by remember { mutableStateOf(false) }

    val allowedCurrencies = remember(selectedMethod) {
        CashBucket.allowedCurrencies(selectedMethod)
    }
    val effectiveCurrency = remember(selectedMethod, selectedCurrency) {
        CashBucket.coerceCurrency(selectedMethod, selectedCurrency)
    }
    val activeBucket = remember(selectedMethod, effectiveCurrency) {
        CashBucket.from(selectedMethod, effectiveCurrency)
    }
    val categories = remember(selectedType) {
        if (selectedType == TransactionType.OUTFLOW) {
            QuickCategories.outflowCategories
        } else {
            QuickCategories.inflowCategories
        }
    }

    val accentColor by animateColorAsState(
        targetValue = if (selectedType == TransactionType.INFLOW) financial.inflow else financial.outflow,
        label = "tx_accent",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (editingTransaction == null) "تسجيل حركة سريعة" else "تعديل الحركة",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_tx_sheet_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                    )
                }
            }

            // 1. Type Toggle: قبض (Green) vs صرف (Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TypeToggleButton(
                    label = TransactionType.INFLOW.fullArabicLabel,
                    selected = selectedType == TransactionType.INFLOW,
                    selectedBg = financial.inflowContainer,
                    selectedBorder = financial.inflow,
                    selectedContent = financial.onInflowContainer,
                    icon = Icons.Rounded.AddCircle,
                    onClick = {
                        if (selectedType != TransactionType.INFLOW) {
                            selectedType = TransactionType.INFLOW
                            if (selectedCategory in QuickCategories.outflowCategories) {
                                selectedCategory = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_inflow_button"),
                )
                TypeToggleButton(
                    label = TransactionType.OUTFLOW.fullArabicLabel,
                    selected = selectedType == TransactionType.OUTFLOW,
                    selectedBg = financial.outflowContainer,
                    selectedBorder = financial.outflow,
                    selectedContent = financial.onOutflowContainer,
                    icon = Icons.Rounded.RemoveCircle,
                    onClick = {
                        if (selectedType != TransactionType.OUTFLOW) {
                            selectedType = TransactionType.OUTFLOW
                            if (selectedCategory in QuickCategories.inflowCategories) {
                                selectedCategory = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_outflow_button"),
                )
            }

            // 2. Amount Input with live thousand separators
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = NumberFormatters.sanitizeNumericInput(input)
                    amountError = false
                },
                label = { Text("المبلغ (${effectiveCurrency.fullArabic})") },
                placeholder = { Text("0") },
                suffix = {
                    Text(
                        text = effectiveCurrency.symbolArabic,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                    )
                },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("يرجى إدخال مبلغ صحيح أكبر من الصفر") }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.tabular().copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                ),
                visualTransformation = ThousandsSeparatorTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    focusedLabelColor = accentColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input"),
            )

            // 3. Payment Method Selector: كاش (الصندوق) | شام كاش
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "وسيلة الدفع",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MethodSelectorCard(
                        label = PaymentMethod.CASH.fullArabic,
                        selected = selectedMethod == PaymentMethod.CASH,
                        accent = financial.cashBadge,
                        container = financial.cashBadgeContainer,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        onClick = {
                            selectedMethod = PaymentMethod.CASH
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("method_cash_button"),
                    )
                    MethodSelectorCard(
                        label = PaymentMethod.SHAM_CASH.fullArabic,
                        selected = selectedMethod == PaymentMethod.SHAM_CASH,
                        accent = financial.shamCashBadge,
                        container = financial.shamCashBadgeContainer,
                        icon = Icons.Rounded.Smartphone,
                        onClick = {
                            selectedMethod = PaymentMethod.SHAM_CASH
                            // Smart guard: Sham Cash only supports SYP and USD (never TRY)
                            selectedCurrency = CashBucket.coerceCurrency(PaymentMethod.SHAM_CASH, selectedCurrency)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("method_sham_cash_button"),
                    )
                }
            }

            // 4. Smart Currency Selector (Cash -> SYP/TRY/USD, Sham Cash -> SYP/USD ONLY)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "العملة",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = activeBucket.arabicTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allowedCurrencies.forEach { currency ->
                        val isSelected = effectiveCurrency == currency
                        Surface(
                            onClick = { selectedCurrency = currency },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else financial.cardBorder,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("currency_${currency.isoCode.lowercase()}_button"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = "${currency.symbolArabic} (${currency.isoCode})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 5. Quick Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (selectedType == TransactionType.OUTFLOW) "تصنيف سريع للمصروف" else "تصنيف سريع للقبض",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (isSelected) "" else cat
                            },
                            label = { Text(cat) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.16f),
                                selectedLabelColor = accentColor,
                                selectedLeadingIconColor = accentColor,
                            ),
                            modifier = Modifier.testTag("category_chip_$cat"),
                        )
                    }
                }
            }

            // 6. Free-text Note (البيان / اسم الزبون أو الفاتورة)
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("البيان / اسم الزبون أو الفاتورة") },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.INFLOW) {
                            "مثال: مبيعات صباحية، دفعة الزبون أبو محمد..."
                        } else {
                            "مثال: فاتورة كهرباء، أجور نقل، ضيافة..."
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
            )

            // 7. Submit Button
            Button(
                onClick = {
                    val parsed = NumberFormatters.parseAmount(amountText)
                    if (parsed == null || parsed <= 0.0) {
                        amountError = true
                    } else {
                        onSave(
                            editingTransaction?.id,
                            selectedType,
                            parsed,
                            activeBucket,
                            selectedCategory,
                            noteText,
                            editingTransaction?.timestampMillis,
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
            ) {
                Text(
                    text = when {
                        editingTransaction != null -> "حفظ التعديلات"
                        selectedType == TransactionType.INFLOW -> "حفظ عملية القبض"
                        else -> "حفظ عملية الصرف"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TypeToggleButton(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    selectedContent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) selectedBg else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) selectedBorder else financial.cardBorder,
        ),
        modifier = modifier.height(52.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) selectedBorder else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MethodSelectorCard(
    label: String,
    selected: Boolean,
    accent: Color,
    container: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financial = LocalFinancialColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) container else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else financial.cardBorder,
        ),
        modifier = modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
