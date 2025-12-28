package com.harmonixia.android.util

import android.util.Log
import com.harmonixia.android.BuildConfig
import java.net.URI

object Logger {
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        logIfDebug(Log.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        logIfDebug(Log.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logIfDebug(Log.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logIfDebug(Log.ERROR, tag, message, throwable)
    }

    private fun logIfDebug(level: Int, tag: String, message: String, throwable: Throwable?) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            when (level) {
                Log.DEBUG -> if (throwable == null) Log.d(tag, message) else Log.d(tag, message, throwable)
                Log.INFO -> if (throwable == null) Log.i(tag, message) else Log.i(tag, message, throwable)
                Log.WARN -> if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
                Log.ERROR -> if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
                else -> if (throwable == null) Log.d(tag, message) else Log.d(tag, message, throwable)
            }
        }
    }

    fun sanitizeUrl(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        val sanitized = URI(
            uri.scheme,
            null,
            uri.host,
            uri.port,
            uri.path,
            null,
            null
        )
        return sanitized.toString()
    }
}
