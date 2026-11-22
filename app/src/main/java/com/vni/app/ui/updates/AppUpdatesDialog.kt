package com.vni.app.ui.updates

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vni.app.data.model.AppVersionItem
import kotlinx.coroutines.launch
import java.io.File

/**
 * Updates Screen & Dialog matching the user's uploaded image:
 * - Top tab header with 🔄 UPDATES
 * - Top Refresh button to query Firebase for real latest releases (v1, v2, v3 cycle)
 * - Displays real release dates and accurately indicates the current installed version
 * - 3 latest app version rows (e.g. Vni V1, Vni V2, Vni V3)
 * - Last update date & app size in MB
 * - Download button [ ⬇️ ] or [ No Uploads ]
 * - Syncs with Firebase Realtime Database
 * - Prompts to install in-place and restart to apply new features seamlessly
 */
@Composable
fun AppUpdatesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var versionList by remember { mutableStateOf(AppUpdateManager.defaultVersions) }
    var downloadingVersionKey by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }

    val currentVersionCode = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            2
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Dialog prompt after download completes
    var completedApkFile by remember { mutableStateOf<File?>(null) }
    var completedVersionName by remember { mutableStateOf("") }
    var showInstallPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppUpdateManager.syncWithFirebase(forceRefresh = false) { loaded ->
            versionList = loaded
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Neon Card Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF0F0724))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFA855F7), Color(0xFF6B21A8), Color(0xFF381566))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── Header Row with Curved Tab & Top Refresh Button matching Requirement 1 ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Curved/Shaped Tab [ 🔄 UPDATES ]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFF4C1D95))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = Color(0xFFC084FC).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Updates",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "UPDATES",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Right actions: [ 🔄 Refresh ] + [ ✕ Close ]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // [ 🔄 Refresh ] Button inside top of update section (Requirements 1 & 3)
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(Color(0xFF1E1045))
                                    .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(17.dp))
                                    .clickable(enabled = !isRefreshing) {
                                        isRefreshing = true
                                        Toast.makeText(context, "Searching Firebase for latest app versions...", Toast.LENGTH_SHORT).show()
                                        AppUpdateManager.syncWithFirebase(forceRefresh = true) { loaded ->
                                            versionList = loaded
                                            isRefreshing = false
                                            Toast.makeText(context, "Latest updates fetched from Firebase!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Updates",
                                        tint = if (isRefreshing) Color(0xFFE879F9) else Color(0xFFCBD5E1),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer(rotationZ = if (isRefreshing) spinAngle else 0f)
                                    )
                                    Text(
                                        text = if (isRefreshing) "Checking..." else "Refresh",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Close Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1045))
                                    .border(1.dp, Color(0xFF7C3AED), CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Version Rows (Latest 3 App Versions) ──
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        versionList.take(3).forEach { version ->
                            VersionUpdateRow(
                                version = version,
                                currentVersionCode = currentVersionCode,
                                isDownloading = (downloadingVersionKey == version.versionKey),
                                downloadProgress = downloadProgress,
                                onDownloadClick = {
                                    if (downloadingVersionKey == null) {
                                        downloadingVersionKey = version.versionKey
                                        downloadProgress = 0.05f
                                        coroutineScope.launch {
                                            try {
                                                val file = AppUpdateManager.downloadUpdate(
                                                    context = context,
                                                    version = version,
                                                    onProgress = { p -> downloadProgress = p }
                                                )
                                                downloadingVersionKey = null
                                                completedApkFile = file
                                                completedVersionName = version.versionName
                                                showInstallPromptDialog = true
                                            } catch (e: Exception) {
                                                downloadingVersionKey = null
                                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Bottom info text
                    Text(
                        text = "VNI in-app updates install seamlessly over existing app without deleting any data.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ── Dialog: Notify to Close and Reopen App (Requirement 3) ──
    if (showInstallPromptDialog && completedApkFile != null) {
        Dialog(onDismissRequest = { showInstallPromptDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.4.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF220E49))
                            .border(1.5.dp, Color(0xFFA855F7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "UPDATE DOWNLOADED",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "$completedVersionName has been downloaded successfully.\n\nPlease confirm the system update prompt, then close and reopen VNI so that the new features are applied!",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Later Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                                .clickable { showInstallPromptDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Later", color = Color(0xFF94A3B8), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Install & Restart Button
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                    )
                                )
                                .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(12.dp))
                                .clickable {
                                    showInstallPromptDialog = false
                                    AppUpdateManager.promptInstallApk(context, completedApkFile!!)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Install & Restart", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Version Row matching Image:
 * - Real release date displayed (e.g. 17/11/26 for V2)
 * - Highlights the CURRENT installed version with [ CURRENT ] badge
 * - Shows [ NEW ] for newer versions
 * - Shows [ | Vni V1    - last upd : 22/07/26 • 4.4 MB     [ ⬇️ ] ]
 * - Shows [ | Vni V3                                      [ No Uploads ] ]
 */
@Composable
private fun VersionUpdateRow(
    version: AppVersionItem,
    currentVersionCode: Int,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownloadClick: () -> Unit
) {
    val isInstalledCurrent = (version.versionCode == currentVersionCode)
    val isNewer = (version.versionCode > currentVersionCode)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF150B2E))
            .border(
                1.2.dp,
                if (isInstalledCurrent) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF4C1D95).copy(alpha = 0.8f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Vertical Indicator Bar & Version Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Vertical Purple/Green Bar Pill matching image
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isInstalledCurrent) Color(0xFF10B981) else Color(0xFFA855F7))
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = version.versionName,
                            color = Color.White,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )

                        if (isInstalledCurrent) {
                            // [ CURRENT ] Badge (Requirement 2)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.22f))
                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CURRENT",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else if (isNewer) {
                            // [ NEW ] Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    color = Color(0xFFE879F9),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (version.lastUpdatedDate.isNotBlank()) {
                        val subtitle = buildString {
                            append("- last upd : ")
                            append(version.lastUpdatedDate)
                            if (version.sizeText.isNotBlank()) {
                                append(" • ")
                                append(version.sizeText)
                            }
                        }
                        Text(
                            text = subtitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right: Download Button [ ⬇️ ] or [ No Uploads ]
            if (version.hasUpload) {
                Box(
                    modifier = Modifier
                        .size(width = if (isInstalledCurrent) 96.dp else 78.dp, height = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isInstalledCurrent) listOf(Color(0xFF047857), Color(0xFF065F46))
                                else listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                            )
                        )
                        .border(
                            1.2.dp,
                            if (isInstalledCurrent) Color(0xFF34D399) else Color(0xFFC084FC),
                            RoundedCornerShape(14.dp)
                        )
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFFA855F7))
                        .clickable(enabled = !isDownloading) { onDownloadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Update",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            if (isInstalledCurrent) {
                                Text(
                                    text = "Current",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // "No Uploads" Pill matching image
                Box(
                    modifier = Modifier
                        .size(width = 150.dp, height = 42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF381566), Color(0xFF230D42))
                            )
                        )
                        .border(1.dp, Color(0xFF6B21A8).copy(alpha = 0.6f), RoundedCornerShape(21.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Uploads",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
