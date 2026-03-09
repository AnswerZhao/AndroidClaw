/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.component

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Badge indicating a plugin is an official ZeroClaw built-in.
 *
 * Uses [MaterialTheme.colorScheme.tertiaryContainer] to distinguish from
 * the category badge. The chip is non-interactive (disabled with custom
 * colours) so TalkBack does not announce it as a button.
 *
 * @param modifier Modifier applied to the chip.
 */
@Composable
fun OfficialPluginBadge(modifier: Modifier = Modifier) {
    val officialLabel = stringResource(R.string.official_plugin_badge_label)
    val officialPluginContentDescription =
        stringResource(R.string.official_plugin_badge_content_description)
    SuggestionChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = officialLabel,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier =
            modifier
                .semantics { contentDescription = officialPluginContentDescription },
        colors =
            SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        border = null,
    )
}
