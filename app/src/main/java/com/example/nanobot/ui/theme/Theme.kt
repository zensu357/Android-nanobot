package com.example.nanobot.ui.theme

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
    primary = Copper,
    onPrimary = Cream,
    primaryContainer = Sand,
    onPrimaryContainer = Charcoal,
    secondary = Moss,
    onSecondary = Cream,
    secondaryContainer = Ivory,
    onSecondaryContainer = Charcoal,
    background = Cream,
    onBackground = Charcoal,
    surface = Ivory,
    onSurface = Charcoal
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = Charcoal,
    primaryContainer = Copper,
    onPrimaryContainer = Cream,
    secondary = Moss,
    onSecondary = Charcoal,
    secondaryContainer = Charcoal,
    onSecondaryContainer = Ivory,
    background = Charcoal,
    onBackground = Ivory,
    surface = ColorTokens.surfaceDark,
    onSurface = Ivory
)

@Composable
fun NanobotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NanobotTypography,
        content = content
    )
}

private object ColorTokens {
    val surfaceDark = androidx.compose.ui.graphics.Color(0xFF273038)
}
