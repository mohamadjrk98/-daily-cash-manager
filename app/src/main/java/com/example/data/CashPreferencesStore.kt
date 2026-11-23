package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.OpeningBalances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cashPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cash_manager_preferences"
)

class CashPreferencesStore(private val context: Context) {

    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val OPENING_CASH_SYP = doublePreferencesKey("opening_cash_syp")
        val OPENING_CASH_TRY = doublePreferencesKey("opening_cash_try")
        val OPENING_CASH_USD = doublePreferencesKey("opening_cash_usd")
        val OPENING_SHAM_CASH_SYP = doublePreferencesKey("opening_sham_cash_syp")
        val OPENING_SHAM_CASH_USD = doublePreferencesKey("opening_sham_cash_usd")
    }

    val isDarkTheme: Flow<Boolean> = context.cashPreferencesDataStore.data.map { prefs ->
        prefs[Keys.IS_DARK_THEME] ?: false
    }

    val openingBalances: Flow<OpeningBalances> = context.cashPreferencesDataStore.data.map { prefs ->
        OpeningBalances(
            cashSyp = prefs[Keys.OPENING_CASH_SYP] ?: 0.0,
            cashTry = prefs[Keys.OPENING_CASH_TRY] ?: 0.0,
            cashUsd = prefs[Keys.OPENING_CASH_USD] ?: 0.0,
            shamCashSyp = prefs[Keys.OPENING_SHAM_CASH_SYP] ?: 0.0,
            shamCashUsd = prefs[Keys.OPENING_SHAM_CASH_USD] ?: 0.0,
        )
    }

    suspend fun getOpeningBalancesOnce(): OpeningBalances = openingBalances.first()

    suspend fun setDarkTheme(enabled: Boolean) {
        context.cashPreferencesDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = enabled
        }
    }

    suspend fun saveOpeningBalances(balances: OpeningBalances) {
        context.cashPreferencesDataStore.edit { prefs ->
            prefs[Keys.OPENING_CASH_SYP] = balances.cashSyp
            prefs[Keys.OPENING_CASH_TRY] = balances.cashTry
            prefs[Keys.OPENING_CASH_USD] = balances.cashUsd
            prefs[Keys.OPENING_SHAM_CASH_SYP] = balances.shamCashSyp
            prefs[Keys.OPENING_SHAM_CASH_USD] = balances.shamCashUsd
        }
    }

    suspend fun resetOpeningBalances() {
        saveOpeningBalances(OpeningBalances.Zero)
    }
}
