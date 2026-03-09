/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zeroclaw.android.R
import com.zeroclaw.android.ZeroClawApplication
import com.zeroclaw.android.model.ServiceState
import com.zeroclaw.android.ui.component.LockGateScreen
import com.zeroclaw.android.ui.component.StatusDot
import com.zeroclaw.android.viewmodel.DaemonViewModel

/** Set of top-level routes where the bottom navigation bar should be visible. */
private val topLevelRoutes = TopLevelDestination.entries.map { it.route::class }

/**
 * Root composable providing the application shell with adaptive navigation
 * and a top app bar.
 *
 * Uses [NavigationSuiteScaffold] to automatically switch between a bottom
 * navigation bar (< 600dp), navigation rail (600-840dp), and navigation
 * drawer (840dp+) based on the current window width.
 *
 * The [StatusDot] is visible in the top bar on all screens to provide
 * persistent daemon status feedback.
 *
 * @param windowWidthSizeClass Current [WindowWidthSizeClass] for responsive layout.
 * @param viewModel The [DaemonViewModel] for daemon state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroClawAppShell(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: DaemonViewModel = viewModel(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as ZeroClawApplication
    val onboardingRepo = app.onboardingRepository
    val onboardingCompleted by onboardingRepo.isCompleted.collectAsStateWithLifecycle(
        initialValue = true,
    )

    val startDestination: Any =
        if (onboardingCompleted) DashboardRoute else OnboardingRoute

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()

    val isLocked by app.sessionLockManager.isLocked.collectAsStateWithLifecycle()
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue =
            com.zeroclaw.android.model
                .AppSettings(),
    )
    val isOnboarding = currentDestination?.hasRoute(OnboardingRoute::class) == true
    val shouldShowLock =
        isLocked &&
            settings.lockEnabled &&
            settings.pinHash.isNotEmpty() &&
            !isOnboarding

    val isTopLevel =
        !isOnboarding &&
            currentDestination?.hierarchy?.any { dest ->
                topLevelRoutes.any { routeClass -> dest.hasRoute(routeClass) }
            } == true

    val edgeMargin =
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp

    Box(modifier = Modifier.fillMaxSize()) {
        if (isOnboarding) {
            Scaffold { innerPadding ->
                ZeroClawNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    edgeMargin = edgeMargin,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        } else if (isTopLevel) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hierarchy?.any { dest ->
                                dest.hasRoute(destination.route::class)
                            } == true
                        val label = context.getString(destination.labelResId)
                        item(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector =
                                        if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                    contentDescription = label,
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                },
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                TopBarTitle(serviceState = serviceState)
                            },
                        )
                    },
                ) { innerPadding ->
                    ZeroClawNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        edgeMargin = edgeMargin,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            TopBarTitle(
                                serviceState = serviceState,
                                title =
                                    screenTitleResFor(currentDestination)?.let(context::getString)
                                        ?: context.getString(R.string.app_name),
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = context.getString(R.string.navigation_back_content_description),
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                ZeroClawNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    edgeMargin = edgeMargin,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        if (shouldShowLock) {
            LockGateScreen(
                pinHash = settings.pinHash,
                onUnlock = { app.sessionLockManager.unlock() },
            )
        }
    }
}

/**
 * Returns a human-readable screen title for a sub-screen destination.
 *
 * @param destination The current [NavDestination], or null.
 * @return String resource ID for the title, or null if the destination is unknown.
 */
@Suppress("CyclomaticComplexMethod")
@StringRes
private fun screenTitleResFor(destination: NavDestination?): Int? {
    if (destination == null) return null
    return when {
        destination.hasRoute(AboutRoute::class) -> R.string.nav_screen_title_about
        destination.hasRoute(AddAgentRoute::class) -> R.string.nav_screen_title_add_connection
        destination.hasRoute(AgentDetailRoute::class) -> R.string.nav_screen_title_connection
        destination.hasRoute(ApiKeyDetailRoute::class) -> R.string.nav_screen_title_api_key
        destination.hasRoute(ApiKeysRoute::class) -> R.string.nav_screen_title_api_keys
        destination.hasRoute(AutonomyRoute::class) -> R.string.nav_screen_title_autonomy
        destination.hasRoute(BatterySettingsRoute::class) -> R.string.nav_screen_title_battery
        destination.hasRoute(ChannelDetailRoute::class) -> R.string.nav_screen_title_channel
        destination.hasRoute(ConnectedChannelsRoute::class) -> R.string.nav_screen_title_channels
        destination.hasRoute(CostDetailRoute::class) -> R.string.nav_screen_title_cost_tracking
        destination.hasRoute(CronJobsRoute::class) -> R.string.nav_screen_title_cron_jobs
        destination.hasRoute(DoctorRoute::class) -> R.string.nav_screen_title_doctor
        destination.hasRoute(GatewayRoute::class) -> R.string.nav_screen_title_gateway
        destination.hasRoute(IdentityRoute::class) -> R.string.nav_screen_title_identity
        destination.hasRoute(LogViewerRoute::class) -> R.string.nav_screen_title_logs
        destination.hasRoute(MemoryAdvancedRoute::class) -> R.string.nav_screen_title_memory
        destination.hasRoute(MemoryBrowserRoute::class) -> R.string.nav_screen_title_memory_browser
        destination.hasRoute(ModelRoutesRoute::class) -> R.string.nav_screen_title_model_routes
        destination.hasRoute(ObservabilityRoute::class) -> R.string.nav_screen_title_observability
        destination.hasRoute(PluginDetailRoute::class) -> R.string.nav_screen_title_plugin
        destination.hasRoute(PluginRegistryRoute::class) -> R.string.nav_screen_title_plugin_registry
        destination.hasRoute(QrScannerRoute::class) -> R.string.nav_screen_title_qr_scanner
        destination.hasRoute(SchedulerRoute::class) -> R.string.nav_screen_title_scheduler
        destination.hasRoute(SecurityOverviewRoute::class) -> R.string.nav_screen_title_security
        destination.hasRoute(ServiceConfigRoute::class) -> R.string.nav_screen_title_service_config
        destination.hasRoute(TunnelRoute::class) -> R.string.nav_screen_title_tunnel
        destination.hasRoute(UpdatesRoute::class) -> R.string.nav_screen_title_updates
        else -> null
    }
}

/**
 * Top app bar title row with app name and daemon [StatusDot].
 *
 * @param serviceState Current [ServiceState] shown in the status dot.
 * @param title Text displayed as the bar title.
 */
@Composable
private fun TopBarTitle(
    serviceState: ServiceState,
    title: String = "ZeroClaw",
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title)
        Spacer(modifier = Modifier.width(8.dp))
        StatusDot(state = serviceState)
    }
}
