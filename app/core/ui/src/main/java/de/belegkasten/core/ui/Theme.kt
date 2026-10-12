package de.belegkasten.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Store-private theme tokens. No remote fonts: system fonts only (offline).
private val Forest = Color(0xFF0F1A17)
private val Card = Color(0xFF16241F)
private val Ink = Color(0xFFF2EFE6)
private val Muted = Color(0xFFA9B5AD)
private val Lime = Color(0xFFD7F75B)

private val Scheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF101510),
    background = Forest,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Card,
    onSurfaceVariant = Muted,
)

@Composable
fun BelegkastenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
