package com.AI.clinetar.Saturday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One fixed theme: pure-black (AMOLED) surfaces with a mint-green accent.
 * No light mode, no dynamic colour — the app is always black.
 */
private val BlackColors = darkColorScheme(
    primary = Color(0xFF9BE38E),
    onPrimary = Color(0xFF04310B),
    primaryContainer = Color(0xFF1F4D23),
    onPrimaryContainer = Color(0xFFB9F0AC),

    secondary = Color(0xFFC2CDB4),
    onSecondary = Color(0xFF2C3421),
    secondaryContainer = Color(0xFF424D37),
    onSecondaryContainer = Color(0xFFDEE9CF),

    tertiary = Color(0xFF9FD0CB),
    onTertiary = Color(0xFF003734),

    background = Color(0xFF000000),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFCACACA),

    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),

    outline = Color(0xFF3B3B3B),
    outlineVariant = Color(0xFF222222),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF3B1512),
    onErrorContainer = Color(0xFFFFDAD5),

    inverseSurface = Color(0xFFECECEC),
    inverseOnSurface = Color(0xFF1B1B1B),
    scrim = Color(0xFF000000),
)

@Composable
fun SaturdayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackColors,
        typography = Typography(),
        content = content,
    )
}
