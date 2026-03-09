/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zeroclaw.android.R
import com.zeroclaw.android.data.ProviderRegistry
import com.zeroclaw.android.model.ProviderCategory
import com.zeroclaw.android.model.ProviderInfo
import com.zeroclaw.android.ui.i18n.localizedDisplayName

/** Spacing between the provider icon and display name in dropdown items. */
private const val ICON_NAME_SPACING_DP = 12

/** Vertical padding for category section headers. */
private const val SECTION_HEADER_PADDING_DP = 8

/**
 * Material 3 dropdown for selecting an AI provider from the [ProviderRegistry].
 *
 * Items are grouped by [ProviderCategory] with section headers. A filter/search
 * field allows quick navigation among 32 entries. Unknown provider IDs from
 * legacy data display as raw strings with a fallback icon.
 *
 * @param selectedProviderId Currently selected provider ID, or empty string.
 * @param onProviderSelected Callback invoked when the user selects a provider.
 * @param modifier Modifier applied to the root layout.
 * @param label Text label for the dropdown field.
 * @param enabled Whether the dropdown is interactive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDropdown(
    selectedProviderId: String,
    onProviderSelected: (ProviderInfo) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
) {
    val fieldLabel =
        if (label.isBlank()) {
            stringResource(R.string.provider_dropdown_label)
        } else {
            label
        }
    val dropdownContentDescription =
        stringResource(
            R.string.provider_dropdown_content_description,
            fieldLabel,
        )
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    val displayText =
        ProviderRegistry.findById(selectedProviderId)?.localizedDisplayName(context)
            ?: selectedProviderId.ifEmpty { "" }

    val grouped =
        remember {
            ProviderRegistry
                .allByCategory()
                .mapValues { (_, providers) -> providers.filter { !it.internal } }
                .filterValues { it.isNotEmpty() }
        }
    val filteredGrouped =
        remember(filterText) {
            if (filterText.isBlank()) {
                grouped
            } else {
                val query = filterText.lowercase()
                grouped
                    .mapValues { (_, providers) ->
                        providers.filter { provider ->
                            provider.localizedDisplayName(context).lowercase().contains(query) ||
                                provider.id.contains(query) ||
                                provider.aliases.any { it.contains(query) }
                        }
                    }.filterValues { it.isNotEmpty() }
            }
        }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) filterText else displayText,
            onValueChange = { filterText = it },
            readOnly = !expanded,
            label = { Text(fieldLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .semantics { contentDescription = dropdownContentDescription },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                filterText = ""
                focusManager.clearFocus()
            },
        ) {
            filteredGrouped.forEach { (category, providers) ->
                DropdownMenuItem(
                    text = {
                        val categoryLabel =
                            when (category) {
                                ProviderCategory.PRIMARY ->
                                    stringResource(R.string.provider_dropdown_category_popular)
                                ProviderCategory.ECOSYSTEM ->
                                    stringResource(R.string.provider_dropdown_category_more_providers)
                                ProviderCategory.CUSTOM ->
                                    stringResource(R.string.provider_dropdown_category_custom_endpoints)
                            }
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = SECTION_HEADER_PADDING_DP.dp),
                        )
                    },
                    onClick = {},
                    enabled = false,
                )

                providers.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProviderIcon(provider = provider.id)
                                Spacer(modifier = Modifier.width(ICON_NAME_SPACING_DP.dp))
                                Text(provider.localizedDisplayName())
                            }
                        },
                        onClick = {
                            onProviderSelected(provider)
                            expanded = false
                            filterText = ""
                            focusManager.clearFocus()
                        },
                    )
                }
            }
        }
    }
}
