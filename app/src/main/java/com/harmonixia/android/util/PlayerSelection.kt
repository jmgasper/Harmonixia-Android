package com.harmonixia.android.util

import android.os.Build
import com.harmonixia.android.domain.model.Player

object PlayerSelection {
    fun isLocalPlayer(player: Player, localPlayerId: String? = null): Boolean {
        if (!localPlayerId.isNullOrBlank() && player.playerId == localPlayerId) {
            return true
        }
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val nameCandidates = listOf(model, Build.DEVICE, Build.PRODUCT)
            .filter { it.isNotBlank() }
        return matchesDevice(player, manufacturer, model, nameCandidates)
    }

    fun selectLocalPlayer(players: List<Player>, localPlayerId: String? = null): Player? {
        if (players.isEmpty()) return null
        if (!localPlayerId.isNullOrBlank()) {
            val localPlayer = players.firstOrNull { it.playerId == localPlayerId }
            if (localPlayer != null) return localPlayer
        }
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val nameCandidates = listOf(model, Build.DEVICE, Build.PRODUCT)
            .filter { it.isNotBlank() }

        val localPlayers = players.filter { player ->
            matchesDevice(player, manufacturer, model, nameCandidates)
        }
        if (localPlayers.isEmpty()) return null
        return localPlayers.firstOrNull { it.available && it.enabled }
            ?: localPlayers.firstOrNull { it.available }
            ?: localPlayers.first()
    }

    private fun matchesDevice(
        player: Player,
        manufacturer: String,
        model: String,
        nameCandidates: List<String>
    ): Boolean {
        val manufacturerMatches = fuzzyMatch(player.deviceManufacturer, manufacturer)
        val modelMatches = fuzzyMatch(player.deviceModel, model)
        val deviceInfoMatches = when {
            !player.deviceModel.isNullOrBlank() && !player.deviceManufacturer.isNullOrBlank() ->
                manufacturerMatches && modelMatches
            !player.deviceModel.isNullOrBlank() -> modelMatches
            else -> false
        }
        val nameMatches = nameCandidates.any { candidate ->
            player.name.contains(candidate, ignoreCase = true)
        }
        return deviceInfoMatches || nameMatches
    }

    private fun fuzzyMatch(left: String?, right: String?): Boolean {
        if (left.isNullOrBlank() || right.isNullOrBlank()) return false
        val normalizedLeft = left.trim().lowercase()
        val normalizedRight = right.trim().lowercase()
        return normalizedLeft == normalizedRight ||
            normalizedLeft.contains(normalizedRight) ||
            normalizedRight.contains(normalizedLeft)
    }
}
