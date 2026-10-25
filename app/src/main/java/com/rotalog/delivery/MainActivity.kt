package com.rotalog.delivery

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rotalog.delivery.login.LoginScreen
import com.rotalog.delivery.login.LoginViewModel
import com.rotalog.delivery.history.HistoryScreen
import com.rotalog.delivery.history.HistoryRouteScreen
import com.rotalog.delivery.model.*
import com.rotalog.delivery.profile.ProfileScreen
import com.rotalog.delivery.routes.*
import com.rotalog.delivery.shared.*
import com.rotalog.delivery.ui.theme.RotalogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RotalogApp((application as RotalogApplication).container) }
    }
}

@Composable
fun RotalogApp(container: AppContainer) {
    val settings: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(container.settings) }
    })
    val theme by settings.theme.collectAsStateWithLifecycle()
    val dark = when (theme) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; else -> isSystemInDarkTheme() }
    val activity = LocalContext.current.findActivity()
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    RotalogTheme(darkTheme = dark) {
        if (theme == null) {
            Surface(Modifier.fillMaxSize()) { Box(Modifier.safeDrawingPadding()) { CircularProgressIndicator() } }
        } else {
            val login: LoginViewModel = viewModel(factory = viewModelFactory {
                initializer { LoginViewModel(createSavedStateHandle()) }
            })
            val loginState by login.uiState.collectAsStateWithLifecycle()
            if (loginState.exploringDemo) {
                AppNavigation(container, settings, theme!!)
            } else {
                LoginScreen(loginState, login::setIdentifier, login::submit, login::exploreDemo)
            }
        }
    }
}

@Composable
private fun AppNavigation(container: AppContainer, settings: SettingsViewModel, theme: ThemeMode) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val actions = remember(context) { ExternalActions(context) }
    var navigateStopId by rememberSaveable { mutableStateOf<String?>(null) }
    var navigationFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(navigateStopId) { navigationFeedback = null }
    val settingsError by settings.error.collectAsStateWithLifecycle()
    LaunchedEffect(settingsError) { settingsError?.let { snackbar.showSnackbar(it); settings.clearError() } }
    val tabs = listOf("routes" to "Rotas", "history" to "Histórico", "profile" to "Perfil")
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = .2f))
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, (destination, label) ->
                        NavigationBarItem(
                            selected = entry?.destination?.hierarchy?.any { it.route == destination } == true,
                            onClick = {
                                nav.navigate(destination) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(listOf(NavigationIcons.Routes, NavigationIcons.History, NavigationIcons.Profile)[index], contentDescription = null) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "routes", route = "main", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            navigation(startDestination = "route", route = "routes") {
                composable("route") { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    RoutesScreen(state, vm::acceptRoute, vm::selectStop, { open -> if (open) state.selectedStopId?.let(vm::openDetails) else vm.closeDetails() },
                        { id -> nav.navigate("stops/$id") }, { navigateStopId = it.id }, { actions.dial(it) }, vm::nextStop,
                        { nav.navigate("preparation/$it") }, { nav.navigate("receipt/$it") }, vm::startRoute, actions::copyAddress, navigationOpen = navigateStopId != null, onProfile = {
                            nav.navigate("profile") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, onStep = vm::selectDepartureStep, onBackStep = vm::previousDepartureStep,
                        onCargoConfirmation = vm::setCargoConfirmation, onOpenIssue = vm::openCargoIssue,
                        onDismissIssue = vm::dismissCargoIssue, onEditIssue = vm::editCargoIssue,
                        onSubmitIssue = vm::submitCargoIssue, onResolveIssue = vm::resolveCargoIssue, onReceive = vm::confirmReceipt)
                }
                composable("preparation/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    LegacyDepartureRedirect(state, backStack.arguments?.getString("routeId"), vm::openLegacyDeparture) {
                        nav.navigate("route") { popUpTo("route") { inclusive = true }; launchSingleTop = true }
                    }
                }
                composable("receipt/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    LegacyDepartureRedirect(state, backStack.arguments?.getString("routeId"), vm::openLegacyDeparture) {
                        nav.navigate("route") { popUpTo("route") { inclusive = true }; launchSingleTop = true }
                    }
                }
                composable("stops/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    val route = state.route?.takeIf { it.id == backStack.arguments?.getString("routeId") }
                    StopsScreen(route, { vm.openDetails(it); nav.popBackStack() }, { nav.popBackStack() })
                }
            }
            navigation(startDestination = "historyHome", route = "history") {
                composable("historyHome") {
                    val history by container.routes.history.collectAsStateWithLifecycle()
                    HistoryScreen(history) { nav.navigate("historyRoute/$it") }
                }
                composable("historyRoute/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val history by container.routes.history.collectAsStateWithLifecycle()
                    HistoryRouteScreen(history.find { it.id == backStack.arguments?.getString("routeId") && it.status == RouteStatus.COMPLETED }) { nav.popBackStack() }
                }
            }
            composable("profile") { backStack ->
                val vm = routeViewModel(nav, backStack, container)
                val state by vm.uiState.collectAsStateWithLifecycle()
                ProfileScreen(theme, state.scenario, state.route?.syncStatus ?: SyncStatus.LOCAL,
                    settings::setTheme, vm::selectScenario, vm::simulateSync, vm::resetScenario)
            }
        }
    }
    val stop = container.routes.route.collectAsStateWithLifecycle().value?.stops?.find { it.id == navigateStopId }
    if (stop != null) {
        AlertDialog(
            onDismissRequest = { navigateStopId = null },
            title = { Text("Escolha como navegar") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stop.address)
                    ActionFeedback(navigationFeedback)
                    OutlinedButton(onClick = { navigationFeedback = actions.navigate(stop, NavigationApp.GOOGLE_MAPS); if (navigationFeedback == null) navigateStopId = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Google Maps") }
                    OutlinedButton(onClick = { navigationFeedback = actions.navigate(stop, NavigationApp.WAZE); if (navigationFeedback == null) navigateStopId = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Waze") }
                    OutlinedButton(onClick = { navigationFeedback = actions.copyAddress(stop) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Copiar endereço") }
                }
            },
            confirmButton = { TextButton(onClick = { navigateStopId = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun routeViewModel(nav: NavHostController, entry: NavBackStackEntry, container: AppContainer): RoutesViewModel {
    val owner = remember(entry) { nav.getBackStackEntry("main") }
    return viewModel(owner, factory = viewModelFactory {
        initializer { RoutesViewModel(container.routes, container.routes, createSavedStateHandle()) }
    })
}

private fun Context.findActivity(): Activity {
    var contexto = this
    while (contexto is ContextWrapper) {
        if (contexto is Activity) return contexto
        contexto = contexto.baseContext
    }
    error("Activity ausente")
}