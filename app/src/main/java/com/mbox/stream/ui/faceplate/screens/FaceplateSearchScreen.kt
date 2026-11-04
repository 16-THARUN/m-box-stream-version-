package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.MusicNote
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
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors
import com.mbox.stream.ui.search.SearchUiState
import com.mbox.stream.ui.search.SearchViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FaceplateSearchScreen(
    viewModel: SearchViewModel,
    onNavigateToAddOns: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val isYouTubeEnabled by viewModel.isYouTubeEnabled.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val resolvingTrackId by viewModel.resolvingTrackId.collectAsState()
    val playbackError by viewModel.playerController.playbackError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    // Listen to ViewModel snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "TRY AGAIN",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLastTrack()
            }
        }
    }

    // Listen to PlayerController errors
    LaunchedEffect(playbackError) {
        playbackError?.let { errMessage ->
            val result = snackbarHostState.showSnackbar(
                message = errMessage,
                actionLabel = "TRY AGAIN",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.playerController.retryPlayback()
            }
            viewModel.playerController.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.surface,
                    contentColor = colors.text,
                    actionColor = colors.accent,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // Screen Title: SEARCH (Display 40sp)
            Text(
                text = "SEARCH",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (isYouTubeEnabled) {
                        viewModel.onQueryChanged(it)
                    }
                },
                placeholder = {
                    Text(
                        "Search YouTube Music...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.accent) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (isResolving) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(bottom = 8.dp),
                    color = colors.accent,
                    trackColor = colors.line
                )
            }

            if (!isYouTubeEnabled) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ENABLE YOUTUBE MUSIC IN ADD-ONS TO SEARCH AND STREAM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            letterSpacing = 0.1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToAddOns,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("GO TO ADD-ONS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.inkOnAccent)
                        }
                    }
                }
            } else {
                when (val state = uiState) {
                    SearchUiState.Initial -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Type a song or artist to search",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                    SearchUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    }
                    is SearchUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                state.message.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSub
                            )
                        }
                    }
                    is SearchUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.tracks) { track ->
                                FaceplateSourceTrackRow(
                                    track = track,
                                    isResolvingThisTrack = (resolvingTrackId == track.id),
                                    onClick = { viewModel.playTrack(track) },
                                    onPlayNext = { viewModel.playNext(track) },
                                    onAddToQueue = { viewModel.addToQueue(track) }
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FaceplateSourceTrackRow(
    track: SourceTrack,
    isResolvingThisTrack: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    val colors = FaceplateThemeColors
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
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

                if (isResolvingThisTrack) {
                    Spacer(modifier = Modifier.width(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.accent,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(colors.surfaceRaised)
        ) {
            DropdownMenuItem(
                text = { Text("PLAY NEXT", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text) },
                onClick = {
                    showMenu = false
                    onPlayNext()
                }
            )
            DropdownMenuItem(
                text = { Text("ADD TO QUEUE", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text) },
                onClick = {
                    showMenu = false
                    onAddToQueue()
                }
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
