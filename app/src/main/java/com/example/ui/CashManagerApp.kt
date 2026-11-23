package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.export.ReportExporter
import com.example.model.NumberFormatters
import com.example.ui.components.OpeningBalanceDialog
import com.example.ui.components.TransactionEntrySheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HandoverReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalFinancialColors
import com.example.viewmodel.AppDestination
import com.example.viewmodel.CashManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class NavItem(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val appNavItems = listOf(
    NavItem(
        destination = AppDestination.TODAY,
        label = "اليوم",
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_today",
    ),
    NavItem(
        destination = AppDestination.REPORT,
        label = "التقرير",
        selectedIcon = Icons.Rounded.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_report",
    ),
    NavItem(
        destination = AppDestination.HISTORY,
        label = "السجل",
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history",
    ),
    NavItem(
        destination = AppDestination.SETTINGS,
        label = "الإعدادات",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings",
    ),
)

@Composable
fun CashManagerApp(
    viewModel: CashManagerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val financial = LocalFinancialColors.current

    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    // SAF zero-permission JSON backup export launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.generateBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("تعذر فتح ملف التصدير")
                    }
                }.onSuccess {
                    viewModel.showMessage("تم تصدير النسخة الاحتياطية JSON بنجاح")
                }.onFailure {
                    viewModel.showMessage("تعذر حفظ ملف النسخة الاحتياطية")
                }
            }
        }
    }

    // SAF zero-permission JSON backup import launcher
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: error("تعذر قراءة الملف المختار")
                    }
                }.onSuccess { rawJson ->
                    pendingImportJson = rawJson
                }.onFailure {
                    viewModel.showMessage("تعذر قراءة ملف النسخة الاحتياطية")
                }
            }
        }
    }

    // Back handling for secondary tabs
    BackHandler(
        enabled = uiState.viewingArchivedDay == null && uiState.currentDestination != AppDestination.TODAY,
    ) {
        viewModel.selectDestination(AppDestination.TODAY)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    // Overlays & Dialogs
    if (uiState.isOpeningBalanceDialogOpen) {
        OpeningBalanceDialog(
            currentBalances = uiState.openingBalances,
            onDismiss = viewModel::dismissOpeningBalanceDialog,
            onSave = viewModel::saveOpeningBalances,
        )
    }

    if (uiState.isTransactionSheetOpen) {
        TransactionEntrySheet(
            editingTransaction = uiState.editingTransaction,
            onDismiss = viewModel::dismissTransactionSheet,
            onSave = viewModel::saveTransaction,
        )
    }

    uiState.pendingDeleteTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteTransaction,
            title = { Text("حذف الحركة") },
            text = {
                Text(
                    "هل تريد حذف «${tx.displayStatement}» بقيمة ${NumberFormatters.formatWithCurrency(tx.amount, tx.bucket.currency)}؟ سيتم إعادة حساب الأرصدة فوراً."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteTransaction,
                    modifier = Modifier.testTag("confirm_delete_transaction_button"),
                ) {
                    Text("حذف", color = financial.outflow)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteTransaction) {
                    Text("إلغاء")
                }
            },
        )
    }

    if (uiState.isCloseDayDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCloseDay,
            title = { Text("إغلاق وتصفير اليوم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "سيتم حفظ نسخة كاملة من تقرير اليوم في «سجل الأيام السابقة»، وتصفير سجل الحركات والأرصدة الافتتاحية لبداية يوم جديد.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "عدد حركات اليوم: ${uiState.activeTransactions.size} حركة.",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmCloseAndArchiveDay,
                    modifier = Modifier.testTag("confirm_close_day_button"),
                ) {
                    Text("تأكيد الإغلاق والتصفير", color = financial.outflow)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCloseDay) {
                    Text("إلغاء")
                }
            },
        )
    }

    pendingImportJson?.let { rawJson ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("استيراد نسخة احتياطية") },
            text = {
                Text("سيتم استبدال بيانات اليوم الحالي وسجل الأيام السابقة بمحتوى ملف النسخة الاحتياطية المختار. هل تريد المتابعة؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importBackupJson(rawJson)
                        pendingImportJson = null
                    },
                    modifier = Modifier.testTag("confirm_import_backup_button"),
                ) {
                    Text("استيراد الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) {
                    Text("إلغاء")
                }
            },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        val showFab = uiState.currentDestination == AppDestination.TODAY && uiState.viewingArchivedDay == null

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Spacer(modifier = Modifier.weight(0.2f))
                    appNavItems.forEach { item ->
                        val selected = uiState.currentDestination == item.destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectDestination(item.destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.testTag),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (showFab) {
                        ExtendedFloatingActionButton(
                            onClick = viewModel::openNewTransactionSheet,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "إضافة حركة جديدة",
                                )
                            },
                            text = {
                                Text(
                                    text = "حركة جديدة",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_transaction_fab"),
                        )
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            appNavItems.forEach { item ->
                                val selected = uiState.currentDestination == item.destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectDestination(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    },
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val constrainedModifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)

                    val archivedViewing = uiState.viewingArchivedDay
                    if (archivedViewing != null) {
                        HandoverReportScreen(
                            report = archivedViewing.report,
                            onCopyWhatsApp = { report ->
                                ReportExporter.copyToClipboard(context, ReportExporter.formatForWhatsApp(report))
                                viewModel.showMessage("تم نسخ التقرير بصيغة الواتساب بنجاح")
                            },
                            onShareWhatsApp = { report ->
                                ReportExporter.shareText(context, ReportExporter.formatForWhatsApp(report))
                            },
                            onPrintPdf = { report ->
                                ReportExporter.printOrSavePdf(context, report)
                            },
                            onRequestCloseDay = null,
                            onNavigateBack = viewModel::closeArchivedDayDetail,
                            modifier = constrainedModifier,
                        )
                    } else {
                        when (uiState.currentDestination) {
                            AppDestination.TODAY -> DashboardScreen(
                                uiState = uiState,
                                onToggleDarkTheme = viewModel::toggleDarkTheme,
                                onOpenOpeningBalances = viewModel::openOpeningBalanceDialog,
                                onOpenHandoverReport = { viewModel.selectDestination(AppDestination.REPORT) },
                                onSelectFilter = viewModel::selectFilter,
                                onEditTransaction = viewModel::openEditTransactionSheet,
                                onDeleteTransaction = viewModel::requestDeleteTransaction,
                                modifier = constrainedModifier,
                            )

                            AppDestination.REPORT -> HandoverReportScreen(
                                report = uiState.liveHandoverReport,
                                onCopyWhatsApp = { report ->
                                    ReportExporter.copyToClipboard(context, ReportExporter.formatForWhatsApp(report))
                                    viewModel.showMessage("تم نسخ التقرير بصيغة الواتساب بنجاح")
                                },
                                onShareWhatsApp = { report ->
                                    ReportExporter.shareText(context, ReportExporter.formatForWhatsApp(report))
                                },
                                onPrintPdf = { report ->
                                    ReportExporter.printOrSavePdf(context, report)
                                },
                                onRequestCloseDay = viewModel::requestCloseDay,
                                onNavigateBack = null,
                                modifier = constrainedModifier,
                            )

                            AppDestination.HISTORY -> HistoryScreen(
                                archivedDays = uiState.archivedDays,
                                onOpenArchivedDay = viewModel::openArchivedDay,
                                onDeleteArchivedDay = viewModel::deleteArchivedDay,
                                modifier = constrainedModifier,
                            )

                            AppDestination.SETTINGS -> SettingsScreen(
                                isDarkTheme = uiState.isDarkTheme,
                                activeCount = uiState.activeTransactions.size,
                                archivedCount = uiState.archivedDays.size,
                                onSetDarkTheme = viewModel::setDarkTheme,
                                onOpenOpeningBalances = viewModel::openOpeningBalanceDialog,
                                onExportJsonBackup = {
                                    val fileName = "cash_manager_backup_${NumberFormatters.formatFileDate()}.json"
                                    exportBackupLauncher.launch(fileName)
                                },
                                onImportJsonBackup = {
                                    importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = constrainedModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}
