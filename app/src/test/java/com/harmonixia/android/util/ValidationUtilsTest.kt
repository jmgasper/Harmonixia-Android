package com.harmonixia.android.util

import org.junit.Assert.assertEquals
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
}
