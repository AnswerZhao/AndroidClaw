/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.R
import com.zeroclaw.android.ZeroClawApplication
import com.zeroclaw.android.data.StorageHealth
import com.zeroclaw.android.ui.component.CollapsibleSection
import com.zeroclaw.android.ui.component.PinEntryMode
import com.zeroclaw.android.ui.component.PinEntrySheet
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow

/**
 * Security overview screen displaying the current security posture.
 *
 * Shows a summary of autonomy settings, API key health, and active
 * security policies. Each section has a colored status indicator
 * (green/amber/red) based on the current configuration.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel ViewModel providing current settings state.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun SecurityOverviewScreen(
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val application = LocalContext.current.applicationContext as ZeroClawApplication
    val apiKeys by application.apiKeyRepository.keys.collectAsStateWithLifecycle(initialValue = emptyList())
    val storageHealth = application.apiKeyRepository.storageHealth

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(SPACING_SMALL))

        AutonomyLevelCard(autonomyLevel = settings.autonomyLevel)

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SectionHeader(title = stringResource(R.string.security_overview_section_access_controls))
        AccessControlItem(
            label = stringResource(R.string.security_overview_workspace_restriction_label),
            enabled = settings.workspaceOnly,
            enabledDescription = stringResource(R.string.security_overview_state_enabled),
            disabledDescription = stringResource(R.string.security_overview_state_disabled),
            disabledSeverity = DisabledSeverity.WARNING,
        )
        AccessControlItem(
            label = stringResource(R.string.security_overview_high_risk_command_blocking_label),
            enabled = settings.blockHighRiskCommands,
            enabledDescription = stringResource(R.string.security_overview_state_enabled),
            disabledDescription = stringResource(R.string.security_overview_state_disabled),
            disabledSeverity = DisabledSeverity.CRITICAL,
        )
        AccessControlItem(
            label = stringResource(R.string.security_overview_medium_risk_approval_label),
            enabled = settings.requireApprovalMediumRisk,
            enabledDescription = stringResource(R.string.security_overview_state_enabled),
            disabledDescription = stringResource(R.string.security_overview_state_disabled),
            disabledSeverity = DisabledSeverity.WARNING,
        )

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SectionHeader(title = stringResource(R.string.security_overview_section_rate_limits))
        RateLimitItem(
            label = stringResource(R.string.security_overview_max_actions_per_hour_label),
            value = settings.maxActionsPerHour.toString(),
        )
        RateLimitItem(
            label = stringResource(R.string.security_overview_max_cost_per_day_label),
            value = formatCentsToDollars(settings.maxCostPerDayCents),
        )

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        SectionHeader(title = stringResource(R.string.security_overview_section_api_key_health))
        ApiKeyHealthSection(
            keyCount = apiKeys.size,
            storageHealth = storageHealth,
        )

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        AppLockSection(
            lockEnabled = settings.lockEnabled,
            pinHash = settings.pinHash,
            lockTimeoutMinutes = settings.lockTimeoutMinutes,
            onLockEnabledChange = { settingsViewModel.updateLockEnabled(it) },
            onLockTimeoutChange = { settingsViewModel.updateLockTimeoutMinutes(it) },
            onPinSet = { hash ->
                settingsViewModel.updatePinHash(hash)
                settingsViewModel.updateLockEnabled(true)
            },
        )

        Spacer(modifier = Modifier.height(SPACING_LARGE))

        AllowedForbiddenSection(
            allowedCommands = settings.allowedCommands,
            forbiddenPaths = settings.forbiddenPaths,
        )

        Spacer(modifier = Modifier.height(SPACING_LARGE))
    }
}

/**
 * Prominent card displaying the current autonomy level with a colored indicator.
 *
 * @param autonomyLevel The current autonomy level string: "readonly", "supervised", or "full".
 */
