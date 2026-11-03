package com.mbox.stream.ui.faceplate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

enum class StageDisplayMode {
    RECORD,
    VISUAL,
    COVER
}

@Composable
fun SegmentedControl(
    selectedMode: StageDisplayMode,
    onModeSelected: (StageDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StageDisplayMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (isSelected) colors.accent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onModeSelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.inkOnAccent else colors.textSub,
                        letterSpacing = 0.12.sp
                    )
                }
            }
        }
    }
}
