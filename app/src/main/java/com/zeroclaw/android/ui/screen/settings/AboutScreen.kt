/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings

import com.zeroclaw.android.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.BuildConfig
import com.zeroclaw.android.ui.component.SectionHeader
import com.zeroclaw.ffi.getVersion

/**
 * About screen displaying app version, licenses, and project links.
 *
 * @param edgeMargin Horizontal padding based on window width size class.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun AboutScreen(
    edgeMargin: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val unknownLabel = stringResource(R.string.common_unknown)
    var crateVersion by remember(unknownLabel) { mutableStateOf(unknownLabel) }

    LaunchedEffect(unknownLabel) {
        @Suppress("TooGenericExceptionCaught")
        try {
            crateVersion = getVersion()
        } catch (_: Exception) {
            crateVersion = unknownLabel
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = edgeMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.about_section_version))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AboutRow(label = stringResource(R.string.about_app_version_label), value = BuildConfig.VERSION_NAME)
                AboutRow(label = stringResource(R.string.about_build_label), value = BuildConfig.VERSION_CODE.toString())
                AboutRow(label = stringResource(R.string.about_crate_version_label), value = crateVersion)
            }
        }

        SectionHeader(title = stringResource(R.string.about_section_links))
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)),
                )
            },
        ) {
            Text(stringResource(R.string.about_view_on_github))
        }
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(LICENSE_URL)),
                )
            },
        ) {
            Text(stringResource(R.string.about_view_license_mit))
        }

        SectionHeader(title = stringResource(R.string.about_section_credits))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.about_credits_built_with),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Accessible label-value row for the about screen.
 *
 * Uses [semantics] with [mergeDescendants] so screen readers announce
 * the label and value as a single phrase.
 *
 * @param label Description of the value.
 * @param value The displayed value string.
 */
@Composable
private fun AboutRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(R.string.about_row_label_prefix, label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private const val GITHUB_URL = "https://github.com/Natfii/ZeroClaw-Android"
private const val LICENSE_URL = "https://github.com/Natfii/ZeroClaw-Android/blob/main/LICENSE"
