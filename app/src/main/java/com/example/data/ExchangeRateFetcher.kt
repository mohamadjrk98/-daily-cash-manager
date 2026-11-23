package com.example.data

import com.example.model.NumberFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FetchedExchangeRates(
    val usdToSypRate: Double,
    val usdToTryRate: Double,
    val fetchedLabel: String,
)

object ExchangeRateFetcher {
    private const val ENDPOINT_URL = "https://open.er-api.com/v6/latest/USD"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    const val OFFLINE_ERROR_MESSAGE = "لا يوجد اتصال بالإنترنت، يمكنك التعديل يدوياً"

    suspend fun fetchLatestUsdRates(): Result<FetchedExchangeRates> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(ENDPOINT_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(IllegalStateException(OFFLINE_ERROR_MESSAGE))
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = parseRatesFromJson(body)
                ?: return@withContext Result.failure(IllegalStateException(OFFLINE_ERROR_MESSAGE))

            Result.success(parsed)
        } catch (_: Exception) {
            Result.failure(IllegalStateException(OFFLINE_ERROR_MESSAGE))
        } finally {
            connection?.disconnect()
        }
    }

    internal fun parseRatesFromJson(
        rawJson: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): FetchedExchangeRates? {
        return runCatching {
            val root = JSONObject(rawJson)
            val rates = root.optJSONObject("rates") ?: return null
            val syp = rates.optDouble("SYP", Double.NaN)
            val tryRate = rates.optDouble("TRY", Double.NaN)
            if (syp.isNaN() || syp.isInfinite() || syp <= 0.0) return null
            if (tryRate.isNaN() || tryRate.isInfinite() || tryRate <= 0.0) return null

            val roundedSyp = kotlin.math.round(syp * 100.0) / 100.0
            val roundedTry = kotlin.math.round(tryRate * 100.0) / 100.0
            val label = "تحديث تلقائي • ${NumberFormatters.formatTimeArabic(nowMillis)}"

            FetchedExchangeRates(
                usdToSypRate = roundedSyp,
                usdToTryRate = roundedTry,
                fetchedLabel = label,
            )
        }.getOrNull()
    }
}
