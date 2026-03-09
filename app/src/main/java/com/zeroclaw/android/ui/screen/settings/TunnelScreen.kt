/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroclaw.android.ui.component.SecretTextField
import com.zeroclaw.android.ui.component.SectionHeader

/** Available tunnel providers matching upstream TunnelProvider. */
private val TUNNEL_PROVIDERS = listOf("none", "cloudflare", "tailscale", "ngrok", "custom")

/**
 * Tunnel configuration screen for exposing the daemon externally.
 *
 * Maps to the upstream `[tunnel]` TOML section with provider-specific
 * sub-configs for Cloudflare, Tailscale, ngrok, and custom tunnels.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param settingsViewModel The shared [SettingsViewModel].
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(
    edgeMargin: Dp,
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var providerExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.tunnel_section_provider))

        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = it },
        ) {
            OutlinedTextField(
                value = settings.tunnelProvider,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.common_provider)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                modifier =
                    Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false },
            ) {
                for (provider in TUNNEL_PROVIDERS) {
                    DropdownMenuItem(
                        text = { Text(provider) },
                        onClick = {
                            settingsViewModel.updateTunnelProvider(provider)
                            providerExpanded = false
                        },
                    )
                }
            }
        }

        when (settings.tunnelProvider) {
            "cloudflare" -> CloudflareSection(settings, settingsViewModel)
            "tailscale" -> TailscaleSection(settings, settingsViewModel)
            "ngrok" -> NgrokSection(settings, settingsViewModel)
            "custom" -> CustomTunnelSection(settings, settingsViewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Cloudflare tunnel configuration fields.
 *
 * @param settings Current app settings.
 * @param viewModel ViewModel for persisting changes.
 */
@Composable
private fun CloudflareSection(
    settings: com.zeroclaw.android.model.AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.tunnel_section_cloudflare))
    SecretTextField(
        value = settings.tunnelCloudflareToken,
        onValueChange = { viewModel.updateTunnelCloudflareToken(it) },
        label = stringResource(R.string.tunnel_token_label),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Tailscale tunnel configuration fields.
 *
 * @param settings Current app settings.
 * @param viewModel ViewModel for persisting changes.
 */
@Composable
private fun TailscaleSection(
    settings: com.zeroclaw.android.model.AppSettings,
    viewModel: SettingsViewModel,
) {
    val tailscaleFunnelContentDescription =
        stringResource(R.string.tunnel_enable_tailscale_funnel_content_description)
    SectionHeader(title = stringResource(R.string.tunnel_section_tailscale))
    Switch(
        checked = settings.tunnelTailscaleFunnel,
        onCheckedChange = { viewModel.updateTunnelTailscaleFunnel(it) },
        modifier = Modifier.semantics { contentDescription = tailscaleFunnelContentDescription },
    )
    Text(stringResource(R.string.tunnel_enable_funnel))
    OutlinedTextField(
        value = settings.tunnelTailscaleHostname,
        onValueChange = { viewModel.updateTunnelTailscaleHostname(it) },
        label = { Text(stringResource(R.string.tunnel_hostname_optional_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * ngrok tunnel configuration fields.
 *
 * @param settings Current app settings.
 * @param viewModel ViewModel for persisting changes.
 */
@Composable
private fun NgrokSection(
    settings: com.zeroclaw.android.model.AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.tunnel_section_ngrok))
    SecretTextField(
        value = settings.tunnelNgrokAuthToken,
        onValueChange = { viewModel.updateTunnelNgrokAuthToken(it) },
        label = stringResource(R.string.tunnel_auth_token_label),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = settings.tunnelNgrokDomain,
        onValueChange = { viewModel.updateTunnelNgrokDomain(it) },
        label = { Text(stringResource(R.string.tunnel_domain_optional_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Custom tunnel configuration fields.
 *
 * @param settings Current app settings.
 * @param viewModel ViewModel for persisting changes.
 */
@Composable
private fun CustomTunnelSection(
    settings: com.zeroclaw.android.model.AppSettings,
    viewModel: SettingsViewModel,
) {
    SectionHeader(title = stringResource(R.string.tunnel_section_custom))
    OutlinedTextField(
        value = settings.tunnelCustomCommand,
        onValueChange = { viewModel.updateTunnelCustomCommand(it) },
        label = { Text(stringResource(R.string.tunnel_start_command_label)) },
        supportingText = { Text(stringResource(R.string.tunnel_start_command_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = settings.tunnelCustomHealthUrl,
        onValueChange = { viewModel.updateTunnelCustomHealthUrl(it) },
        label = { Text(stringResource(R.string.tunnel_health_url_optional_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = settings.tunnelCustomUrlPattern,
        onValueChange = { viewModel.updateTunnelCustomUrlPattern(it) },
        label = { Text(stringResource(R.string.tunnel_url_pattern_optional_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
