/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.zeroclaw.android.R

/**
 * Top-level navigation destinations displayed in the bottom navigation bar.
 *
 * Each entry defines both selected and unselected icons along with a
 * content description label used for accessibility.
 *
 * @property selectedIcon Icon displayed when this destination is active.
 * @property unselectedIcon Icon displayed when this destination is inactive.
 * @property labelResId String resource ID for the destination label.
 * @property route Navigation route object for this destination.
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelResId: Int,
    val route: Any,
) {
    /** Dashboard overview with daemon status and activity feed. */
    DASHBOARD(
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        labelResId = R.string.top_level_destination_dashboard,
        route = DashboardRoute,
    ),

    /** Connection list and management. */
    AGENTS(
        selectedIcon = Icons.Filled.SmartToy,
        unselectedIcon = Icons.Outlined.SmartToy,
        labelResId = R.string.top_level_destination_connections,
        route = AgentsRoute,
    ),

    /** Plugin list and management. */
    PLUGINS(
        selectedIcon = Icons.Filled.Extension,
        unselectedIcon = Icons.Outlined.Extension,
        labelResId = R.string.top_level_destination_plugins,
        route = PluginsRoute,
    ),

    /** Interactive terminal REPL for commands and scripting. */
    TERMINAL(
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal,
        labelResId = R.string.top_level_destination_terminal,
        route = TerminalRoute,
    ),

    /** Application settings and configuration. */
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelResId = R.string.top_level_destination_settings,
        route = SettingsRoute,
    ),
}
