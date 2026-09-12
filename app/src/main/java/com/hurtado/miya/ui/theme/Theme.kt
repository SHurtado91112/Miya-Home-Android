package com.hurtado.miya.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = MiyaAccent,
    background = MiyaBackgroundLight,
    surface = MiyaSurfaceLight,
    onBackground = MiyaOnSurfaceLight,
    onSurface = MiyaOnSurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = MiyaAccentDark,
    background = MiyaBackgroundDark,
    surface = MiyaSurfaceDark,
    onBackground = MiyaOnSurfaceDark,
    onSurface = MiyaOnSurfaceDark,
)

/**
 * Root theme, analogue of the SwiftUI app applying `Font.swift`'s role overrides + AccentColor
 * asset globally via the root `View`. Dynamic color is intentionally opt-out-by-default (false)
 * since the iOS app's palette isn't wallpaper-derived; flip to true if that's ever desired.
 */
@Composable
fun MiyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiyaTypography,
        content = content,
    )
}
