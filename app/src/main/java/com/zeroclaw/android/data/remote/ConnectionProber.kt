/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.data.remote

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight reachability probe for local AI server base URLs.
 *
 * Sends a HEAD request (falling back to GET) to the `/models` or root
 * endpoint to determine whether the server at [baseUrl] is responsive.
 * Used to detect stale connections to servers that have gone offline.
 */
object ConnectionProber {
    /** Connect timeout for probe requests. */
    private const val PROBE_CONNECT_TIMEOUT_MS = 2000

    /** Read timeout for probe requests. */
    private const val PROBE_READ_TIMEOUT_MS = 2000

    /** HTTP status codes considered successful for a probe. */
    private const val MAX_SUCCESS_CODE = 399

    /**
     * Probes whether a base URL is reachable.
     *
     * Attempts a HEAD request to the base URL. Returns true if the server
     * responds with any 2xx or 3xx status code within the timeout window.
     *
     * Safe to call from the main thread; dispatches to [Dispatchers.IO].
     *
     * @param baseUrl The provider base URL to probe (e.g. "http://192.168.1.50:1234/v1").
     * @return True if the server responded within the timeout, false otherwise.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun isReachable(baseUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) return@withContext false
            try {
                val url = URL(baseUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = PROBE_CONNECT_TIMEOUT_MS
                connection.readTimeout = PROBE_READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                try {
                    connection.connect()
                    connection.responseCode in 1..MAX_SUCCESS_CODE
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                false
            }
        }
}
