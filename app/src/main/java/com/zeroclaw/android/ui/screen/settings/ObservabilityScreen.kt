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

/** Available observability backends matching upstream options. */
private val OBS_BACKENDS = listOf("none", "log", "otel")

/**
 * Observability backend configuration screen.
 *
 * Maps to the upstream `[observability]` TOML section: backend selection
 * and OpenTelemetry collector endpoint/service name for distributed tracing.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservabilityScreen(
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var backendExpanded by remember { mutableStateOf(false) }
    val isOtel = settings.observabilityBackend == "otel"

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.observability_section_backend))

        ExposedDropdownMenuBox(
            expanded = backendExpanded,
            onExpandedChange = { backendExpanded = it },
        ) {
            OutlinedTextField(
                value = observabilityBackendLabel(settings.observabilityBackend),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.observability_backend_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(backendExpanded) },
                modifier =
                    Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = backendExpanded,
                onDismissRequest = { backendExpanded = false },
            ) {
                for (backend in OBS_BACKENDS) {
                    DropdownMenuItem(
                        text = { Text(observabilityBackendLabel(backend)) },
                        onClick = {
                            settingsViewModel.updateObservabilityBackend(backend)
                            backendExpanded = false
                        },
                    )
                }
            }
        }

        Text(
            text =
                when (settings.observabilityBackend) {
                    "log" -> stringResource(R.string.observability_backend_log_description)
                    "otel" -> stringResource(R.string.observability_backend_otel_description)
                    else -> stringResource(R.string.observability_backend_none_description)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isOtel) {
            SectionHeader(title = stringResource(R.string.observability_section_open_telemetry))

            OutlinedTextField(
                value = settings.observabilityOtelEndpoint,
                onValueChange = { settingsViewModel.updateObservabilityOtelEndpoint(it) },
                label = { Text(stringResource(R.string.observability_collector_endpoint_label)) },
                supportingText = { Text(stringResource(R.string.observability_collector_endpoint_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = settings.observabilityOtelServiceName,
                onValueChange = { settingsViewModel.updateObservabilityOtelServiceName(it) },
                label = { Text(stringResource(R.string.observability_service_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun observabilityBackendLabel(backend: String): String =
    when (backend) {
        "none" -> stringResource(R.string.observability_backend_none_label)
        "log" -> stringResource(R.string.observability_backend_log_label)
        "otel" -> stringResource(R.string.observability_backend_otel_label)
        else -> backend
    }
