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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = KoraPrimaryLight,
    onPrimary = KoraOnPrimaryLight,
    primaryContainer = KoraPrimaryContainerLight,
    onPrimaryContainer = KoraOnPrimaryContainerLight,
    secondary = KoraSecondaryLight,
    onSecondary = KoraOnSecondaryLight,
    secondaryContainer = KoraSecondaryContainerLight,
    onSecondaryContainer = KoraOnSecondaryContainerLight,
    tertiary = KoraTertiaryLight,
    onTertiary = KoraOnTertiaryLight,
    tertiaryContainer = KoraTertiaryContainerLight,
    onTertiaryContainer = KoraOnTertiaryContainerLight,
    background = KoraBackgroundLight,
    onBackground = KoraOnBackgroundLight,
    surface = KoraBackgroundLight,
    onSurface = KoraOnBackgroundLight,
    surfaceVariant = KoraSurfaceVariantLight,
    onSurfaceVariant = KoraOnSurfaceVariantLight,
    outline = KoraOutlineLight,
    outlineVariant = KoraOutlineVariantLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9)
)

private val DarkColorScheme = darkColorScheme(
    primary = KoraPrimaryDark,
    onPrimary = KoraOnPrimaryDark,
    primaryContainer = KoraPrimaryContainerDark,
    onPrimaryContainer = KoraOnPrimaryContainerDark,
    secondary = KoraSecondaryDark,
    onSecondary = KoraOnSecondaryDark,
    secondaryContainer = KoraSecondaryContainerDark,
    onSecondaryContainer = KoraOnSecondaryContainerDark,
    tertiary = KoraTertiaryDark,
    onTertiary = KoraOnTertiaryDark,
    tertiaryContainer = KoraTertiaryContainerDark,
    onTertiaryContainer = KoraOnTertiaryContainerDark,
    background = KoraBackgroundDark,
    onBackground = KoraOnBackgroundDark,
    surface = KoraBackgroundDark,
    onSurface = KoraOnBackgroundDark,
    surfaceVariant = KoraSurfaceVariantDark,
    onSurfaceVariant = KoraOnSurfaceVariantDark,
    outline = KoraOutlineDark,
    outlineVariant = KoraOutlineVariantDark,
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A)
)

/**
 * Status colours that Material 3 doesn't define (success / warning / info). Each has a light and
 * dark variant so status badges stay readable in both themes instead of using fixed hex colours.
 */
@Immutable
data class StatusColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val bookmark: Color
)

private val LightStatus = StatusColors(
    success = Color(0xFF1B7F3A),
    warning = Color(0xFFA14F00),
    info = Color(0xFF00658E),
    bookmark = Color(0xFFB37800)
)

private val DarkStatus = StatusColors(
    success = Color(0xFF7DDC93),
    warning = Color(0xFFFFB77C),
    info = Color(0xFF8BCEFF),
    bookmark = Color(0xFFFFCC4D)
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

/** Shortcut: `MaterialTheme.status.success` etc. */
val MaterialTheme.status: StatusColors
    @Composable get() = LocalStatusColors.current

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own Kora colours; wallpaper colours are still available.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
