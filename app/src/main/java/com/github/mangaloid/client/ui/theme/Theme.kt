package com.github.mangaloid.client.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
  primary = Color(0xFF5A6266),
  primaryVariant = Purple700,
  secondary = Teal200,
  background = Color(0xFFC6CAD7),
  surface = Color(0xFFC6CAD7),
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = Color.Black,
  onSurface = Color.Black,
)

@Composable
fun MangaloidclientTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colors = LightColorPalette,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}