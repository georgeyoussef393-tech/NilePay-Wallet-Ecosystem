package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NilePrimary,
    onPrimary = NileOnPrimary,
    primaryContainer = NilePrimaryContainer,
    onPrimaryContainer = NileOnPrimaryContainer,
    secondary = NileSecondary,
    onSecondary = NileOnSecondary,
    secondaryContainer = NileSecondaryContainer,
    onSecondaryContainer = NileOnSecondaryContainer,
    tertiary = NileTertiary,
    onTertiary = NileOnTertiary,
    background = NileDarkBackground,
    onBackground = NileOnBackground,
    surface = NileDarkSurface,
    onSurface = NileOnSurface,
    surfaceVariant = NileDarkSurfaceVariant,
    onSurfaceVariant = NileOnSurfaceVariant,
    error = NileError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF825500),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    tertiary = NileTertiary,
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1D),
    surface = Color.White,
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFE0E3E3),
    onSurfaceVariant = Color(0xFF3F484A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek fintech dark mode for NilePay
    dynamicColor: Boolean = false, // Keep branded Egyptian fintech colors consistent
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
