package com.mbox.stream.ui.faceplate.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FaceplateColors(
    val ground: Color = Color(0xFF0E0F10),
    val surface: Color = Color(0xFF141618),
    val surfaceRaised: Color = Color(0xFF17191B),
    val line: Color = Color(0xFF2A2D30),
    val lineStrong: Color = Color(0xFF3A3D41),
    val text: Color = Color(0xFFECEAE4),
    val textSub: Color = Color(0xFFC9C6BE),
    val textMuted: Color = Color(0xFF9A9C9F),
    val accent: Color = Color(0xFFFFB000),
    val metal: Color = Color(0xFFCFCBC2),
    val inkOnAccent: Color = Color(0xFF0E0F10)
)

val LocalFaceplateColors = staticCompositionLocalOf { FaceplateColors() }

val FaceplateThemeColors: FaceplateColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFaceplateColors.current
