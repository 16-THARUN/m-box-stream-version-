package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.mediaplayer.data.model.NowPlayingState
import com.mediaplayer.data.model.toTimeString
import com.mediaplayer.ui.components.NowPlayingArtwork
import com.mediaplayer.ui.components.PillSeekBar
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.faceplate.components.SegmentedControl
import com.mbox.stream.ui.faceplate.components.SignalCard
import com.mbox.stream.ui.faceplate.components.StageDisplayMode
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateNowPlayingScreen(
    nowPlayingState: NowPlayingState,
    playerController: StreamPlayerController,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = nowPlayingState.currentTrack ?: return
    val colors = FaceplateThemeColors

    var stageMode by rememberSaveable { mutableStateOf(StageDisplayMode.RECORD) }
    var showSignalPathSheet by remember { mutableStateOf(false) }
    var isLiked by rememberSaveable { mutableStateOf(false) }

    if (showSignalPathSheet) {
        FaceplateSignalPathSheet(
            nowPlayingState = nowPlayingState,
            onDismiss = { showSignalPathSheet = false }
        )
    }

    val progress = if (nowPlayingState.durationMs > 0)
        nowPlayingState.positionMs.toFloat() / nowPlayingState.durationMs.toFloat()
    else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = colors.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.12.sp
                )
                Text(
                    text = "YOUTUBE MUSIC",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    letterSpacing = 0.12.sp
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = "Play on",
                    tint = colors.textSub,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Segmented Control over Stage
        SegmentedControl(
            selectedMode = stageMode,
            onModeSelected = { stageMode = it },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 16.dp)
        )

        // Stage Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (stageMode) {
                StageDisplayMode.RECORD, StageDisplayMode.VISUAL -> {
                    NowPlayingArtwork(
                        imageUri = track.albumArtUri,
                        isPlaying = nowPlayingState.isPlaying,
                        playerState = nowPlayingState,
                        onSeekTo = { playerController.seekTo(it) },
                        playerVm = playerController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                StageDisplayMode.COVER -> {
                    val slideOffset = (progress.coerceIn(0f, 1f) * 60).dp

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(280.dp)
                    ) {
                        // Record sliding out from behind cover
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF111111),
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .size(220.dp)
                                .offset(x = slideOffset)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.accent,
                                    modifier = Modifier.size(70.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF111111),
                                            modifier = Modifier.size(16.dp)
                                        ) {}
                                    }
                                }
                            }
                        }

                        // Cover Art Box
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .size(240.dp)
                                .shadow(elevation = 12.dp, shape = RoundedCornerShape(6.dp))
                        ) {
                            if (!track.albumArtUri.isNullOrBlank()) {
                                GlideImage(
                                    model = track.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(64.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title and Time in Mono
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.title.uppercase(),
                fontSize = if (track.title.length > 20) 24.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nowPlayingState.positionMs.toTimeString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    text = " / ${nowPlayingState.durationMs.toTimeString()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = colors.textSub
                )
            }
        }

        // Artist on its own line
        Text(
            text = track.artist,
            fontSize = 14.sp,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 12.dp)
        )

        // Seek Bar
        PillSeekBar(
            currentPositionMs = nowPlayingState.positionMs,
            totalDurationMs = nowPlayingState.durationMs,
            accentColor = colors.accent,
            onSeek = { playerController.seekTo(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        // Signal Card
        SignalCard(
            nowPlayingState = nowPlayingState,
            onOpenSignalPath = { showSignalPathSheet = true },
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Transport Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { isLiked = !isLiked }) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) colors.accent else colors.textSub,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { playerController.previous() }) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = colors.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Accent Play/Pause Block 72 x 64
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.accent,
                modifier = Modifier
                    .size(width = 72.dp, height = 64.dp)
                    .clickable { playerController.togglePlayPause() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (nowPlayingState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (nowPlayingState.isPlaying) "Pause" else "Play",
                        tint = colors.inkOnAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(onClick = { playerController.next() }) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = colors.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = colors.textSub,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Chips Row: LYRICS | EQ | SLEEP
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { },
                label = {
                    Text(
                        "LYRICS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surface,
                    labelColor = colors.textSub
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = colors.line
                )
            )

            FilterChip(
                selected = false,
                enabled = false,
                onClick = { },
                label = {
                    Text(
                        "EQ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    disabledContainerColor = colors.surface.copy(alpha = 0.5f),
                    disabledLabelColor = colors.textMuted
                )
            )

            FilterChip(
                selected = false,
                enabled = false,
                onClick = { },
                label = {
                    Text(
                        "SLEEP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    disabledContainerColor = colors.surface.copy(alpha = 0.5f),
                    disabledLabelColor = colors.textMuted
                )
            )
        }
    }
}
