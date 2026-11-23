package com.example.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object NumberFormatters {
    private val usSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun formatAmount(amount: Double, showSign: Boolean = false): String {
        val isWhole = abs(amount - floor(amount)) < 0.0001
        val pattern = if (isWhole) "#,##0" else "#,##0.##"
        val formatter = DecimalFormat(pattern, usSymbols)
        val formattedAbs = formatter.format(abs(amount))
        return when {
            amount < -0.0001 -> "-$formattedAbs"
            showSign && amount > 0.0001 -> "+$formattedAbs"
            else -> formattedAbs
        }
    }

    fun formatWithCurrency(amount: Double, currency: Currency, showSign: Boolean = false): String {
        return "${formatAmount(amount, showSign)} ${currency.symbolArabic}"
    }

    /**
     * Normalizes Arabic-Indic and Eastern Arabic numerals into standard ASCII digits,
     * keeping at most one decimal dot and up to 2 decimal places.
     */
    fun sanitizeNumericInput(raw: String): String {
        val normalized = buildString(raw.length) {
            for (ch in raw) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                    in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
                    '.', '\u066B', ',' -> {
                        if (ch != ',') append('.')
                    }
                }
            }
        }
        val dotIndex = normalized.indexOf('.')
        if (dotIndex == -1) {
            return normalized.take(14)
        }
        val intPart = normalized.substring(0, dotIndex).take(12)
        val decPart = normalized.substring(dotIndex + 1).replace(".", "").take(2)
        return "$intPart.$decPart"
    }

    fun parseAmount(input: String): Double? {
        val clean = sanitizeNumericInput(input)
        if (clean.isBlank() || clean == ".") return null
        return clean.toDoubleOrNull()
    }

    fun amountToEditableString(amount: Double): String {
        if (amount == 0.0) return ""
        val isWhole = abs(amount - floor(amount)) < 0.0001
        return if (isWhole) {
            DecimalFormat("0", usSymbols).format(amount)
        } else {
            DecimalFormat("0.##", usSymbols).format(amount)
        }
    }

    fun formatArabicDate(millis: Long = System.currentTimeMillis()): String {
        val arabicLocale = Locale("ar")
        val dayName = SimpleDateFormat("EEEE", arabicLocale).format(Date(millis))
        val numericDate = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
        return "$dayName، $numericDate"
    }

    fun formatFileDate(millis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
    }

    fun formatTimeArabic(millis: Long): String {
        val time = SimpleDateFormat("hh:mm", Locale.US).format(Date(millis))
        val amPm = SimpleDateFormat("a", Locale.US).format(Date(millis))
        val suffix = if (amPm.equals("AM", ignoreCase = true)) "ص" else "م"
        return "$time $suffix"
    }
}

/**
 * Formats plain numeric input strings (e.g. "1250000.5") with live thousand separators
 * ("1,250,000.5") while keeping text cursor offsets completely accurate.
 */
class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val dotIndex = original.indexOf('.')
        val intPart = if (dotIndex == -1) original else original.substring(0, dotIndex)
        val decPart = if (dotIndex == -1) "" else original.substring(dotIndex)

        val formattedInt = buildString {
            val len = intPart.length
            for (i in 0 until len) {
                append(intPart[i])
                val remaining = len - 1 - i
                if (remaining > 0 && remaining % 3 == 0) {
                    append(',')
                }
            }
        }
        val transformed = formattedInt + decPart

        val originalToTransformed = IntArray(original.length + 1)
        val transformedToOriginal = IntArray(transformed.length + 1)

        var tIdx = 0
        for (oIdx in original.indices) {
            while (tIdx < transformed.length && transformed[tIdx] == ',') {
                transformedToOriginal[tIdx] = oIdx
                tIdx++
            }
            originalToTransformed[oIdx] = tIdx
            if (tIdx < transformed.length) {
                transformedToOriginal[tIdx] = oIdx
                tIdx++
            }
        }
        originalToTransformed[original.length] = transformed.length
        transformedToOriginal[transformed.length] = original.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, original.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformed.length)]
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
