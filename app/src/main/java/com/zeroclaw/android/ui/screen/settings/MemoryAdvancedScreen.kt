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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.ui.component.SecretTextField
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.android.ui.component.SettingsToggleRow

/** Number of slider steps for weight sliders. */
private const val WEIGHT_STEPS = 10

/**
 * Advanced memory configuration screen for embedding, hygiene, recall tuning, and Qdrant vector store.
 *
 * Maps to upstream `[memory]` TOML section extended fields: hygiene
 * (archive/purge thresholds), embedding provider/model, and
 * vector/keyword weights for recall scoring.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun MemoryAdvancedScreen(
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val vectorWeightSliderContentDescription =
        stringResource(R.string.memory_advanced_vector_weight_slider_content_description)
    val keywordWeightSliderContentDescription =
        stringResource(R.string.memory_advanced_keyword_weight_slider_content_description)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.memory_advanced_section_hygiene))

        SettingsToggleRow(
            title = stringResource(R.string.memory_advanced_enable_hygiene_title),
            subtitle = stringResource(R.string.memory_advanced_enable_hygiene_subtitle),
            checked = settings.memoryHygieneEnabled,
            onCheckedChange = { settingsViewModel.updateMemoryHygieneEnabled(it) },
            contentDescription = stringResource(R.string.memory_advanced_enable_hygiene_content_description),
        )

        OutlinedTextField(
            value = settings.memoryArchiveAfterDays.toString(),
            onValueChange = { v ->
                v
                    .toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?.let { settingsViewModel.updateMemoryArchiveAfterDays(it) }
            },
            label = { Text(stringResource(R.string.memory_advanced_archive_after_days)) },
            singleLine = true,
            enabled = settings.memoryHygieneEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.memoryPurgeAfterDays.toString(),
            onValueChange = { v ->
                v
                    .toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?.let { settingsViewModel.updateMemoryPurgeAfterDays(it) }
            },
            label = { Text(stringResource(R.string.memory_advanced_purge_after_days)) },
            singleLine = true,
            enabled = settings.memoryHygieneEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.memory_advanced_section_embedding))

        OutlinedTextField(
            value = settings.memoryEmbeddingProvider,
            onValueChange = { settingsViewModel.updateMemoryEmbeddingProvider(it) },
            label = { Text(stringResource(R.string.memory_advanced_embedding_provider)) },
            supportingText = { Text(stringResource(R.string.memory_advanced_embedding_provider_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.memoryEmbeddingModel,
            onValueChange = { settingsViewModel.updateMemoryEmbeddingModel(it) },
            label = { Text(stringResource(R.string.memory_advanced_embedding_model)) },
            singleLine = true,
            enabled = settings.memoryEmbeddingProvider != "none",
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = stringResource(R.string.memory_advanced_section_recall_weights))

        Text(
            text = stringResource(R.string.memory_advanced_vector_weight_value, settings.memoryVectorWeight),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = settings.memoryVectorWeight,
            onValueChange = { settingsViewModel.updateMemoryVectorWeight(it) },
            valueRange = 0f..1f,
            steps = WEIGHT_STEPS - 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = vectorWeightSliderContentDescription },
        )

        Text(
            text = stringResource(R.string.memory_advanced_keyword_weight_value, settings.memoryKeywordWeight),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = settings.memoryKeywordWeight,
            onValueChange = { settingsViewModel.updateMemoryKeywordWeight(it) },
            valueRange = 0f..1f,
            steps = WEIGHT_STEPS - 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = keywordWeightSliderContentDescription },
        )

        SectionHeader(title = stringResource(R.string.memory_advanced_section_qdrant))

        OutlinedTextField(
            value = settings.memoryQdrantUrl,
            onValueChange = { settingsViewModel.updateMemoryQdrantUrl(it) },
            label = { Text(stringResource(R.string.memory_advanced_qdrant_url)) },
            supportingText = { Text(stringResource(R.string.memory_advanced_qdrant_url_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.memoryQdrantCollection,
            onValueChange = { settingsViewModel.updateMemoryQdrantCollection(it) },
            label = { Text(stringResource(R.string.memory_advanced_collection_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SecretTextField(
            value = settings.memoryQdrantApiKey,
            onValueChange = { settingsViewModel.updateMemoryQdrantApiKey(it) },
            label = stringResource(R.string.provider_credential_api_key_label),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
