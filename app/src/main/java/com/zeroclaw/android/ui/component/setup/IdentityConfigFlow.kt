/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.component.setup

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R
import com.zeroclaw.android.ui.theme.ZeroClawTheme

/** Spacing after the title text. */
private val TitleSpacing = 16.dp

/** Vertical spacing between form fields. */
private val FieldSpacing = 16.dp

/** Spacing between major form sections. */
private val SectionSpacing = 24.dp

/** Spacing between the section label and its control. */
private val LabelSpacing = 8.dp

/** Spacing between communication style chips. */
private val ChipSpacing = 8.dp

/** Vertical spacing between identity format cards. */
private val CardSpacing = 8.dp

/** Internal padding for each identity format card. */
private val CardPadding = 16.dp

/** Border width for the selected identity format card. */
private val SelectedBorderWidth = 2.dp

/**
 * Describes a communication style option.
 *
 * @property id Machine-readable style identifier used in configuration.
 * @property label Human-readable chip label.
 */
private data class StyleOption(
    val id: String,
    val labelRes: Int,
)

/** Available communication styles including a "None" option. */
private val STYLE_OPTIONS =
    listOf(
        StyleOption(id = "", labelRes = R.string.identity_style_none),
        StyleOption(id = "professional", labelRes = R.string.identity_style_professional),
        StyleOption(id = "casual", labelRes = R.string.identity_style_casual),
        StyleOption(id = "concise", labelRes = R.string.identity_style_concise),
        StyleOption(id = "detailed", labelRes = R.string.identity_style_detailed),
    )

/**
 * Describes an identity format option for the card selector.
 *
 * @property id Machine-readable format identifier matching upstream TOML `identity.format`.
 * @property title Human-readable option name.
 * @property description Brief explanation of the format.
 */
private data class FormatOption(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
)

/** Available identity format options. */
private val FORMAT_OPTIONS =
    listOf(
        FormatOption(
            id = "aieos",
            titleRes = R.string.identity_format_aieos_title,
            descriptionRes = R.string.identity_format_aieos_description,
        ),
        FormatOption(
            id = "openclaw",
            titleRes = R.string.identity_format_openclaw_title,
            descriptionRes = R.string.identity_format_openclaw_description,
        ),
    )

/**
 * Agent identity configuration form.
 *
 * Collects agent identity information including name, user name, timezone,
 * communication style, and identity format. The agent name field validates
 * as required after the user first interacts with it.
 *
 * Field mappings to upstream configuration:
 * - Agent name and user name become part of the identity JSON blob.
 * - Identity format maps to the TOML `[identity].format` field via
 *   [ConfigTomlBuilder][com.zeroclaw.android.service.ConfigTomlBuilder].
 *
 * @param agentName Current agent name value.
 * @param userName Current user name value.
 * @param timezone Current timezone string (e.g. "America/New_York").
 * @param communicationStyle Current communication style: "", "professional",
 *   "casual", "concise", or "detailed".
 * @param identityFormat Current identity format: "openclaw" or "aieos".
 * @param onAgentNameChanged Callback when the agent name changes.
 * @param onUserNameChanged Callback when the user name changes.
 * @param onTimezoneChanged Callback when the timezone changes.
 * @param onCommunicationStyleChanged Callback when the communication style changes.
 * @param onIdentityFormatChanged Callback when the identity format changes.
 * @param modifier Modifier applied to the root scrollable [Column].
 */
@Composable
fun IdentityConfigFlow(
    agentName: String,
    userName: String,
    timezone: String,
    communicationStyle: String,
    identityFormat: String,
    onAgentNameChanged: (String) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onTimezoneChanged: (String) -> Unit,
    onCommunicationStyleChanged: (String) -> Unit,
    onIdentityFormatChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasInteractedWithName by remember { mutableStateOf(false) }
    val showNameError = hasInteractedWithName && agentName.isBlank()
    val agentNameRequiredContentDescription =
        stringResource(R.string.identity_agent_name_required_content_description)
    val userNameContentDescription = stringResource(R.string.identity_user_name_label)
    val timezoneContentDescription = stringResource(R.string.identity_timezone_label)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.identity_config_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(TitleSpacing))

        OutlinedTextField(
            value = agentName,
            onValueChange = {
                hasInteractedWithName = true
                onAgentNameChanged(it)
            },
            label = { Text(stringResource(R.string.identity_agent_name_required_label)) },
            singleLine = true,
            isError = showNameError,
            supportingText =
                if (showNameError) {
                    { Text(stringResource(R.string.identity_agent_name_required_error)) }
                } else {
                    null
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = agentNameRequiredContentDescription },
        )

        Spacer(modifier = Modifier.height(FieldSpacing))

        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChanged,
            label = { Text(stringResource(R.string.identity_user_name_label)) },
            singleLine = true,
            supportingText = { Text(stringResource(R.string.identity_user_name_supporting)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = userNameContentDescription },
        )

        Spacer(modifier = Modifier.height(FieldSpacing))

        OutlinedTextField(
            value = timezone,
            onValueChange = onTimezoneChanged,
            label = { Text(stringResource(R.string.identity_timezone_label)) },
            singleLine = true,
            supportingText = { Text(stringResource(R.string.identity_timezone_supporting)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = timezoneContentDescription },
        )

        Spacer(modifier = Modifier.height(SectionSpacing))

        CommunicationStylePicker(
            selected = communicationStyle,
            onStyleChanged = onCommunicationStyleChanged,
        )

        Spacer(modifier = Modifier.height(SectionSpacing))

        IdentityFormatSelector(
            selectedFormat = identityFormat,
            onFormatChanged = onIdentityFormatChanged,
        )
    }
}

