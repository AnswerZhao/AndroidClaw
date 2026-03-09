// Copyright 2026 ZeroClaw Community, MIT License

package com.zeroclaw.android.ui.screen.settings.cron

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R

/** Index for the "Cron" (recurring) mode tab. */
private const val MODE_RECURRING = 0

/** Index for the "Delay" (one-shot) mode tab. */
private const val MODE_ONE_SHOT = 1

/** Index for the "At Time" (specific timestamp) mode tab. */
private const val MODE_AT_TIME = 2

/** Index for the "Interval" (fixed-interval repeating) mode tab. */
private const val MODE_INTERVAL = 3

/** Total number of schedule mode tabs. */
private const val MODE_COUNT = 4

/** Minimum number of cron expression parts (standard cron has 5). */
private const val MIN_CRON_PARTS = 5

/** Maximum number of cron expression parts (extended cron has 6 with seconds). */
private const val MAX_CRON_PARTS = 6

/** Number of milliseconds in one minute. */
private const val MS_PER_MINUTE = 60_000L

/**
 * Dialog for adding a new cron job.
 *
 * Provides four schedule modes:
 * - **Cron**: cron-expression-based recurring jobs.
 * - **Delay**: one-shot jobs that fire after a human-readable delay.
 * - **At Time**: one-shot jobs that fire at a specific RFC 3339 timestamp.
 * - **Interval**: fixed-interval repeating jobs specified in minutes.
 *
 * @param onAddRecurring Called when the user submits a recurring (cron) job.
 *   Receives the cron expression and command.
 * @param onAddOneShot Called when the user submits a one-shot (delay) job.
 *   Receives the delay string and command.
 * @param onAddAtTime Called when the user submits an at-time job.
 *   Receives the RFC 3339 timestamp and command.
 * @param onAddInterval Called when the user submits a fixed-interval job.
 *   Receives the interval in milliseconds and command.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
fun AddCronJobDialog(
    onAddRecurring: (expression: String, command: String) -> Unit,
    onAddOneShot: (delay: String, command: String) -> Unit,
    onAddAtTime: (timestampRfc3339: String, command: String) -> Unit,
    onAddInterval: (intervalMs: ULong, command: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val jobTypeSelectionDescription = stringResource(R.string.cron_job_type_selection_desc)
    var selectedMode by remember { mutableIntStateOf(MODE_RECURRING) }
    var expression by remember { mutableStateOf("") }
    var delay by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf("") }
    var intervalMinutes by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var expressionError by remember { mutableStateOf<CronValidationError?>(null) }
    var delayError by remember { mutableStateOf<CronValidationError?>(null) }
    var timestampError by remember { mutableStateOf<CronValidationError?>(null) }
    var intervalError by remember { mutableStateOf<CronValidationError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cron_add_scheduled_job_title)) },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = jobTypeSelectionDescription
                            },
                ) {
                    SegmentedButton(
                        selected = selectedMode == MODE_RECURRING,
                        onClick = { selectedMode = MODE_RECURRING },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = MODE_RECURRING,
                                count = MODE_COUNT,
                            ),
                    ) {
                        Text(stringResource(R.string.cron_mode_cron))
                    }
                    SegmentedButton(
                        selected = selectedMode == MODE_ONE_SHOT,
                        onClick = { selectedMode = MODE_ONE_SHOT },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = MODE_ONE_SHOT,
                                count = MODE_COUNT,
                            ),
                    ) {
                        Text(stringResource(R.string.cron_mode_delay))
                    }
                    SegmentedButton(
                        selected = selectedMode == MODE_AT_TIME,
                        onClick = { selectedMode = MODE_AT_TIME },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = MODE_AT_TIME,
                                count = MODE_COUNT,
                            ),
                    ) {
                        Text(stringResource(R.string.cron_mode_at))
                    }
                    SegmentedButton(
                        selected = selectedMode == MODE_INTERVAL,
                        onClick = { selectedMode = MODE_INTERVAL },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = MODE_INTERVAL,
                                count = MODE_COUNT,
                            ),
                    ) {
                        Text(stringResource(R.string.cron_mode_every))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ScheduleInputFields(
                    selectedMode = selectedMode,
                    expression = expression,
                    onExpressionChange = {
                        expression = it
                        expressionError = null
                    },
                    expressionError = expressionError,
                    delay = delay,
                    onDelayChange = {
                        delay = it
                        delayError = null
                    },
                    delayError = delayError,
                    timestamp = timestamp,
                    onTimestampChange = {
                        timestamp = it
                        timestampError = null
                    },
                    timestampError = timestampError,
                    intervalMinutes = intervalMinutes,
                    onIntervalChange = {
                        intervalMinutes = it
                        intervalError = null
                    },
                    intervalError = intervalError,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(R.string.cron_command_label)) },
                    placeholder = { Text(stringResource(R.string.cron_command_placeholder)) },
                    maxLines = MAX_COMMAND_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    handleConfirm(
                        selectedMode = selectedMode,
                        expression = expression,
                        delay = delay,
                        timestamp = timestamp,
                        intervalMinutes = intervalMinutes,
                        command = command,
                        onExpressionError = { expressionError = it },
                        onDelayError = { delayError = it },
                        onTimestampError = { timestampError = it },
                        onIntervalError = { intervalError = it },
                        onAddRecurring = onAddRecurring,
                        onAddOneShot = onAddOneShot,
                        onAddAtTime = onAddAtTime,
                        onAddInterval = onAddInterval,
                        onDismiss = onDismiss,
                    )
                },
                enabled = command.isNotBlank(),
            ) {
                Text(stringResource(R.string.cron_add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cron_cancel_button))
            }
        },
    )
}

/**
 * Renders the mode-specific input fields for the selected schedule type.
 *
 * @param selectedMode The currently selected mode index.
 * @param expression Current cron expression value.
 * @param onExpressionChange Callback for cron expression changes.
 * @param expressionError Validation error for the cron expression field.
 * @param delay Current delay value.
 * @param onDelayChange Callback for delay changes.
 * @param delayError Validation error for the delay field.
 * @param timestamp Current RFC 3339 timestamp value.
 * @param onTimestampChange Callback for timestamp changes.
 * @param timestampError Validation error for the timestamp field.
 * @param intervalMinutes Current interval value in minutes.
 * @param onIntervalChange Callback for interval changes.
 * @param intervalError Validation error for the interval field.
 */
