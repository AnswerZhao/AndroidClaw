/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

@file:Suppress("MatchingDeclarationName")

package com.zeroclaw.android.ui.screen.settings.doctor

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.model.CheckStatus
import com.zeroclaw.android.model.DiagnosticCategory
import com.zeroclaw.android.model.DiagnosticCheck
import com.zeroclaw.android.model.DoctorSummary
import com.zeroclaw.android.ui.component.CollapsibleSection

/**
 * Aggregated state for the doctor content composable.
 *
 * @property checks All diagnostic check results.
 * @property isRunning Whether checks are currently executing.
 * @property summary Aggregated check summary, null before first run.
 */
data class DoctorState(
    val checks: List<DiagnosticCheck>,
    val isRunning: Boolean,
    val summary: DoctorSummary?,
)

/**
 * ZeroClaw Doctor diagnostics screen.
 *
 * Thin stateful wrapper that collects ViewModel flows and delegates
 * rendering to [DoctorContent].
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param onNavigateToRoute Callback invoked when a diagnostic action button is tapped,
 *   receiving the route string from [DiagnosticCheck.actionRoute].
 * @param doctorViewModel The [DoctorViewModel] for diagnostic state.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun DoctorScreen(
    edgeMargin: Dp,
    onNavigateToRoute: (String) -> Unit = {},
    doctorViewModel: DoctorViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val checks by doctorViewModel.checks.collectAsStateWithLifecycle()
    val isRunning by doctorViewModel.isRunning.collectAsStateWithLifecycle()
    val summary by doctorViewModel.summary.collectAsStateWithLifecycle()

    DoctorContent(
        state =
            DoctorState(
                checks = checks,
                isRunning = isRunning,
                summary = summary,
            ),
        edgeMargin = edgeMargin,
        onNavigateToRoute = onNavigateToRoute,
        onRunDiagnostics = doctorViewModel::runAllChecks,
        modifier = modifier,
    )
}

/**
 * Stateless doctor content composable for testing.
 *
 * @param state Aggregated doctor state snapshot.
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param onNavigateToRoute Callback for diagnostic action navigation.
 * @param onRunDiagnostics Callback to start diagnostic checks.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
internal fun DoctorContent(
    state: DoctorState,
    edgeMargin: Dp,
    onNavigateToRoute: (String) -> Unit,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emptyHintText = stringResource(R.string.doctor_empty_hint)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SummaryBanner(
            summary = state.summary,
            isRunning = state.isRunning,
            onRunDiagnostics = onRunDiagnostics,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.checks.isEmpty() && !state.isRunning) {
            Text(
                text = emptyHintText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            CheckResultsList(checks = state.checks, onAction = onNavigateToRoute)
        }
    }
}

/**
 * Summary card showing pass/warn/fail counts and the run button.
 *
 * @param summary Aggregated check summary, null before first run.
 * @param isRunning Whether checks are currently executing.
 * @param onRunDiagnostics Callback to start diagnostic checks.
 */
@Composable
private fun SummaryBanner(
    summary: DoctorSummary?,
    isRunning: Boolean,
    onRunDiagnostics: () -> Unit,
) {
    val summaryContentDescription =
        summary?.let {
            stringResource(
                R.string.doctor_summary_content_description,
                it.passCount,
                it.warnCount,
                it.failCount,
            )
        } ?: stringResource(R.string.doctor_summary_no_runs)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = summaryContentDescription
                },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.doctor_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (summary != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SummaryChip(
                        label = stringResource(R.string.doctor_status_pass),
                        count = summary.passCount,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    SummaryChip(
                        label = stringResource(R.string.doctor_status_warn),
                        count = summary.warnCount,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    SummaryChip(
                        label = stringResource(R.string.doctor_status_fail),
                        count = summary.failCount,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onRunDiagnostics,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.doctor_running))
                } else {
                    Text(
                        if (summary != null) {
                            stringResource(R.string.doctor_rerun_diagnostics)
                        } else {
                            stringResource(R.string.doctor_run_diagnostics)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Small count chip for the summary banner.
 *
 * @param label Category label (e.g. "Pass").
 * @param count Number of checks in this category.
 * @param color Color for the count text and dot.
 */
@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.doctor_summary_chip_value, count, label),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/**
 * Scrollable list of check results organized by category.
 *
 * @param checks All diagnostic check results to display.
 * @param onAction Callback invoked with the action route when a check's action button is tapped.
 */
@Composable
private fun CheckResultsList(
    checks: List<DiagnosticCheck>,
    onAction: (String) -> Unit,
) {
    val grouped = remember(checks) { checks.groupBy { it.category } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (category in DiagnosticCategory.entries) {
            val categoryChecks = grouped[category] ?: continue
            item(key = "section-${category.name}") {
                CollapsibleSection(
                    title = categoryDisplayName(category),
                    initiallyExpanded = categoryChecks.any { it.status == CheckStatus.FAIL },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        categoryChecks.forEach { check ->
                            CheckResultRow(check = check, onAction = onAction)
                        }
                    }
                }
            }
        }

        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Single check result row with status dot, title, detail, and optional action.
 *
 * @param check The diagnostic check result to display.
 * @param onAction Callback invoked with the action route when the action button is tapped.
 */
@Composable
private fun CheckResultRow(
    check: DiagnosticCheck,
    onAction: (String) -> Unit,
) {
    val statusColor =
        when (check.status) {
            CheckStatus.PASS -> MaterialTheme.colorScheme.primary
            CheckStatus.WARN -> MaterialTheme.colorScheme.tertiary
            CheckStatus.FAIL -> MaterialTheme.colorScheme.error
            CheckStatus.RUNNING -> MaterialTheme.colorScheme.outline
        }
    val statusLabel =
        when (check.status) {
            CheckStatus.PASS -> stringResource(R.string.doctor_status_passed)
            CheckStatus.WARN -> stringResource(R.string.doctor_status_warning)
            CheckStatus.FAIL -> stringResource(R.string.doctor_status_failed)
            CheckStatus.RUNNING -> stringResource(R.string.doctor_status_running)
        }
    val checkResultContentDescription =
        stringResource(
            R.string.doctor_check_result_content_description,
            check.title,
            statusLabel,
            check.detail,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = checkResultContentDescription
                },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = check.title,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (check.detail.isNotEmpty()) {
                Text(
                    text = check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (check.actionLabel != null && check.actionRoute != null) {
            TextButton(onClick = { onAction(check.actionRoute) }) {
                Text(check.actionLabel)
            }
        }
    }
}

/**
 * Maps a [DiagnosticCategory] to its human-readable display name.
 *
 * @param category The category to map.
 * @return Human-readable section title.
 */
@Composable
private fun categoryDisplayName(category: DiagnosticCategory): String =
    when (category) {
        DiagnosticCategory.CONFIG -> stringResource(R.string.doctor_category_configuration)
        DiagnosticCategory.API_KEYS -> stringResource(R.string.doctor_category_api_keys)
        DiagnosticCategory.CONNECTIVITY -> stringResource(R.string.doctor_category_connectivity)
        DiagnosticCategory.DAEMON_HEALTH -> stringResource(R.string.doctor_category_daemon_health)
        DiagnosticCategory.CHANNELS -> stringResource(R.string.doctor_category_channels)
        DiagnosticCategory.RUNTIME_TRACES -> stringResource(R.string.doctor_category_runtime_traces)
        DiagnosticCategory.SYSTEM -> stringResource(R.string.doctor_category_system)
    }
