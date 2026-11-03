package com.mbox.stream.ui.faceplate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.ui.addons.AddOnViewModel
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceplateAddOnsScreen(
    viewModel: AddOnViewModel,
    selectedAccentColor: Color,
    onAccentColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FaceplateThemeColors
    val isYouTubeEnabled by viewModel.isYouTubeEnabled.collectAsState()
    var showWarningDialog by remember { mutableStateOf(false) }

    val accentOptions = listOf(
        Color(0xFFFFB000) to "GOLD",
        Color(0xFF7FD3A8) to "MINT",
        Color(0xFFF2EDE3) to "CREAM"
    )

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
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
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

        // Section 2: LOOK & ACCENT
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
            modifier = Modifier.fillMaxWidth()
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
    }
}
