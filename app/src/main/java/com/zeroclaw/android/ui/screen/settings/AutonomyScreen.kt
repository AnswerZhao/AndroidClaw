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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow

/** Available autonomy levels matching upstream AutonomyLevel enum. */
private val AUTONOMY_LEVELS = listOf("readonly", "supervised", "full")

/**
 * Autonomy level and security policy configuration screen.
 *
 * Maps to the upstream `[autonomy]` TOML section: level picker,
 * workspace restriction, allowed commands, forbidden paths, action
 * limits, and risk approval toggles.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutonomyScreen(
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var levelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.autonomy_section_level))

        ExposedDropdownMenuBox(
            expanded = levelExpanded,
            onExpandedChange = { levelExpanded = it },
        ) {
            OutlinedTextField(
                value = autonomyLevelLabel(settings.autonomyLevel),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.autonomy_level_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(levelExpanded) },
                modifier =
                    Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = levelExpanded,
                onDismissRequest = { levelExpanded = false },
            ) {
                for (level in AUTONOMY_LEVELS) {
                    DropdownMenuItem(
                        text = { Text(autonomyLevelLabel(level)) },
                        onClick = {
                            settingsViewModel.updateAutonomyLevel(level)
                            levelExpanded = false
                        },
                    )
                }
            }
        }

        Text(
            text =
                when (settings.autonomyLevel) {
                    "readonly" -> stringResource(R.string.autonomy_level_readonly_description)
                    "full" -> stringResource(R.string.autonomy_level_full_description)
                    else -> stringResource(R.string.autonomy_level_supervised_description)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader(title = stringResource(R.string.autonomy_section_workspace))

        SettingsToggleRow(
            title = stringResource(R.string.autonomy_workspace_only_title),
            subtitle = stringResource(R.string.autonomy_workspace_only_subtitle),
            checked = settings.workspaceOnly,
            onCheckedChange = { settingsViewModel.updateWorkspaceOnly(it) },
            contentDescription = stringResource(R.string.autonomy_workspace_only_content_description),
        )

        SectionHeader(title = stringResource(R.string.autonomy_section_commands))

        OutlinedTextField(
            value = settings.allowedCommands,
            onValueChange = { settingsViewModel.updateAllowedCommands(it) },
            label = { Text(stringResource(R.string.autonomy_allowed_commands_label)) },
            supportingText = { Text(stringResource(R.string.autonomy_allowed_commands_hint)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.forbiddenPaths,
            onValueChange = { settingsViewModel.updateForbiddenPaths(it) },
            label = { Text(stringResource(R.string.autonomy_forbidden_paths_label)) },
            supportingText = { Text(stringResource(R.string.autonomy_forbidden_paths_hint)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.autonomy_section_limits))

        OutlinedTextField(
            value = settings.maxActionsPerHour.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { settingsViewModel.updateMaxActionsPerHour(it) } },
            label = { Text(stringResource(R.string.autonomy_max_actions_per_hour_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.maxCostPerDayCents.toString(),
            onValueChange = { v ->
                v.toIntOrNull()?.let { settingsViewModel.updateMaxCostPerDayCents(it) }
            },
            label = { Text(stringResource(R.string.autonomy_max_cost_per_day_label)) },
            supportingText = {
                Text(stringResource(R.string.autonomy_max_cost_hint))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.autonomy_section_risk_management))

        SettingsToggleRow(
            title = stringResource(R.string.autonomy_require_approval_medium_title),
            subtitle = stringResource(R.string.autonomy_require_approval_medium_subtitle),
            checked = settings.requireApprovalMediumRisk,
            onCheckedChange = { settingsViewModel.updateRequireApprovalMediumRisk(it) },
            contentDescription = stringResource(R.string.autonomy_require_approval_medium_content_description),
        )

        SettingsToggleRow(
            title = stringResource(R.string.autonomy_block_high_risk_title),
            subtitle = stringResource(R.string.autonomy_block_high_risk_subtitle),
            checked = settings.blockHighRiskCommands,
            onCheckedChange = { settingsViewModel.updateBlockHighRiskCommands(it) },
            contentDescription = stringResource(R.string.autonomy_block_high_risk_content_description),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun autonomyLevelLabel(level: String): String =
    when (level) {
        "readonly" -> stringResource(R.string.autonomy_level_readonly_label)
        "supervised" -> stringResource(R.string.autonomy_level_supervised_label)
        "full" -> stringResource(R.string.autonomy_level_full_label)
        else -> level
    }
