/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zeroclaw.android.R
import com.zeroclaw.android.model.ProviderInfo

private fun String.providerHelpTextResIdOrNull(): Int? =
    when (this.lowercase()) {
        "openai" -> R.string.provider_help_text_openai
        "anthropic" -> R.string.provider_help_text_anthropic
        "openrouter" -> R.string.provider_help_text_openrouter
        "google-gemini" -> R.string.provider_help_text_google_gemini
        "ollama" -> R.string.provider_help_text_ollama
        "lmstudio" -> R.string.provider_help_text_lmstudio
        "vllm" -> R.string.provider_help_text_vllm
        "localai" -> R.string.provider_help_text_localai
        "groq" -> R.string.provider_help_text_groq
        "mistral" -> R.string.provider_help_text_mistral
        "xai" -> R.string.provider_help_text_xai
        "deepseek" -> R.string.provider_help_text_deepseek
        "together" -> R.string.provider_help_text_together
        "bedrock" -> R.string.provider_help_text_bedrock
        "novita" -> R.string.provider_help_text_novita
        "telnyx" -> R.string.provider_help_text_telnyx
        else -> null
    }

private fun String.providerKeyPrefixHintResIdOrNull(): Int? =
    when (this.lowercase()) {
        "openai" -> R.string.provider_key_prefix_hint_openai
        "anthropic" -> R.string.provider_key_prefix_hint_anthropic
        "openrouter" -> R.string.provider_key_prefix_hint_openrouter
        "google-gemini" -> R.string.provider_key_prefix_hint_google_gemini
        "groq" -> R.string.provider_key_prefix_hint_groq
        "xai" -> R.string.provider_key_prefix_hint_xai
        "deepseek" -> R.string.provider_key_prefix_hint_deepseek
        else -> null
    }

fun ProviderInfo.localizedHelpText(context: Context): String =
    id.providerHelpTextResIdOrNull()?.let(context::getString) ?: helpText

fun ProviderInfo.localizedKeyPrefixHint(context: Context): String =
    id.providerKeyPrefixHintResIdOrNull()?.let(context::getString) ?: keyPrefixHint

@Composable
fun ProviderInfo.localizedHelpText(): String =
    id.providerHelpTextResIdOrNull()?.let { stringResource(it) } ?: helpText

@Composable
fun ProviderInfo.localizedKeyPrefixHint(): String =
    id.providerKeyPrefixHintResIdOrNull()?.let { stringResource(it) } ?: keyPrefixHint
