package com.harmonixia.android.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.ui.unit.Dp

class ImageQualityManager(private val context: Context) {
    fun getOptimalImageSize(baseSize: Dp): Dp {
        val batteryState = readBatteryState()
        val onCellular = isOnCellular()
        val onWifi = isOnWifi()
        return when {
            onCellular && batteryState.isLow && !batteryState.isCharging -> baseSize * 0.8f
            onWifi && batteryState.isCharging -> baseSize * 1.1f
            else -> baseSize
        }
    }

    fun getOptimalBitmapConfig(): Bitmap.Config {
        val batteryState = readBatteryState()
        return if (isOnCellular() && batteryState.isLow && !batteryState.isCharging) {
            Bitmap.Config.RGB_565
        } else {
            Bitmap.Config.ARGB_8888
        }
    }

    private fun readBatteryState(): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) {
            return BatteryState(isLow = false, isCharging = false)
        }
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) level / scale.toFloat() else 1f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryState(isLow = batteryPct <= LOW_BATTERY_THRESHOLD, isCharging = isCharging)
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isOnCellular(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private data class BatteryState(
        val isLow: Boolean,
        val isCharging: Boolean
    )

    private companion object {
        private const val LOW_BATTERY_THRESHOLD = 0.2f
    }
}
