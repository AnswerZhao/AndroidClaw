/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings.channels

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.model.ChannelType
import com.zeroclaw.android.model.ConnectedChannel
import com.zeroclaw.android.ui.component.EmptyState
import com.zeroclaw.android.ui.component.setup.ChannelSelectionGrid
import com.zeroclaw.android.ui.i18n.localizedDisplayName

/** Minimum touch target height in dp. */
private const val MIN_TOUCH_TARGET_DP = 48

/** Standard row padding in dp. */
private const val ROW_PADDING_DP = 16

/** Icon-to-text spacing in dp. */
private const val ICON_TEXT_SPACING_DP = 12

/** Item vertical spacing in dp. */
private const val ITEM_SPACING_DP = 8

/** Vertical spacer height in dp. */
private const val SPACER_HEIGHT_DP = 16

/** Maximum height for the channel type picker dialog content. */
private const val DIALOG_MAX_HEIGHT_DP = 400

/**
 * List screen for connected channels management.
 *
 * Shows configured channels with enable/disable toggles and delete buttons.
 * A FAB opens a channel type picker for adding new channels.
 *
 * @param onNavigateToDetail Callback to navigate to channel detail for editing or adding.
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param channelsViewModel The [ChannelsViewModel] for channel list state.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun ConnectedChannelsScreen(
    onNavigateToDetail: (channelId: String?, channelType: String?) -> Unit,
    edgeMargin: Dp,
    channelsViewModel: ChannelsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val channels by channelsViewModel.channels.collectAsStateWithLifecycle()
    val snackbarMessage by channelsViewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTypePicker by remember { mutableStateOf(false) }
    var channelToDelete by remember { mutableStateOf<ConnectedChannel?>(null) }
    val addConnectedChannelContentDescription =
        stringResource(R.string.connected_channels_add_channel_content_description)
    val noChannelsConnectedMessage = stringResource(R.string.connected_channels_empty_message)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            channelsViewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypePicker = true },
                modifier =
                    Modifier.semantics {
                        contentDescription = addConnectedChannelContentDescription
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = edgeMargin),
        ) {
            Spacer(modifier = Modifier.height(SPACER_HEIGHT_DP.dp))

            if (channels.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Forum,
                    message = noChannelsConnectedMessage,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(ITEM_SPACING_DP.dp),
                ) {
                    items(
                        items = channels,
                        key = { it.id },
                        contentType = { "channel" },
                    ) { channel ->
                        ChannelListItem(
                            channel = channel,
                            onToggle = { channelsViewModel.toggleChannel(channel.id) },
                            onClick = { onNavigateToDetail(channel.id, null) },
                            onDelete = { channelToDelete = channel },
                        )
                    }
                }
            }
        }
    }

    val configuredTypes = remember(channels) { channels.map { it.type }.toSet() }

    if (showTypePicker) {
        ChannelTypePickerDialog(
            configuredTypes = configuredTypes,
            onTypeSelected = { type ->
                showTypePicker = false
                onNavigateToDetail(null, type.name)
            },
            onDismiss = { showTypePicker = false },
        )
    }

    channelToDelete?.let { channel ->
        ConfirmDeleteDialog(
            channelName = channel.type.localizedDisplayName(),
            onConfirm = {
                channelsViewModel.deleteChannel(channel.id)
                channelToDelete = null
            },
            onDismiss = { channelToDelete = null },
        )
    }
}

/**
 * Single channel row in the list with type name, enable toggle, and delete button.
 *
 * @param channel The channel to display.
 * @param onToggle Callback when the enable switch is toggled.
 * @param onClick Callback when the row is tapped for editing.
 * @param onDelete Callback when the delete button is tapped.
 */
@Composable
private fun ChannelListItem(
    channel: ConnectedChannel,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val channelName = channel.type.localizedDisplayName()
    val enabledLabel = stringResource(R.string.common_enabled)
    val disabledLabel = stringResource(R.string.common_disabled)
    val channelStatus =
        if (channel.isEnabled) {
            enabledLabel
        } else {
            disabledLabel
        }
    val channelToggleContentDescription =
        stringResource(
            R.string.connected_channels_toggle_content_description,
            channelName,
            channelStatus,
        )
    val deleteChannelContentDescription =
        stringResource(R.string.connected_channels_delete_content_description, channelName)

    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_TOUCH_TARGET_DP.dp),
    ) {
        Row(
            modifier = Modifier.padding(ROW_PADDING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(ICON_TEXT_SPACING_DP.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (channel.isEnabled) enabledLabel else disabledLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = channel.isEnabled,
                onCheckedChange = { onToggle() },
                modifier =
                    Modifier.semantics {
                        contentDescription = channelToggleContentDescription
                    },
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = deleteChannelContentDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Dialog showing available channel types for adding a new channel.
 *
 * Uses [ChannelSelectionGrid] in single-select mode: tapping a channel type
 * that is not already configured immediately triggers [onTypeSelected] and
 * closes the dialog. Already-configured types display a checkmark indicator
 * but can still be tapped to view their configuration.
 *
 * @param configuredTypes Set of channel types that are already configured.
 * @param onTypeSelected Callback when a type is selected.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
private fun ChannelTypePickerDialog(
    configuredTypes: Set<ChannelType>,
    onTypeSelected: (ChannelType) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingSelection by remember { mutableStateOf<Set<ChannelType>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connected_channels_add_channel)) },
        text = {
            ChannelSelectionGrid(
                selectedTypes = pendingSelection,
                configuredTypes = configuredTypes,
                onSelectionChanged = { updated ->
                    val newlySelected = updated - configuredTypes - pendingSelection
                    val target = newlySelected.firstOrNull()
                    if (target != null) {
                        onTypeSelected(target)
                    } else {
                        pendingSelection = updated - configuredTypes
                    }
                },
                modifier = Modifier.heightIn(max = DIALOG_MAX_HEIGHT_DP.dp),
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * Confirmation dialog shown before deleting a channel.
 *
 * @param channelName Display name of the channel to delete.
 * @param onConfirm Called when the user confirms deletion.
 * @param onDismiss Called when the user cancels.
 */
@Composable
private fun ConfirmDeleteDialog(
    channelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connected_channels_delete_title, channelName)) },
        text = {
            Text(stringResource(R.string.connected_channels_delete_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
