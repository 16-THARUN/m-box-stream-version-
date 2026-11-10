package com.mbox.stream.ui.faceplate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

data class VisualizerModeOption(val id: String, val name: String)

val visualizerModeOptions = listOf(
    VisualizerModeOption("circular_rings", "CIRCULAR SPECTRUM"),
    VisualizerModeOption("orbiting_stardust", "ORBITING STARDUST"),
    VisualizerModeOption("acoustic_ripples", "ACOUSTIC RIPPLES"),
    VisualizerModeOption("groove_bleed", "GROOVE BLEED"),
    VisualizerModeOption("liquid_spline", "LIQUID SPLINE"),
    VisualizerModeOption("glass_bars", "GLASS BARS"),
    VisualizerModeOption("bottom_glass_bars", "BOTTOM GLASS BARS"),
    VisualizerModeOption("symmetrical_waves", "SYMMETRICAL WAVES"),
    VisualizerModeOption("particle_swarms", "PARTICLE SWARMS"),
    VisualizerModeOption("digital_bars", "DIGITAL BARS"),
    VisualizerModeOption("oscilloscope", "OSCILLOSCOPE"),
    VisualizerModeOption("plain_bars", "PLAIN BARS"),
    VisualizerModeOption("aurora_borealis", "AURORA BOREALIS"),
    VisualizerModeOption("anamorphic_flare", "ANAMORPHIC FLARE"),
    VisualizerModeOption("hexagonal_grid", "HEXAGONAL GRID"),
    VisualizerModeOption("analog_glitch", "ANALOG GLITCH"),
    VisualizerModeOption("volumetric_particles", "VOLUMETRIC PARTICLES"),
    VisualizerModeOption("ambient_cover", "AMBIENT COVER"),
    VisualizerModeOption("off", "OFF")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerPickerSheet(
    selectedModeId: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FaceplateThemeColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.ground,
        contentColor = colors.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.lineStrong) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "SELECT VISUALIZER RING",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(visualizerModeOptions) { option ->
                    val isSelected = (option.id == selectedModeId)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.surfaceRaised else colors.surface,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.accent else colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModeSelected(option.id)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.accent else colors.text,
                                letterSpacing = 0.1.sp
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
