package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.data.model.NowPlayingState
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceplateSignalPathSheet(
    nowPlayingState: NowPlayingState,
    onDismiss: () -> Unit
) {
    val colors = FaceplateThemeColors
    val signal = nowPlayingState.signalPath
    val isLossless = signal?.format?.contains("lossless", ignoreCase = true) == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.ground,
        contentColor = colors.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.lineStrong) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Big Status Word
            Text(
                text = if (isLossless) "LOSSLESS STREAM" else "LOSSY STREAM",
                fontFamily = FontFamily.SansSerif,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Plain sentence
            Text(
                text = if (isLossless)
                    "Audio is passed without loss of original resolution to output hardware."
                else
                    "Audio is compressed using YouTube's Opus codec at 160 kbps and played via ExoPlayer.",
                fontSize = 14.sp,
                color = colors.textSub,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AUDIO SIGNAL PATH",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Timeline Steps
            val steps = listOf(
                Triple("SOURCE", signal?.format ?: "—", "OK"),
                Triple("DECODER", signal?.decoder ?: "—", "OK"),
                Triple("RESAMPLER", "NOT USED", "BYPASSED"),
                Triple("PROCESSING", "EQ OFF", "BYPASSED"),
                Triple("VOLUME", "100%", "PASSTHROUGH"),
                Triple("OUTPUT", nowPlayingState.activeOutputDevice.uppercase() + " (AUDIOTRACK)", "OK")
            )

            steps.forEachIndexed { index, (label, valText, status) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (index == 0 || index == steps.lastIndex) colors.accent else colors.lineStrong)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 0.12.sp
                            )
                            Text(
                                text = valText.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.text
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.line)
                    ) {
                        Text(
                            text = status,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status == "OK") colors.accent else colors.textMuted,
                            letterSpacing = 0.1.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(2.dp)
                            .height(10.dp)
                            .background(colors.line)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "What would change the path" Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "WHAT WOULD CHANGE THE PATH?",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "• Turn on Equalizer → Processing becomes ACTIVE\n" +
                                "• Connect Bluetooth → Audio is re-encoded by codec (AAC / LDAC / SBC)\n" +
                                "• Connect USB DAC → Direct bitstream output (if supported)",
                        fontSize = 12.sp,
                        color = colors.textSub,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
