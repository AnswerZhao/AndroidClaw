/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.util

import android.content.Context
import com.zeroclaw.android.R
import com.zeroclaw.ffi.FfiException

/**
 * Sanitises exception messages before they reach the UI layer.
 *
 * Prevents leaking internal Rust panic backtraces, raw JSON parse errors,
 * or state corruption details to the user. Used by ViewModels and bridge
 * classes that catch [FfiException] or JSON parse exceptions.
 */
object ErrorSanitizer {
    /** Maximum length for a user-visible error message. */
    private const val MAX_UI_MESSAGE_LENGTH = 200

    /** Known error message patterns mapped to resource IDs. */
    private val KNOWN_PATTERN_RES_IDS: Map<String, Int> =
        mapOf(
            "daemon not running" to R.string.error_sanitizer_daemon_not_running,
            "daemon already running" to R.string.error_sanitizer_daemon_already_running,
            "registry url must use https" to R.string.error_sanitizer_registry_https_required,
            "registry fetch failed" to R.string.error_sanitizer_registry_fetch_failed,
            "empty response body from registry" to R.string.error_sanitizer_registry_empty_response,
            "registry response exceeds" to R.string.error_sanitizer_registry_response_too_large,
            "native layer returned invalid status json" to R.string.error_sanitizer_native_status_json_invalid,
            "malformed status json" to R.string.error_sanitizer_native_status_json_invalid,
            "storage directory" to R.string.error_sanitizer_storage_unavailable,
            "read-back mismatch in" to R.string.error_sanitizer_storage_readback_failed,
        )

    /**
     * Returns a localized user-safe error message from an exception.
     *
     * @param context Context used for resolving string resources.
     * @param e The exception to sanitise.
     * @return A localized, user-safe error string.
     */
    fun sanitizeForUi(
        context: Context,
        e: Exception,
    ): String =
        when (e) {
            is FfiException.InternalPanic ->
                context.getString(R.string.error_sanitizer_internal_error_restart_service)
            is FfiException.StateCorrupted ->
                context.getString(R.string.error_sanitizer_internal_state_corrupted_restart_app)
            is org.json.JSONException ->
                context.getString(R.string.error_sanitizer_malformed_native_data)
            else -> sanitizeMessage(context, e.message)
        }

    /**
     * Sanitises a raw error message string for user display using resources.
     *
     * @param context Context used for resolving string resources.
     * @param raw The raw error message, possibly null.
     * @return A localized, user-safe error string.
     */
    fun sanitizeMessage(
        context: Context,
        raw: String?,
    ): String {
        val msg = raw?.removePrefix("detail=")?.trim() ?: return context.getString(R.string.error_sanitizer_unknown_error)
        KNOWN_PATTERN_RES_IDS.forEach { (pattern, resId) ->
            if (msg.contains(pattern, ignoreCase = true)) return context.getString(resId)
        }
        return if (msg.length > MAX_UI_MESSAGE_LENGTH) {
            msg.take(MAX_UI_MESSAGE_LENGTH) + "..."
        } else {
            msg
        }
    }

}
