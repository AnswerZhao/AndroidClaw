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
import com.zeroclaw.android.model.ChannelType

/** Returns localized display names for channel types when resource mapping exists. */
private fun ChannelType.displayNameResIdOrNull(): Int? =
    when (this) {
        ChannelType.TELEGRAM -> R.string.channel_type_name_telegram
        ChannelType.DISCORD -> R.string.channel_type_name_discord
        ChannelType.SLACK -> R.string.channel_type_name_slack
        ChannelType.WHATSAPP -> R.string.channel_type_name_whatsapp
        ChannelType.MATRIX -> R.string.channel_type_name_matrix
        ChannelType.EMAIL -> R.string.channel_type_name_email
        ChannelType.IRC -> R.string.channel_type_name_irc
        ChannelType.LARK -> R.string.channel_type_name_lark
        ChannelType.WEBHOOK -> R.string.channel_type_name_webhook_deprecated
        ChannelType.MATTERMOST -> R.string.channel_type_name_mattermost
        ChannelType.SIGNAL -> R.string.channel_type_name_signal
        ChannelType.LINQ -> R.string.channel_type_name_linq
        ChannelType.WATI -> R.string.channel_type_name_wati
        ChannelType.NEXTCLOUD_TALK -> R.string.channel_type_name_nextcloud_talk
        ChannelType.FEISHU -> R.string.channel_type_name_feishu
        ChannelType.DINGTALK -> R.string.channel_type_name_dingtalk
        ChannelType.QQ -> R.string.channel_type_name_qq
        ChannelType.NOSTR -> R.string.channel_type_name_nostr
        ChannelType.CLAWDTALK -> R.string.channel_type_name_clawdtalk
    }

/** Localized channel display name for non-Compose call sites. */
fun ChannelType.localizedDisplayName(context: Context): String =
    displayNameResIdOrNull()?.let(context::getString) ?: displayName

/** Localized channel display name for Compose call sites. */
@Composable
fun ChannelType.localizedDisplayName(): String =
    displayNameResIdOrNull()?.let { stringResource(it) } ?: displayName
