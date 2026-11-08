package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SingularityDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF07090E),
    primaryContainer = Color(0xFF0E3042),
    onPrimaryContainer = CyanGlow,
    secondary = EmeraldBid,
    onSecondary = Color(0xFF07090E),
    tertiary = RoseAsk,
    onTertiary = Color.White,
    background = TermBackground,
    onBackground = TextPrimary,
    surface = TermSurface,
    onSurface = TextPrimary,
    surfaceVariant = TermSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TermBorder,
    outlineVariant = TermBorderHighlight
)

@Composable
fun SingularityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SingularityDarkColorScheme,
        typography = Typography,
        content = content
    )
}
