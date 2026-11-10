package com.mbox.stream.ui.faceplate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.mediaplayer.data.model.NowPlayingState
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateMiniPlayer(
    nowPlayingState: NowPlayingState,
    playerController: StreamPlayerController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = nowPlayingState.currentTrack ?: return
    val colors = FaceplateThemeColors
    val isReconnecting = nowPlayingState.currentAudioSpec.contains("RECONNECTING", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceRaised,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!track.albumArtUri.isNullOrBlank()) {
                GlideImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.textMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isReconnecting) "RECONNECTING…" else "${track.artist.uppercase()} · ${nowPlayingState.currentAudioSpec.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = colors.accent,
                    letterSpacing = 0.08.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.accent,
                modifier = Modifier
                    .size(width = 48.dp, height = 40.dp)
                    .clickable { playerController.togglePlayPause() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (nowPlayingState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (nowPlayingState.isPlaying) "Pause" else "Play",
                        tint = colors.inkOnAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
