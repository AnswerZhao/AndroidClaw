/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R
import com.zeroclaw.android.data.remote.NetworkScanner
import com.zeroclaw.android.model.DiscoveredServer
import com.zeroclaw.android.model.LocalServerType
import com.zeroclaw.android.model.ScanState
import com.zeroclaw.android.util.LocalPowerSaveMode

/** Padding around the bottom sheet content. */
private const val SHEET_PADDING_DP = 24

/** Spacing between the header and the content area. */
private const val HEADER_SPACING_DP = 16

/** Spacing between elements inside a server card. */
private const val CARD_INTERNAL_SPACING_DP = 8

/** Size of the server type icon in scan results. */
private const val SERVER_ICON_SIZE_DP = 40

/** Vertical padding for each server card. */
private const val CARD_PADDING_DP = 12

/** Horizontal padding for each server card. */
private const val CARD_H_PADDING_DP = 16

/** Spacing between list items. */
private const val LIST_SPACING_DP = 8

/** Height of the bottom sheet content area. */
private const val SHEET_CONTENT_HEIGHT_DP = 400

/**
 * Bottom sheet that scans the local network for AI inference servers.
 *
 * Automatically starts scanning when displayed. Shows a progress bar
 * during the scan, then lists discovered servers with their loaded models.
 * Tapping a server card invokes [onServerSelected] with the discovered
 * server's details so the caller can auto-fill the base URL and model.
 *
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onServerSelected Callback when the user selects a discovered server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScanSheet(
    onDismiss: () -> Unit,
    onServerSelected: (DiscoveredServer) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    var discoveredServers by remember { mutableStateOf(emptyList<DiscoveredServer>()) }
    var scanTrigger by remember { mutableStateOf(0) }
    val isPowerSave = LocalPowerSaveMode.current

    LaunchedEffect(scanTrigger) {
        discoveredServers = emptyList()
        NetworkScanner.scan(context).collect { state ->
            scanState = state
            if (state is ScanState.Completed) {
                discoveredServers = state.servers
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SHEET_PADDING_DP.dp)
                    .navigationBarsPadding(),
        ) {
            ScanSheetHeader(scanState)
            Spacer(modifier = Modifier.height(HEADER_SPACING_DP.dp))
            ScanSheetProgress(scanState)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(LIST_SPACING_DP.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(SHEET_CONTENT_HEIGHT_DP.dp),
            ) {
                if (scanState is ScanState.Completed && discoveredServers.isEmpty()) {
                    item {
                        EmptyScanResult()
                    }
                }

                itemsIndexed(
                    items = discoveredServers,
                    key = { _, server -> "${server.host}:${server.port}" },
                    contentType = { _, _ -> "server_card" },
                ) { index, server ->
                    AnimatedVisibility(
                        visible = true,
                        enter =
                            if (isPowerSave) {
                                EnterTransition.None
                            } else {
                                fadeIn() + slideInVertically { it / 2 }
                            },
                    ) {
                        ServerCard(
                            server = server,
                            onClick = { onServerSelected(server) },
                        )
                    }
                }
            }

            ScanSheetFooter(
                scanState = scanState,
                onRetry = { scanTrigger++ },
                onDismiss = onDismiss,
            )
            Spacer(modifier = Modifier.height(HEADER_SPACING_DP.dp))
        }
    }
}

/**
 * Header row with title and scan status description.
 *
 * @param scanState Current scan state for subtitle text.
 */
