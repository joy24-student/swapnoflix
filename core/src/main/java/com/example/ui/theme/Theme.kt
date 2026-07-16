package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Semantic app colors container
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBg: Color,
    val cardBgSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val primary: Color,
    val border: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0C0911),
    surface = Color(0xFF130F1A),
    surfaceVariant = Color(0xFF130F1C),
    cardBg = Color(0xFF1E172E),
    cardBgSecondary = Color(0xFF130F1F),
    textPrimary = Color.White,
    textSecondary = Color(0xFFE0E0E0),
    textMuted = Color.White.copy(alpha = 0.5f),
    accent = Color(0xFF00FFC4),
    primary = Color(0xFFE50914),
    border = Color.White.copy(alpha = 0.1f)
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEEE),
    cardBg = Color.White,
    cardBgSecondary = Color(0xFFF9F9F9),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF424242),
    textMuted = Color(0xFF757575),
    accent = Color(0xFF00FFC4),
    primary = Color(0xFFE50914),
    border = Color(0xFFE0E0E0)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    secondary = Color(0xFF00FFC4),
    onSecondary = Color.Black,
    tertiary = Color(0xFF1E172E),
    background = Color(0xFF0C0911),
    onBackground = Color.White,
    surface = Color(0xFF130F1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E172E),
    onSurfaceVariant = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    secondary = Color(0xFF00FFC4),
    onSecondary = Color.Black,
    tertiary = Color(0xFFEEEEEE),
    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
