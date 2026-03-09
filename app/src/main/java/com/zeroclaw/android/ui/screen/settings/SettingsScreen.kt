/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.model.AppSettings
import com.zeroclaw.android.model.ThemeMode
import com.zeroclaw.android.navigation.SettingsNavAction
import com.zeroclaw.android.ui.component.RestartRequiredBanner
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsListItem

/**
 * Root settings screen displaying a sectioned list of configuration options.
 *
 * Thin stateful wrapper that collects ViewModel flows and delegates
 * rendering to [SettingsContent].
 *
 * @param onNavigate Callback invoked with a [SettingsNavAction] when the user taps a setting.
 * @param onRerunWizard Callback to reset onboarding and navigate to the setup wizard.
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel ViewModel providing current settings for dynamic subtitles.
 * @param restartRequired Whether the daemon needs a restart to apply settings changes.
 * @param onRestartDaemon Callback invoked when the user taps the restart button.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun SettingsScreen(
    onNavigate: (SettingsNavAction) -> Unit,
    onRerunWizard: () -> Unit,
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    restartRequired: Boolean = false,
    onRestartDaemon: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    SettingsContent(
        settings = settings,
        restartRequired = restartRequired,
        edgeMargin = edgeMargin,
        onNavigate = onNavigate,
        onRerunWizard = onRerunWizard,
        onRestartDaemon = onRestartDaemon,
        onThemeSelected = settingsViewModel::updateTheme,
        modifier = modifier,
    )
}

/**
 * Stateless settings content composable for testing.
 *
 * @param settings Current app settings snapshot.
 * @param restartRequired Whether the daemon needs a restart.
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param onNavigate Callback for settings navigation.
 * @param onRerunWizard Callback to reset onboarding.
 * @param onRestartDaemon Callback to restart the daemon.
 * @param onThemeSelected Callback when a theme is chosen.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
internal fun SettingsContent(
    settings: AppSettings,
    restartRequired: Boolean,
    edgeMargin: Dp,
    onNavigate: (SettingsNavAction) -> Unit,
    onRerunWizard: () -> Unit,
    onRestartDaemon: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRerunDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (restartRequired) {
            RestartRequiredBanner(
                edgeMargin = edgeMargin,
                onRestartDaemon = onRestartDaemon,
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_daemon))
        SettingsListItem(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.settings_item_service_configuration),
            subtitle =
                "${settings.host}:${settings.port}" +
                    if (settings.autoStartOnBoot) {
                        " | ${stringResource(R.string.settings_service_auto_start_suffix)}"
                    } else {
                        ""
                    },
            onClick = { onNavigate(SettingsNavAction.ServiceConfig) },
        )
        SettingsListItem(
            icon = Icons.Outlined.BatteryAlert,
            title = stringResource(R.string.settings_item_battery_settings),
            subtitle = stringResource(R.string.settings_item_battery_settings_subtitle),
            onClick = { onNavigate(SettingsNavAction.Battery) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Fingerprint,
            title = stringResource(R.string.settings_item_agent_identity),
            subtitle =
                if (settings.identityJson.isNotBlank()) {
                    stringResource(R.string.settings_state_configured)
                } else {
                    stringResource(R.string.settings_state_not_set)
                },
            onClick = { onNavigate(SettingsNavAction.Identity) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_security))
        SettingsListItem(
            icon = Icons.Outlined.VerifiedUser,
            title = stringResource(R.string.settings_item_security_overview),
            subtitle = stringResource(R.string.settings_item_security_overview_subtitle),
            onClick = { onNavigate(SettingsNavAction.SecurityOverview) },
        )
        SettingsListItem(
            icon = Icons.Outlined.GppGood,
            title = stringResource(R.string.settings_item_security_advanced),
            subtitle = stringResource(R.string.settings_item_security_advanced_subtitle),
            onClick = { onNavigate(SettingsNavAction.SecurityAdvanced) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Key,
            title = stringResource(R.string.settings_item_api_keys),
            subtitle = stringResource(R.string.settings_item_api_keys_subtitle),
            onClick = { onNavigate(SettingsNavAction.ApiKeys) },
        )
        SettingsListItem(
            icon = Icons.Outlined.AccountCircle,
            title = stringResource(R.string.settings_item_auth_profiles),
            subtitle = stringResource(R.string.settings_item_auth_profiles_subtitle),
            onClick = { onNavigate(SettingsNavAction.AuthProfiles) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.settings_item_autonomy_level),
            subtitle = settings.autonomyLevel,
            onClick = { onNavigate(SettingsNavAction.Autonomy) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Forum,
            title = stringResource(R.string.settings_item_connected_channels),
            subtitle = stringResource(R.string.settings_item_connected_channels_subtitle),
            onClick = { onNavigate(SettingsNavAction.Channels) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_network))
        SettingsListItem(
            icon = Icons.Outlined.Hub,
            title = stringResource(R.string.settings_item_gateway_pairing),
            subtitle =
                if (settings.gatewayRequirePairing) {
                    stringResource(R.string.settings_state_pairing_required)
                } else {
                    stringResource(R.string.settings_state_open_access)
                },
            onClick = { onNavigate(SettingsNavAction.Gateway) },
        )
        SettingsListItem(
            icon = Icons.Outlined.VpnKey,
            title = stringResource(R.string.settings_item_tunnel),
            subtitle = settings.tunnelProvider,
            onClick = { onNavigate(SettingsNavAction.Tunnel) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Sync,
            title = stringResource(R.string.settings_item_plugin_registry),
            subtitle =
                if (settings.pluginSyncEnabled) {
                    stringResource(R.string.settings_state_auto_sync_enabled)
                } else {
                    stringResource(R.string.settings_state_manual_only)
                },
            onClick = { onNavigate(SettingsNavAction.PluginRegistry) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_advanced_configuration))
        SettingsListItem(
            icon = Icons.Outlined.Route,
            title = stringResource(R.string.settings_item_model_routes),
            subtitle = stringResource(R.string.settings_item_model_routes_subtitle),
            onClick = { onNavigate(SettingsNavAction.ModelRoutes) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Memory,
            title = stringResource(R.string.settings_item_memory_advanced),
            subtitle = stringResource(R.string.settings_item_memory_advanced_subtitle),
            onClick = { onNavigate(SettingsNavAction.MemoryAdvanced) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Layers,
            title = stringResource(R.string.settings_item_embedding_routes),
            subtitle = stringResource(R.string.settings_item_embedding_routes_subtitle),
            onClick = { onNavigate(SettingsNavAction.EmbeddingRoutes) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Schedule,
            title = stringResource(R.string.settings_item_scheduler_heartbeat),
            subtitle =
                if (settings.schedulerEnabled) {
                    stringResource(R.string.settings_state_scheduler_on)
                } else {
                    stringResource(R.string.settings_state_scheduler_off)
                },
            onClick = { onNavigate(SettingsNavAction.Scheduler) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Speed,
            title = stringResource(R.string.settings_item_observability),
            subtitle = settings.observabilityBackend,
            onClick = { onNavigate(SettingsNavAction.Observability) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_diagnostics))
        SettingsListItem(
            icon = Icons.AutoMirrored.Outlined.Subject,
            title = stringResource(R.string.settings_item_log_viewer),
            subtitle = stringResource(R.string.settings_item_log_viewer_subtitle),
            onClick = { onNavigate(SettingsNavAction.LogViewer) },
        )
        SettingsListItem(
            icon = Icons.Outlined.HealthAndSafety,
            title = stringResource(R.string.settings_item_zeroclaw_doctor),
            subtitle = stringResource(R.string.settings_item_zeroclaw_doctor_subtitle),
            onClick = { onNavigate(SettingsNavAction.Doctor) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_inspect_browse))
        SettingsListItem(
            icon = Icons.Outlined.Psychology,
            title = stringResource(R.string.settings_item_memory_browser),
            subtitle = stringResource(R.string.settings_item_memory_browser_subtitle),
            onClick = { onNavigate(SettingsNavAction.MemoryBrowser) },
        )
        SettingsListItem(
            icon = Icons.Outlined.TaskAlt,
            title = stringResource(R.string.settings_item_scheduled_tasks),
            subtitle = stringResource(R.string.settings_item_scheduled_tasks_subtitle),
            onClick = { onNavigate(SettingsNavAction.CronJobs) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader(title = stringResource(R.string.settings_section_app))
        SettingsListItem(
            icon = Icons.Outlined.DarkMode,
            title = stringResource(R.string.settings_item_theme),
            subtitle = themeModeLabel(settings.theme),
            onClick = { showThemeDialog = true },
        )
        SettingsListItem(
            icon = Icons.Outlined.Refresh,
            title = stringResource(R.string.settings_item_rerun_setup_wizard),
            subtitle = stringResource(R.string.settings_item_rerun_setup_wizard_subtitle),
            onClick = { showRerunDialog = true },
        )
        SettingsListItem(
            icon = Icons.Outlined.SystemUpdate,
            title = stringResource(R.string.settings_item_updates),
            subtitle = stringResource(R.string.settings_item_updates_subtitle),
            onClick = { onNavigate(SettingsNavAction.Updates) },
        )
        SettingsListItem(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.settings_item_about),
            subtitle = stringResource(R.string.settings_item_about_subtitle),
            onClick = { onNavigate(SettingsNavAction.About) },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = settings.theme,
            onThemeSelected = { theme ->
                onThemeSelected(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showRerunDialog) {
        RerunWizardDialog(
            onConfirm = {
                showRerunDialog = false
                onRerunWizard()
            },
            onDismiss = { showRerunDialog = false },
        )
    }
}

/**
 * Dialog for picking the app theme from [ThemeMode] options.
 *
 * Displays three radio-button rows: System, Light, and Dark.
 *
 * @param currentTheme The currently active [ThemeMode].
 * @param onThemeSelected Called with the chosen [ThemeMode] when the user taps an option.
 * @param onDismiss Called when the dialog is dismissed without selection.
 */
@Composable
private fun ThemePickerDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_choose_theme_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val label = themeModeLabel(mode)
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = mode == currentTheme,
                                    onClick = { onThemeSelected(mode) },
                                    role = Role.RadioButton,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == currentTheme,
                            onClick = null,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * Confirmation dialog shown before re-running the setup wizard.
 *
 * @param onConfirm Called when the user confirms.
 * @param onDismiss Called when the user cancels.
 */
@Composable
private fun RerunWizardDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_rerun_setup_wizard_title)) },
        text = {
            Text(
                stringResource(R.string.settings_rerun_setup_wizard_message),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String =
    when (mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system_default)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
