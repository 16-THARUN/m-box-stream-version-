package com.mbox.stream.ui.faceplate.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun FaceplateTheme(
    accentColor: Color = Color(0xFFFFB000),
    content: @Composable () -> Unit
) {
    val colors = FaceplateColors(accent = accentColor)
    val typography = FaceplateTypography()

    val darkScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color(0xFF0E0F10),
        surface = Color(0xFF141618),
        onSurface = Color(0xFFECEAE4),
        background = Color(0xFF0E0F10),
        onBackground = Color(0xFFECEAE4)
    )

    CompositionLocalProvider(
        LocalFaceplateColors provides colors,
        LocalFaceplateTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = darkScheme,
            content = content
        )
    }
}
