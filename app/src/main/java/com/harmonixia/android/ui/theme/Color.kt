package com.harmonixia.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val LosslessQualityGreenLight = Color(0xFFDFF5D8)
private val LosslessQualityOnGreenLight = Color(0xFF1B5E20)
private val LosslessQualityGreenDark = Color(0xFF1B5E20)
private val LosslessQualityOnGreenDark = Color(0xFFE8F5E9)

private val CompressedQualityOrangeLight = Color(0xFFFFE3C2)
private val CompressedQualityOnOrangeLight = Color(0xFF7A3E00)
private val CompressedQualityOrangeDark = Color(0xFFB35A00)
private val CompressedQualityOnOrangeDark = Color(0xFFFFEAD6)

val ExternalPlaybackGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.primaryContainer

val ExternalPlaybackOnGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.onPrimaryContainer

val LosslessQualityGreen: Color
    @Composable
    get() = if (isDarkTheme()) LosslessQualityGreenDark else LosslessQualityGreenLight

val LosslessQualityOnGreen: Color
    @Composable
    get() = if (isDarkTheme()) LosslessQualityOnGreenDark else LosslessQualityOnGreenLight

val CompressedQualityOrange: Color
    @Composable
    get() = if (isDarkTheme()) CompressedQualityOrangeDark else CompressedQualityOrangeLight

val CompressedQualityOnOrange: Color
    @Composable
    get() = if (isDarkTheme()) CompressedQualityOnOrangeDark else CompressedQualityOnOrangeLight

@Composable
private fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}
