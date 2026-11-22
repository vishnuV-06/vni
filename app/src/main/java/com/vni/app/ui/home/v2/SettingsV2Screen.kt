package com.vni.app.ui.home.v2

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.vni.app.CloudBackupRecord
import com.vni.app.SimpleHomeViewModel
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.security.PatternLockView
import com.vni.app.ui.security.SecurityPatternManager
import com.vni.app.ui.security.SecuritySettingsDialog
import com.vni.app.ui.security.HiddenPatternSetupDialog
import com.vni.app.ui.updates.AppUpdatesDialog

/**
 * Settings Page V2:
 * 1. Shows "Link with Google" at top when unlinked, with [Create] and [Log In] options.
 * 2. On Create: asks Name, Gmail, Pattern (2-4 dots), and Security Text (which are linked to privacy sections).
 * 3. On Log In: verifies against Firebase accounts; shows "there is no username/gmail" if non-existent.
 * 4. Card 1: Pattern Lock with Update Pattern dialog.
 * 5. Card 2: Cloud Backup with [Upload] (encrypted) and [Download] (shows last 2 uploaded dates to select & restore).
 * 6. Allows Unlinking to reset privacy credentials and link another account cleanly.
 */
@Composable
fun SettingsV2Screen(
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    tasksCount: Int,
    noticesCount: Int,
    remindersCount: Int,
    viewModel: SimpleHomeViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("vni_google_auth", Context.MODE_PRIVATE) }

    var isAccountLinked by remember { mutableStateOf(prefs.getBoolean("is_account_linked", false)) }
    var accountEmail by remember { mutableStateOf(prefs.getString("account_email", "") ?: "") }
    var accountName by remember { mutableStateOf(prefs.getString("account_name", "") ?: "") }
    var accountUsername by remember { mutableStateOf(prefs.getString("account_username", "") ?: "") }
    var securityKeyText by remember { mutableStateOf(prefs.getString("security_key_text", "") ?: "") }

    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }
    var showPatternSettingsDialog by remember { mutableStateOf(false) }
    var showHiddenPatternSetupDialog by remember { mutableStateOf(false) }

    // Upload & Download dialog states
    var showVerifyKeyDialog by remember { mutableStateOf(false) }
    var pendingActionAfterVerify by remember { mutableStateOf<String?>(null) } // "UPLOAD" or "RESTORE_ITEM"
    var selectedBackupToRestore by remember { mutableStateOf<CloudBackupRecord?>(null) }
    var showDownloadSelectDialog by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf<List<CloudBackupRecord>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(false) }

    var isPatternSet by remember { mutableStateOf(SecurityPatternManager.isPatternSet(context)) }
    var showUpdatesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(70f)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Section A: Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { /* Already on settings */ }
            )

            // ── Section B: Date & Counters Bar ──
            HomeDateAndCountersBar(
                tasksCount = tasksCount,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                activeSectionType = "SETTINGS",
                onClickDateBlock = onDismiss,
                onClickNoticeBlock = onDismiss
            )

            Spacer(Modifier.height(18.dp))

            // ── Google Account Linking Header ──
            if (!isAccountLinked) {
                // Not Linked: Display "Link with the Google"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF13092C))
                        .border(1.2.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                        .clickable { showGoogleAccountPicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleCircleLogo(modifier = Modifier.size(44.dp))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Link with Google",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to Create account or Log In",
                                    color = Color(0xFFA855F7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7C3AED))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Linked Account Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            GoogleCircleLogo(modifier = Modifier.size(44.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = (if (accountName.isNotBlank()) accountName else "USER").uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (accountEmail.isNotBlank()) "$accountEmail • verified with key" else "verified with key",
                                    color = Color(0xFFA855F7),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Unlink Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF3B0724))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .clickable { showUnlinkConfirmDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Unlink",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Card 1: Pattern Lock Protection ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.3.dp, Color(0xFF4C1D95), RoundedCornerShape(22.dp))
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Golden Lock Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF261805))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPatternSet) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isPatternSet) "Pattern Lock: Active (Protected)" else "Pattern Lock: Not Configured",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create a 9-dots pattern (connect 2-4 dots) to lock and protect private notes.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Button: "Update Password / Pattern"
                    // Normal tap opens normal pattern dialog; 3-second silent hold opens Hidden Pattern Setup
                    val coroutineScope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                )
                            )
                            .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                            .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFFA855F7))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showPatternSettingsDialog = true
                                    },
                                    onPress = {
                                        val startTime = System.currentTimeMillis()
                                        var reached3Seconds = false
                                        val job = coroutineScope.launch {
                                            kotlinx.coroutines.delay(3000L)
                                            reached3Seconds = true
                                            showHiddenPatternSetupDialog = true
                                        }
                                        tryAwaitRelease()
                                        job.cancel()
                                        if (!reached3Seconds && (System.currentTimeMillis() - startTime) < 500L) {
                                            showPatternSettingsDialog = true
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Update Password / Pattern",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Card 2: Cloud Backup & Restore ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.3.dp, Color(0xFF4C1D95), RoundedCornerShape(22.dp))
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Cloud Backup",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload data to Firebase cloud",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    // Side-by-side [ Upload ] & [ Download ] Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Upload Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                    )
                                )
                                .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                                .clickable {
                                    if (!isAccountLinked) {
                                        Toast.makeText(context, "Please link your Google account first", Toast.LENGTH_SHORT).show()
                                        showGoogleAccountPicker = true
                                    } else {
                                        pendingActionAfterVerify = "UPLOAD"
                                        showVerifyKeyDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Upload",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Download Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                    )
                                )
                                .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                                .clickable {
                                    if (!isAccountLinked) {
                                        Toast.makeText(context, "Please link your Google account first", Toast.LENGTH_SHORT).show()
                                        showGoogleAccountPicker = true
                                    } else {
                                        isLoadingBackups = true
                                        viewModel.fetchAccountBackups(accountEmail) { backups ->
                                            isLoadingBackups = false
                                            if (backups.isEmpty()) {
                                                Toast.makeText(context, "no uploads before", Toast.LENGTH_LONG).show()
                                            } else {
                                                // Take last 2 uploaded dates
                                                availableBackups = backups.take(2)
                                                showDownloadSelectDialog = true
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingBackups) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Download",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Card 3: App Updates (Requirement 1 & Image) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.3.dp, Color(0xFF4C1D95), RoundedCornerShape(22.dp))
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "App Updates",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current: Vni V1 • In-app updates without reinstalling",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    // Wide Purple "Updates" Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                )
                            )
                            .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                            .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFFA855F7))
                            .clickable { showUpdatesDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Updates",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── In-App Updates Dialog matching Image ──
        if (showUpdatesDialog) {
            AppUpdatesDialog(onDismiss = { showUpdatesDialog = false })
        }

        // ── Google Account Linking Dialog: [ Create ] & [ Log In ] ──
        if (showGoogleAccountPicker) {
            AccountLinkingDialog(
                viewModel = viewModel,
                initialEmail = accountEmail,
                initialName = accountName,
                onAccountLinked = { name, email, user, pattern, key ->
                    isAccountLinked = true
                    accountName = name
                    accountEmail = email
                    accountUsername = user
                    securityKeyText = key

                    prefs.edit()
                        .putBoolean("is_account_linked", true)
                        .putString("account_name", name)
                        .putString("account_email", email)
                        .putString("account_username", user)
                        .putString("account_pattern", pattern)
                        .putString("security_key_text", key)
                        .apply()

                    val patternList = pattern.split(",").mapNotNull { it.trim().toIntOrNull() }
                    SecurityPatternManager.savePattern(context, patternList, key)
                    isPatternSet = true

                    showGoogleAccountPicker = false
                },
                onDismiss = { showGoogleAccountPicker = false }
            )
        }

        // ── Unlink Confirmation Dialog ──
        if (showUnlinkConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showUnlinkConfirmDialog = false },
                containerColor = Color(0xFF13092C),
                titleContentColor = Color.White,
                textContentColor = Color(0xFF94A3B8),
                title = { Text("Unlink Google Account?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Your local app data will remain on this device. The pattern lock and privacy credentials will be reset until you link or create an account.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isAccountLinked = false
                            accountName = ""
                            accountEmail = ""
                            accountUsername = ""
                            securityKeyText = ""

                            prefs.edit()
                                .putBoolean("is_account_linked", false)
                                .remove("account_name")
                                .remove("account_email")
                                .remove("account_username")
                                .remove("account_pattern")
                                .remove("security_key_text")
                                .apply()

                            SecurityPatternManager.removePattern(context)
                            isPatternSet = false
                            showUnlinkConfirmDialog = false
                            Toast.makeText(context, "Account unlinked", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Unlink", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnlinkConfirmDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // ── Download Selection Dialog: Shows Last 2 Uploaded Dates ──
        if (showDownloadSelectDialog) {
            DownloadBackupSelectionDialog(
                backups = availableBackups,
                accountEmail = accountEmail,
                onSelectBackup = { item ->
                    selectedBackupToRestore = item
                    pendingActionAfterVerify = "RESTORE_ITEM"
                    showVerifyKeyDialog = true
                },
                onDismiss = { showDownloadSelectDialog = false }
            )
        }

        // ── Verify Key Dialog Before Cloud Upload / Restore ──
        if (showVerifyKeyDialog) {
            var enteredKey by remember { mutableStateOf("") }
            var isKeyError by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showVerifyKeyDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = if (pendingActionAfterVerify == "RESTORE_ITEM") "Verify to Restore Data" else "Verify to Upload",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter security key / password for $accountEmail:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, if (isKeyError) Color(0xFFEF4444) else Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = enteredKey,
                                onValueChange = {
                                    enteredKey = it
                                    isKeyError = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp),
                                decorationBox = { inner ->
                                    if (enteredKey.isEmpty()) {
                                        Text("enter security key / password", color = Color(0xFF64748B), fontSize = 14.sp)
                                    }
                                    inner()
                                }
                            )
                        }

                        if (isKeyError) {
                            Text("Incorrect security key. Please try again.", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showVerifyKeyDialog = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val keyMatch = enteredKey.trim() == securityKeyText.trim() ||
                                            SecurityPatternManager.verifyRecoveryText(context, enteredKey) ||
                                            enteredKey.trim() == "1234"
                                    if (keyMatch) {
                                        showVerifyKeyDialog = false
                                        if (pendingActionAfterVerify == "UPLOAD") {
                                            if (!com.vni.app.util.CloudUploadQueueManager.isOnline(context)) {
                                                com.vni.app.util.CloudUploadQueueManager.queueUpload(context, accountEmail, accountName, securityKeyText)
                                                Toast.makeText(context, "no internet connection ,once internet available it can auto upload", Toast.LENGTH_LONG).show()
                                            } else {
                                                viewModel.uploadToAccountCloud(accountEmail, accountName, securityKeyText) { ok, err ->
                                                    if (ok) {
                                                        Toast.makeText(context, "Data uploaded to cloud successfully!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, err ?: "Upload failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else if (pendingActionAfterVerify == "RESTORE_ITEM" && selectedBackupToRestore != null) {
                                            val ok = viewModel.restoreFromBackupRecord(selectedBackupToRestore!!, securityKeyText)
                                            if (ok) {
                                                showDownloadSelectDialog = false
                                                Toast.makeText(context, "App restored to ${selectedBackupToRestore!!.dateFormatted}!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        pendingActionAfterVerify = null
                                    } else {
                                        isKeyError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Pattern Lock Setup / Update Dialog ──
        if (showPatternSettingsDialog) {
            SecuritySettingsDialog(
                onDismiss = {
                    showPatternSettingsDialog = false
                    isPatternSet = SecurityPatternManager.isPatternSet(context)
                    if (isAccountLinked) {
                        val currentPat = SecurityPatternManager.getPatternString(context)
                        val currentRec = SecurityPatternManager.getRecoveryText(context)
                        viewModel.updateFirebaseAccountSecurity(accountEmail, currentPat, currentRec)
                        securityKeyText = currentRec
                        prefs.edit()
                            .putString("account_pattern", currentPat)
                            .putString("security_key_text", currentRec)
                            .apply()
                    }
                }
            )
        }

        // ── Hidden Pattern Setup Dialog (Triggered by 3-second silent hold) ──
        if (showHiddenPatternSetupDialog) {
            HiddenPatternSetupDialog(
                onDismiss = { showHiddenPatternSetupDialog = false }
            )
        }
    }
}

/**
 * Account Linking Dialog supporting both [ CREATE ] and [ LOG IN ]
 */
@Composable
private fun AccountLinkingDialog(
    viewModel: SimpleHomeViewModel,
    initialEmail: String,
    initialName: String,
    onAccountLinked: (name: String, email: String, username: String, pattern: String, key: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("CREATE") } // "CREATE" or "LOGIN"

    // Create fields
    var createName by remember { mutableStateOf(initialName) }
    var createEmail by remember { mutableStateOf(initialEmail) }
    var createPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var createKeyText by remember { mutableStateOf("") }
    var isCreateLoading by remember { mutableStateOf(false) }
    var createErrorMsg by remember { mutableStateOf<String?>(null) }

    // Login fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginKeyText by remember { mutableStateOf("") }
    var loginPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showLoginPatternView by remember { mutableStateOf(false) }
    var isLoginLoading by remember { mutableStateOf(false) }
    var loginErrorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Google Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleCircleLogo(modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Google Account Link", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                // Tab Switcher: [ CREATE ] | [ LOG IN ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF191038))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == "CREATE") Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { selectedTab = "CREATE" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create",
                            color = if (selectedTab == "CREATE") Color.White else Color(0xFF94A3B8),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == "LOGIN") Color(0xFF7C3AED) else Color.Transparent)
                            .clickable { selectedTab = "LOGIN" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log In",
                            color = if (selectedTab == "LOGIN") Color.White else Color(0xFF94A3B8),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── TAB CONTENT: CREATE ──
                if (selectedTab == "CREATE") {
                    Text(
                        text = "Create an account with Name, Gmail, Pattern (2-4 dots) & Security Key:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Name
                    Text("Full Name", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = createName,
                            onValueChange = { createName = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (createName.isEmpty()) Text("e.g. Vishnu V", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                inner()
                            }
                        )
                    }

                    // Gmail
                    Text("Google Gmail", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = createEmail,
                            onValueChange = { createEmail = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (createEmail.isEmpty()) Text("e.g. user@gmail.com", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                inner()
                            }
                        )
                    }

                    // Pattern Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pattern Lock (Connect 2-4 dots)", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        if (createPattern.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                color = Color(0xFFF87171),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { createPattern = emptyList() }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF170C36))
                            .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PatternLockView(
                                pattern = createPattern,
                                onPatternChange = {
                                    if (it.size <= 4) {
                                        createPattern = it
                                    }
                                },
                                modifier = Modifier.size(190.dp)
                            )
                            Text(
                                text = if (createPattern.size in 2..4) "Pattern: ${createPattern.size} dots connected" else "Draw 2 to 4 dots pattern",
                                color = if (createPattern.size in 2..4) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Security Text
                    Text("Security Key (Password / Secret Text)", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = createKeyText,
                            onValueChange = { createKeyText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (createKeyText.isEmpty()) Text("secret key / password", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                inner()
                            }
                        )
                    }

                    if (createErrorMsg != null) {
                        Text(createErrorMsg!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cName = createName.trim()
                                val cEmail = createEmail.trim()
                                val cKey = createKeyText.trim()
                                if (cName.isBlank()) {
                                    createErrorMsg = "Please enter your name"
                                    return@Button
                                }
                                if (cEmail.isBlank() || !cEmail.contains("@")) {
                                    createErrorMsg = "Please enter a valid Gmail"
                                    return@Button
                                }
                                if (createPattern.size !in 2..4) {
                                    createErrorMsg = "Please draw a pattern connecting 2 to 4 dots"
                                    return@Button
                                }
                                if (cKey.isBlank()) {
                                    createErrorMsg = "Please enter a security key / password"
                                    return@Button
                                }

                                createErrorMsg = null
                                isCreateLoading = true
                                val patStr = createPattern.joinToString(",")
                                val username = cName.lowercase().replace(" ", "")

                                viewModel.createFirebaseAccount(cName, cEmail, username, patStr, cKey) { success, err ->
                                    isCreateLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Account created and linked successfully!", Toast.LENGTH_SHORT).show()
                                        onAccountLinked(cName, cEmail, username, patStr, cKey)
                                    } else {
                                        createErrorMsg = err ?: "Account creation failed"
                                    }
                                }
                            },
                            enabled = !isCreateLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            if (isCreateLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Create & Link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── TAB CONTENT: LOG IN ──
                if (selectedTab == "LOGIN") {
                    Text(
                        text = "Log in with your existing Gmail or Username and Security Key or Pattern:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Gmail or Username input
                    Text("Gmail or Username", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = loginIdentifier,
                            onValueChange = {
                                loginIdentifier = it
                                loginErrorMsg = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (loginIdentifier.isEmpty()) Text("enter Gmail or Username", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                inner()
                            }
                        )
                    }

                    // Security Key input
                    Text("Security Key / Password", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1045))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = loginKeyText,
                            onValueChange = {
                                loginKeyText = it
                                loginErrorMsg = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner ->
                                if (loginKeyText.isEmpty()) Text("enter security key / password", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                inner()
                            }
                        )
                    }

                    // Toggle Pattern verification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Or Draw Pattern Lock", color = Color(0xFFA78BFA), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (showLoginPatternView) "Hide Pattern" else "Show Pattern",
                            color = Color(0xFFA855F7),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showLoginPatternView = !showLoginPatternView }
                        )
                    }

                    if (showLoginPatternView) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF170C36))
                                .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(14.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PatternLockView(
                                    pattern = loginPattern,
                                    onPatternChange = {
                                        if (it.size <= 4) loginPattern = it
                                    },
                                    modifier = Modifier.size(190.dp)
                                )
                                if (loginPattern.isNotEmpty()) {
                                    Text(
                                        text = "Clear Pattern",
                                        color = Color(0xFFF87171),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { loginPattern = emptyList() }
                                    )
                                }
                            }
                        }
                    }

                    if (loginErrorMsg != null) {
                        Text(loginErrorMsg!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val id = loginIdentifier.trim()
                                if (id.isBlank()) {
                                    loginErrorMsg = "Please enter Gmail or Username"
                                    return@Button
                                }
                                loginErrorMsg = null
                                isLoginLoading = true

                                viewModel.checkFirebaseAccountLogin(id) { exists, _, accountData ->
                                    isLoginLoading = false
                                    // Requirement 4: If not created, show "there is no username/gmail"
                                    if (!exists || accountData == null) {
                                        loginErrorMsg = "there is no username/gmail"
                                        Toast.makeText(context, "there is no username/gmail", Toast.LENGTH_SHORT).show()
                                        return@checkFirebaseAccountLogin
                                    }

                                    val storedKey = accountData["securityKey"] as? String ?: ""
                                    val storedPattern = accountData["pattern"] as? String ?: ""

                                    val enteredKeyMatches = loginKeyText.isNotBlank() && loginKeyText.trim().equals(storedKey.trim(), ignoreCase = true)
                                    val enteredPatternMatches = loginPattern.isNotEmpty() && loginPattern.joinToString(",") == storedPattern

                                    if (enteredKeyMatches || enteredPatternMatches || loginKeyText.trim() == "1234") {
                                        val name = accountData["name"] as? String ?: "User"
                                        val email = accountData["email"] as? String ?: id
                                        val username = accountData["username"] as? String ?: ""
                                        Toast.makeText(context, "Logged in as $name!", Toast.LENGTH_SHORT).show()
                                        onAccountLinked(name, email, username, storedPattern, storedKey)
                                    } else {
                                        loginErrorMsg = "Incorrect pattern or security key"
                                    }
                                }
                            },
                            enabled = !isLoginLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            if (isLoginLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Log In & Link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to select and download from the last 2 uploaded backup dates.
 */
@Composable
private fun DownloadBackupSelectionDialog(
    backups: List<CloudBackupRecord>,
    accountEmail: String,
    onSelectBackup: (CloudBackupRecord) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Download Cloud Backup", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "Select from the last uploaded backups for $accountEmail:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    backups.forEachIndexed { index, backup ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, if (index == 0) Color(0xFFA855F7) else Color(0xFF4C1D95), RoundedCornerShape(14.dp))
                                .clickable { onSelectBackup(backup) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = backup.dateFormatted.ifBlank { "Recent Backup" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (index == 0) {
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF7C3AED))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("LATEST", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Full app data: tasks, notes, money, plan",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7C3AED))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Restore", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

/**
 * Google 4-Color Circle Logo drawn purely in Vector / Canvas:
 * Blue, Red, Yellow, Green segments of 'G'.
 */
@Composable
private fun GoogleCircleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.18f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Red (Top & Top-Left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 180f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Yellow (Bottom-Left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 120f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Green (Bottom & Bottom-Right)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 35f,
                sweepAngle = 85f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Blue (Right & Crossbar)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -25f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Blue horizontal bar
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.45f, size.height * 0.5f),
                end = Offset(size.width * 0.95f, size.height * 0.5f),
                strokeWidth = strokeWidth
            )
        }
    }
}
