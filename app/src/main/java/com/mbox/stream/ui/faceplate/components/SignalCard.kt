package com.mbox.stream.ui.faceplate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.data.model.NowPlayingState
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@Composable
fun SignalCard(
    nowPlayingState: NowPlayingState,
    onOpenSignalPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val signal = nowPlayingState.signalPath
    val isLossless = signal?.format?.contains("lossless", ignoreCase = true) == true

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Three columns: SOURCE | ENGINE | OUTPUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: SOURCE
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOURCE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "YouTube",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = signal?.format ?: "Opus · 160k",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Column 2: ENGINE
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ENGINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ExoPlayer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "EQ Off",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Column 3: OUTPUT
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OUTPUT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nowPlayingState.activeOutputDevice,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "44.1 kHz",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Honesty row + PATH › button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSignalPath() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLossless) "Lossless stream · bit-perfect" else "Lossy stream · not bit-perfect",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSub
                    )
                }

                Text(
                    text = "PATH ›",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    letterSpacing = 0.12.sp
                )
            }
        }
    }
}
