package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.ProviderBadge
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class ResolveProviderBadgeUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(
        providerKey: String?,
        providerDomains: List<String> = emptyList()
    ): Result<ProviderBadge?> {
        return repository.resolveProviderBadge(providerKey, providerDomains)
    }
}
