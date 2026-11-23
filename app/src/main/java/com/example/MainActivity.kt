package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CashLedgerRepository
import com.example.data.CashPreferencesStore
import com.example.ui.CashManagerApp
import com.example.ui.theme.CashManagerTheme
import com.example.viewmodel.CashManagerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val preferencesStore = CashPreferencesStore(applicationContext)
        val repository = CashLedgerRepository(
            cashDao = database.cashDao(),
            preferencesStore = preferencesStore,
        )
        val factory = CashManagerViewModel.Factory(repository)

        setContent {
            val viewModel: CashManagerViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }

            CashManagerTheme(darkTheme = uiState.isDarkTheme) {
                CashManagerApp(viewModel = viewModel)
            }
        }
    }
}
