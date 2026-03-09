/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Plugin registry sync configuration screen.
 *
 * Provides controls for enabling/disabling automatic sync, setting
 * the registry URL, choosing the sync interval, and triggering a
 * manual sync. Shows the last successful sync timestamp.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param onSyncNow Callback to trigger an immediate plugin registry sync.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginRegistryScreen(
    edgeMargin: Dp,
    onSyncNow: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.plugin_registry_section_auto_sync))

        SettingsToggleRow(
            title = stringResource(R.string.plugin_registry_enable_automatic_sync_title),
            subtitle = stringResource(R.string.plugin_registry_enable_automatic_sync_subtitle),
            checked = settings.pluginSyncEnabled,
            onCheckedChange = { settingsViewModel.updatePluginSyncEnabled(it) },
            contentDescription = stringResource(R.string.plugin_registry_enable_automatic_sync_content_description),
        )

        SectionHeader(title = stringResource(R.string.plugin_registry_section_registry_url))

        OutlinedTextField(
            value = settings.pluginRegistryUrl,
            onValueChange = { settingsViewModel.updatePluginRegistryUrl(it) },
            label = { Text(stringResource(R.string.plugin_registry_url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.plugin_registry_section_sync_interval))

        SyncIntervalDropdown(
            selectedHours = settings.pluginSyncIntervalHours,
            onSelected = { settingsViewModel.updatePluginSyncIntervalHours(it) },
        )

        SectionHeader(title = stringResource(R.string.plugin_registry_section_last_sync))

        Text(
            text =
                if (settings.lastPluginSyncTimestamp > 0L) {
                    formatTimestamp(settings.lastPluginSyncTimestamp)
                } else {
                    stringResource(R.string.plugin_registry_never_synced)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FilledTonalButton(
            onClick = onSyncNow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.plugin_registry_sync_now))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Dropdown for selecting the plugin sync interval.
 *
 * @param selectedHours Currently selected interval in hours.
 * @param onSelected Callback when a new interval is chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncIntervalDropdown(
    selectedHours: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = INTERVAL_OPTIONS
    val selectedLabel =
        if (selectedHours in options) {
            stringResource(R.string.plugin_registry_interval_every_hours, selectedHours)
        } else {
            stringResource(R.string.plugin_registry_selected_hours_fallback, selectedHours)
        }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.plugin_registry_interval_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { hours ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plugin_registry_interval_every_hours, hours)) },
                    onClick = {
                        onSelected(hours)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Date-time formatter for plugin registry sync timestamps. */
private val registryTimestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

/**
 * Formats a Unix timestamp in milliseconds to a human-readable date/time string.
 *
 * @param timestamp Milliseconds since epoch.
 * @return Formatted date/time string.
 */
private fun formatTimestamp(timestamp: Long): String = registryTimestampFormat.format(Instant.ofEpochMilli(timestamp))

/** Sync interval options in hours. */
private val INTERVAL_OPTIONS =
    listOf(
        6,
        12,
        24,
        48,
    )
