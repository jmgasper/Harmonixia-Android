package com.harmonixia.android.util

import android.os.Bundle
import com.harmonixia.android.domain.model.ProviderMapping

fun Bundle.putProviderExtras(provider: String, providerMappings: List<ProviderMapping>) {
    val trimmedProvider = provider.trim()
    if (trimmedProvider.isNotBlank()) {
        putString(EXTRA_PROVIDER_ID, trimmedProvider)
    }
    val domains = providerMappings.mapNotNull { mapping ->
        val domain = mapping.providerDomain.trim()
        domain.takeIf { it.isNotBlank() }
    }.distinct()
    if (domains.isNotEmpty()) {
        putStringArray(EXTRA_PROVIDER_DOMAINS, domains.toTypedArray())
    }
}
