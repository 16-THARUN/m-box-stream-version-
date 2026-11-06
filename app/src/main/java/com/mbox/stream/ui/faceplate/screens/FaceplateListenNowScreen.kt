package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.mediaplayer.data.model.SourceTrack
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateListenNowScreen(
    playerController: StreamPlayerController,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val nowPlayingState by playerController.nowPlayingState.collectAsState()
    val currentTrack = nowPlayingState.currentTrack

    var selectedFilterPill by remember { mutableStateOf("All") }
    val filterPills = listOf("All", "Online", "On this device")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LISTEN NOW",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                letterSpacing = 0.5.sp
            )

            IconButton(
                onClick = onNavigateToSearch,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = colors.accent)
            }
        }

        // Filter Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            filterPills.forEach { pill ->
                val isSelected = (selectedFilterPill == pill)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterPill = pill },
                    label = {
                        Text(
                            pill.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isSelected) colors.accent else colors.surface,
                        labelColor = if (isSelected) colors.inkOnAccent else colors.textSub
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) colors.accent else colors.line
                    )
                )
            }
        }

        // Hero Tile
        if (currentTrack != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { playerController.play() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!currentTrack.albumArtUri.isNullOrBlank()) {
                        GlideImage(
                            model = currentTrack.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceRaised,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.textMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LAST PLAYED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            letterSpacing = 0.12.sp
                        )
                        Text(
                            text = currentTrack.title.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentTrack.artist.uppercase()} · ${nowPlayingState.currentAudioSpec.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.accent,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = colors.inkOnAccent)
                        }
                    }
                }
            }
        }

        // Section Title: RECENTLY PLAYED
        Text(
            text = "RECENTLY PLAYED",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            letterSpacing = 0.12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Recently Played Grid
        val queueItems by playerController.queueManager.items.collectAsState()
        val recentTracks = queueItems.map { it.sourceTrack }.distinctBy { it.id }.take(6)

        if (recentTracks.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recentTracks) { track ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerController.playSourceTrack(
                                    track,
                                    com.mediaplayer.data.model.ResolvedStream(
                                        url = "mbox://yt/${track.id}",
                                        codec = "Opus",
                                        bitrateKbps = 160,
                                        sampleRateHz = 48000,
                                        container = "webm",
                                        lossy = true,
                                        expiresAtMs = System.currentTimeMillis() + 14400000L
                                    )
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!track.artworkUrl.isNullOrBlank()) {
                                GlideImage(
                                    model = track.artworkUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surfaceRaised,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.textMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = track.title.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist.uppercase(),
                                fontSize = 10.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No songs played yet",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToSearch,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SEARCH SONGS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.inkOnAccent)
                    }
                }
            }
        }
    }
}
