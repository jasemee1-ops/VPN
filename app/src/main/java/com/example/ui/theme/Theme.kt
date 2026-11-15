package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PesDarkColorScheme = darkColorScheme(
    primary = PitchEmerald,
    onPrimary = ObsidianBg,
    primaryContainer = PitchEmeraldContainer,
    onPrimaryContainer = PitchEmerald,
    secondary = ChampGold,
    onSecondary = ObsidianBg,
    secondaryContainer = ChampGoldContainer,
    onSecondaryContainer = ChampGold,
    tertiary = GermanCrimson,
    onTertiary = TextPrimary,
    tertiaryContainer = GermanCrimsonContainer,
    onTertiaryContainer = GermanCrimson,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StadiumNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = StadiumNavy,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = ElevatedSurface,
    surfaceContainerHighest = ColorTokens.HighestSurface,
    outline = OutlineSubtle,
    outlineVariant = OutlineSubtle.copy(alpha = 0.55f),
    error = GermanCrimson,
    onError = TextPrimary
)

private object ColorTokens {
    val HighestSurface = androidx.compose.ui.graphics.Color(0xFF21334F)
}

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PesVpnTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PesDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
