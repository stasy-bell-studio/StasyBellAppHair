package com.stasybell.os.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.ui.screens.AppointmentDetailScreen
import com.stasybell.os.ui.screens.CalendarScreen
import com.stasybell.os.ui.screens.ClientDetailScreen
import com.stasybell.os.ui.screens.ClientsListScreen
import com.stasybell.os.ui.screens.FinanceScreen
import com.stasybell.os.ui.screens.LoginScreen
import com.stasybell.os.ui.screens.MaterialsScreen
import com.stasybell.os.ui.screens.MoreAndSettingsScreen
import com.stasybell.os.ui.screens.NewAppointmentScreen
import com.stasybell.os.ui.screens.QuickTransactionPrefill
import com.stasybell.os.ui.screens.ReportsScreen
import com.stasybell.os.ui.screens.ScheduleScreen
import com.stasybell.os.ui.screens.ServicesScreen
import com.stasybell.os.ui.screens.TodayScreen
import com.stasybell.os.ui.screens.WebsitePreviewScreen
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.MainViewModel
import com.stasybell.os.ui.viewmodel.UiMessage
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable data object TodayRoute
@Serializable data object CalendarRoute
@Serializable data object ClientsRoute
@Serializable data object FinanceRoute
@Serializable data object MoreRoute
@Serializable data class NewAppointmentRoute(val dateIso: String, val clientId: String? = null)
@Serializable data class AppointmentDetailRoute(val entryId: String)
@Serializable data class ClientDetailRoute(val clientId: String)
@Serializable data object ServicesRoute
@Serializable data object ScheduleRoute
@Serializable data object MaterialsRoute
@Serializable data object ReportsRoute
@Serializable data object WebsitePreviewRoute

