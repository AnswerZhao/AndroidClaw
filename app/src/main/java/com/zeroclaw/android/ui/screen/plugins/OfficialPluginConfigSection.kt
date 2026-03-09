/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.plugins

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R
import com.zeroclaw.android.model.AppSettings
import com.zeroclaw.android.model.OfficialPlugins
import com.zeroclaw.android.ui.component.SecretTextField
import com.zeroclaw.android.ui.component.SettingsToggleRow
import com.zeroclaw.android.ui.screen.settings.SettingsViewModel

/** Available web search engine options. */
private val WEB_SEARCH_ENGINES = listOf("duckduckgo")

/**
 * Renders a purpose-built configuration form for an official plugin.
 *
 * Dispatches to a per-plugin section composable based on [officialPluginId].
 * Each section reads from [settings] and writes changes via [viewModel],
 * mirroring the fields previously found in `WebAccessScreen` and
 * `ToolManagementScreen`.
 *
 * @param officialPluginId One of the [OfficialPlugins] constant IDs.
 * @param settings Current application settings.
 * @param viewModel The [SettingsViewModel] for persisting changes.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun OfficialPluginConfigSection(
    officialPluginId: String,
    settings: AppSettings,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when (officialPluginId) {
            OfficialPlugins.WEB_SEARCH -> WebSearchConfig(settings, viewModel)
            OfficialPlugins.WEB_FETCH -> WebFetchConfig(settings, viewModel)
            OfficialPlugins.HTTP_REQUEST -> HttpRequestConfig(settings, viewModel)
            OfficialPlugins.COMPOSIO -> ComposioConfig(settings, viewModel)
            OfficialPlugins.VISION -> VisionConfig(settings, viewModel)
            OfficialPlugins.TRANSCRIPTION -> TranscriptionConfig(settings, viewModel)
            OfficialPlugins.QUERY_CLASSIFICATION -> QueryClassificationConfig(settings, viewModel)
        }
    }
}

/**
 * Web search plugin configuration.
 *
 * Controls the search engine provider, max results, and timeout.
 * Maps to upstream `[tools.web_search]` TOML section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebSearchConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    var engineExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = engineExpanded,
        onExpandedChange = { engineExpanded = it },
    ) {
        OutlinedTextField(
            value = settings.webSearchProvider,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.official_plugin_search_engine_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(engineExpanded) },
            enabled = settings.webSearchEnabled,
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = engineExpanded,
            onDismissRequest = { engineExpanded = false },
        ) {
            for (engine in WEB_SEARCH_ENGINES) {
                DropdownMenuItem(
                    text = { Text(engine) },
                    onClick = {
                        viewModel.updateWebSearchProvider(engine)
                        engineExpanded = false
                    },
                )
            }
        }
    }

    OutlinedTextField(
        value = settings.webSearchMaxResults.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateWebSearchMaxResults(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_results_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_search_results_hint)) },
        singleLine = true,
        enabled = settings.webSearchEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.webSearchTimeoutSecs.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateWebSearchTimeoutSecs(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_timeout_seconds_label)) },
        singleLine = true,
        enabled = settings.webSearchEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Web fetch plugin configuration.
 *
 * Controls domain allowlists, blocklists, response size limits, and
 * timeouts. Maps to upstream `[tools.web_fetch]` TOML section.
 */
@Composable
private fun WebFetchConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    OutlinedTextField(
        value = settings.webFetchAllowedDomains,
        onValueChange = { viewModel.updateWebFetchAllowedDomains(it) },
        label = { Text(stringResource(R.string.official_plugin_allowed_domains_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_allowed_domains_optional_hint)) },
        enabled = settings.webFetchEnabled,
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.webFetchBlockedDomains,
        onValueChange = { viewModel.updateWebFetchBlockedDomains(it) },
        label = { Text(stringResource(R.string.official_plugin_blocked_domains_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_blocked_domains_hint)) },
        enabled = settings.webFetchEnabled,
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.webFetchMaxResponseSize.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateWebFetchMaxResponseSize(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_response_size_label)) },
        singleLine = true,
        enabled = settings.webFetchEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.webFetchTimeoutSecs.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateWebFetchTimeoutSecs(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_timeout_seconds_label)) },
        singleLine = true,
        enabled = settings.webFetchEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * HTTP request plugin configuration.
 *
 * Controls domain allowlists, response size limits, and timeouts.
 * Uses a deny-by-default policy. Maps to upstream `[tools.http_request]`
 * TOML section.
 */
