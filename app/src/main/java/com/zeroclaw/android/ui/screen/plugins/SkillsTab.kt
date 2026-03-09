// Copyright 2026 ZeroClaw Community, MIT License

package com.zeroclaw.android.ui.screen.plugins

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroclaw.android.model.Skill
import com.zeroclaw.android.ui.component.CategoryBadge
import com.zeroclaw.android.ui.component.EmptyState
import com.zeroclaw.android.ui.component.ErrorCard
import com.zeroclaw.android.ui.component.LoadingIndicator

/** Maximum number of tool name badges to show before truncating. */
private const val MAX_TOOL_BADGES = 3

/**
 * Content for the Skills tab inside the combined Plugins and Skills screen.
 *
 * Shows a search-filtered list of skills loaded from the workspace. Each
 * skill card displays metadata, tool count, and a remove button.
 *
 * @param skillsViewModel ViewModel providing skills state and actions.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun SkillsTab(
    skillsViewModel: SkillsViewModel,
    modifier: Modifier = Modifier,
) {
    val filteredState by skillsViewModel.filteredUiState.collectAsStateWithLifecycle()
    val searchQuery by skillsViewModel.searchQuery.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { skillsViewModel.updateSearch(it) },
            label = { Text(stringResource(R.string.plugins_search_skills)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = filteredState) {
            is SkillsUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            is SkillsUiState.Error -> {
                ErrorCard(
                    message = state.detail,
                    onRetry = { skillsViewModel.loadSkills() },
                )
            }
            is SkillsUiState.Content -> {
                if (state.data.isEmpty()) {
                    val emptyMessage =
                        if (searchQuery.isBlank()) {
                            stringResource(R.string.skills_tab_empty_no_skills)
                        } else {
                            stringResource(R.string.skills_tab_empty_no_match)
                        }
                    EmptyState(
                        icon = Icons.Outlined.AutoFixHigh,
                        message = emptyMessage,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.data,
                            key = { it.name },
                            contentType = { "skill" },
                        ) { skill ->
                            SkillCard(
                                skill = skill,
                                onRemove = { skillsViewModel.removeSkill(skill.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a single skill with metadata and action controls.
 *
 * Shows the skill name, description, version, author, tags, tool count,
 * and a delete button. Tool names are shown as badges up to [MAX_TOOL_BADGES].
 *
 * @param skill The skill to display.
 * @param onRemove Callback to remove this skill.
 * @param modifier Modifier applied to the card.
 */
@Composable
private fun SkillCard(
    skill: Skill,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardContentDescription =
        stringResource(
            R.string.skills_tab_skill_card_content_description,
            skill.name,
            skill.description,
            skill.toolCount,
            skill.version,
        )
    val removeSkillContentDescription =
        stringResource(
            R.string.skills_tab_remove_skill_content_description,
            skill.name,
        )
    val versionLabel = stringResource(R.string.skills_tab_version_label, skill.version)
    val toolsCountLabel = stringResource(R.string.skills_tab_tools_count, skill.toolCount)
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = cardContentDescription
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier =
                        Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { contentDescription = removeSkillContentDescription },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                skill.author?.let { author ->
                    Text(
                        text = stringResource(R.string.skills_tab_author_with_bullet, author),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = toolsCountLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (skill.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    skill.tags.forEach { tag ->
                        CategoryBadge(category = tag)
                    }
                }
            }

            if (skill.toolNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val displayNames = skill.toolNames.take(MAX_TOOL_BADGES)
                val remaining = skill.toolNames.size - displayNames.size
                Text(
                    text =
                        displayNames.joinToString(", ") +
                            if (remaining > 0) {
                                stringResource(
                                    R.string.skills_tab_more_tools_suffix,
                                    remaining,
                                )
                            } else {
                                ""
                            },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
