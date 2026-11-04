package com.mbox.stream.ui.faceplate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
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
import com.mbox.stream.queue.QueueItem
import com.mbox.stream.queue.QueueManager
import com.mbox.stream.queue.QueueSection
import com.mbox.stream.queue.RepeatMode
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateQueueSheet(
    queueManager: QueueManager,
    onJumpToTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FaceplateThemeColors
    val items by queueManager.items.collectAsState()
    val currentIndex by queueManager.currentIndex.collectAsState()
    val repeatMode by queueManager.repeatMode.collectAsState()
    val isShuffled by queueManager.isShuffled.collectAsState()

    val currentItem = items.getOrNull(currentIndex)
    val seedTitle = currentItem?.sourceTrack?.title?.uppercase() ?: "CURRENT SONG"

    val nowPlayingItems = items.filterIndexed { index, _ -> index == currentIndex }
    val upNextItemsWithIndex = items.mapIndexed { index, item -> index to item }
        .filter { (index, item) -> index > currentIndex && item.section == QueueSection.UP_NEXT }
    val radioItemsWithIndex = items.mapIndexed { index, item -> index to item }
        .filter { (index, item) -> index > currentIndex && item.section == QueueSection.RADIO }

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
            // Header: QUEUE Title + Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUEUE",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text,
                    letterSpacing = 0.5.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { queueManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffled) colors.accent else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { queueManager.toggleRepeatMode() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) colors.accent else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Section 1: NOW PLAYING
                if (nowPlayingItems.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOW PLAYING",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                letterSpacing = 0.12.sp
                            )
                        }
                    }

                    itemsIndexed(nowPlayingItems) { _, item ->
                        QueueRow(
                            item = item,
                            isCurrent = true,
                            isDimmed = false,
                            onTap = { },
                            onRemove = null
                        )
                    }
                }

                // Section 2: UP NEXT
                if (upNextItemsWithIndex.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UP NEXT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 0.12.sp
                            )

                            TextButton(
                                onClick = { queueManager.clearUpNext() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "CLEAR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }

                    itemsIndexed(upNextItemsWithIndex) { _, (realIdx, item) ->
                        QueueRow(
                            item = item,
                            isCurrent = false,
                            isDimmed = false,
                            onTap = {
                                onJumpToTrack(realIdx)
                                onDismiss()
                            },
                            onRemove = { queueManager.removeAt(realIdx) }
                        )
                    }
                }

                // Section 3: RADIO
                if (radioItemsWithIndex.isNotEmpty()) {
                    item {
                        Text(
                            text = "RADIO — FROM $seedTitle",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = 0.12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(radioItemsWithIndex) { _, (realIdx, item) ->
                        QueueRow(
                            item = item,
                            isCurrent = false,
                            isDimmed = true,
                            onTap = {
                                onJumpToTrack(realIdx)
                                onDismiss()
                            },
                            onRemove = null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    isDimmed: Boolean,
    onTap: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val colors = FaceplateThemeColors
    val track = item.sourceTrack

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrent) colors.surfaceRaised else colors.surface.copy(alpha = if (isDimmed) 0.6f else 1f),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) colors.accent else colors.line),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
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
                    color = if (isCurrent) colors.accent else if (isDimmed) colors.textSub else colors.text,
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

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(track.durationMs),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.textSub
                )
                Text(
                    text = "YOUTUBE · OPUS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    letterSpacing = 0.08.sp
                )
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
