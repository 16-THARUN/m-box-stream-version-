package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.faceplate.components.SegmentedControl
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateLibraryScreen(
    playerController: StreamPlayerController,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val queueItems by playerController.queueManager.items.collectAsState()

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
                text = "LIBRARY",
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

        // Segmented Control
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

        if (selectedFilterPill == "On this device") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Downloads arrive in a later update.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        } else {
            // Spines Section
            Text(
                text = "YOUR CRATE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val recentTracks = queueItems.map { it.sourceTrack }.distinctBy { it.id }

            if (recentTracks.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    GlideImage(
                                        model = track.artworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surfaceRaised,
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
                                        text = track.artist.uppercase(),
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.accent,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = colors.inkOnAccent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your library is empty",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
