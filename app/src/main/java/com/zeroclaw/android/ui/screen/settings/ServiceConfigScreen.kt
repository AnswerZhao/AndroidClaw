/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.R
import com.zeroclaw.android.model.AppSettings
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow

/** Maximum slider value for temperature. */
private const val TEMPERATURE_MAX = 2.0f

/** Number of steps for temperature slider. */
private const val TEMPERATURE_STEPS = 20

/** Minimum valid port number. */
private const val PORT_MIN = 1

/** Maximum valid port number. */
private const val PORT_MAX = 65535

/** Maximum provider retry count. */
private const val RETRIES_MAX = 10

/** Maximum cost warning threshold percentage. */
private const val WARN_PERCENT_MAX = 100

/** Available memory backend options. */
private val MEMORY_BACKENDS = listOf("sqlite", "none", "markdown", "lucid")

/**
 * Service configuration sub-screen for host, port, auto-start,
 * inference, memory, reliability, and cost settings.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun ServiceConfigScreen(
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

        SectionHeader(title = stringResource(R.string.service_config_section_network))

        OutlinedTextField(
            value = settings.host,
            onValueChange = { settingsViewModel.updateHost(it) },
            label = { Text(stringResource(R.string.service_config_host_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val portText = settings.port.toString()
        val portError = settings.port !in PORT_MIN..PORT_MAX

        OutlinedTextField(
            value = portText,
            onValueChange = { value ->
                value.toIntOrNull()?.let { settingsViewModel.updatePort(it) }
            },
            label = { Text(stringResource(R.string.service_config_port_label)) },
            singleLine = true,
            isError = portError,
            supportingText =
                if (portError) {
                    {
                        Text(
                            stringResource(
                                R.string.service_config_port_error,
                                PORT_MIN,
                                PORT_MAX,
                            ),
                        )
                    }
                } else {
                    null
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.service_config_section_startup))

        SettingsToggleRow(
            title = stringResource(R.string.service_config_auto_start_title),
            subtitle = stringResource(R.string.service_config_auto_start_subtitle),
            checked = settings.autoStartOnBoot,
            onCheckedChange = { settingsViewModel.updateAutoStartOnBoot(it) },
            contentDescription = stringResource(R.string.service_config_auto_start_title),
        )

        DefaultsSection(settings = settings, viewModel = settingsViewModel)
        InferenceSection(settings = settings, viewModel = settingsViewModel)
        MemorySection(settings = settings, viewModel = settingsViewModel)
        ReliabilitySection(settings = settings, viewModel = settingsViewModel)
        CostLimitsSection(settings = settings, viewModel = settingsViewModel)
        ProxySection(settings = settings, viewModel = settingsViewModel)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Default provider and model section: free-form text fields for the
 * provider ID and model name applied to new agents.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@Composable
private fun DefaultsSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.service_config_section_defaults))

    OutlinedTextField(
        value = settings.defaultProvider,
        onValueChange = { viewModel.updateDefaultProvider(it) },
        label = { Text(stringResource(R.string.service_config_default_provider_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = settings.defaultModel,
        onValueChange = { viewModel.updateDefaultModel(it) },
        label = { Text(stringResource(R.string.service_config_default_model_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

/**
 * Inference settings section: temperature slider and compact context toggle.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@Composable
private fun InferenceSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    val temperatureSliderDescription =
        stringResource(R.string.service_config_temperature_slider_content_description)

    SectionHeader(title = stringResource(R.string.service_config_section_inference))

    Text(
        text =
            stringResource(
                R.string.service_config_temperature_value,
                settings.defaultTemperature,
            ),
        style = MaterialTheme.typography.bodyLarge,
    )
    Slider(
        value = settings.defaultTemperature,
        onValueChange = { viewModel.updateDefaultTemperature(it) },
        valueRange = 0f..TEMPERATURE_MAX,
        steps = TEMPERATURE_STEPS - 1,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = temperatureSliderDescription
                },
    )

    SettingsToggleRow(
        title = stringResource(R.string.service_config_compact_context_title),
        subtitle = stringResource(R.string.service_config_compact_context_subtitle),
        checked = settings.compactContext,
        onCheckedChange = { viewModel.updateCompactContext(it) },
        contentDescription = stringResource(R.string.service_config_compact_context_title),
    )

    SettingsToggleRow(
        title = stringResource(R.string.service_config_strip_thinking_tags_title),
        subtitle = stringResource(R.string.service_config_strip_thinking_tags_subtitle),
        checked = settings.stripThinkingTags,
        onCheckedChange = { viewModel.updateStripThinkingTags(it) },
        contentDescription = stringResource(R.string.service_config_strip_thinking_tags_content_description),
    )
}

/**
 * Memory backend dropdown section.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemorySection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.service_config_section_memory))

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = settings.memoryBackend,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.service_config_memory_backend_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (backend in MEMORY_BACKENDS) {
                DropdownMenuItem(
                    text = { Text(backend) },
                    onClick = {
                        viewModel.updateMemoryBackend(backend)
                        expanded = false
                    },
                )
            }
        }
    }

    SettingsToggleRow(
        title = stringResource(R.string.service_config_memory_auto_save_title),
        subtitle = stringResource(R.string.service_config_memory_auto_save_subtitle),
        checked = settings.memoryAutoSave,
        onCheckedChange = { viewModel.updateMemoryAutoSave(it) },
        contentDescription = stringResource(R.string.service_config_memory_auto_save_title),
    )
}

/**
 * Reliability settings section: retries and fallback providers.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@Composable
private fun ReliabilitySection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.service_config_section_reliability))

    val retriesError = settings.providerRetries !in 0..RETRIES_MAX

    OutlinedTextField(
        value = settings.providerRetries.toString(),
        onValueChange = { value ->
            value.toIntOrNull()?.let { viewModel.updateProviderRetries(it) }
        },
        label = { Text(stringResource(R.string.service_config_provider_retries_label)) },
        singleLine = true,
        isError = retriesError,
        supportingText =
            if (retriesError) {
                { Text(stringResource(R.string.service_config_provider_retries_error, RETRIES_MAX)) }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.fallbackProviders,
        onValueChange = { viewModel.updateFallbackProviders(it) },
        label = { Text(stringResource(R.string.service_config_fallback_providers_label)) },
        supportingText = { Text(stringResource(R.string.service_config_fallback_providers_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.reliabilityBackoffMs.toString(),
        onValueChange = { value ->
            value.toLongOrNull()?.let { viewModel.updateReliabilityBackoffMs(it) }
        },
        label = { Text(stringResource(R.string.service_config_provider_backoff_label)) },
        supportingText = { Text(stringResource(R.string.service_config_provider_backoff_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.reliabilityApiKeysJson,
        onValueChange = { viewModel.updateReliabilityApiKeysJson(it) },
        label = { Text(stringResource(R.string.service_config_provider_api_keys_label)) },
        supportingText = { Text(stringResource(R.string.service_config_provider_api_keys_hint)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Cost limits settings section: enable toggle, daily/monthly limits, and
 * warning threshold.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@Composable
private fun CostLimitsSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.service_config_section_cost_limits))

    Text(
        text = stringResource(R.string.service_config_budget_tracking_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SettingsToggleRow(
        title = stringResource(R.string.service_config_enable_cost_limits_title),
        subtitle = stringResource(R.string.service_config_enable_cost_limits_subtitle),
        checked = settings.costEnabled,
        onCheckedChange = { viewModel.updateCostEnabled(it) },
        contentDescription = stringResource(R.string.service_config_enable_cost_limits_title),
    )

    val dailyError = settings.costEnabled && settings.dailyLimitUsd < 0f

    OutlinedTextField(
        value = settings.dailyLimitUsd.toString(),
        onValueChange = { value ->
            value.toFloatOrNull()?.let { viewModel.updateDailyLimitUsd(it) }
        },
        label = { Text(stringResource(R.string.service_config_daily_limit_label)) },
        singleLine = true,
        isError = dailyError,
        supportingText =
            if (dailyError) {
                { Text(stringResource(R.string.service_config_positive_amount_error)) }
            } else {
                null
            },
        enabled = settings.costEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )

    val monthlyError = settings.costEnabled && settings.monthlyLimitUsd < 0f

    OutlinedTextField(
        value = settings.monthlyLimitUsd.toString(),
        onValueChange = { value ->
            value.toFloatOrNull()?.let { viewModel.updateMonthlyLimitUsd(it) }
        },
        label = { Text(stringResource(R.string.service_config_monthly_limit_label)) },
        singleLine = true,
        isError = monthlyError,
        supportingText =
            if (monthlyError) {
                { Text(stringResource(R.string.service_config_positive_amount_error)) }
            } else {
                null
            },
        enabled = settings.costEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )

    val warnError = settings.costEnabled && settings.costWarnAtPercent !in 0..WARN_PERCENT_MAX

    OutlinedTextField(
        value = settings.costWarnAtPercent.toString(),
        onValueChange = { value ->
            value.toIntOrNull()?.let { viewModel.updateCostWarnAtPercent(it) }
        },
        label = { Text(stringResource(R.string.service_config_warn_percent_label)) },
        singleLine = true,
        isError = warnError,
        supportingText =
            if (warnError) {
                { Text(stringResource(R.string.service_config_warn_percent_error, WARN_PERCENT_MAX)) }
            } else {
                null
            },
        enabled = settings.costEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Proxy configuration section.
 *
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 */
@Composable
private fun ProxySection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.service_config_section_proxy))

    SettingsToggleRow(
        title = stringResource(R.string.service_config_enable_proxy_title),
        subtitle = stringResource(R.string.service_config_enable_proxy_subtitle),
        checked = settings.proxyEnabled,
        onCheckedChange = { viewModel.updateProxyEnabled(it) },
        contentDescription = stringResource(R.string.service_config_enable_proxy_title),
    )

    OutlinedTextField(
        value = settings.proxyHttpProxy,
        onValueChange = { viewModel.updateProxyHttpProxy(it) },
        label = { Text(stringResource(R.string.service_config_http_proxy_label)) },
        supportingText = { Text(stringResource(R.string.service_config_http_proxy_hint)) },
        singleLine = true,
        enabled = settings.proxyEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.proxyHttpsProxy,
        onValueChange = { viewModel.updateProxyHttpsProxy(it) },
        label = { Text(stringResource(R.string.service_config_https_proxy_label)) },
        singleLine = true,
        enabled = settings.proxyEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.proxyAllProxy,
        onValueChange = { viewModel.updateProxyAllProxy(it) },
        label = { Text(stringResource(R.string.service_config_all_proxy_label)) },
        supportingText = { Text(stringResource(R.string.service_config_all_proxy_hint)) },
        singleLine = true,
        enabled = settings.proxyEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.proxyNoProxy,
        onValueChange = { viewModel.updateProxyNoProxy(it) },
        label = { Text(stringResource(R.string.service_config_no_proxy_label)) },
        supportingText = { Text(stringResource(R.string.service_config_no_proxy_hint)) },
        enabled = settings.proxyEnabled,
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.proxyScope,
        onValueChange = { viewModel.updateProxyScope(it) },
        label = { Text(stringResource(R.string.service_config_scope_label)) },
        supportingText = { Text(stringResource(R.string.service_config_scope_hint)) },
        singleLine = true,
        enabled = settings.proxyEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.proxyServiceSelectors,
        onValueChange = { viewModel.updateProxyServiceSelectors(it) },
        label = { Text(stringResource(R.string.service_config_service_selectors_label)) },
        supportingText = { Text(stringResource(R.string.service_config_service_selectors_hint)) },
        enabled = settings.proxyEnabled,
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}
