package com.mbox.stream.ui.faceplate.screens

import android.content.Context
import android.media.MediaRouter2
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ConnectedTv
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbox.stream.ui.faceplate.theme.FaceplateThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceplatePlayOnSheet(
    activeOutputDevice: String,
    onDismiss: () -> Unit
) {
    val colors = FaceplateThemeColors
    val context = LocalContext.current

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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "PLAY ON",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 1: THIS PHONE
            Text(
                text = "THIS DEVICE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = activeOutputDevice.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    }
                    Icon(Icons.Rounded.Check, contentDescription = "Active", tint = colors.accent)
                }
            }

            // Section 2: M-BOX CAST
            Text(
                text = "M-BOX CAST (TVs)",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ConnectedTv, contentDescription = null, tint = colors.textMuted)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Open m-Box on a TV on the same Wi-Fi and it appears here",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            // Section 3: OTHER DEVICES (BLUETOOTH / SYSTEM)
            Text(
                text = "OTHER DEVICES",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            if (Build.VERSION.SDK_INT >= 34) {
                                val router = context.getSystemService(MediaRouter2::class.java)
                                router?.showSystemOutputSwitcher()
                            }
                        } catch (_: Exception) {
                            // Fallback
                        }
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CONNECT BLUETOOTH / OUTPUT SWITCHER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                }
            }
        }
    }
}
