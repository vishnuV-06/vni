package com.vni.app.ui.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.SimpleHomeViewModel
import com.vni.app.data.model.MotiveHeading
import com.vni.app.ui.home.v2.HomeDateAndCountersBar
import com.vni.app.ui.home.v2.HomeTopMotiveBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Focus Section Root Screen:
 * - Image 1: Day View (Circular goal gauge, weekday indicator, todays summary 24h timeline)
 * - Image 2: Week View (Weekly bar chart, 10h/5h/0h, Total Spent Time, Avg Spent)
 * - Image 3: Dashboard View (Monthly calendar grid, Blocked Apps card, Time Spent card)
 * - Image 4: Bottom Sheet for configuring Timer, Stopwatch, Intervals, and starting Focus
 */
@Composable
fun FocusScreen(
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    tasksCount: Int,
    noticesCount: Int,
    remindersCount: Int,
    viewModel: SimpleHomeViewModel,
    onLaunchLiveFocus: (name: String, mode: String, durationSec: Long, breakSec: Long, intWorkSec: Long, intBreakSec: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vni_focus_prefs", Context.MODE_PRIVATE) }

    // Active Category Tab: "DAY", "WEEK", "DASHBOARD"
    var activeCategoryTab by remember { mutableStateOf("DAY") }

    // Daily Focus Goal in Minutes (default 6h = 360m)
    var focusGoalMinutes by remember { mutableStateOf(prefs.getInt("focus_goal_mins", 360)) }

    // Start Focus Bottom Sheet state (Image 4)
    var showStartBottomSheet by remember { mutableStateOf(false) }

    // Edit Goal Dialog state
    var showEditGoalDialog by remember { mutableStateOf(false) }

    // Blocked Apps Dialog state
    var showBlockedAppsDialog by remember { mutableStateOf(false) }

    // Focus Session Name prompt dialog
    var showNamePromptDialog by remember { mutableStateOf(false) }

    // Current Bottom Sheet Configuration states
    var configMode by remember { mutableStateOf("TIMER") } // "TIMER", "STOPWATCH", "INTERVALS"
    var configDurationMinutes by remember { mutableStateOf(25) }
    var configBreakMinutes by remember { mutableStateOf(5) }
    var configIntervalWorkMinutes by remember { mutableStateOf(30) }
    var configIntervalBreakMinutes by remember { mutableStateOf(5) }
    var configTotalHours by remember { mutableStateOf(3) }
    var configSessionName by remember { mutableStateOf("STUDY") }

    // Blocked apps package names
    var blockedAppPackages by remember {
        mutableStateOf(
            prefs.getStringSet("blocked_apps", setOf("com.google.android.youtube", "com.android.chrome")) ?: setOf("com.google.android.youtube", "com.android.chrome")
        )
    }

    // Today focus sessions list (simulated/saved)
    val calendar = Calendar.getInstance()
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Calculate today's focused minutes from sessions or mock
    var todayFocusedMinutes by remember {
        mutableStateOf(prefs.getInt("today_focused_mins_${todayKey}", 39))
    }

    // Selected Date in Dashboard Calendar (default today)
    var selectedCalendarDate by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

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
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Section A: Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { /* No-op */ }
            )

            // ── Section B: Date & Counters Bar ──
            HomeDateAndCountersBar(
                tasksCount = tasksCount,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                activeSectionType = "FOCUS",
                onClickDateBlock = onDismiss,
                onClickNoticeBlock = onDismiss
            )

            Spacer(Modifier.height(14.dp))

            // ── Tabs Row: [ Day ] | [ Week ] | [ Dashboard ] ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF13092C))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FocusTabPill(
                    title = "Day",
                    isSelected = (activeCategoryTab == "DAY"),
                    modifier = Modifier.weight(1f),
                    onClick = { activeCategoryTab = "DAY" }
                )
                FocusTabPill(
                    title = "Week",
                    isSelected = (activeCategoryTab == "WEEK"),
                    modifier = Modifier.weight(1f),
                    onClick = { activeCategoryTab = "WEEK" }
                )
                FocusTabPill(
                    title = "Dashboard",
                    isSelected = (activeCategoryTab == "DASHBOARD"),
                    modifier = Modifier.weight(1.1f),
                    onClick = { activeCategoryTab = "DASHBOARD" }
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── ACTIVE TAB CONTENT ──
            when (activeCategoryTab) {
                "DAY" -> {
                    FocusDayTabContent(
                        todayFocusedMinutes = todayFocusedMinutes,
                        focusGoalMinutes = focusGoalMinutes,
                        onOpenBottomSheet = { showStartBottomSheet = true },
                        onEditGoal = { showEditGoalDialog = true }
                    )
                }
                "WEEK" -> {
                    FocusWeekTabContent(
                        weeklyFocusedMinutes = listOf(480, 420, 390, 330, 219, 0, 0), // M, T, W, T, F, S, S
                        todayMinutes = todayFocusedMinutes
                    )
                }
                "DASHBOARD" -> {
                    FocusDashboardTabContent(
                        selectedDay = selectedCalendarDate,
                        onSelectDay = { selectedCalendarDate = it },
                        blockedAppsCount = blockedAppPackages.size,
                        onOpenBlockedApps = { showBlockedAppsDialog = true },
                        focusedMinutes = todayFocusedMinutes,
                        goalMinutes = focusGoalMinutes
                    )
                }
            }
        }

        // ── Bottom Sheet Configuration (Image 4) ──
        if (showStartBottomSheet) {
            FocusConfigBottomSheet(
                configMode = configMode,
                onModeChange = { configMode = it },
                durationMinutes = configDurationMinutes,
                onDurationChange = { configDurationMinutes = it },
                breakMinutes = configBreakMinutes,
                onBreakChange = { configBreakMinutes = it },
                blockedAppsCount = blockedAppPackages.size,
                onOpenBlockedApps = { showBlockedAppsDialog = true },
                onStartClick = {
                    showStartBottomSheet = false
                    showNamePromptDialog = true
                },
                onDismiss = { showStartBottomSheet = false }
            )
        }

        // ── Session Name Prompt Dialog ──
        if (showNamePromptDialog) {
            FocusSessionNameDialog(
                initialName = configSessionName,
                onConfirm = { chosenName ->
                    configSessionName = chosenName
                    showNamePromptDialog = false

                    val durSec = configDurationMinutes * 60L
                    val breakSec = configBreakMinutes * 60L
                    val intWorkSec = configIntervalWorkMinutes * 60L
                    val intBreakSec = configIntervalBreakMinutes * 60L

                    onLaunchLiveFocus(chosenName, configMode, durSec, breakSec, intWorkSec, intBreakSec)
                },
                onDismiss = { showNamePromptDialog = false }
            )
        }

        // ── Edit Goal Dialog ──
        if (showEditGoalDialog) {
            EditGoalDialog(
                currentGoalMins = focusGoalMinutes,
                onSaveGoal = { newGoalMins ->
                    focusGoalMinutes = newGoalMins
                    prefs.edit().putInt("focus_goal_mins", newGoalMins).apply()
                    showEditGoalDialog = false
                    Toast.makeText(context, "Goal updated to ${newGoalMins / 60}h", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showEditGoalDialog = false }
            )
        }

        // ── Blocked Apps Selector Dialog ──
        if (showBlockedAppsDialog) {
            BlockedAppsDialog(
                selectedPackages = blockedAppPackages,
                onSaveBlocked = { updatedSet ->
                    blockedAppPackages = updatedSet
                    prefs.edit().putStringSet("blocked_apps", updatedSet).apply()
                    showBlockedAppsDialog = false
                    Toast.makeText(context, "${updatedSet.size} apps blocked during focus", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showBlockedAppsDialog = false }
            )
        }
    }
}

