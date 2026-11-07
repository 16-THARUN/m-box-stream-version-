package com.mbox.stream.ui.faceplate.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.mediaplayer.data.model.NowPlayingState
import com.mediaplayer.data.model.toTimeString
import com.mediaplayer.ui.components.PillSeekBar
import com.mediaplayer.ui.components.VinylStage
import com.mediaplayer.util.DeviceState
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.faceplate.components.FaceplateQueueSheet
import com.mbox.stream.ui.faceplate.components.SegmentedControl
import com.mbox.stream.ui.faceplate.components.SignalCard
import com.mbox.stream.ui.faceplate.components.StageDisplayMode
import com.mbox.stream.ui.faceplate.components.VisualizerPickerSheet
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val context = LocalContext.current

    var stageMode by rememberSaveable { mutableStateOf(StageDisplayMode.RECORD) }
    var selectedVisualizerId by rememberSaveable { mutableStateOf("circular_rings") }
    var showVisualizerPicker by remember { mutableStateOf(false) }
    var showSignalPathSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var isLiked by rememberSaveable { mutableStateOf(false) }

    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var showAudioPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            try {
                val intent = android.content.Intent(context, com.mbox.stream.service.StreamMediaSessionService::class.java).apply {
                    action = "RESTART_VISUALIZER"
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(stageMode) {
        if (stageMode == StageDisplayMode.VISUAL) {
            if (!hasRecordAudioPermission) {
                showAudioPermissionDialog = true
            }
            Log.i("MBoxVis", "VISUAL mode entered")
            Log.i("MBoxVis", "Selected style id: $selectedVisualizerId")

            while (isActive) {
                delay(1000)
                val bands = playerController.visualizerManager.fftFlow.value
                val maxVal = if (bands.isNotEmpty()) bands.maxOrNull() ?: 0f else 0f
                val meanVal = if (bands.isNotEmpty()) bands.average().toFloat() else 0f
                Log.i("MBoxVis", "updates/s=10, max band=${String.format(java.util.Locale.US, "%.3f", maxVal)}, mean band=${String.format(java.util.Locale.US, "%.3f", meanVal)}")
            }
        } else {
            Log.i("MBoxVis", "VISUAL mode left")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (stageMode == StageDisplayMode.VISUAL) {
                Log.i("MBoxVis", "VISUAL mode left (Disposed)")
            }
        }
    }

    if (showAudioPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionDialog = false },
            title = {
                Text(
                    "AUDIO ACCESS NEEDED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Text(
                    "To animate the visualizer, Android needs the 'record audio' permission to read the music's levels. m-Box never records your microphone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSub
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAudioPermissionDialog = false
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GRANT ACCESS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.inkOnAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioPermissionDialog = false }) {
                    Text("NOT NOW", fontFamily = FontFamily.Monospace, color = colors.text)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.textSub,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showSignalPathSheet) {
        FaceplateSignalPathSheet(
            nowPlayingState = nowPlayingState,
            onDismiss = { showSignalPathSheet = false }
        )
    }

    if (showVisualizerPicker) {
        VisualizerPickerSheet(
            selectedModeId = selectedVisualizerId,
            onModeSelected = {
                selectedVisualizerId = it
                Log.i("MBoxVis", "Style chosen: $it")
                playerController.setVisualizerMode(it)
            },
            onDismiss = { showVisualizerPicker = false }
        )
    }

    if (showQueueSheet) {
        FaceplateQueueSheet(
            queueManager = playerController.queueManager,
            onJumpToTrack = { idx ->
                val queueItem = playerController.queueManager.jumpTo(idx)
                if (queueItem != null) {
                    playerController.playSourceTrack(
                        queueItem.sourceTrack,
                        com.mediaplayer.data.model.ResolvedStream(
                            url = "mbox://yt/${queueItem.sourceTrack.id}",
                            codec = "Opus",
                            bitrateKbps = 160,
                            sampleRateHz = 48000,
                            container = "webm",
                            lossy = true,
                            expiresAtMs = System.currentTimeMillis() + 14400000L
                        )
                    )
                }
            },
            onDismiss = { showQueueSheet = false }
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
            onLongClickVisual = {
                Log.i("MBoxVis", "Long-press on VISUAL received")
                Log.i("MBoxVis", "Picker opened")
                showVisualizerPicker = true
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 12.dp)
                .zIndex(10f)
        )

        // Stage Area Box
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val maxStageWidth = maxWidth * 0.85f
            val stageDim = min(maxStageWidth, maxHeight)
            val density = LocalDensity.current
            val stageDimPx = with(density) { stageDim.toPx() }

            when (stageMode) {
                StageDisplayMode.RECORD, StageDisplayMode.VISUAL -> {
                    Box(contentAlignment = Alignment.Center) {
                        VinylStage(
                            activeVisualizerMode = if (stageMode == StageDisplayMode.VISUAL) selectedVisualizerId else "off",
                            fftFlow = playerController.visualizerManager.fftFlow,
                            accentColor = colors.accent,
                            deviceState = DeviceState.OPTIMAL,
                            isPlaying = nowPlayingState.isPlaying,
                            imageUri = track.albumArtUri,
                            playerState = if (stageMode == StageDisplayMode.VISUAL) {
                                nowPlayingState.copy(activeVisualizerMode = selectedVisualizerId)
                            } else {
                                nowPlayingState.copy(activeVisualizerMode = "off")
                            },
                            onSeekTo = { playerController.seekTo(it) },
                            playerVm = playerController,
                            showLyrics = false,
                            vinylDiameterPx = stageDimPx * 0.85f,
                            stageSizePx = stageDimPx,
                            onStagePositioned = { center, radius ->
                                Log.i("MBoxVis", "Frames drawn: VinylStage center=$center, radius=$radius")
                            },
                            modifier = Modifier.size(stageDim)
                        )

                        if (stageMode == StageDisplayMode.VISUAL && !hasRecordAudioPermission) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surface.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, colors.accent),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable { showAudioPermissionDialog = true }
                            ) {
                                Text(
                                    text = "ALLOW AUDIO ACCESS TO ANIMATE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    letterSpacing = 0.1.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                StageDisplayMode.COVER -> {
                    val slideOffset = (progress.coerceIn(0f, 1f) * (stageDim.value * 0.25f)).dp
                    val coverSize = stageDim * 0.75f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(stageDim)
                    ) {
                        // Record sliding out from behind cover
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF111111),
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .size(coverSize * 0.9f)
                                .offset(x = slideOffset)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.accent,
                                    modifier = Modifier.size(coverSize * 0.35f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF111111),
                                            modifier = Modifier.size(14.dp)
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
                                .size(coverSize)
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
                fontSize = if (track.title.length > 20) 22.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp,
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
                .padding(bottom = 12.dp)
        )

        // Signal Card
        SignalCard(
            nowPlayingState = nowPlayingState,
            onOpenSignalPath = { showSignalPathSheet = true },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Transport Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
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

            IconButton(onClick = { showQueueSheet = true }) {
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
            modifier = Modifier.padding(bottom = 4.dp)
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
