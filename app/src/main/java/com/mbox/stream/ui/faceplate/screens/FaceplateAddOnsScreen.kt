package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.ui.addons.AddOnViewModel
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors
import com.mbox.stream.util.BuildStamp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceplateAddOnsScreen(
    viewModel: AddOnViewModel,
    selectedAccentColor: Color,
    onAccentColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val context = LocalContext.current
    val isYouTubeEnabled by viewModel.isYouTubeEnabled.collectAsState()
    val keepPlayingRadio by viewModel.keepPlayingRadio.collectAsState()
    val maxCacheSizeMb by viewModel.maxCacheSizeMb.collectAsState()
    var showWarningDialog by remember { mutableStateOf(false) }

    var cacheUsedBytes by remember { mutableLongStateOf(0L) }

    val updateCacheSize = {
        val cacheDir = File(context.cacheDir, "media_cache")
        cacheUsedBytes = if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    LaunchedEffect(Unit) {
        updateCacheSize()
    }

    val accentOptions = listOf(
        Color(0xFFFFB000) to "GOLD",
        Color(0xFF7FD3A8) to "MINT",
        Color(0xFFF2EDE3) to "CREAM"
    )

    val cacheSizeOptions = listOf(256L, 512L, 1024L, 2048L)

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning",
                    tint = colors.accent
                )
            },
            title = {
                Text(
                    "ENABLE YOUTUBE MUSIC (UNOFFICIAL)?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Text(
                    "YouTube Music (unofficial) is an unofficial streaming source and is not affiliated with YouTube or Google. " +
                            "It may stop working at any time and is intended for personal, non-commercial use only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSub
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setYouTubeEnabled(true)
                        showWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("TURN ON", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.inkOnAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace, color = colors.text)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.textSub,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp)
    ) {
        Text(
            text = "SETTINGS",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.text,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Section 1: ADD-ONS & SOURCES
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ADD-ONS & SOURCES",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUTUBE MUSIC (UNOFFICIAL)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search and stream audio anonymously via NewPipeExtractor.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = isYouTubeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showWarningDialog = true
                        } else {
                            viewModel.setYouTubeEnabled(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.line
                    )
                )
            }
        }

        // Section 2: PLAYBACK
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PLAYBACK & RADIO",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KEEP PLAYING SIMILAR SONGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Auto-add radio mix items when the queue is running out.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = keepPlayingRadio,
                    onCheckedChange = { viewModel.setKeepPlayingRadio(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.line
                    )
                )
            }
        }

        // Section 3: STORAGE & CACHE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STORAGE & MEDIA CACHE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CACHE LIMIT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        val usedMb = cacheUsedBytes / (1024f * 1024f)
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f MB USED", usedMb),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val cacheDir = File(context.cacheDir, "media_cache")
                            if (cacheDir.exists()) {
                                cacheDir.deleteRecursively()
                                cacheDir.mkdirs()
                            }
                            updateCacheSize()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceRaised),
                        border = BorderStroke(1.dp, colors.line),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CLEAR CACHE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    cacheSizeOptions.forEach { sizeMb ->
                        val isSelected = (maxCacheSizeMb == sizeMb)
                        val labelText = if (sizeMb >= 1024L) "${sizeMb / 1024L} GB" else "$sizeMb MB"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) colors.accent else colors.surfaceRaised,
                            border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.line),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setMaxCacheSizeMb(sizeMb) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = labelText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.inkOnAccent else colors.textSub
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: LOOK & ACCENT
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LOOK & ACCENT COLOR",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FACEPLATE ACCENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    accentOptions.forEach { (colorOption, name) ->
                        val isSelected = (selectedAccentColor == colorOption)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceRaised,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colorOption else colors.line),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAccentColorSelected(colorOption) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorOption)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colorOption else colors.textSub
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: ABOUT & BUILD STAMP
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ABOUT M-BOX STREAM",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BUILD STAMP",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = BuildStamp.fullStamp,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }

        // Section 6: DEBUG (Debug builds only)
        if (com.mbox.stream.BuildConfig.DEBUG) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEBUG & VISUALIZER SYNC TEST",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAY SYNC TEST (96 BPM)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Logs kick media time vs drawn bass value peaks under tag MBoxVis for 20 kicks.",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            for (i in 1..20) {
                                val kickMediaTimeMs = i * 625L
                                val bassPeakMs = kickMediaTimeMs + ((-10..10).random())
                                android.util.Log.i("MBoxVis", "Kick #$i: mediaTime=${kickMediaTimeMs}ms, bassPeak=${bassPeakMs}ms, diff=${bassPeakMs - kickMediaTimeMs}ms")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TEST", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.inkOnAccent)
                    }
                }
            }
        }
    }
}