/**
 * Tab pill button: Day | Week | Dashboard
 */
@Composable
private fun FocusTabPill(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF7C3AED) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * ── DAY TAB CONTENT (Image 1) ──
 */
@Composable
private fun FocusDayTabContent(
    todayFocusedMinutes: Int,
    focusGoalMinutes: Int,
    onOpenBottomSheet: () -> Unit,
    onEditGoal: () -> Unit
) {
    val cal = Calendar.getInstance()
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 6=Fri, 7=Sat
    // Weekday labels: M, T, W, T, F, S, S
    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayIdx = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 4
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Weekday Indicators: M T W T F S S (Image 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekdays.forEachIndexed { index, letter ->
                val isToday = (index == todayIdx)
                if (isToday) {
                    // Highlighted active day pill
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF261048))
                            .border(1.2.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = letter, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA855F7))
                            )
                        }
                    }
                } else {
                    // Regular day circle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = letter, color = Color(0xFF64748B), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF13092C))
                                .border(1.dp, Color(0xFF334155), CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Center Circular Progress Gauge with "start" action (Image 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clickable { onOpenBottomSheet() },
                contentAlignment = Alignment.Center
            ) {
                // Background Track and Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 32f
                    val pad = stroke / 2f
                    val arcRect = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(pad, pad)

                    // Dark track
                    drawArc(
                        color = Color(0xFF1C1335),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = Stroke(width = stroke)
                    )

                    // Violet progress arc
                    val pct = (todayFocusedMinutes.toFloat() / focusGoalMinutes.coerceAtLeast(1)).coerceIn(0.02f, 1f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFF7C3AED))
                        ),
                        startAngle = -90f,
                        sweepAngle = pct * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                // Center Info: Time + Goal
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hrs = todayFocusedMinutes / 60
                    val mins = todayFocusedMinutes % 60
                    val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Focus goal ${focusGoalMinutes / 60}h",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // "start" button area on the right (Image 1)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "start",
                    color = Color(0xFF64748B),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF13092C))
                        .border(1.2.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                        .clickable { onOpenBottomSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // [ Edit Goal ] Pill Button (Image 1)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1045))
                .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .clickable { onEditGoal() }
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(text = "Edit Goal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Todays summary: 24h Timeline Bar with violet intervals (Image 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0724))
                .border(1.2.dp, Color(0xFF4C1D95), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Todays summary",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )

                // 24h Timeline Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF1E143C))
                ) {
                    // Violet intervals filling from what time to what time (e.g. 5:30pm to 6:15pm)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.12f)
                            .offset(x = 180.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFA855F7))
                    )
                }

                // Time markers: 12am, 6am, 12pm, 6pm, 12am
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("12am", "6am", "12pm", "6pm", "12am").forEach { marker ->
                        Text(text = marker, color = Color(0xFF64748B), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * ── WEEK TAB CONTENT (Image 2) ──
 */
@Composable
private fun FocusWeekTabContent(
    weeklyFocusedMinutes: List<Int>,
    todayMinutes: Int
) {
    val totalMins = weeklyFocusedMinutes.sum() + todayMinutes
    val totalHrs = totalMins / 60
    val avgHrs = (totalHrs / 7).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today Focus Header: e.g. 3h 39m
        val todayH = todayMinutes / 60
        val todayM = todayMinutes % 60
        Text(
            text = "${todayH}h ${todayM}m",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )

        // Date navigation: < Today - Fri, 13 Nov >
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            val nowStr = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
            Text(text = "Today - $nowStr", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(10.dp))

        // Weekly Bar Chart: 10h, 5h, 0h with rounded vertical bars (Image 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F0724))
                .border(1.dp, Color(0xFF3B1668), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-Axis labels (10h, 5h, 0h)
                Column(
                    modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10h", color = Color(0xFF64748B), fontSize = 10.5.sp)
                    Text("5h", color = Color(0xFF64748B), fontSize = 10.5.sp)
                    Text("0h", color = Color(0xFF64748B), fontSize = 10.5.sp)
                }

                Spacer(Modifier.width(14.dp))

                // Bars for M, T, W, T, F, S, S
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val mockHours = listOf(8.2f, 7.5f, 6.8f, 5.2f, (todayMinutes / 60f).coerceAtLeast(3.6f), 0f, 0f)

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEachIndexed { index, d ->
                        val hrs = mockHours.getOrElse(index) { 0f }
                        val isToday = (index == 4) // Friday / Today
                        val barHeightFactor = (hrs / 10f).coerceIn(0.04f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Rounded Bar
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(barHeightFactor * 0.78f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (isToday) Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED)))
                                        else SolidColor(Color(0xFF261E38))
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(text = d, color = Color(0xFF94A3B8), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Two Bottom Summary Cards: Total Spent Time & Avg Spent (Image 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: 22 hr Total Spent Time
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalHrs hr",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Total Spent Time",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Card 2: 5 hr Avg Spent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$avgHrs hr",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Avg Spent",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * ── DASHBOARD TAB CONTENT (Image 3) ──
 */
@Composable
private fun FocusDashboardTabContent(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    blockedAppsCount: Int,
    onOpenBlockedApps: () -> Unit,
    focusedMinutes: Int,
    goalMinutes: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Monthly Calendar Grid (Image 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0724))
                .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Month Header: < NOVEMBER 2026 >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Month", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    val monthHeader = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
                    Text(text = monthHeader, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }

                // Days of Week Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEach { dow ->
                        Text(
                            text = dow,
                            color = if (dow == "SU") Color(0xFFA855F7) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Days 1..30 Grid
                val totalDaysInMonth = 30
                val rows = (totalDaysInMonth + 6) / 7
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val dayNum = r * 7 + c + 1
                            if (dayNum <= totalDaysInMonth) {
                                val isSelected = (dayNum == selectedDay)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF3B156A) else Color(0xFF160D2E))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.8.dp,
                                            color = if (isSelected) Color(0xFFA855F7) else Color(0xFF332058),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectDay(dayNum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }

        // Two Bottom Cards: Blocked Apps & Time Spent (Image 3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Blocked Apps Settings
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(18.dp))
                    .clickable { onOpenBlockedApps() }
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF261048))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$blockedAppsCount apps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "BLOCKED APPS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Card 2: Time Spent vs Goal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val hrs = focusedMinutes / 60
                    val mins = focusedMinutes % 60
                    val goalH = goalMinutes / 60
                    Text(
                        text = String.format("%d:%02d hr", hrs, mins),
                        color = Color(0xFFA855F7),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$goalH:00 Total Goal",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Time Spent",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * ── BOTTOM SHEET FOR FOCUS CONFIGURATION (Image 4) ──
 */
@Composable
private fun FocusConfigBottomSheet(
    configMode: String,
    onModeChange: (String) -> Unit,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    breakMinutes: Int,
    onBreakChange: (Int) -> Unit,
    blockedAppsCount: Int,
    onOpenBlockedApps: () -> Unit,
    onStartClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0B041E))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF64748B))
                )

                // 3 Mode Tabs: [ Timer ] | [ Stopwatch ] | [ intervals ] (Image 4)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF191038))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("TIMER" to "Timer", "STOPWATCH" to "Stopwatch", "INTERVALS" to "intervals").forEach { (key, label) ->
                        val isSelected = (configMode == key)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Color(0xFF7C3AED) else Color.Transparent)
                                .clickable { onModeChange(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Option 1: Focus Time
                if (configMode != "STOPWATCH") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF150A2E))
                            .clickable {
                                // Cycle duration: 25 -> 45 -> 60 -> 90 -> 15
                                val next = when (durationMinutes) {
                                    15 -> 25
                                    25 -> 45
                                    45 -> 60
                                    60 -> 90
                                    else -> 25
                                }
                                onDurationChange(next)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Focus time", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$durationMinutes mins", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Option 2: Break
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF150A2E))
                            .clickable {
                                // Cycle break: 5 -> 10 -> 15 -> 0
                                val next = when (breakMinutes) {
                                    5 -> 10
                                    10 -> 15
                                    15 -> 5
                                    else -> 5
                                }
                                onBreakChange(next)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Break", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$breakMinutes mins break", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF150A2E))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Open focus session: counts up until you tap stop.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }

                // Bottom Row: Blocked Apps Card (Left) + Arc Reactor Button (Right) (Image 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Blocked apps card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF150A2E))
                            .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(16.dp))
                            .clickable { onOpenBlockedApps() }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                                Text("$blockedAppsCount apps", color = Color(0xFFA855F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Blocked apps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Right: Iron Man Arc Reactor Glowing Button to START focus! (Image 4)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clickable { onStartClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        ArcReactorView(
                            reactorSize = 76.dp,
                            isRunning = true,
                            isPaused = false,
                            progress = 0f,
                            showProgressRing = true,
                            showPowerBeam = false,
                            onSingleTap = { onStartClick() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Focus Session Name Dialog: asks session name before starting focus
 */
@Composable
private fun FocusSessionNameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    val presets = listOf("STUDY", "CODING", "READING", "WORK", "EXERCISE")

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "Name Your Focus Session",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (nameInput == p) Color(0xFF7C3AED) else Color(0xFF1E1045))
                                .clickable { nameInput = p }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(p, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Text field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1045))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clean = if (nameInput.isNotBlank()) nameInput.trim() else "STUDY"
                            onConfirm(clean)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Start Focus", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Edit Daily Goal Dialog
 */
@Composable
private fun EditGoalDialog(
    currentGoalMins: Int,
    onSaveGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var goalHours by remember { mutableStateOf(currentGoalMins / 60) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Set Daily Focus Goal", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("How many hours do you want to focus each day?", color = Color(0xFF94A3B8), fontSize = 12.5.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(2, 4, 6, 8, 10).forEach { h ->
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (goalHours == h) Color(0xFF7C3AED) else Color(0xFF1E1045))
                                .border(1.dp, if (goalHours == h) Color(0xFFA855F7) else Color(0xFF4C1D95), CircleShape)
                                .clickable { goalHours = h },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${h}h", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveGoal(goalHours * 60) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Save Goal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Blocked Apps Selector Dialog
 */
@Composable
private fun BlockedAppsDialog(
    selectedPackages: Set<String>,
    onSaveBlocked: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSelected by remember { mutableStateOf(selectedPackages.toMutableSet()) }

    // Popular apps list + installed apps detection
    val sampleApps = remember {
        listOf(
            "com.google.android.youtube" to "YouTube",
            "com.android.chrome" to "Chrome Browser",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "X (Twitter)",
            "com.whatsapp" to "WhatsApp",
            "com.facebook.katana" to "Facebook",
            "com.netflix.mediaclient" to "Netflix",
            "com.reddit.frontpage" to "Reddit"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
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
                    Text("Select Blocked Apps", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "Selected apps will be blocked and prevented from distracting you while in Focus Mode:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleApps) { (pkg, name) ->
                        val isChecked = currentSelected.contains(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A0E38))
                                .clickable {
                                    val updated = currentSelected.toMutableSet()
                                    if (isChecked) updated.remove(pkg) else updated.add(pkg)
                                    currentSelected = updated
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = currentSelected.toMutableSet()
                                    if (checked) updated.add(pkg) else updated.remove(pkg)
                                    currentSelected = updated
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF7C3AED)
                                )
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveBlocked(currentSelected) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Save (${currentSelected.size})", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
