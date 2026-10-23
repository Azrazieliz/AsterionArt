package com.asterion.art.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B101C)
val Panel = Color(0xFF182234)
val Raised = Color(0xFF263349)
val Gold = Color(0xFFEBC88C)
val Blue = Color(0xFF9CB9FF)
val Mist = Color(0xFFAFBED5)
val White = Color(0xFFF4F6FD)
val Green = Color(0xFF8FDAAF)
val Red = Color(0xFFFFA5AB)

private val palette = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Blue,
    onSecondary = Ink,
    tertiary = Green,
    background = Ink,
    onBackground = White,
    surface = Panel,
    onSurface = White,
    surfaceVariant = Raised,
    onSurfaceVariant = Mist,
    error = Red
)

@Composable
fun AsterionArtTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette, content = content)
}