@Composable
private fun AutonomyLevelCard(autonomyLevel: String) {
    val color = autonomyLevelColor(autonomyLevel)
    val description = autonomyLevelDescription(autonomyLevel)
    val displayName = autonomyLevelDisplayName(autonomyLevel)
    val a11yDescription = stringResource(R.string.security_overview_autonomy_a11y, displayName, description)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = a11yDescription
                    liveRegion = LiveRegionMode.Polite
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Row(
            modifier = Modifier.padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(STATUS_DOT_SIZE)
                        .clip(CircleShape)
                        .background(color),
            )
            Spacer(modifier = Modifier.width(SPACING_MEDIUM))
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
                Spacer(modifier = Modifier.height(SPACING_TINY))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Single access control item with a status dot indicating enabled/disabled state.
 *
 * @param label Display name of the access control setting.
 * @param enabled Whether the control is currently active.
 * @param enabledDescription Accessibility text when enabled.
 * @param disabledDescription Accessibility text when disabled.
 * @param disabledSeverity Severity level when the control is disabled.
 */
@Composable
private fun AccessControlItem(
    label: String,
    enabled: Boolean,
    enabledDescription: String,
    disabledDescription: String,
    disabledSeverity: DisabledSeverity,
) {
    val color =
        if (enabled) {
            statusGreen()
        } else {
            when (disabledSeverity) {
                DisabledSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                DisabledSeverity.CRITICAL -> MaterialTheme.colorScheme.error
            }
        }
    val statusText = if (enabled) enabledDescription else disabledDescription
    val a11yDescription = stringResource(R.string.security_overview_label_value_a11y, label, statusText)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL)
                .semantics(mergeDescendants = true) {
                    contentDescription = a11yDescription
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(STATUS_DOT_SIZE_SMALL)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(SPACING_MEDIUM))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/**
 * Displays a rate limit entry with label and value.
 *
 * @param label Description of the rate limit.
 * @param value Formatted value string to display.
 */
@Composable
private fun RateLimitItem(
    label: String,
    value: String,
) {
    val a11yDescription = stringResource(R.string.security_overview_label_value_a11y, label, value)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL)
                .semantics(mergeDescendants = true) {
                    contentDescription = a11yDescription
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Section displaying API key storage health and stored key count.
 *
 * @param keyCount Number of API keys currently stored.
 * @param storageHealth Current health state of the encrypted key storage backend.
 */
@Composable
private fun ApiKeyHealthSection(
    keyCount: Int,
    storageHealth: StorageHealth,
) {
    val healthLabel = storageHealthLabel(storageHealth)
    val healthColor = storageHealthColor(storageHealth)
    val a11yDescription = stringResource(R.string.security_overview_api_key_storage_a11y, healthLabel, keyCount)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = a11yDescription
                    liveRegion = LiveRegionMode.Polite
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(STATUS_DOT_SIZE_SMALL)
                        .clip(CircleShape)
                        .background(healthColor),
            )
            Spacer(modifier = Modifier.width(SPACING_MEDIUM))
            Text(
                text = stringResource(R.string.security_overview_storage_value, healthLabel),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = healthLabel,
                style = MaterialTheme.typography.labelMedium,
                color = healthColor,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier.width(STATUS_DOT_SIZE_SMALL + SPACING_MEDIUM),
            )
            Text(
                text = stringResource(R.string.security_overview_keys_stored_count, keyCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Collapsible sections displaying allowed commands and forbidden paths.
 *
 * Each entry is rendered as a suggestion chip for easy scanning.
 *
 * @param allowedCommands Comma-separated list of allowed shell commands.
 * @param forbiddenPaths Comma-separated list of forbidden filesystem paths.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllowedForbiddenSection(
    allowedCommands: String,
    forbiddenPaths: String,
) {
    val commands = remember(allowedCommands) { parseCommaSeparated(allowedCommands) }
    val paths = remember(forbiddenPaths) { parseCommaSeparated(forbiddenPaths) }

    CollapsibleSection(title = stringResource(R.string.security_overview_allowed_commands_title, commands.size)) {
        if (commands.isEmpty()) {
            Text(
                text = stringResource(R.string.security_overview_no_commands_configured),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL),
                verticalArrangement = Arrangement.spacedBy(SPACING_TINY),
            ) {
                commands.forEach { command ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(command) },
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(SPACING_MEDIUM))

    CollapsibleSection(title = stringResource(R.string.security_overview_forbidden_paths_title, paths.size)) {
        if (paths.isEmpty()) {
            Text(
                text = stringResource(R.string.security_overview_no_paths_configured),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL),
                verticalArrangement = Arrangement.spacedBy(SPACING_TINY),
            ) {
                paths.forEach { path ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(path) },
                    )
                }
            }
        }
    }
}

/**
 * Section for configuring the app-wide session lock (PIN-only).
 *
 * Provides PIN setup/change, lock enable/disable toggle, and a timeout picker.
 *
 * @param lockEnabled Whether the session lock is currently active.
 * @param pinHash The stored PIN hash, empty if no PIN is set.
 * @param lockTimeoutMinutes Current lock timeout in minutes.
 * @param onLockEnabledChange Callback for the lock toggle.
 * @param onLockTimeoutChange Callback for timeout changes.
 * @param onPinSet Callback with the new PIN hash after setup/change.
 */
@Suppress("LongParameterList")
@Composable
private fun AppLockSection(
    lockEnabled: Boolean,
    pinHash: String,
    lockTimeoutMinutes: Int,
    onLockEnabledChange: (Boolean) -> Unit,
    onLockTimeoutChange: (Int) -> Unit,
    onPinSet: (String) -> Unit,
) {
    val pinHashSet = pinHash.isNotEmpty()
    var showPinSheet by remember { mutableStateOf(false) }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    SectionHeader(title = stringResource(R.string.security_overview_section_app_lock))

    FilledTonalButton(
        onClick = { showPinSheet = true },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL),
    ) {
        Text(
            if (pinHashSet) {
                stringResource(R.string.security_overview_change_pin)
            } else {
                stringResource(R.string.security_overview_set_up_pin)
            },
        )
    }

    if (pinHashSet) {
        SettingsToggleRow(
            title = stringResource(R.string.security_overview_enable_app_lock_title),
            subtitle = stringResource(R.string.security_overview_enable_app_lock_subtitle),
            checked = lockEnabled,
            onCheckedChange = onLockEnabledChange,
            contentDescription = stringResource(R.string.security_overview_enable_app_lock_content_description),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.security_overview_lock_after),
                style = MaterialTheme.typography.bodyLarge,
            )
            Box {
                TextButton(onClick = { showTimeoutMenu = true }) {
                    Text(formatTimeout(lockTimeoutMinutes))
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showTimeoutMenu,
                    onDismissRequest = { showTimeoutMenu = false },
                ) {
                    TIMEOUT_OPTIONS.forEach { minutes ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(formatTimeout(minutes)) },
                            onClick = {
                                onLockTimeoutChange(minutes)
                                showTimeoutMenu = false
                            },
                        )
                    }
                }
            }
        }
    }

    if (showPinSheet) {
        PinEntrySheet(
            mode = if (pinHashSet) PinEntryMode.CHANGE else PinEntryMode.SETUP,
            currentPinHash = pinHash,
            onPinSet = { hash ->
                onPinSet(hash)
                showPinSheet = false
            },
            onDismiss = { showPinSheet = false },
        )
    }
}

