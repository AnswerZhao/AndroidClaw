/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.R
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow

/**
 * Scheduler and heartbeat configuration screen.
 *
 * Maps to upstream `[scheduler]` and `[heartbeat]` TOML sections:
 * task scheduling limits and periodic heartbeat tick configuration.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun SchedulerScreen(
    edgeMargin: Dp,
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

        SectionHeader(title = stringResource(R.string.scheduler_section_task_scheduler))

        SettingsToggleRow(
            title = stringResource(R.string.scheduler_enable_scheduler_title),
            subtitle = stringResource(R.string.scheduler_enable_scheduler_subtitle),
            checked = settings.schedulerEnabled,
            onCheckedChange = { settingsViewModel.updateSchedulerEnabled(it) },
            contentDescription = stringResource(R.string.scheduler_enable_scheduler_content_description),
        )

        OutlinedTextField(
            value = settings.schedulerMaxTasks.toString(),
            onValueChange = { v ->
                v.toIntOrNull()?.let { settingsViewModel.updateSchedulerMaxTasks(it) }
            },
            label = { Text(stringResource(R.string.scheduler_max_tasks_label)) },
            singleLine = true,
            enabled = settings.schedulerEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.schedulerMaxConcurrent.toString(),
            onValueChange = { v ->
                v.toIntOrNull()?.let { settingsViewModel.updateSchedulerMaxConcurrent(it) }
            },
            label = { Text(stringResource(R.string.scheduler_max_concurrent_label)) },
            singleLine = true,
            enabled = settings.schedulerEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.scheduler_section_heartbeat))

        SettingsToggleRow(
            title = stringResource(R.string.scheduler_enable_heartbeat_title),
            subtitle = stringResource(R.string.scheduler_enable_heartbeat_subtitle),
            checked = settings.heartbeatEnabled,
            onCheckedChange = { settingsViewModel.updateHeartbeatEnabled(it) },
            contentDescription = stringResource(R.string.scheduler_enable_heartbeat_content_description),
        )

        OutlinedTextField(
            value = settings.heartbeatIntervalMinutes.toString(),
            onValueChange = { v ->
                v.toIntOrNull()?.let { settingsViewModel.updateHeartbeatIntervalMinutes(it) }
            },
            label = { Text(stringResource(R.string.scheduler_interval_minutes_label)) },
            singleLine = true,
            enabled = settings.heartbeatEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
