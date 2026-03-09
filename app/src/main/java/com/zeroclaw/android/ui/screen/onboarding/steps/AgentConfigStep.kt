/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.onboarding.steps

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R

/**
 * Onboarding step for naming the daemon.
 *
 * Allows the user to set a name that becomes both the AIEOS identity
 * and the nickname for their first connection profile. The name is
 * propagated to [com.zeroclaw.android.ui.screen.onboarding.OnboardingCoordinator]
 * and persisted when onboarding completes.
 *
 * @param agentName Current daemon name value.
 * @param onAgentNameChanged Callback when the user edits the name.
 */
@Composable
fun AgentConfigStep(
    agentName: String,
    onAgentNameChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier.imePadding(),
    ) {
        Text(
            text = stringResource(R.string.agent_config_step_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.agent_config_step_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))

        var hasInteracted by remember { mutableStateOf(false) }
        val showError = hasInteracted && agentName.isBlank()

        OutlinedTextField(
            value = agentName,
            onValueChange = {
                hasInteracted = true
                onAgentNameChanged(it)
            },
            label = { Text(stringResource(R.string.identity_agent_name_required_label)) },
            singleLine = true,
            isError = showError,
            supportingText =
                if (showError) {
                    { Text(stringResource(R.string.identity_agent_name_required_error)) }
                } else {
                    null
                },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.agent_config_step_footer_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
