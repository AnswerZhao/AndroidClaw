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
import com.zeroclaw.android.data.ProviderRegistry
import com.zeroclaw.android.model.ProviderInfo

private fun String.providerDisplayNameResIdOrNull(): Int? =
    when (this.lowercase()) {
        "openai" -> R.string.provider_name_openai
        "openai-codex" -> R.string.provider_name_openai_codex
        "anthropic" -> R.string.provider_name_anthropic
        "openrouter" -> R.string.provider_name_openrouter
        "google-gemini" -> R.string.provider_name_google_gemini
        "ollama" -> R.string.provider_name_ollama
        "lmstudio" -> R.string.provider_name_lmstudio
        "vllm" -> R.string.provider_name_vllm
        "localai" -> R.string.provider_name_localai
        "groq" -> R.string.provider_name_groq
        "mistral" -> R.string.provider_name_mistral
        "xai" -> R.string.provider_name_xai
        "deepseek" -> R.string.provider_name_deepseek
        "together" -> R.string.provider_name_together
        "fireworks" -> R.string.provider_name_fireworks
        "perplexity" -> R.string.provider_name_perplexity
        "cohere" -> R.string.provider_name_cohere
        "github-copilot" -> R.string.provider_name_github_copilot
        "venice" -> R.string.provider_name_venice
        "vercel" -> R.string.provider_name_vercel
        "moonshot" -> R.string.provider_name_moonshot
        "minimax" -> R.string.provider_name_minimax
        "glm" -> R.string.provider_name_glm
        "qianfan" -> R.string.provider_name_qianfan
        "cloudflare" -> R.string.provider_name_cloudflare
        "bedrock" -> R.string.provider_name_bedrock
        "novita" -> R.string.provider_name_novita
        "telnyx" -> R.string.provider_name_telnyx
        "synthetic" -> R.string.provider_name_synthetic
        "opencode-zen" -> R.string.provider_name_opencode_zen
        "zai" -> R.string.provider_name_zai
        "custom-openai" -> R.string.provider_name_custom_openai
        "custom-anthropic" -> R.string.provider_name_custom_anthropic
        else -> null
    }

fun ProviderInfo.localizedDisplayName(context: Context): String =
    id.providerDisplayNameResIdOrNull()?.let(context::getString) ?: displayName

@Composable
fun ProviderInfo.localizedDisplayName(): String =
    id.providerDisplayNameResIdOrNull()?.let { stringResource(it) } ?: displayName

fun localizedProviderDisplayName(
    context: Context,
    providerId: String,
    fallback: String = providerId,
): String {
    val resolvedId = ProviderRegistry.findById(providerId)?.id ?: providerId
    return resolvedId.providerDisplayNameResIdOrNull()?.let(context::getString) ?: fallback
}

@Composable
fun localizedProviderDisplayName(
    providerId: String,
    fallback: String = providerId,
): String {
    val resolvedId = ProviderRegistry.findById(providerId)?.id ?: providerId
    return resolvedId.providerDisplayNameResIdOrNull()?.let { stringResource(it) } ?: fallback
}
