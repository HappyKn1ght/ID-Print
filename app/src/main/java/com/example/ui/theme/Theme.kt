package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = StudioCobaltLight,
    onPrimary = Color(0xFF081B4B),
    primaryContainer = StudioCobaltContainerDark,
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = EmeraldReadyDark,
    onSecondary = Color(0xFF042F1E),
    secondaryContainer = EmeraldReadyBgDark,
    onSecondaryContainer = EmeraldReadyDark,
    background = DeskDark,
    onBackground = InkPrimaryDark,
    surface = CardDark,
    onSurface = InkPrimaryDark,
    surfaceVariant = CardMutedDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = DangerRedDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudioCobalt,
    onPrimary = Color.White,
    primaryContainer = StudioCobaltContainer,
    onPrimaryContainer = StudioCobaltDark,
    secondary = EmeraldReady,
    onSecondary = Color.White,
    secondaryContainer = EmeraldReadyBg,
    onSecondaryContainer = EmeraldReady,
    background = DeskLight,
    onBackground = InkPrimaryLight,
    surface = CardLight,
    onSurface = InkPrimaryLight,
    surfaceVariant = CardMutedLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = DangerRed,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
