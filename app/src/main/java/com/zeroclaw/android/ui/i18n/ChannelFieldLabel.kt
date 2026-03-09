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
import com.zeroclaw.android.model.ChannelFieldSpec

private fun String.channelFieldLabelResIdOrNull(): Int? =
    when (this) {
        "access_token" -> R.string.channel_field_label_access_token
        "account" -> R.string.channel_field_label_account_phone_number
        "allowed_destinations" -> R.string.channel_field_label_allowed_destinations
        "allowed_from" -> R.string.channel_field_label_allowed_from
        "allowed_numbers" -> R.string.channel_field_label_allowed_numbers
        "allowed_pubkeys" -> R.string.channel_field_label_allowed_pubkeys
        "allowed_senders" -> R.string.channel_field_label_allowed_senders
        "allowed_users" -> R.string.channel_field_label_allowed_users
        "api_key" -> R.string.channel_field_label_api_key
        "api_token" -> R.string.channel_field_label_api_token
        "api_url" -> R.string.channel_field_label_api_url
        "app_id" -> R.string.channel_field_label_app_id
        "app_secret" -> R.string.channel_field_label_app_secret
        "app_token" -> R.string.channel_field_label_app_token
        "base_url" -> R.string.channel_field_label_server_url
        "bot_token" -> R.string.channel_field_label_bot_token
        "channel_id" -> R.string.channel_field_label_channel_id
        "channels" -> R.string.channel_field_label_channels
        "client_id" -> R.string.channel_field_label_client_id
        "client_secret" -> R.string.channel_field_label_client_secret
        "connection_id" -> R.string.channel_field_label_connection_id
        "encrypt_key" -> R.string.channel_field_label_encrypt_key
        "from_address" -> R.string.channel_field_label_from_address
        "from_number" -> R.string.channel_field_label_from_number
        "from_phone" -> R.string.channel_field_label_from_phone_number
        "group_id" -> R.string.channel_field_label_group_id
        "guild_id" -> R.string.channel_field_label_guild_id
        "homeserver" -> R.string.channel_field_label_homeserver_url
        "http_url" -> R.string.channel_field_label_signal_cli_http_url
        "ignore_attachments" -> R.string.channel_field_label_ignore_attachments
        "ignore_stories" -> R.string.channel_field_label_ignore_stories
        "imap_folder" -> R.string.channel_field_label_imap_folder
        "imap_host" -> R.string.channel_field_label_imap_host
        "imap_port" -> R.string.channel_field_label_imap_port
        "listen_to_bots" -> R.string.channel_field_label_listen_to_bots
        "mention_only" -> R.string.channel_field_label_mention_only
        "nickname" -> R.string.channel_field_label_nickname
        "nickserv_password" -> R.string.channel_field_label_nickserv_password
        "pair_code" -> R.string.channel_field_label_pair_code_web_mode
        "pair_phone" -> R.string.channel_field_label_pair_phone_web_mode
        "password" -> R.string.channel_field_label_password
        "phone_number_id" -> R.string.channel_field_label_phone_number_id
        "poll_interval_secs" -> R.string.channel_field_label_poll_interval_seconds
        "port" -> R.string.channel_field_label_port
        "private_key" -> R.string.channel_field_label_private_key
        "receive_mode" -> R.string.channel_field_label_receive_mode
        "relays" -> R.string.channel_field_label_relays
        "room_id" -> R.string.channel_field_label_room_id
        "sasl_password" -> R.string.channel_field_label_sasl_password
        "secret" -> R.string.channel_field_label_secret
        "server" -> R.string.channel_field_label_server
        "server_password" -> R.string.channel_field_label_server_password
        "session_path" -> R.string.channel_field_label_session_path_web_mode
        "signing_secret" -> R.string.channel_field_label_signing_secret
        "smtp_host" -> R.string.channel_field_label_smtp_host
        "smtp_port" -> R.string.channel_field_label_smtp_port
        "smtp_tls" -> R.string.channel_field_label_smtp_tls
        "tenant_id" -> R.string.channel_field_label_tenant_id
        "thread_replies" -> R.string.channel_field_label_thread_replies
        "url" -> R.string.channel_field_label_server_url
        "username" -> R.string.channel_field_label_username
        "verification_token" -> R.string.channel_field_label_verification_token
        "verify_tls" -> R.string.channel_field_label_verify_tls
        "verify_token" -> R.string.channel_field_label_verify_token
        "webhook_secret" -> R.string.channel_field_label_webhook_secret
        else -> null
    }

fun ChannelFieldSpec.localizedLabel(context: Context): String =
    key.channelFieldLabelResIdOrNull()?.let(context::getString) ?: label

@Composable
fun ChannelFieldSpec.localizedLabel(): String =
    key.channelFieldLabelResIdOrNull()?.let { stringResource(it) } ?: label
