package com.harmonixia.android.util

import java.net.URI
import java.net.URLDecoder

fun resolveAutoArtworkUrl(rawUrl: String?): String? {
    val trimmed = rawUrl?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val parsedUri = runCatching { URI(trimmed) }.getOrNull() ?: return trimmed
    val encodedPath = parsedUri.rawPath ?: return trimmed
    if (!encodedPath.endsWith("/imageproxy")) return trimmed
    val pathParam = parsedUri.rawQuery.queryParameter("path")?.trim().orEmpty()
    if (pathParam.isBlank()) return trimmed
    if (pathParam.startsWith("http://") || pathParam.startsWith("https://")) {
        return pathParam
    }
    val provider = parsedUri.rawQuery.queryParameter("provider").orEmpty()
    if (provider == "builtin") {
        val base = trimmed.substringBefore("/imageproxy")
        val normalizedPath = pathParam.trimStart('/')
        if (normalizedPath.isNotBlank()) {
            return "$base/$normalizedPath"
        }
    }
    return trimmed
}

private fun String?.queryParameter(name: String): String? {
    val query = this ?: return null
    if (query.isBlank()) return null
    return query.split('&')
        .asSequence()
        .mapNotNull { pair ->
            val key = pair.substringBefore("=", missingDelimiterValue = pair)
            val value = pair.substringAfter("=", "")
            decodeQueryValue(key) to decodeQueryValue(value)
        }
        .firstOrNull { (key, _) -> key == name }
        ?.second
}

private fun decodeQueryValue(value: String): String {
    return runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }
        .getOrDefault(value)
}
