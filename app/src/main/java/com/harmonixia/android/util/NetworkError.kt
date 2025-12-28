package com.harmonixia.android.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class NetworkError {
    data class ConnectionError(val cause: Throwable? = null) : NetworkError()
    data class AuthenticationError(val cause: Throwable? = null) : NetworkError()
    data class ServerError(val cause: Throwable? = null) : NetworkError()
    data class TimeoutError(val cause: Throwable? = null) : NetworkError()
    data class UnknownError(val cause: Throwable? = null) : NetworkError()
}

fun Throwable.toNetworkError(): NetworkError {
    return when (this) {
        is SocketTimeoutException -> NetworkError.TimeoutError(this)
        is UnknownHostException, is ConnectException -> NetworkError.ConnectionError(this)
        is SecurityException -> NetworkError.AuthenticationError(this)
        is IOException -> {
            val message = message.orEmpty().lowercase()
            when {
                "401" in message || "unauthorized" in message -> NetworkError.AuthenticationError(this)
                "500" in message || "server" in message -> NetworkError.ServerError(this)
                else -> NetworkError.ConnectionError(this)
            }
        }
        else -> NetworkError.UnknownError(this)
    }
}