/**
 * Communication style picker using selectable [FilterChip] options.
 *
 * Presents "None", "Professional", "Casual", "Concise", and "Detailed" as
 * chip choices in a flow row.
 *
 * @param selected Current communication style identifier.
 * @param onStyleChanged Callback when the user selects a different style.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunicationStylePicker(
    selected: String,
    onStyleChanged: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.identity_communication_style_title),
        style = MaterialTheme.typography.titleMedium,
    )

    Spacer(modifier = Modifier.height(LabelSpacing))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        STYLE_OPTIONS.forEach { option ->
            val isSelected = selected == option.id
            val label = stringResource(option.labelRes)
            val selectionState =
                if (isSelected) {
                    stringResource(R.string.common_state_selected)
                } else {
                    stringResource(R.string.common_state_not_selected)
                }
            val styleChipContentDescription =
                stringResource(
                    R.string.identity_style_chip_content_description,
                    label,
                    selectionState,
                )

            FilterChip(
                selected = isSelected,
                onClick = { onStyleChanged(option.id) },
                label = { Text(label) },
                modifier = Modifier.semantics {
                    contentDescription = styleChipContentDescription
                    role = Role.RadioButton
                },
            )
        }
    }
}

/**
 * Identity format selector with two selectable [Card] options.
 *
 * Presents "OpenClaw" (default) and "AIEOS" as card choices with descriptions.
 * The selected card receives primary-container styling and a primary border.
 *
 * @param selectedFormat Current identity format identifier.
 * @param onFormatChanged Callback when the user selects a different format.
 */
@Composable
private fun IdentityFormatSelector(
    selectedFormat: String,
    onFormatChanged: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.identity_format_title),
        style = MaterialTheme.typography.titleMedium,
    )

    Spacer(modifier = Modifier.height(LabelSpacing))

    FORMAT_OPTIONS.forEach { option ->
        val isSelected = selectedFormat == option.id
        val title = stringResource(option.titleRes)
        val description = stringResource(option.descriptionRes)
        val selectionState =
            if (isSelected) {
                stringResource(R.string.common_state_selected)
            } else {
                stringResource(R.string.common_state_not_selected)
            }
        val formatContentDescription =
            stringResource(
                R.string.identity_format_content_description,
                title,
                selectionState,
            )

        Card(
            onClick = { onFormatChanged(option.id) },
            colors =
                if (isSelected) {
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer,
                    )
                } else {
                    CardDefaults.cardColors()
                },
            border =
                if (isSelected) {
                    BorderStroke(
                        SelectedBorderWidth,
                        MaterialTheme.colorScheme.primary,
                    )
                } else {
                    null
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = formatContentDescription
                        role = Role.RadioButton
                        selected = isSelected
                    },
        ) {
            Column(
                modifier = Modifier.padding(CardPadding),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(CardSpacing))
    }
}

@Preview(name = "Identity - Empty")
@Composable
private fun PreviewEmpty() {
    ZeroClawTheme {
        Surface {
            IdentityConfigFlow(
                agentName = "",
                userName = "",
                timezone = "America/New_York",
                communicationStyle = "",
                identityFormat = "openclaw",
                onAgentNameChanged = {},
                onUserNameChanged = {},
                onTimezoneChanged = {},
                onCommunicationStyleChanged = {},
                onIdentityFormatChanged = {},
            )
        }
    }
}

@Preview(name = "Identity - Filled")
@Composable
private fun PreviewFilled() {
    ZeroClawTheme {
        Surface {
            IdentityConfigFlow(
                agentName = "ZeroClaw",
                userName = "Alice",
                timezone = "Europe/London",
                communicationStyle = "professional",
                identityFormat = "aieos",
                onAgentNameChanged = {},
                onUserNameChanged = {},
                onTimezoneChanged = {},
                onCommunicationStyleChanged = {},
                onIdentityFormatChanged = {},
            )
        }
    }
}

@Preview(name = "Identity - Casual OpenClaw")
@Composable
private fun PreviewCasual() {
    ZeroClawTheme {
        Surface {
            IdentityConfigFlow(
                agentName = "MyAgent",
                userName = "",
                timezone = "Asia/Tokyo",
                communicationStyle = "casual",
                identityFormat = "openclaw",
                onAgentNameChanged = {},
                onUserNameChanged = {},
                onTimezoneChanged = {},
                onCommunicationStyleChanged = {},
                onIdentityFormatChanged = {},
            )
        }
    }
}

@Preview(
    name = "Identity - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewDark() {
    ZeroClawTheme {
        Surface {
            IdentityConfigFlow(
                agentName = "ZeroClaw",
                userName = "Bob",
                timezone = "UTC",
                communicationStyle = "concise",
                identityFormat = "openclaw",
                onAgentNameChanged = {},
                onUserNameChanged = {},
                onTimezoneChanged = {},
                onCommunicationStyleChanged = {},
                onIdentityFormatChanged = {},
            )
        }
    }
}
