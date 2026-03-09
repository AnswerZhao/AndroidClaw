/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.service

import android.content.Context
import android.util.Log
import com.zeroclaw.android.R
import com.zeroclaw.android.data.repository.ActivityRepository
import com.zeroclaw.android.model.ActivityType
import com.zeroclaw.android.model.DaemonEvent
import com.zeroclaw.ffi.FfiEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Bridge between the Rust event callback interface and the Kotlin reactive layer.
 *
 * Implements [FfiEventListener] so it can be registered with the native daemon via
 * [com.zeroclaw.ffi.registerEventListener]. Incoming JSON event strings are parsed
 * into [DaemonEvent] instances and emitted on a [SharedFlow] for ViewModel consumption.
 * Each event is also persisted to the [ActivityRepository] for the dashboard feed.
 *
 * The internal [MutableSharedFlow] uses [BufferOverflow.DROP_OLDEST] with a capacity of
 * 64 events to avoid back-pressure blocking the native callback thread.
 *
 * @param activityRepository Repository for persisting events to the activity feed.
 * @param scope Coroutine scope for asynchronous emission and persistence.
 */
class EventBridge(
    private val context: Context,
    private val activityRepository: ActivityRepository,
    private val scope: CoroutineScope,
) : FfiEventListener {
    private val _events =
        MutableSharedFlow<DaemonEvent>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Observable stream of daemon events, parsed from the native callback JSON. */
    val events: SharedFlow<DaemonEvent> = _events.asSharedFlow()

    /**
     * Called by the native layer when a new event is produced.
     *
     * Parses the JSON string into a [DaemonEvent], emits it on [events],
     * and records it in the [ActivityRepository]. Malformed JSON is logged at
     * warning level and dropped to avoid crashing the native callback thread.
     *
     * @param eventJson Raw JSON event string from the Rust daemon.
     */
    override fun onEvent(eventJson: String) {
        val event = parseEvent(eventJson) ?: return
        scope.launch {
            _events.emit(event)
            val (type, message) = event.toActivityRecord(context)
            activityRepository.record(type, message)
        }
    }

    /**
     * Registers this bridge as the active event listener with the native daemon.
     *
     * Only one listener may be registered at a time; calling this replaces any
     * previously registered listener.
     */
    fun register() {
        com.zeroclaw.ffi.registerEventListener(this)
    }

    /**
     * Removes this bridge as the active event listener from the native daemon.
     *
     * After calling this method, no further [onEvent] callbacks will be received.
     */
    fun unregister() {
        com.zeroclaw.ffi.unregisterEventListener()
    }

    /** Constants for [EventBridge]. */
    companion object {
        private const val BUFFER_CAPACITY = 64
    }
}

private const val EVENT_BRIDGE_TAG = "EventBridge"

/**
 * Parses a raw JSON event string into a [DaemonEvent].
 *
 * Expected schema: `{"id":N,"timestamp_ms":N,"kind":"...","data":{...}}`.
 * Malformed JSON is logged at warning level and returns null to avoid
 * crashing the native callback thread.
 *
 * @param json Raw JSON string from the native callback.
 * @return Parsed [DaemonEvent], or `null` if the JSON is malformed.
 */
private fun parseEvent(json: String): DaemonEvent? =
    try {
        val obj = JSONObject(json)
        val dataObj = obj.optJSONObject("data") ?: JSONObject()
        val dataMap = mutableMapOf<String, String>()
        for (key in dataObj.keys()) {
            dataMap[key] = dataObj.optString(key, "")
        }
        DaemonEvent(
            id = obj.getLong("id"),
            timestampMs = obj.getLong("timestamp_ms"),
            kind = obj.getString("kind"),
            data = dataMap,
        )
    } catch (
        @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
    ) {
        Log.w(EVENT_BRIDGE_TAG, "Malformed event JSON — skipping")
        null
    }

/**
 * Maps a [DaemonEvent] to an [ActivityType] and human-readable message for persistence.
 *
 * @receiver The daemon event to convert.
 * @return Pair of [ActivityType] and formatted message string.
 */
private fun DaemonEvent.toActivityRecord(context: Context): Pair<ActivityType, String> {
    val type =
        when (kind) {
            "error" -> ActivityType.DAEMON_ERROR
            else -> ActivityType.FFI_CALL
        }
    val provider = data["provider"].orUnknown(context)
    val model = data["model"].orUnknown(context)
    val duration = data["duration_ms"].asDurationLabel(context)
    val tool = data["tool"].orUnknown(context)
    val channel = data["channel"].orUnknown(context)
    val direction = data["direction"].orUnknown(context)
    val component = data["component"].orUnknown(context)
    val message =
        when (kind) {
            "llm_request" -> context.getString(R.string.event_bridge_message_llm_request, provider, model)
            "llm_response" -> context.getString(R.string.event_bridge_message_llm_response, provider, duration)
            "tool_call" -> context.getString(R.string.event_bridge_message_tool_call, tool, duration)
            "tool_call_start" -> context.getString(R.string.event_bridge_message_tool_call_start, tool)
            "channel_message" -> context.getString(R.string.event_bridge_message_channel_message, channel, direction)
            "error" -> {
                val safeMessage = sanitizeActivityMessage(data["message"], context)
                context.getString(R.string.event_bridge_message_error, component, safeMessage)
            }
            "heartbeat_tick" -> context.getString(R.string.event_bridge_message_heartbeat_tick)
            "turn_complete" -> context.getString(R.string.event_bridge_message_turn_complete)
            "agent_start" -> context.getString(R.string.event_bridge_message_agent_start, provider, model)
            "agent_end" -> context.getString(R.string.event_bridge_message_agent_end, duration)
            else -> context.getString(R.string.event_bridge_message_generic, kind)
        }
    return type to message
}

/** Maximum length for error messages recorded in the activity feed. */
private const val MAX_ACTIVITY_MESSAGE_LENGTH = 120

/** Pattern matching URLs in error messages. */
private val URL_PATTERN = Regex("""https?://\S+""")

/**
 * Truncates and strips URLs from an error message for activity feed display.
 *
 * @param msg Raw error message from the daemon event, or `null`.
 * @return Sanitised message safe for the activity feed.
 */
private fun sanitizeActivityMessage(
    msg: String?,
    context: Context,
): String {
    if (msg.isNullOrBlank()) return context.getString(R.string.event_bridge_unknown)
    val stripped = msg.replace(URL_PATTERN, context.getString(R.string.event_bridge_url_redacted))
    return if (stripped.length > MAX_ACTIVITY_MESSAGE_LENGTH) {
        stripped.take(MAX_ACTIVITY_MESSAGE_LENGTH) + "..."
    } else {
        stripped
    }
}

private fun String?.orUnknown(context: Context): String =
    if (this.isNullOrBlank()) {
        context.getString(R.string.event_bridge_unknown)
    } else {
        this
    }

private fun String?.asDurationLabel(context: Context): String =
    if (this.isNullOrBlank()) {
        context.getString(R.string.event_bridge_unknown)
    } else {
        context.getString(R.string.event_bridge_duration_ms, this)
    }