@Composable
@Suppress("LongParameterList")
private fun ScheduleInputFields(
    selectedMode: Int,
    expression: String,
    onExpressionChange: (String) -> Unit,
    expressionError: CronValidationError?,
    delay: String,
    onDelayChange: (String) -> Unit,
    delayError: CronValidationError?,
    timestamp: String,
    onTimestampChange: (String) -> Unit,
    timestampError: CronValidationError?,
    intervalMinutes: String,
    onIntervalChange: (String) -> Unit,
    intervalError: CronValidationError?,
) {
    when (selectedMode) {
        MODE_RECURRING -> {
            OutlinedTextField(
                value = expression,
                onValueChange = onExpressionChange,
                label = { Text(stringResource(R.string.cron_expression_label)) },
                placeholder = { Text(stringResource(R.string.cron_expression_placeholder)) },
                isError = expressionError != null,
                supportingText = errorSupportingText(expressionError),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MODE_ONE_SHOT -> {
            OutlinedTextField(
                value = delay,
                onValueChange = onDelayChange,
                label = { Text(stringResource(R.string.cron_delay_label)) },
                placeholder = { Text(stringResource(R.string.cron_delay_placeholder)) },
                isError = delayError != null,
                supportingText = errorSupportingText(delayError),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MODE_AT_TIME -> {
            OutlinedTextField(
                value = timestamp,
                onValueChange = onTimestampChange,
                label = { Text(stringResource(R.string.cron_timestamp_label)) },
                placeholder = { Text(stringResource(R.string.cron_timestamp_placeholder)) },
                isError = timestampError != null,
                supportingText = errorSupportingText(timestampError),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MODE_INTERVAL -> {
            OutlinedTextField(
                value = intervalMinutes,
                onValueChange = onIntervalChange,
                label = { Text(stringResource(R.string.cron_interval_minutes_label)) },
                placeholder = { Text(stringResource(R.string.cron_interval_minutes_placeholder)) },
                isError = intervalError != null,
                supportingText =
                    intervalError?.let { error ->
                        {
                            Text(
                                text = cronErrorMessage(error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } ?: {
                        Text(stringResource(R.string.cron_interval_supporting))
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Creates a supporting text composable for an error message, or null if no error.
 *
 * @param error The error to display, or null if the field is valid.
 * @return A composable lambda displaying the error, or null.
 */
@Composable
private fun errorSupportingText(error: CronValidationError?): @Composable (() -> Unit)? =
    error?.let { msg ->
        {
            Text(
                text = cronErrorMessage(msg),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

@Composable
private fun cronErrorMessage(error: CronValidationError): String =
    when (error) {
        CronValidationError.ExpressionRequired -> stringResource(R.string.cron_error_expression_required)
        CronValidationError.ExpectedFiveOrSixFields -> stringResource(R.string.cron_error_expected_fields)
        is CronValidationError.InvalidCharactersInField ->
            stringResource(R.string.cron_error_invalid_characters, error.field)
        CronValidationError.DelayRequired -> stringResource(R.string.cron_error_delay_required)
        CronValidationError.DelayFormatInvalid -> stringResource(R.string.cron_error_delay_format)
        CronValidationError.TimestampRequired -> stringResource(R.string.cron_error_timestamp_required)
        CronValidationError.TimestampFormatInvalid -> stringResource(R.string.cron_error_timestamp_format)
        CronValidationError.IntervalRequired -> stringResource(R.string.cron_error_interval_required)
        CronValidationError.IntervalMustBePositive -> stringResource(R.string.cron_error_interval_positive)
    }

/** Maximum number of visible lines for the command text field. */
private const val MAX_COMMAND_LINES = 3

/**
 * Handles the confirm button click by validating the selected mode's input
 * and dispatching to the appropriate callback.
 *
 * @param selectedMode The currently selected mode index.
 * @param expression Current cron expression value.
 * @param delay Current delay value.
 * @param timestamp Current RFC 3339 timestamp value.
 * @param intervalMinutes Current interval value in minutes as a string.
 * @param command The command text.
 * @param onExpressionError Setter for cron expression validation error.
 * @param onDelayError Setter for delay validation error.
 * @param onTimestampError Setter for timestamp validation error.
 * @param onIntervalError Setter for interval validation error.
 * @param onAddRecurring Callback for recurring jobs.
 * @param onAddOneShot Callback for one-shot jobs.
 * @param onAddAtTime Callback for at-time jobs.
 * @param onAddInterval Callback for interval jobs.
 * @param onDismiss Callback to dismiss the dialog.
 */
@Suppress("LongParameterList")
private fun handleConfirm(
    selectedMode: Int,
    expression: String,
    delay: String,
    timestamp: String,
    intervalMinutes: String,
    command: String,
    onExpressionError: (CronValidationError?) -> Unit,
    onDelayError: (CronValidationError?) -> Unit,
    onTimestampError: (CronValidationError?) -> Unit,
    onIntervalError: (CronValidationError?) -> Unit,
    onAddRecurring: (String, String) -> Unit,
    onAddOneShot: (String, String) -> Unit,
    onAddAtTime: (String, String) -> Unit,
    onAddInterval: (ULong, String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (selectedMode) {
        MODE_RECURRING -> {
            val error = validateCronExpression(expression)
            if (error != null) {
                onExpressionError(error)
            } else {
                onAddRecurring(expression.trim(), command.trim())
                onDismiss()
            }
        }
        MODE_ONE_SHOT -> {
            val error = validateDelay(delay)
            if (error != null) {
                onDelayError(error)
            } else {
                onAddOneShot(delay.trim(), command.trim())
                onDismiss()
            }
        }
        MODE_AT_TIME -> {
            val error = validateTimestamp(timestamp)
            if (error != null) {
                onTimestampError(error)
            } else {
                onAddAtTime(timestamp.trim(), command.trim())
                onDismiss()
            }
        }
        MODE_INTERVAL -> {
            val error = validateInterval(intervalMinutes)
            if (error != null) {
                onIntervalError(error)
            } else {
                val minutes = intervalMinutes.trim().toLong()
                val ms = (minutes * MS_PER_MINUTE).toULong()
                onAddInterval(ms, command.trim())
                onDismiss()
            }
        }
    }
}

/**
 * Validates a cron expression for basic structural correctness.
 *
 * Checks that the expression has 5 or 6 space-separated parts and each part
 * contains only valid cron characters. This is a client-side pre-check;
 * the daemon performs full validation.
 *
 * @param expression The cron expression string to validate.
 * @return An error message if invalid, or null if the expression passes basic validation.
 */
private fun validateCronExpression(expression: String): CronValidationError? {
    val trimmed = expression.trim()
    if (trimmed.isBlank()) return CronValidationError.ExpressionRequired
    val parts = trimmed.split("\\s+".toRegex())
    if (parts.size !in MIN_CRON_PARTS..MAX_CRON_PARTS) {
        return CronValidationError.ExpectedFiveOrSixFields
    }
    val validChars = Regex("^[0-9*,/\\-?LW#]+$")
    for (part in parts) {
        if (!validChars.matches(part)) {
            return CronValidationError.InvalidCharactersInField(part)
        }
    }
    return null
}

/**
 * Validates a delay string for basic format correctness.
 *
 * Checks that the delay is non-blank and ends with a recognised time unit
 * suffix (s, m, h, d). The daemon performs full parsing.
 *
 * @param delay The delay string to validate.
 * @return An error message if invalid, or null if the delay passes basic validation.
 */
private fun validateDelay(delay: String): CronValidationError? {
    val trimmed = delay.trim()
    if (trimmed.isBlank()) return CronValidationError.DelayRequired
    if (!trimmed.matches(Regex("^\\d+[smhd]$"))) {
        return CronValidationError.DelayFormatInvalid
    }
    return null
}

/**
 * Validates an RFC 3339 timestamp string for basic format correctness.
 *
 * Checks that the timestamp is non-blank and roughly matches the expected
 * ISO 8601 / RFC 3339 pattern. The daemon performs full parsing.
 *
 * @param timestamp The timestamp string to validate.
 * @return An error message if invalid, or null if the timestamp passes basic validation.
 */
private fun validateTimestamp(timestamp: String): CronValidationError? {
    val trimmed = timestamp.trim()
    if (trimmed.isBlank()) return CronValidationError.TimestampRequired
    val rfc3339Pattern =
        Regex(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.\\d+]*)?(Z|[+-]\\d{2}:\\d{2})$",
        )
    if (!rfc3339Pattern.matches(trimmed)) {
        return CronValidationError.TimestampFormatInvalid
    }
    return null
}

/**
 * Validates an interval string for basic format correctness.
 *
 * Checks that the interval is a positive integer representing minutes.
 *
 * @param interval The interval string to validate.
 * @return An error message if invalid, or null if the interval passes basic validation.
 */
private fun validateInterval(interval: String): CronValidationError? {
    val trimmed = interval.trim()
    if (trimmed.isBlank()) return CronValidationError.IntervalRequired
    val minutes = trimmed.toLongOrNull()
    if (minutes == null || minutes <= 0) {
        return CronValidationError.IntervalMustBePositive
    }
    return null
}

private sealed class CronValidationError {
    object ExpressionRequired : CronValidationError()

    object ExpectedFiveOrSixFields : CronValidationError()

    data class InvalidCharactersInField(
        val field: String,
    ) : CronValidationError()

    object DelayRequired : CronValidationError()

    object DelayFormatInvalid : CronValidationError()

    object TimestampRequired : CronValidationError()

    object TimestampFormatInvalid : CronValidationError()

    object IntervalRequired : CronValidationError()

    object IntervalMustBePositive : CronValidationError()
}
