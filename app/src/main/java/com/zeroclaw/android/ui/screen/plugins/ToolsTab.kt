/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.plugins

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.model.ToolSpec
import com.zeroclaw.android.ui.component.CategoryBadge
import com.zeroclaw.android.ui.component.EmptyState
import com.zeroclaw.android.ui.component.ErrorCard
import com.zeroclaw.android.ui.component.LoadingIndicator
import com.zeroclaw.android.ui.screen.settings.tools.SOURCE_ALL
import com.zeroclaw.android.ui.screen.settings.tools.ToolsBrowserViewModel
import com.zeroclaw.android.ui.screen.settings.tools.ToolsUiState

/**
 * Content for the Tools tab inside the combined Plugins screen.
 *
 * Shows a searchable, filterable list of tools loaded from the daemon.
 * Reuses [ToolsBrowserViewModel] for state management.
 *
 * @param toolsBrowserViewModel ViewModel providing tools state and actions.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun ToolsTab(
    toolsBrowserViewModel: ToolsBrowserViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val filteredState by toolsBrowserViewModel.filteredUiState.collectAsStateWithLifecycle()
    val searchQuery by toolsBrowserViewModel.searchQuery.collectAsStateWithLifecycle()
    val sourceFilter by toolsBrowserViewModel.sourceFilter.collectAsStateWithLifecycle()
    val sources by toolsBrowserViewModel.availableSources.collectAsStateWithLifecycle()
    val crossChannelNote = stringResource(R.string.tools_tab_cross_channel_note)
    val emptyNoToolsAvailable = stringResource(R.string.tools_tab_empty_no_tools_available)
    val emptyNoToolsMatch = stringResource(R.string.tools_tab_empty_no_tools_match)

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = crossChannelNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { toolsBrowserViewModel.updateSearch(it) },
            label = { Text(stringResource(R.string.plugins_search_tools)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))

        SourceFilterRow(
            sources = sources,
            selected = sourceFilter,
            onSelect = { toolsBrowserViewModel.setSourceFilter(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (val state = filteredState) {
            is ToolsUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            is ToolsUiState.Error -> {
                ErrorCard(
                    message = state.detail,
                    onRetry = { toolsBrowserViewModel.loadTools() },
                )
            }
            is ToolsUiState.Content -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Build,
                        message =
                            if (searchQuery.isBlank() && sourceFilter == SOURCE_ALL) {
                                emptyNoToolsAvailable
                            } else {
                                emptyNoToolsMatch
                            },
                        modifier =
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                } else {
                    ToolsList(tools = state.data)
                }
            }
        }
    }
}

/**
 * Horizontal row of filter chips for selecting tool source.
 *
 * @param sources Available source filter values.
 * @param selected Currently selected source.
 * @param onSelect Callback when a chip is selected.
 */
@Composable
private fun SourceFilterRow(
    sources: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sources.forEach { source ->
            val filterBySourceContentDescription =
                stringResource(R.string.tools_tab_filter_by_source_content_description, source)
            FilterChip(
                selected = source == selected,
                onClick = { onSelect(source) },
                label = { Text(source) },
                modifier =
                    Modifier.semantics {
                        contentDescription = filterBySourceContentDescription
                    },
            )
        }
    }
}

/**
 * Lazy column of tool cards.
 *
 * @param tools List of tools to display.
 */
@Composable
private fun ToolsList(tools: List<ToolSpec>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = tools,
            key = { it.name },
            contentType = { "tool" },
        ) { tool ->
            ToolCard(tool = tool)
        }
    }
}

/**
 * Card displaying a single tool with its name, description, and source.
 *
 * The card merges its descendants for accessibility and builds a content
 * description that gracefully omits the description when it is blank.
 *
 * @param tool The tool specification to display.
 * @param modifier Modifier applied to the card.
 */
@Composable
private fun ToolCard(
    tool: ToolSpec,
    modifier: Modifier = Modifier,
) {
    val statusLabel =
        if (tool.isActive) {
            stringResource(R.string.tools_tab_status_active)
        } else {
            stringResource(R.string.tools_tab_status_inactive_with_reason, tool.inactiveReason)
        }
    val toolCardContentDescription =
        if (tool.description.isNotBlank()) {
            stringResource(
                R.string.tools_tab_tool_card_content_description_with_description,
                tool.name,
                tool.description,
                tool.source,
                statusLabel,
            )
        } else {
            stringResource(
                R.string.tools_tab_tool_card_content_description_no_description,
                tool.name,
                tool.source,
                statusLabel,
            )
        }
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = toolCardContentDescription
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (tool.isActive) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        if (tool.isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                CategoryBadge(category = tool.source)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!tool.isActive && tool.inactiveReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.inactiveReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
