package com.mbox.stream

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.ui.addons.AddOnViewModel
import com.mbox.stream.ui.faceplate.components.FaceplateMiniPlayer
import com.mbox.stream.ui.faceplate.screens.FaceplateAddOnsScreen
import com.mbox.stream.ui.faceplate.screens.FaceplateNowPlayingScreen
import com.mbox.stream.ui.faceplate.screens.FaceplateSearchScreen
import com.mbox.stream.ui.faceplate.theme.FaceplateColors
import com.mbox.stream.ui.faceplate.theme.FaceplateTheme
import com.mbox.stream.ui.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: StreamPlayerController

    private val searchViewModel: SearchViewModel by viewModels()
    private val addOnViewModel: AddOnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            var selectedAccentColor by remember { mutableStateOf(Color(0xFFFFB000)) }

            FaceplateTheme(accentColor = selectedAccentColor) {
                MBoxStreamApp(
                    playerController = playerController,
                    searchViewModel = searchViewModel,
                    addOnViewModel = addOnViewModel,
                    selectedAccentColor = selectedAccentColor,
                    onAccentColorSelected = { selectedAccentColor = it }
                )
            }
        }
    }
}

@Composable
fun MBoxStreamApp(
    playerController: StreamPlayerController,
    searchViewModel: SearchViewModel,
    addOnViewModel: AddOnViewModel,
    selectedAccentColor: Color,
    onAccentColorSelected: (Color) -> Unit
) {
    val nowPlayingState by playerController.nowPlayingState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val colors = FaceplateColors(accent = selectedAccentColor)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.ground
    ) {
        if (isPlayerExpanded && nowPlayingState.currentTrack != null) {
            FaceplateNowPlayingScreen(
                nowPlayingState = nowPlayingState,
                playerController = playerController,
                onCollapse = { isPlayerExpanded = false }
            )
        } else {
            Scaffold(
                containerColor = colors.ground,
                bottomBar = {
                    Column {
                        if (nowPlayingState.currentTrack != null) {
                            FaceplateMiniPlayer(
                                nowPlayingState = nowPlayingState,
                                playerController = playerController,
                                onClick = { isPlayerExpanded = true }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.line)
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                contentColor = colors.text,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Rounded.Search, contentDescription = "SEARCH") },
                                    label = { Text("SEARCH", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.accent,
                                        selectedTextColor = colors.accent,
                                        indicatorColor = colors.accent.copy(alpha = 0.15f),
                                        unselectedIconColor = colors.textMuted,
                                        unselectedTextColor = colors.textMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Rounded.Extension, contentDescription = "SETTINGS") },
                                    label = { Text("SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.accent,
                                        selectedTextColor = colors.accent,
                                        indicatorColor = colors.accent.copy(alpha = 0.15f),
                                        unselectedIconColor = colors.textMuted,
                                        unselectedTextColor = colors.textMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        0 -> FaceplateSearchScreen(
                            viewModel = searchViewModel,
                            onNavigateToAddOns = { selectedTab = 1 }
                        )
                        1 -> FaceplateAddOnsScreen(
                            viewModel = addOnViewModel,
                            selectedAccentColor = selectedAccentColor,
                            onAccentColorSelected = onAccentColorSelected
                        )
                    }
                }
            }
        }
    }
}
