package com.harmonixia.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun normalizeUrl_uppercaseHttpsScheme_preservesHttps() {
        val normalized = ValidationUtils.normalizeUrl("HTTPS://example.com/")

        assertEquals("https://example.com", normalized)
    }

    @Test
    fun normalizeUrl_uppercaseWssScheme_convertsToHttps() {
        val normalized = ValidationUtils.normalizeUrl("WSS://example.com/")

        assertEquals("https://example.com", normalized)
    }

    @Test
    fun isValidUrl_uppercaseHttpsScheme_isValid() {
        val isValid = ValidationUtils.isValidUrl("HTTPS://example.com")

        assertTrue(isValid)
    }

    @Test
    fun normalizeUrl_mixedCaseWsScheme_withPort_normalizesToHttp() {
        val normalized = ValidationUtils.normalizeUrl("Ws://example.com:8095/")

        assertEquals("http://example.com:8095", normalized)
    }

    @Test
    fun isValidUrl_withExplicitPort_isValid() {
        val isValid = ValidationUtils.isValidUrl("https://example.com:8095")

        assertTrue(isValid)
    }

    @Test
    fun isValidUrl_withOutOfRangePort_isInvalid() {
        val isValid = ValidationUtils.isValidUrl("https://example.com:65536")

        assertFalse(isValid)
    }

    @Test
    fun isValidUrl_withUserInfo_isInvalid() {
        val isValid = ValidationUtils.isValidUrl("https://user:pass@example.com")

        assertFalse(isValid)
    }

    @Test
    fun isValidUrl_withIpv6Literal_isValid() {
        val isValid = ValidationUtils.isValidUrl("https://[2001:db8::1]")

        assertTrue(isValid)
    }

    @Test
    fun isValidUrl_withIpv6LiteralAndPort_isValid() {
        val isValid = ValidationUtils.isValidUrl("https://[2001:db8::1]:8095")

        assertTrue(isValid)
    }
}
