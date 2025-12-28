package com.harmonixia.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

data class AdaptiveSpacing(
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp
)

@Composable
fun rememberAdaptiveSpacing(): AdaptiveSpacing {
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    return remember(windowSizeClass.widthSizeClass) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> AdaptiveSpacing(
                small = 4.dp,
                medium = 8.dp,
                large = 16.dp,
                extraLarge = 24.dp
            )
            WindowWidthSizeClass.Medium -> AdaptiveSpacing(
                small = 6.dp,
                medium = 12.dp,
                large = 20.dp,
                extraLarge = 32.dp
            )
            WindowWidthSizeClass.Expanded -> AdaptiveSpacing(
                small = 8.dp,
                medium = 16.dp,
                large = 24.dp,
                extraLarge = 40.dp
            )
            else -> AdaptiveSpacing(
                small = 4.dp,
                medium = 8.dp,
                large = 16.dp,
                extraLarge = 24.dp
            )
        }
    }
}

@Composable
fun HarmonixiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