@Composable
private fun HttpRequestConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    OutlinedTextField(
        value = settings.httpRequestAllowedDomains,
        onValueChange = { viewModel.updateHttpRequestAllowedDomains(it) },
        label = { Text(stringResource(R.string.official_plugin_allowed_domains_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_allowed_domains_required_hint)) },
        enabled = settings.httpRequestEnabled,
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.httpRequestMaxResponseSize.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateHttpRequestMaxResponseSize(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_response_size_label)) },
        singleLine = true,
        enabled = settings.httpRequestEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.httpRequestTimeoutSecs.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateHttpRequestTimeoutSecs(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_timeout_seconds_label)) },
        singleLine = true,
        enabled = settings.httpRequestEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = stringResource(R.string.official_plugin_http_request_policy_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )

    if (settings.httpRequestEnabled && settings.httpRequestAllowedDomains.isBlank()) {
        Text(
            text = stringResource(R.string.official_plugin_http_request_empty_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Composio integration plugin configuration.
 *
 * Controls the API key and entity ID for third-party tool integrations
 * via Composio. Maps to upstream `[composio]` TOML section.
 */
@Composable
private fun ComposioConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SecretTextField(
        value = settings.composioApiKey,
        onValueChange = { viewModel.updateComposioApiKey(it) },
        label = stringResource(R.string.official_plugin_composio_api_key_label),
        enabled = settings.composioEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.composioEntityId,
        onValueChange = { viewModel.updateComposioEntityId(it) },
        label = { Text(stringResource(R.string.official_plugin_entity_id_label)) },
        singleLine = true,
        enabled = settings.composioEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    if (settings.composioEnabled && settings.composioApiKey.isBlank()) {
        Text(
            text = stringResource(R.string.official_plugin_composio_api_key_required_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Vision / multimodal plugin configuration.
 *
 * Controls image limits and remote fetch behaviour. Maps to upstream
 * `[multimodal]` TOML section.
 */
@Composable
private fun VisionConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    OutlinedTextField(
        value = settings.multimodalMaxImages.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateMultimodalMaxImages(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_images_per_request_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_max_images_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.multimodalMaxImageSizeMb.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateMultimodalMaxImageSizeMb(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_image_size_mb_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_max_image_size_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    SettingsToggleRow(
        title = stringResource(R.string.official_plugin_allow_remote_fetch_title),
        subtitle = stringResource(R.string.official_plugin_allow_remote_fetch_subtitle),
        checked = settings.multimodalAllowRemoteFetch,
        onCheckedChange = { viewModel.updateMultimodalAllowRemoteFetch(it) },
        contentDescription = stringResource(R.string.official_plugin_allow_remote_fetch_content_description),
    )
}

/**
 * Transcription plugin configuration.
 *
 * Controls the Whisper-compatible API endpoint, model, language, and
 * max duration. Maps to upstream `[transcription]` TOML section.
 */
@Composable
private fun TranscriptionConfig(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    OutlinedTextField(
        value = settings.transcriptionApiUrl,
        onValueChange = { viewModel.updateTranscriptionApiUrl(it) },
        label = { Text(stringResource(R.string.official_plugin_api_url_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_transcription_endpoint_hint)) },
        singleLine = true,
        enabled = settings.transcriptionEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.transcriptionModel,
        onValueChange = { viewModel.updateTranscriptionModel(it) },
        label = { Text(stringResource(R.string.official_plugin_model_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_transcription_model_hint)) },
        singleLine = true,
        enabled = settings.transcriptionEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.transcriptionLanguage,
        onValueChange = { viewModel.updateTranscriptionLanguage(it) },
        label = { Text(stringResource(R.string.official_plugin_language_hint_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_language_hint_supporting)) },
        singleLine = true,
        enabled = settings.transcriptionEnabled,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = settings.transcriptionMaxDurationSecs.toString(),
        onValueChange = { v ->
            v.toIntOrNull()?.let { viewModel.updateTranscriptionMaxDurationSecs(it) }
        },
        label = { Text(stringResource(R.string.official_plugin_max_duration_seconds_label)) },
        supportingText = { Text(stringResource(R.string.official_plugin_max_duration_supporting)) },
        singleLine = true,
        enabled = settings.transcriptionEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Query classification plugin configuration.
 *
 * This plugin has no additional configuration beyond the enable toggle
 * (handled by the parent screen). Shows a brief description of the
 * feature.
 */
@Composable
private fun QueryClassificationConfig(
    @Suppress("UNUSED_PARAMETER") settings: AppSettings,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel,
) {
    Text(
        text = stringResource(R.string.official_plugin_query_classification_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}