@Composable
private fun ScanSheetHeader(scanState: ScanState) {
    Text(
        text = stringResource(R.string.network_scan_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.height(CARD_INTERNAL_SPACING_DP.dp))
    val subtitleText =
        when (scanState) {
            is ScanState.Idle -> stringResource(R.string.network_scan_preparing)
            is ScanState.Scanning -> stringResource(R.string.network_scan_scanning)
            is ScanState.Completed -> {
                val count = scanState.servers.size
                if (count == 0) {
                    stringResource(R.string.network_scan_no_servers_found)
                } else {
                    pluralStringResource(
                        R.plurals.network_scan_servers_found,
                        count,
                        count,
                    )
                }
            }
            is ScanState.Error -> scanState.message
        }
    Text(
        text = subtitleText,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (scanState is ScanState.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

/**
 * Linear progress indicator shown during scanning.
 *
 * @param scanState Current scan state for progress fraction.
 */
@Composable
private fun ScanSheetProgress(scanState: ScanState) {
    if (scanState is ScanState.Scanning) {
        val progressContentDescription =
            stringResource(R.string.network_scan_progress_content_description)
        LinearProgressIndicator(
            progress = { scanState.progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = progressContentDescription },
        )
        Spacer(modifier = Modifier.height(HEADER_SPACING_DP.dp))
    }
}

/**
 * Card displaying a discovered AI server with its loaded models.
 *
 * @param server The discovered server to display.
 * @param onClick Callback when the card is tapped.
 */
@Composable
private fun ServerCard(
    server: DiscoveredServer,
    onClick: () -> Unit,
) {
    val serverCardContentDescription =
        stringResource(
            R.string.network_scan_server_card_content_description,
            server.serverType.label,
            server.host,
            server.port,
        )

    ElevatedCard(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = serverCardContentDescription },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CARD_H_PADDING_DP.dp, vertical = CARD_PADDING_DP.dp),
        ) {
            Icon(
                imageVector =
                    when (server.serverType) {
                        LocalServerType.OLLAMA -> Icons.Default.Dns
                        LocalServerType.OPENAI_COMPATIBLE -> Icons.Default.Storage
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SERVER_ICON_SIZE_DP.dp),
            )
            Spacer(modifier = Modifier.width(CARD_H_PADDING_DP.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.serverType.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.network_scan_host_port,
                            server.host,
                            server.port,
                        ),
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (server.models.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(CARD_INTERNAL_SPACING_DP.dp))
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.network_scan_models_loaded,
                                server.models.size,
                                server.models.size,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = server.models.take(MAX_PREVIEW_MODELS).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.network_scan_verified_server),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SHEET_PADDING_DP.dp),
            )
        }
    }
}

/**
 * Empty state shown when a scan completes with no servers found.
 */
@Composable
private fun EmptyScanResult() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SHEET_CONTENT_EMPTY_PADDING_DP.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(EMPTY_ICON_SIZE_DP.dp),
        )
        Spacer(modifier = Modifier.height(HEADER_SPACING_DP.dp))
        Text(
            text = stringResource(R.string.network_scan_no_servers_found),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(CARD_INTERNAL_SPACING_DP.dp))
        Text(
            text = stringResource(R.string.network_scan_empty_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Footer with Retry and Cancel buttons.
 *
 * @param scanState Current scan state to determine button visibility.
 * @param onRetry Callback to restart the scan.
 * @param onDismiss Callback to close the sheet.
 */
@Composable
private fun ScanSheetFooter(
    scanState: ScanState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (scanState is ScanState.Completed || scanState is ScanState.Error) {
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(FOOTER_ICON_SIZE_DP.dp),
                )
                Spacer(modifier = Modifier.width(CARD_INTERNAL_SPACING_DP.dp))
                Text(stringResource(R.string.network_scan_scan_again))
            }
            Spacer(modifier = Modifier.width(CARD_INTERNAL_SPACING_DP.dp))
        }
        TextButton(onClick = onDismiss) {
            Text(
                if (scanState is ScanState.Scanning) {
                    stringResource(R.string.common_cancel)
                } else {
                    stringResource(R.string.common_close)
                },
            )
        }
    }
}

/** Maximum number of model names to preview in a server card. */
private const val MAX_PREVIEW_MODELS = 5

/** Vertical padding for the empty state container. */
private const val SHEET_CONTENT_EMPTY_PADDING_DP = 48

/** Size of the empty state icon. */
private const val EMPTY_ICON_SIZE_DP = 48

/** Size of icons in the footer buttons. */
private const val FOOTER_ICON_SIZE_DP = 18