/**
 * Resolves the display color for the given autonomy level.
 *
 * @return Green for "readonly", tertiary/amber for "supervised", error/red for "full".
 */
@Composable
private fun autonomyLevelColor(level: String): Color =
    when (level) {
        AUTONOMY_READONLY -> statusGreen()
        AUTONOMY_SUPERVISED -> MaterialTheme.colorScheme.tertiary
        AUTONOMY_FULL -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

/**
 * Returns a human-readable description of the autonomy level.
 *
 * @return Description string explaining what the level permits.
 */
@Composable
private fun autonomyLevelDescription(level: String): String =
    when (level) {
        AUTONOMY_READONLY -> stringResource(R.string.security_overview_autonomy_readonly_description)
        AUTONOMY_SUPERVISED -> stringResource(R.string.security_overview_autonomy_supervised_description)
        AUTONOMY_FULL -> stringResource(R.string.security_overview_autonomy_full_description)
        else -> stringResource(R.string.security_overview_autonomy_unknown)
    }

/**
 * Returns a formatted display name for the autonomy level.
 *
 * @return Capitalized level name suitable for UI display.
 */
@Composable
private fun autonomyLevelDisplayName(level: String): String =
    when (level) {
        AUTONOMY_READONLY -> stringResource(R.string.security_overview_autonomy_readonly)
        AUTONOMY_SUPERVISED -> stringResource(R.string.security_overview_autonomy_supervised)
        AUTONOMY_FULL -> stringResource(R.string.security_overview_autonomy_full)
        else -> level.replaceFirstChar { it.uppercase() }
    }

/**
 * Returns the display label for the given storage health state.
 *
 * @return "Healthy", "Degraded", or "Error" string.
 */
@Composable
private fun storageHealthLabel(health: StorageHealth): String =
    when (health) {
        is StorageHealth.Healthy -> stringResource(R.string.security_overview_storage_healthy)
        is StorageHealth.Recovered -> stringResource(R.string.security_overview_storage_recovered)
        is StorageHealth.Degraded -> stringResource(R.string.security_overview_storage_degraded)
    }

/**
 * Returns the status color for the given storage health state.
 *
 * @return Green for healthy, tertiary/amber for recovered, error/red for degraded.
 */
@Composable
private fun storageHealthColor(health: StorageHealth): Color =
    when (health) {
        is StorageHealth.Healthy -> statusGreen()
        is StorageHealth.Recovered -> MaterialTheme.colorScheme.tertiary
        is StorageHealth.Degraded -> MaterialTheme.colorScheme.error
    }

/**
 * Returns the Material 3 primary color, used as the "green" status indicator.
 *
 * The primary color in M3 dynamic theming provides sufficient contrast
 * against surface backgrounds while harmonizing with the overall palette.
 *
 * @return The [MaterialTheme] primary color.
 */
@Composable
private fun statusGreen(): Color = MaterialTheme.colorScheme.primary

/**
 * Formats a cent amount as a dollar string.
 *
 * @return Formatted string such as "$5.00".
 */
private fun formatCentsToDollars(cents: Int): String {
    val dollars = cents / CENTS_PER_DOLLAR
    val remainder = cents % CENTS_PER_DOLLAR
    return "$$dollars.${remainder.toString().padStart(DECIMAL_PLACES, '0')}"
}

/**
 * Parses a comma-separated string into a list of trimmed, non-empty entries.
 *
 * @return List of individual entries with leading/trailing whitespace removed.
 */
private fun parseCommaSeparated(value: String): List<String> =
    value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

/**
 * Severity level for a disabled access control, determining the status dot color.
 *
 * [WARNING] uses tertiary/amber to indicate a non-optimal but acceptable state.
 * [CRITICAL] uses error/red to indicate a significant security gap.
 */
private enum class DisabledSeverity {
    /** Non-optimal but acceptable state (tertiary/amber). */
    WARNING,

    /** Significant security gap (error/red). */
    CRITICAL,
}

/**
 * Formats a timeout value in minutes to a human-readable string.
 *
 * @return e.g. "1 min", "5 min", "30 min".
 */
@Composable
private fun formatTimeout(minutes: Int): String = stringResource(R.string.security_overview_timeout_minutes, minutes)

/** Available lock timeout options in minutes. */
private val TIMEOUT_OPTIONS = listOf(1, 5, 15, 30)

/** Autonomy level constant for read-only mode. */
private const val AUTONOMY_READONLY = "readonly"

/** Autonomy level constant for supervised mode. */
private const val AUTONOMY_SUPERVISED = "supervised"

/** Autonomy level constant for full/unrestricted mode. */
private const val AUTONOMY_FULL = "full"

/** Number of cents in one dollar for formatting. */
private const val CENTS_PER_DOLLAR = 100

/** Number of decimal places for dollar formatting. */
private const val DECIMAL_PLACES = 2

/** Tiny spacing between closely related elements (4dp). */
private val SPACING_TINY = 4.dp

/** Small spacing for list item vertical padding (8dp). */
private val SPACING_SMALL = 8.dp

/** Medium spacing between grouped elements (12dp). */
private val SPACING_MEDIUM = 12.dp

/** Large spacing between sections (16dp). */
private val SPACING_LARGE = 16.dp

/** Internal padding for card content (16dp). */
private val CARD_PADDING = 16.dp

/** Size of the status dot in the autonomy card (12dp). */
private val STATUS_DOT_SIZE = 12.dp

/** Size of the status dot in list items (8dp). */
private val STATUS_DOT_SIZE_SMALL = 8.dp
