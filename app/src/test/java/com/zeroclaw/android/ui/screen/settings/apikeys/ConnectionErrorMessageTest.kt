/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.settings.apikeys

import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [mapConnectionError], the error-classification helper used by
 * [ApiKeysViewModel.testConnection] to convert HTTP and network exceptions
 * into stable error kinds.
 */
@DisplayName("Connection error message mapping")
class ConnectionErrorMessageTest {
    @Test
    @DisplayName("maps HTTP 401 to HTTP_401 kind")
    fun `maps HTTP 401 to HTTP_401 kind`() {
        val e = IOException("HTTP 401 from https://api.openai.com/v1/models")
        assertEquals(
            ConnectionErrorKind.HTTP_401,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps HTTP 403 to HTTP_403 kind")
    fun `maps HTTP 403 to HTTP_403 kind`() {
        val e = IOException("HTTP 403 from https://api.anthropic.com/v1/models")
        assertEquals(
            ConnectionErrorKind.HTTP_403,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps HTTP 404 to HTTP_404 kind")
    fun `maps HTTP 404 to HTTP_404 kind`() {
        val e = IOException("HTTP 404 from https://localhost:11434/models")
        assertEquals(
            ConnectionErrorKind.HTTP_404,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps HTTP 429 to HTTP_429 kind")
    fun `maps HTTP 429 to HTTP_429 kind`() {
        val e = IOException("HTTP 429 from https://api.openai.com/v1/models")
        assertEquals(
            ConnectionErrorKind.HTTP_429,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps other HTTP errors to HTTP_OTHER kind")
    fun `maps other HTTP errors to HTTP_OTHER kind`() {
        val e = IOException("HTTP 500 from https://api.openai.com/v1/models")
        assertEquals(
            ConnectionErrorKind.HTTP_OTHER,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps HTTP 502 to HTTP_OTHER kind")
    fun `maps HTTP 502 to HTTP_OTHER kind`() {
        val e = IOException("HTTP 502 from https://api.openai.com/v1/models")
        assertEquals(
            ConnectionErrorKind.HTTP_OTHER,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps SocketTimeoutException to TIMEOUT kind")
    fun `maps SocketTimeoutException to TIMEOUT kind`() {
        val e = SocketTimeoutException("connect timeout")
        assertEquals(
            ConnectionErrorKind.TIMEOUT,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps timeout in message to TIMEOUT kind")
    fun `maps timeout in message to TIMEOUT kind`() {
        val e = IOException("Read timed out")
        assertEquals(
            ConnectionErrorKind.TIMEOUT,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps timeout case-insensitively to TIMEOUT kind")
    fun `maps timeout case-insensitively to TIMEOUT kind`() {
        val e = IOException("Connection TIMEOUT after 5000ms")
        assertEquals(
            ConnectionErrorKind.TIMEOUT,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps unknown exception to GENERIC kind")
    fun `maps unknown exception to GENERIC kind`() {
        val e = IOException("Connection refused")
        assertEquals(
            ConnectionErrorKind.GENERIC,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("handles null exception message gracefully")
    fun `handles null exception message gracefully`() {
        val e = RuntimeException()
        assertEquals(
            ConnectionErrorKind.GENERIC,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("does not false-positive on URL containing 401")
    fun `does not false-positive on URL containing 401`() {
        val e = IOException("Connection refused to https://example.com:4013/models")
        assertEquals(
            ConnectionErrorKind.GENERIC,
            mapConnectionError(e),
        )
    }

    @Test
    @DisplayName("maps JSON-body auth error (HTTP 401) to HTTP_401 kind")
    fun `maps JSON-body auth error to HTTP_401 kind`() {
        val e = IOException("HTTP 401 from https://api.anthropic.com/v1/models (auth error in response body)")
        assertEquals(
            ConnectionErrorKind.HTTP_401,
            mapConnectionError(e),
        )
    }
}