private data class TopTab(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TopTabs = listOf(
    TopTab("Главная", TodayRoute, Icons.Filled.Home, Icons.Outlined.Home, "tab_today"),
    TopTab("Календарь", CalendarRoute, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendar"),
    TopTab("Клиенты", ClientsRoute, Icons.Filled.People, Icons.Outlined.People, "tab_clients"),
    TopTab("Финансы", FinanceRoute, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_finance"),
    TopTab("Ещё", MoreRoute, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "tab_more")
)

@Composable
fun StasyBellApp(
    viewModel: MainViewModel,
    onRequestBiometricAuth: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastMessageIsError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            when (msg) {
                is UiMessage.Success -> {
                    lastMessageIsError = false
                    snackbarHostState.showSnackbar(msg.text)
                }
                is UiMessage.Error -> {
                    lastMessageIsError = true
                    snackbarHostState.showSnackbar(msg.text)
                }
            }
        }
    }

    if (!state.isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ForestPalette.Primary)
        }
        return
    }

    if (!state.isAuthenticated) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (lastMessageIsError) ForestPalette.Error else ForestPalette.PrimaryDeep,
                        contentColor = Color.White
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LoginScreen(
                    state = state,
                    onLogin = viewModel::login,
                    onBiometricClick = onRequestBiometricAuth,
                    onSwitchMode = viewModel::switchDataMode
                )
            }
        }
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    var financePrefill by remember { mutableStateOf<QuickTransactionPrefill?>(null) }

    val showChrome = currentDest == null ||
        currentDest.hasRoute<TodayRoute>() ||
        currentDest.hasRoute<CalendarRoute>() ||
        currentDest.hasRoute<ClientsRoute>() ||
        currentDest.hasRoute<FinanceRoute>() ||
        currentDest.hasRoute<MoreRoute>()

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ForestPalette.Canvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (lastMessageIsError) ForestPalette.Error else ForestPalette.PrimaryDeep,
                        contentColor = Color.White
                    )
                }
            },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = ForestPalette.Surface,
                        tonalElevation = 2.dp
                    ) {
                        TopTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ForestPalette.Primary,
                                    selectedTextColor = ForestPalette.Primary,
                                    indicatorColor = ForestPalette.MintSoft,
                                    unselectedIconColor = ForestPalette.Muted,
                                    unselectedTextColor = ForestPalette.Muted
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (showChrome && useRail) {
                    NavigationRail(containerColor = ForestPalette.Surface) {
                        TopTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TodayRoute,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable<TodayRoute> {
                        TodayScreen(
                            state = state,
                            onNewAppointment = {
                                navController.navigate(NewAppointmentRoute(state.selectedDate.toString()))
                            },
                            onQuickIncome = {
                                financePrefill = QuickTransactionPrefill(FinanceKind.INCOME)
                                navigateTopTab(FinanceRoute)
                            },
                            onQuickExpense = {
                                financePrefill = QuickTransactionPrefill(FinanceKind.EXPENSE)
                                navigateTopTab(FinanceRoute)
                            },
                            onOpenCalendar = { navigateTopTab(CalendarRoute) },
                            onOpenEntryDetail = { id -> navController.navigate(AppointmentDetailRoute(id)) }
                        )
                    }

                    composable<CalendarRoute> {
                        CalendarScreen(
                            state = state,
                            onSelectDate = viewModel::selectDate,
                            onNewAppointment = { d -> navController.navigate(NewAppointmentRoute(d.toString())) },
                            onCreateBlock = { start, dur, note -> viewModel.createBlock(start, dur, note) },
                            onOpenEntryDetail = { id -> navController.navigate(AppointmentDetailRoute(id)) },
                            onOpenSchedule = { navController.navigate(ScheduleRoute) }
                        )
                    }

                    composable<ClientsRoute> {
                        ClientsListScreen(
                            state = state,
                            onOpenClient = { cid -> navController.navigate(ClientDetailRoute(cid)) },
                            onSaveClient = { id, name, phone, notes, ch ->
                                viewModel.saveClient(id, name, phone, notes, ch)
                            },
                            onBookClient = { cid ->
                                navController.navigate(NewAppointmentRoute(state.selectedDate.toString(), cid))
                            }
                        )
                    }

                    composable<FinanceRoute> {
                        FinanceScreen(
                            state = state,
                            prefillRequest = financePrefill,
                            onConsumePrefill = { financePrefill = null },
                            onSelectMonth = viewModel::selectMonth,
                            onAddTransaction = { kind, cat, kopecks, at, note, entryId, key ->
                                viewModel.addFinanceTransaction(kind, cat, kopecks, at, note, entryId, key)
                            },
                            onReverseTransaction = viewModel::reverseFinanceTransaction,
                            onCloseMonth = viewModel::closeMonth,
                            onReopenMonth = { ym, reason -> viewModel.reopenMonth(ym, reason) }
                        )
                    }

                    composable<MoreRoute> {
                        MoreAndSettingsScreen(
                            state = state,
                            onOpenServices = { navController.navigate(ServicesRoute) },
                            onOpenSchedule = { navController.navigate(ScheduleRoute) },
                            onOpenMaterials = { navController.navigate(MaterialsRoute) },
                            onOpenReports = { navController.navigate(ReportsRoute) },
                            onOpenWebsite = { navController.navigate(WebsitePreviewRoute) },
                            onToggleBiometric = viewModel::setBiometricEnabled,
                            onCheckHealth = viewModel::checkServerHealth,
                            onSwitchMode = viewModel::switchDataMode,
                            onRevokeSession = viewModel::revokeSession,
                            onLogout = viewModel::logout
                        )
                    }

                    composable<NewAppointmentRoute> { entry ->
                        val args = entry.toRoute<NewAppointmentRoute>()
                        NewAppointmentScreen(
                            state = state,
                            initialDate = LocalDate.parse(args.dateIso),
                            initialClientId = args.clientId,
                            onBack = { navController.popBackStack() },
                            onQuickCreateClient = { name, phone, onCreated ->
                                viewModel.saveClient(null, name, phone, "", PreferredChannel.NONE, onCreated)
                            },
                            onSubmit = { cid, sid, sname, src, start, dur, buf, price, note, key ->
                                viewModel.createManualAppointment(
                                    clientId = cid,
                                    serviceId = sid,
                                    serviceName = sname,
                                    source = src,
                                    startsAt = start,
                                    durationMinutes = dur,
                                    bufferMinutes = buf,
                                    quotedPriceKopecks = price,
                                    note = note,
                                    idempotencyKey = key,
                                    onCreated = { navController.popBackStack() }
                                )
                            }
                        )
                    }

                    composable<AppointmentDetailRoute> { entry ->
                        val args = entry.toRoute<AppointmentDetailRoute>()
                        AppointmentDetailScreen(
                            entryId = args.entryId,
                            state = state,
                            eventsFlow = remember(args.entryId) { viewModel.observeEntryEvents(args.entryId) },
                            onBack = { navController.popBackStack() },
                            onUpdateStatus = { st -> viewModel.updateEntryStatus(args.entryId, st) },
                            onMove = { nextStart -> viewModel.moveEntry(args.entryId, nextStart) },
                            onSoftDelete = { viewModel.softDeleteEntry(args.entryId) },
                            onRestore = { viewModel.restoreEntry(args.entryId) },
                            onRecordManualPayment = { eid, kopecks, note ->
                                financePrefill = QuickTransactionPrefill(FinanceKind.INCOME, kopecks, note, eid)
                                navigateTopTab(FinanceRoute)
                            }
                        )
                    }

                    composable<ClientDetailRoute> { entry ->
                        val args = entry.toRoute<ClientDetailRoute>()
                        ClientDetailScreen(
                            clientId = args.clientId,
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSaveClient = { id, name, phone, notes, ch ->
                                viewModel.saveClient(id, name, phone, notes, ch)
                            },
                            onBookClient = { cid ->
                                navController.navigate(NewAppointmentRoute(state.selectedDate.toString(), cid))
                            },
                            onOpenEntry = { eid -> navController.navigate(AppointmentDetailRoute(eid)) }
                        )
                    }

                    composable<ServicesRoute> {
                        ServicesScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSaveService = { id, cat, name, variant, price, dur, buf, active ->
                                viewModel.saveService(id, cat, name, variant, price, dur, buf, active)
                            }
                        )
                    }

                    composable<ScheduleRoute> {
                        ScheduleScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSaveWindows = { d, list -> viewModel.saveWorkingWindows(d, list) }
                        )
                    }

                    composable<MaterialsRoute> {
                        MaterialsScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSaveMaterial = { id, name, unit, thr -> viewModel.saveMaterial(id, name, unit, thr) },
                            onRecordMovement = { mid, delta, reason -> viewModel.recordMaterialMovement(mid, delta, reason) }
                        )
                    }

                    composable<ReportsRoute> {
                        ReportsScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSelectMonth = viewModel::selectMonth,
                            onLinkLegacyVisit = viewModel::linkLegacyVisitToClient
                        )
                    }

                    composable<WebsitePreviewRoute> {
                        WebsitePreviewScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onSubmitWebsiteTestBooking = viewModel::createWebsiteTestBooking
                        )
                    }
                }
            }
        }
    }
}
