/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.component

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroclaw.android.ui.screen.setup.SetupProgress
import com.zeroclaw.android.ui.screen.setup.SetupStepStatus
import com.zeroclaw.android.util.LocalPowerSaveMode
import kotlinx.coroutines.flow.StateFlow

/** Icon size for step status indicators in the bottom sheet. */
private val StepIconSize = 20.dp

/** Spacing between the status icon and the step label. */
private val IconLabelSpacing = 12.dp

/** Horizontal padding for the sheet content. */
private val SheetHorizontalPadding = 16.dp

/** Bottom padding for the sheet content. */
private val SheetBottomPadding = 8.dp

/** Stroke width for the running step progress indicator. */
private val ProgressStrokeWidth = 2.dp

/** Minimum touch target height for interactive rows (WCAG AA). */
private val MinTouchTarget = 48.dp

/** Vertical padding on step rows. */
private val StepRowVerticalPadding = 4.dp

/**
 * Material 3 modal bottom sheet showing daemon restart and channel
 * initialization progress during hot-reload operations.
 *
 * Observes [progressFlow] via [collectAsStateWithLifecycle] and renders
 * each setup step with the same status icon pattern used by the setup
 * screen: a clock for pending, a spinning indicator for running (static
 * ellipsis in power-save mode), a green check for success, and a red
 * cancel icon for failure. A full-width "Done" button appears once
 * [SetupProgress.isComplete] is `true`.
 *
 * Only the steps relevant to hot-reload are displayed: "Restarting daemon"
 * (from [SetupProgress.daemonStart]), "Checking health" (from
 * [SetupProgress.daemonHealth]), plus one row per configured channel.
 *
 * @param progressFlow [StateFlow] emitting the current [SetupProgress].
 * @param onDismiss Callback invoked when the user taps Done or dismisses
 *   the sheet.
 * @param modifier Modifier applied to the [ModalBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupBottomSheet(
    progressFlow: StateFlow<SetupProgress>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val progress by progressFlow.collectAsStateWithLifecycle()
    val powerSave = LocalPowerSaveMode.current
    val applyingChangesTitle = stringResource(R.string.setup_bottom_sheet_applying_changes_title)
    val restartingDaemonLabel =
        stringResource(R.string.setup_bottom_sheet_restarting_daemon_label)
    val checkingHealthLabel =
        stringResource(R.string.setup_bottom_sheet_checking_health_label)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetHorizontalPadding)
                    .padding(bottom = SheetBottomPadding)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = applyingChangesTitle,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            HotReloadStepRow(
                label = restartingDaemonLabel,
                status = progress.daemonStart,
                powerSave = powerSave,
            )
            HotReloadStepRow(
                label = checkingHealthLabel,
                status = progress.daemonHealth,
                powerSave = powerSave,
            )

            progress.channels.forEach { (name, status) ->
                HotReloadStepRow(
                    label = name.replaceFirstChar { it.uppercase() },
                    status = status,
                    powerSave = powerSave,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val enter = if (powerSave) EnterTransition.None else fadeIn()
            val exit = if (powerSave) ExitTransition.None else fadeOut()

            AnimatedVisibility(
                visible = progress.isComplete,
                enter = enter,
                exit = exit,
            ) {
                Button(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = MinTouchTarget),
                ) {
                    Text(stringResource(R.string.common_done))
                }
            }

            Spacer(modifier = Modifier.height(SheetBottomPadding))
        }
    }
}

/**
 * A single row displaying a hot-reload step's label and status icon.
 *
 * The icon reflects [status]: a clock for pending, a spinning progress
 * indicator for running (static ellipsis in power-save mode), a green
 * check for success, or a red cancel icon with error text for failure.
 *
 * @param label Human-readable name of the step.
 * @param status Current execution status of this step.
 * @param powerSave Whether power-save mode is active, disabling animations.
 * @param modifier Modifier applied to the root [Row].
 */
@Composable
private fun HotReloadStepRow(
    label: String,
    status: SetupStepStatus,
    powerSave: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusText =
        when (status) {
            SetupStepStatus.Pending -> stringResource(R.string.setup_status_pending)
            SetupStepStatus.Running -> stringResource(R.string.setup_status_running)
            SetupStepStatus.Success -> stringResource(R.string.setup_status_success)
            is SetupStepStatus.Failed -> stringResource(R.string.setup_status_failed)
        }
    val stepContentDescription =
        stringResource(R.string.setup_step_content_description, label, statusText)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MinTouchTarget)
                .padding(vertical = StepRowVerticalPadding)
                .semantics { contentDescription = stepContentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HotReloadStepIcon(status = status, powerSave = powerSave)
        Spacer(modifier = Modifier.width(IconLabelSpacing))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (status is SetupStepStatus.Failed) {
                Text(
                    text = status.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Renders the appropriate icon for a [SetupStepStatus] in the hot-reload
 * bottom sheet.
 *
 * Uses [Icons.Filled.Schedule] for pending, a [CircularProgressIndicator]
 * for running (or static ellipsis text in power-save mode),
 * [Icons.Filled.CheckCircle] for success, and [Icons.Filled.Cancel]
 * for failed.
 *
 * @param status The step status to render.
 * @param powerSave Whether power-save mode is active, disabling animations.
 */
@Composable
private fun HotReloadStepIcon(
    status: SetupStepStatus,
    powerSave: Boolean,
) {
    when (status) {
        SetupStepStatus.Pending -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(StepIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SetupStepStatus.Running -> {
            if (powerSave) {
                Text(
                    text = stringResource(R.string.common_ellipsis),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.size(StepIconSize),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(StepIconSize),
                    strokeWidth = ProgressStrokeWidth,
                )
            }
        }

        SetupStepStatus.Success -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(StepIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        is SetupStepStatus.Failed -> {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(StepIconSize),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
