package com.sadhana.card.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RustPrimary,
    onPrimary = Color(0xFFFBF1DF),
    primaryContainer = SaffronSoft,
    onPrimaryContainer = SaffronDark,
    secondary = SaffronAccent,
    onSecondary = Color.White,
    secondaryContainer = TileBg,
    onSecondaryContainer = TextDark,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = TileBg,
    onSurfaceVariant = TextMuted,
    outline = BorderCream,
    outlineVariant = DividerCream
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBg,
    primaryContainer = DarkSaffronSoft,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSaffronAccent,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = GoldAccent,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun SadhanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SadhanaTypography,
        shapes = SadhanaShapes,
        content = content
    )
}
