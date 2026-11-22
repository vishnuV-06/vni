package com.vni.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.Task
import com.vni.app.data.model.UniTask
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.platform.LocalContext
import com.vni.app.notifications.NotificationHelper
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt
import com.vni.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Progress Detail Panel matching Image 2 & Uni-Tasks mode:
 * - Comes from bottom with smooth spring animation
 * - Purple drag handle pill at top; dragging it down closes the panel
 * - Toggle: [ Tasks ]  [ Uni-Tasks ]  [ Sleep ]  [ Wasted ]
 * - Tasks mode:
 *   - WEEKLY PROGRESS graph (Week/Month toggle, smooth curve, dots, purple gradient)
 *   - 4 Metric cards: Consistency, Best day, Week progress, Total time spent
 *   - ACTIVE TASKS with circular progress rings (clickable to view individual task progress)
 *   - PAST TASKS with delete "✕" button
 * - Uni-Tasks mode:
 *   - UNI-TASK PROGRESS graph
 *   - Selecting any day along the x-axis shows the number of uni-tasks completed on that day
 *   - Clicking the day again deselects and returns to previous UI
 *   - Individual uni-task progress rings and metrics
 * - Sleep mode:
 *   - Wake & sleep graph, averages, and targets
 * - Wasted mode:
 *   - End of day wasted time graph, weekly metrics, and daily wasted time logging card
 */
@Composable
fun ProgressDetailScreen(
    tasks: List<Task>,
    pastTasks: List<Task>,
    completedCount: Int,
    totalCount: Int,
    uniTasks: List<UniTask> = emptyList(),
    isUniTaskModeInitial: Boolean = false,
    weekWastedMinutes: List<Int> = listOf(45, 60, 30, 90, 40, 110, 60),
    onLogWastedMinutes: (Int) -> Unit = {},
    onDeletePastTask: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sheetHeightDp = 640.dp
    val sheetHeightPx = with(density) { sheetHeightDp.toPx() }
    val offsetY = remember { Animatable(sheetHeightPx) }
    val scope = rememberCoroutineScope()

    // Smooth spring slide-up from bottom
    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun dismiss() {
        scope.launch {
            offsetY.animateTo(sheetHeightPx, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
    }

    var progressType by remember { mutableStateOf(if (isUniTaskModeInitial) "Uni-Tasks" else "Tasks") }
    var graphMode by remember { mutableStateOf("Week") } // "Week" or "Month"
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedUniTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedDayIdx by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vni_app_prefs_v2", Context.MODE_PRIVATE) }
    var sleepGraphMode by remember { mutableStateOf("Week") }
    var targetWakeHour by remember { mutableIntStateOf(prefs.getInt("target_wake_hour", 7)) }
    var targetWakeMin by remember { mutableIntStateOf(prefs.getInt("target_wake_min", 0)) }
    var targetSleepHour by remember { mutableIntStateOf(prefs.getInt("target_sleep_hour", 10)) }
    var targetSleepMin by remember { mutableIntStateOf(prefs.getInt("target_sleep_min", 0)) }

    val cal = remember { Calendar.getInstance() }
    val todayDayOfWeekIndex = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6) // Sunday = 0
    val daysElapsed = (todayDayOfWeekIndex + 1).coerceAtLeast(1)

    var todayLoggedWastedMin by remember(weekWastedMinutes, todayDayOfWeekIndex) {
        mutableIntStateOf(weekWastedMinutes.getOrElse(todayDayOfWeekIndex) { 60 })
    }

    val weekWakeData = remember { listOf(7.0f, 7.6f, 7.5f, 7.9f, 6.5f, 7.3f, 7.9f) }
    val weekSleepData = remember { listOf(10.0f, 9.4f, 8.7f, 7.9f, 8.4f, 9.4f, 9.8f) }
    val monthWakeData = remember { listOf(7.2f, 7.5f, 6.8f, 7.1f) }
    val monthSleepData = remember { listOf(9.8f, 9.2f, 8.6f, 9.5f) }

    val dayNames = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")

    // ── Regular Tasks Calculations ──────────────────────────────────────────
    val selectedTask = tasks.find { it.id == selectedTaskId }
    val activeWeekData: List<Int> = remember(tasks, selectedTask, todayDayOfWeekIndex) {
        (0..6).map { dayIdx ->
            if (dayIdx <= todayDayOfWeekIndex) {
                if (selectedTask != null) {
                    val spent = selectedTask.weekLoggedMinutes.getOrElse(dayIdx) { 0 }
                    val target = selectedTask.targetMinutes.coerceAtLeast(1)
                    if (spent > 0) ((spent.toFloat() / target) * 100).toInt().coerceIn(0, 100) else 0
                } else {
                    val daySpent = tasks.sumOf { it.weekLoggedMinutes.getOrElse(dayIdx) { 0 } }
                    val dayTarget = tasks.sumOf { it.targetMinutes }.coerceAtLeast(1)
                    if (daySpent > 0) ((daySpent.toFloat() / dayTarget) * 100).toInt().coerceIn(0, 100) else 0
                }
            } else {
                0
            }
        }
    }

    val overallMonthData = remember(activeWeekData, daysElapsed) {
        val currentWeekAvg = if (daysElapsed > 0) activeWeekData.take(daysElapsed).average().toInt() else 0
        listOf(
            (currentWeekAvg * 0.7f).toInt().coerceIn(0, 100),
            (currentWeekAvg * 0.85f).toInt().coerceIn(0, 100),
            currentWeekAvg,
            0
        )
    }

    val taskActiveData = if (graphMode == "Week") activeWeekData else overallMonthData

    val totalTimeSpentMin = remember(tasks, selectedTask, todayDayOfWeekIndex) {
        if (selectedTask != null) {
            (0..todayDayOfWeekIndex).sumOf { selectedTask.weekLoggedMinutes.getOrElse(it) { 0 } }
        } else {
            (0..todayDayOfWeekIndex).sumOf { dayIdx ->
                tasks.sumOf { it.weekLoggedMinutes.getOrElse(dayIdx) { 0 } }
            }
        }
    }

    val totalTargetMin = remember(tasks, selectedTask, todayDayOfWeekIndex, daysElapsed) {
        if (selectedTask != null) {
            daysElapsed * selectedTask.targetMinutes
        } else {
            daysElapsed * tasks.sumOf { it.targetMinutes }
        }
    }

    val weekProgressPct = remember(totalTimeSpentMin, totalTargetMin) {
        if (totalTargetMin > 0) {
            ((totalTimeSpentMin.toFloat() / totalTargetMin) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    val taskConsistencyPct = remember(tasks, selectedTask, todayDayOfWeekIndex, daysElapsed) {
        val daysWithData = (0..todayDayOfWeekIndex).count { dayIdx ->
            if (selectedTask != null) {
                selectedTask.weekLoggedMinutes.getOrElse(dayIdx) { 0 } > 0
            } else {
                tasks.any { it.weekLoggedMinutes.getOrElse(dayIdx) { 0 } > 0 }
            }
        }
        if (daysElapsed > 0) ((daysWithData.toFloat() / daysElapsed) * 100).toInt().coerceIn(0, 100) else 0
    }

    val taskBestDayName = remember(tasks, selectedTask, todayDayOfWeekIndex) {
        val bestIdx = (0..todayDayOfWeekIndex).maxByOrNull { dayIdx ->
            if (selectedTask != null) {
                selectedTask.weekLoggedMinutes.getOrElse(dayIdx) { 0 }
            } else {
                tasks.sumOf { it.weekLoggedMinutes.getOrElse(dayIdx) { 0 } }
            }
        } ?: 0
        val bestSpent = if (selectedTask != null) {
            selectedTask.weekLoggedMinutes.getOrElse(bestIdx) { 0 }
        } else {
            tasks.sumOf { it.weekLoggedMinutes.getOrElse(bestIdx) { 0 } }
        }
        if (bestSpent > 0) dayNames.getOrElse(bestIdx) { "SUNDAY" } else "None"
    }

    // ── Uni-Tasks Calculations ──────────────────────────────────────────────
    val selectedUniTask = uniTasks.find { it.id == selectedUniTaskId }
    val uniTaskWeekData: List<Int> = remember(uniTasks, selectedUniTask, todayDayOfWeekIndex) {
        (0..6).map { dayIdx ->
            if (dayIdx <= todayDayOfWeekIndex) {
                if (selectedUniTask != null) {
                    val comp = if (dayIdx == todayDayOfWeekIndex) selectedUniTask.completedToday else selectedUniTask.weekCompleted.getOrElse(dayIdx) { false }
                    if (comp) 100 else 0
                } else {
                    val completedCount = uniTasks.count {
                        if (dayIdx == todayDayOfWeekIndex) it.completedToday else it.weekCompleted.getOrElse(dayIdx) { false }
                    }
                    val total = uniTasks.size.coerceAtLeast(1)
                    ((completedCount.toFloat() / total) * 100).toInt().coerceIn(0, 100)
                }
            } else {
                0
            }
        }
    }

    val uniTaskConsistencyPct = remember(uniTasks, selectedUniTask, todayDayOfWeekIndex, daysElapsed) {
        val daysWithData = (0..todayDayOfWeekIndex).count { dayIdx ->
            if (selectedUniTask != null) {
                if (dayIdx == todayDayOfWeekIndex) selectedUniTask.completedToday else selectedUniTask.weekCompleted.getOrElse(dayIdx) { false }
            } else {
                uniTasks.any {
                    if (dayIdx == todayDayOfWeekIndex) it.completedToday else it.weekCompleted.getOrElse(dayIdx) { false }
                }
            }
        }
        if (daysElapsed > 0) ((daysWithData.toFloat() / daysElapsed) * 100).toInt().coerceIn(0, 100) else 0
    }

    val uniTaskBestDayName = remember(uniTasks, selectedUniTask, todayDayOfWeekIndex) {
        val bestIdx = (0..todayDayOfWeekIndex).maxByOrNull { dayIdx ->
            uniTasks.count {
                if (dayIdx == todayDayOfWeekIndex) it.completedToday else it.weekCompleted.getOrElse(dayIdx) { false }
            }
        } ?: 0
        dayNames.getOrElse(bestIdx) { "SUNDAY" }
    }

    val uniTaskTotalCompletedCount = remember(uniTasks, todayDayOfWeekIndex) {
        (0..todayDayOfWeekIndex).sumOf { dayIdx ->
            uniTasks.count {
                if (dayIdx == todayDayOfWeekIndex) it.completedToday else it.weekCompleted.getOrElse(dayIdx) { false }
            }
        }
    }

    val uniTaskTodayCompletedCount = uniTasks.count { it.completedToday }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { dismiss() }
            .zIndex(50f)
    ) {
        // Full Screen Page matching Requirement 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C071E))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(enabled = false) {}
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Top Row: Handle Pill, "Progress" Title in middle, [✕ Close] on right
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(56.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.size(34.dp)) // Spacer to balance the close button

                            // Top Middle Title: "Progress"
                            Text(
                                text = "Progress",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Clean [✕ Close] button on top right (single close button)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF140D30))
                                    .border(1.dp, Color(0xFF3B1E7E), RoundedCornerShape(8.dp))
                                    .clickable { dismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // ── Type Selector: [ Tasks ]  [ Uni-Tasks ]  [ Sleep ]  [ Wasted ] ──────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF130B30))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(16.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("Tasks", Icons.Default.FormatListBulleted, "Tasks"),
                            Triple("Uni-Tasks", Icons.Default.CheckCircle, "Uni-Tasks"),
                            Triple("Sleep", Icons.Default.Bedtime, "Sleep"),
                            Triple("Wasted", Icons.Default.HourglassEmpty, "Wasted")
                        ).forEach { (type, icon, label) ->
                            val isSelected = progressType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(
                                            if (type == "Wasted") listOf(Color(0xFFEA580C), Color(0xFFDC2626))
                                            else listOf(Color(0xFF7C3AED), Color(0xFF9333EA))
                                        ) else Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    )
                                    .clickable {
                                        progressType = type
                                        selectedDayIdx = null
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else TextGrey,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextGrey,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (progressType == "Sleep") {
                    // ── 1. Sleep Schedule Card matching Image 4 ───────────────
                    item {
                        SleepScheduleCard(
                            graphMode = sleepGraphMode,
                            onModeChange = { sleepGraphMode = it },
                            wakeData = if (sleepGraphMode == "Week") weekWakeData else monthWakeData,
                            sleepData = if (sleepGraphMode == "Week") weekSleepData else monthSleepData,
                            isWeek = sleepGraphMode == "Week"
                        )
                    }

                    // ── 2. Average (This Week) Card ───────────────────────────
                    item {
                        SleepAveragesCard()
                    }

                    // ── 3. What time you want to wake up? Card ────────────────
                    item {
                        SleepTargetCard(
                            isWake = true,
                            hour = targetWakeHour,
                            minute = targetWakeMin,
                            onHourChange = { targetWakeHour = it },
                            onMinuteChange = { targetWakeMin = it }
                        )
                    }

                    // ── 4. What time you want to go sleep? Card ───────────────
                    item {
                        SleepTargetCard(
                            isWake = false,
                            hour = targetSleepHour,
                            minute = targetSleepMin,
                            onHourChange = { targetSleepHour = it },
                            onMinuteChange = { targetSleepMin = it }
                        )
                    }

                    // ── 5. Save Schedule Button ───────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF7C3AED),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                )
                                .clickable {
                                    prefs.edit()
                                        .putInt("target_wake_hour", targetWakeHour)
                                        .putInt("target_wake_min", targetWakeMin)
                                        .putInt("target_sleep_hour", targetSleepHour)
                                        .putInt("target_sleep_min", targetSleepMin)
                                        .apply()
                                    NotificationHelper.scheduleSleepWakeSchedule(
                                        context = context,
                                        wakeHour = targetWakeHour,
                                        wakeMin = targetWakeMin,
                                        sleepHour = targetSleepHour,
                                        sleepMin = targetSleepMin
                                    )
                                    Toast.makeText(context, "Schedule saved! Reminders scheduled.", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save Schedule",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (progressType == "Wasted") {
                    // ── 1. Wasted Time Graph Card ──────────────────────────────
                    item {
                        WastedScheduleCard(
                            wastedData = weekWastedMinutes,
                            todayIndex = todayDayOfWeekIndex,
                            dayNames = dayNames
                        )
                    }

                    // ── 2. Wasted Time Metrics ────────────────────────────────
                    item {
                        WastedMetricsCard(
                            wastedData = weekWastedMinutes,
                            todayIndex = todayDayOfWeekIndex,
                            dayNames = dayNames
                        )
                    }

                    // ── 3. Daily Wasted Logging Card ──────────────────────────
                    item {
                        WastedLogInputCard(
                            todayMinutes = todayLoggedWastedMin,
                            onMinutesChange = { todayLoggedWastedMin = it },
                            onSave = {
                                onLogWastedMinutes(todayLoggedWastedMin)
                                Toast.makeText(context, "Logged ${todayLoggedWastedMin}m wasted time today", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                // ── 1. Graph Card matching Image 2 ───────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0D0825))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Title + Toggle (Week/Month for Tasks)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (progressType == "Uni-Tasks") {
                                        if (selectedUniTask != null) "${selectedUniTask.name.uppercase()} PROGRESS" else "UNI-TASK PROGRESS"
                                    } else {
                                        if (graphMode == "Week") "WEEKLY PROGRESS" else "MONTHLY PROGRESS"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                if (progressType == "Tasks") {
                                    // Week / Month Toggle
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF130B30))
                                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(14.dp))
                                    ) {
                                        listOf("Week", "Month").forEach { mode ->
                                            val isSelected = graphMode == mode
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) PurplePrimary else Color.Transparent)
                                                    .clickable { graphMode = mode }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = mode,
                                                    color = if (isSelected) Color.White else TextGrey,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Tap x-axis day to view",
                                        color = PurpleLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Line Graph Canvas
                            val chartData = if (progressType == "Uni-Tasks") uniTaskWeekData else taskActiveData
                            ProgressLineGraphCanvas(
                                data = chartData,
                                isWeek = progressType == "Uni-Tasks" || graphMode == "Week",
                                selectedDayIdx = selectedDayIdx,
                                onSelectDay = { dayIdx ->
                                    selectedDayIdx = if (selectedDayIdx == dayIdx) null else dayIdx
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                // ── Day Details Card (When day is selected in x-axis in Uni-Tasks) ──
                if (progressType == "Uni-Tasks" && selectedDayIdx != null) {
                    val idx = selectedDayIdx!!
                    val dayName = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")[idx]
                    val completedOnDay = uniTasks.count {
                        if (idx == todayDayOfWeekIndex) it.completedToday else it.weekCompleted.getOrElse(idx) { false }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF130B30))
                                .border(1.5.dp, PurplePrimary, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$dayName: $completedOnDay / ${uniTasks.size} Uni-Tasks Completed",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E103E))
                                            .clickable { selectedDayIdx = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // List of tasks on that day
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    uniTasks.forEach { ut ->
                                        val isDone = if (idx == todayDayOfWeekIndex) ut.completedToday else ut.weekCompleted.getOrElse(idx) { false }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ut.name,
                                                color = if (isDone) Color.White else Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            if (isDone) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .border(1.dp, Color(0xFF64748B), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 2. 4 Metric Cards Row ────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (progressType == "Tasks") {
                            MetricCard(title = "Consistency", value = "$taskConsistencyPct%", modifier = Modifier.weight(1f))
                            MetricCard(title = "Best day", value = taskBestDayName, modifier = Modifier.weight(1.15f))
                            MetricCard(title = "Week progress", value = "$weekProgressPct%", modifier = Modifier.weight(1.05f))
                            MetricCard(title = "Total time spent", value = "$totalTimeSpentMin MIN", modifier = Modifier.weight(1.2f))
                        } else {
                            MetricCard(title = "Consistency", value = "$uniTaskConsistencyPct%", modifier = Modifier.weight(1f))
                            MetricCard(title = "Best day", value = uniTaskBestDayName, modifier = Modifier.weight(1.15f))
                            MetricCard(title = "Week count", value = "$uniTaskTotalCompletedCount Done", modifier = Modifier.weight(1.05f))
                            MetricCard(title = "Today", value = "$uniTaskTodayCompletedCount / ${uniTasks.size}", modifier = Modifier.weight(1.2f))
                        }
                    }
                }

                // ── 3. ACTIVE ITEMS Section ──────────────────────────────────
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (progressType == "Uni-Tasks") "ACTIVE UNI-TASKS" else "ACTIVE TASKS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (progressType == "Uni-Tasks") selectedUniTaskId = null else selectedTaskId = null
                                }
                            ) {
                                Text(
                                    text = if ((progressType == "Uni-Tasks" && selectedUniTaskId != null) || (progressType == "Tasks" && selectedTaskId != null)) "Show all" else "See all",
                                    color = PurpleLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = PurpleLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val ringColors = listOf(
                            Color(0xFFF43F5E),
                            Color(0xFFF59E0B),
                            Color(0xFF06B6D4),
                            Color(0xFF10B981),
                            Color(0xFF8B5CF6),
                            Color(0xFFEAB308)
                        )

                        if (progressType == "Tasks") {
                            // Tasks Grid
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    tasks.take(3).forEachIndexed { idx, task ->
                                        val taskSpent = (0..todayDayOfWeekIndex).sumOf { task.weekLoggedMinutes.getOrElse(it) { 0 } }
                                        val taskTarget = daysElapsed * task.targetMinutes
                                        val pct = if (taskTarget > 0) ((taskSpent.toFloat() / taskTarget) * 100).toInt().coerceIn(0, 100) else 0
                                        ActiveTaskRingCard(
                                            name = task.name,
                                            percent = pct,
                                            ringColor = ringColors.getOrElse(idx) { PurplePrimary },
                                            isSelected = task.id == selectedTaskId,
                                            onClick = { selectedTaskId = if (selectedTaskId == task.id) null else task.id },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (tasks.size > 3) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        tasks.drop(3).take(2).forEachIndexed { idx, task ->
                                            val taskSpent = (0..todayDayOfWeekIndex).sumOf { task.weekLoggedMinutes.getOrElse(it) { 0 } }
                                            val taskTarget = daysElapsed * task.targetMinutes
                                            val pct = if (taskTarget > 0) ((taskSpent.toFloat() / taskTarget) * 100).toInt().coerceIn(0, 100) else 0
                                            ActiveTaskRingCard(
                                                name = task.name,
                                                percent = pct,
                                                ringColor = ringColors.getOrElse(idx + 3) { PurplePrimary },
                                                isSelected = task.id == selectedTaskId,
                                                onClick = { selectedTaskId = if (selectedTaskId == task.id) null else task.id },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(0.5f))
                                    }
                                }
                            }
                        } else {
                            // Uni-Tasks Grid
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uniTasks.take(3).forEachIndexed { idx, ut ->
                                        val pct = ((ut.getWeekCompletedCount().toFloat() / 7) * 100).toInt().coerceIn(0, 100)
                                        ActiveTaskRingCard(
                                            name = ut.name,
                                            percent = pct,
                                            ringColor = ringColors.getOrElse(idx) { PurplePrimary },
                                            isSelected = ut.id == selectedUniTaskId,
                                            onClick = { selectedUniTaskId = if (selectedUniTaskId == ut.id) null else ut.id },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (uniTasks.size > 3) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        uniTasks.drop(3).take(3).forEachIndexed { idx, ut ->
                                            val pct = ((ut.getWeekCompletedCount().toFloat() / 7) * 100).toInt().coerceIn(0, 100)
                                            ActiveTaskRingCard(
                                                name = ut.name,
                                                percent = pct,
                                                ringColor = ringColors.getOrElse(idx + 3) { PurplePrimary },
                                                isSelected = ut.id == selectedUniTaskId,
                                                onClick = { selectedUniTaskId = if (selectedUniTaskId == ut.id) null else ut.id },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 4. PAST TASKS Section ────────────────────────────────────
                if (progressType == "Tasks") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PAST TASKS",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            if (pastTasks.isEmpty()) {
                                Text(text = "No past tasks yet", color = TextGrey, fontSize = 12.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pastTasks.forEach { pt ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF0D0825))
                                                .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = pt.name, color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(text = "Archived", color = PurpleLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF20133A))
                                                        .clickable { onDeletePastTask(pt.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete past task",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

// ── Metric Card ─────────────────────────────────────────────────────────────
@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextGrey,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ── Active Task Card with Circular Progress Ring ─────────────────────────────
@Composable
private fun ActiveTaskRingCard(
    name: String,
    percent: Int,
    ringColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1E103E) else Color(0xFF0D0825))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PurplePrimary else Color(0xFF2E1C6A),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ring
            Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 3.5.dp.toPx()
                    drawCircle(color = Color(0xFF1E1438), style = Stroke(strokeW))
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = (percent / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$percent%",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}

// ── Progress Line Graph Canvas matching Image 2 ──────────────────────────────
@Composable
private fun ProgressLineGraphCanvas(
    data: List<Int>,
    isWeek: Boolean,
    selectedDayIdx: Int? = null,
    onSelectDay: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val labels = if (isWeek) listOf("S", "M", "T", "W", "T", "F", "S") else listOf("W1", "W2", "W3", "W4")

    Row(modifier = modifier) {
        // Y-axis labels: 100, 75, 50, 25, 0
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf("100", "75", "50", "25", "0").forEach { label ->
                Text(text = label, color = TextGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Canvas Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 26.dp, end = 6.dp)) {
                val w = size.width
                val h = size.height

                // Horizontal dashed gridlines for 100, 75, 50, 25, 0
                for (i in 0..4) {
                    val y = i * (h / 4f)
                    drawLine(
                        color = Color(0xFF2E1C6A).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }

                if (data.isEmpty()) return@Canvas

                val step = if (data.size > 1) w / (data.size - 1).toFloat() else w / 2f

                // Map points
                val points = data.mapIndexed { idx, v ->
                    val x = idx * step
                    val y = h - ((v.coerceIn(0, 100) / 100f) * h)
                    Offset(x, y)
                }

                // Smooth cubic Bézier curve
                val path = Path()
                path.moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx1 = (p0.x + p1.x) / 2
                    val cy1 = p0.y
                    val cx2 = (p0.x + p1.x) / 2
                    val cy2 = p1.y
                    path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }

                // Gradient fill under the curve
                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(points.last().x, h)
                fillPath.lineTo(points.first().x, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PurplePrimary.copy(alpha = 0.45f),
                            PurplePrimary.copy(alpha = 0.05f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // Curve stroke
                drawPath(
                    path = path,
                    color = Color(0xFFC084FC),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Circles on points
                points.forEachIndexed { idx, pt ->
                    val isHighlight = selectedDayIdx == idx
                    if (isHighlight) {
                        drawCircle(color = PurplePrimary, radius = 9.dp.toPx(), center = pt)
                    }
                    drawCircle(color = PurplePrimary.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFFC084FC), radius = 3.5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = pt)
                }
            }

            // X-axis labels at bottom (Clickable to select day in Uni-Tasks!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEachIndexed { idx, lbl ->
                    val isSelected = selectedDayIdx == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDay(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PurplePrimary else Color.Transparent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = lbl,
                                color = if (isSelected) Color.White else TextGrey,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sleep Schedule Card matching Image 4 ────────────────────────────────────
@Composable
private fun SleepScheduleCard(
    graphMode: String,
    onModeChange: (String) -> Unit,
    wakeData: List<Float>,
    sleepData: List<Float>,
    isWeek: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Sleep Schedule",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (isWeek) "Your wake up and sleep times this week" else "Your wake up and sleep times this month",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                // Week / Month Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF130B30))
                        .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(14.dp))
                        .padding(2.dp)
                ) {
                    listOf("Week", "Month").forEach { mode ->
                        val isSelected = graphMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PurplePrimary else Color.Transparent)
                                .clickable { onModeChange(mode) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Sleep Dual Curve Graph
            SleepDualLineGraphCanvas(
                wakeData = wakeData,
                sleepData = sleepData,
                isWeek = isWeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(Modifier.height(14.dp))

            // Legend: [• Wake up]  [• Sleep]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Wake up",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.width(28.dp))

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEC4899))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Sleep",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Sleep Dual Line Graph Canvas ────────────────────────────────────────────
@Composable
private fun SleepDualLineGraphCanvas(
    wakeData: List<Float>,
    sleepData: List<Float>,
    isWeek: Boolean,
    modifier: Modifier = Modifier
) {
    val labels = if (isWeek) listOf("S", "M", "T", "W", "T", "F", "S") else listOf("W1", "W2", "W3", "W4")
    val yLabels = listOf("6", "7", "8", "9", "10", "11")

    Row(modifier = modifier) {
        // Y-axis labels: 6, 7, 8, 9, 10, 11 (from top to bottom)
        Column(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach { label ->
                Text(
                    text = label,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Canvas Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 26.dp, end = 6.dp)) {
                val w = size.width
                val h = size.height

                // Horizontal dashed gridlines for 6, 7, 8, 9, 10, 11 (5 equal intervals)
                for (i in 0..5) {
                    val y = i * (h / 5f)
                    drawLine(
                        color = Color(0xFF2E1C6A).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }

                val n = labels.size
                val step = if (n > 1) w / (n - 1).toFloat() else w / 2f

                // Vertical dashed gridlines for each column
                for (i in 0 until n) {
                    val x = i * step
                    drawLine(
                        color = Color(0xFF2E1C6A).copy(alpha = 0.35f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }

                // Map float hours (6.0 to 11.0) to pixel points
                fun mapPoints(data: List<Float>): List<Offset> {
                    val dataStep = if (data.size > 1) w / (data.size - 1).toFloat() else w / 2f
                    return data.mapIndexed { idx, v ->
                        val x = idx * dataStep
                        val clamped = v.coerceIn(6.0f, 11.0f)
                        val yRatio = (clamped - 6.0f) / 5.0f
                        val y = yRatio * h
                        Offset(x, y)
                    }
                }

                val wakePoints = mapPoints(wakeData)
                val sleepPoints = mapPoints(sleepData)

                fun drawCurve(points: List<Offset>, strokeColor: Color, glowColor: Color, fillGradient: Brush?) {
                    if (points.isEmpty()) return

                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx1 = (p0.x + p1.x) / 2
                        val cy1 = p0.y
                        val cx2 = (p0.x + p1.x) / 2
                        val cy2 = p1.y
                        path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    }

                    if (fillGradient != null) {
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(points.last().x, h)
                        fillPath.lineTo(points.first().x, h)
                        fillPath.close()
                        drawPath(path = fillPath, brush = fillGradient)
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEach { pt ->
                        drawCircle(color = glowColor.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = pt)
                        drawCircle(color = strokeColor, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                    }
                }

                // 1. Draw Wake Up Curve (Purple)
                drawCurve(
                    points = wakePoints,
                    strokeColor = Color(0xFFA78BFA),
                    glowColor = Color(0xFF8B5CF6),
                    fillGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // 2. Draw Sleep Curve (Pink)
                drawCurve(
                    points = sleepPoints,
                    strokeColor = Color(0xFFF472B6),
                    glowColor = Color(0xFFEC4899),
                    fillGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEC4899).copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )
            }

            // X-axis labels at bottom (S, M, T, W, T, F, S)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { lbl ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lbl,
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Average (This Week) Card ────────────────────────────────────────────────
@Composable
private fun SleepAveragesCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Average (This Week)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Wake up tile
                SleepAverageTile(
                    icon = Icons.Default.WbSunny,
                    iconTint = Color(0xFFA78BFA),
                    iconBg = Color(0xFF2E1065),
                    borderColor = Color(0xFF4C1D95),
                    title = "Wake up",
                    value = "7:12",
                    modifier = Modifier.weight(1f)
                )

                // 2. Sleep tile
                SleepAverageTile(
                    icon = Icons.Default.Bedtime,
                    iconTint = Color(0xFFF472B6),
                    iconBg = Color(0xFF4C0519),
                    borderColor = Color(0xFFEC4899).copy(alpha = 0.5f),
                    title = "Sleep",
                    value = "10:18",
                    modifier = Modifier.weight(1f)
                )

                // 3. Sleeping time tile
                SleepAverageTile(
                    icon = Icons.Default.Hotel,
                    iconTint = Color(0xFF60A5FA),
                    iconBg = Color(0xFF172554),
                    borderColor = Color(0xFF1E3A8A),
                    title = "Sleeping time",
                    value = "7:06",
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}

// ── Single Average Tile ─────────────────────────────────────────────────────
@Composable
private fun SleepAverageTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    borderColor: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF130B30))
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Target Sleep/Wake Time Card ─────────────────────────────────────────────
@Composable
private fun SleepTargetCard(
    isWake: Boolean,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isWake) Color(0xFF2E1065) else Color(0xFF4C0519))
                        .border(1.dp, if (isWake) Color(0xFF6B21A8) else Color(0xFF9D174D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWake) Icons.Default.WbSunny else Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = if (isWake) Color(0xFFA78BFA) else Color(0xFFF472B6),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isWake) "What time you want to wake up?" else "What time you want to go sleep?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isWake) "Set your daily wake up time for notifications" else "Set your daily sleep time for notifications",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            TimeSpinnerControls(
                hour = hour,
                minute = minute,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange
            )
        }
    }
}

// ── Time Spinner Controls matching Image 4 ──────────────────────────────────
@Composable
private fun TimeSpinnerControls(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Hour spinner box
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF130B30))
                .border(1.2.dp, Color(0xFF2E1C6A), RoundedCornerShape(13.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increment Hour",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onHourChange(if (hour >= 23) 0 else hour + 1) }
                )
                Text(
                    text = hour.toString(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrement Hour",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onHourChange(if (hour <= 0) 23 else hour - 1) }
                )
            }
        }

        // Colon separator
        Text(
            text = ":",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        // Minute spinner box
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF130B30))
                .border(1.2.dp, Color(0xFF2E1C6A), RoundedCornerShape(13.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increment Minute",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onMinuteChange(if (minute >= 55) 0 else minute + 5) }
                )
                Text(
                    text = String.format(Locale.getDefault(), "%02d", minute),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrement Minute",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onMinuteChange(if (minute <= 0) 55 else minute - 5) }
                )
            }
        }
    }
}

// ── Wasted Time Schedule Card ───────────────────────────────────────────────
@Composable
private fun WastedScheduleCard(
    wastedData: List<Int>,
    todayIndex: Int,
    dayNames: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedDayIdx by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(wastedData) { (wastedData.maxOrNull() ?: 120).coerceAtLeast(60) }
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wasted Time",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = selectedDayIdx?.let { idx ->
                            val mins = wastedData.getOrElse(idx) { 0 }
                            "${dayNames.getOrElse(idx) { "" }}: ${mins / 60}h ${mins % 60}m (${mins} mins)"
                        } ?: "Daily unproductive time this week",
                        color = if (selectedDayIdx != null) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedDayIdx != null) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3B1515))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "This Week",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Canvas Line/Area Graph for Wasted Time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf(
                            "${maxVal}m",
                            "${(maxVal * 0.75f).toInt()}m",
                            "${(maxVal * 0.5f).toInt()}m",
                            "${(maxVal * 0.25f).toInt()}m",
                            "0m"
                        ).forEach { lbl ->
                            Text(text = lbl, color = TextGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Canvas
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 26.dp, end = 6.dp)) {
                            val w = size.width
                            val h = size.height

                            // Horizontal dashed lines
                            for (i in 0..4) {
                                val y = i * (h / 4f)
                                drawLine(
                                    color = Color(0xFF2E1C6A).copy(alpha = 0.5f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                            }

                            if (wastedData.isEmpty()) return@Canvas

                            val step = if (wastedData.size > 1) w / (wastedData.size - 1).toFloat() else w / 2f

                            val points = wastedData.mapIndexed { idx, v ->
                                val x = idx * step
                                val ratio = (v.toFloat() / maxVal).coerceIn(0f, 1f)
                                val y = h - (ratio * h)
                                Offset(x, y)
                            }

                            val path = Path()
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx1 = (p0.x + p1.x) / 2
                                val cy1 = p0.y
                                val cx2 = (p0.x + p1.x) / 2
                                val cy2 = p1.y
                                path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                            }

                            // Fill
                            val fillPath = Path()
                            fillPath.addPath(path)
                            fillPath.lineTo(points.last().x, h)
                            fillPath.lineTo(points.first().x, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.45f),
                                        Color(0xFFF59E0B).copy(alpha = 0.05f)
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Stroke
                            drawPath(
                                path = path,
                                color = Color(0xFFF97316),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Dots
                            points.forEachIndexed { idx, pt ->
                                val isHighlight = selectedDayIdx == idx || (selectedDayIdx == null && idx == todayIndex)
                                if (isHighlight) {
                                    drawCircle(color = Color(0xFFEF4444), radius = 9.dp.toPx(), center = pt)
                                }
                                drawCircle(color = Color(0xFFF97316).copy(alpha = 0.4f), radius = 6.dp.toPx(), center = pt)
                                drawCircle(color = Color(0xFFF59E0B), radius = 3.5.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = pt)
                            }
                        }

                        // X-axis day buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            labels.forEachIndexed { idx, lbl ->
                                val isSelected = selectedDayIdx == idx || (selectedDayIdx == null && idx == todayIndex)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDayIdx = if (selectedDayIdx == idx) null else idx
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFFEF4444) else Color.Transparent)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = lbl,
                                            color = if (isSelected) Color.White else TextGrey,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Wasted Time Metrics ─────────────────────────────────────────────────────
@Composable
private fun WastedMetricsCard(
    wastedData: List<Int>,
    todayIndex: Int,
    dayNames: List<String>,
    modifier: Modifier = Modifier
) {
    val totalMins = remember(wastedData) { wastedData.sum() }
    val avgMins = remember(wastedData) { if (wastedData.isNotEmpty()) wastedData.average().toInt() else 0 }
    val maxIdx = remember(wastedData) { wastedData.indices.maxByOrNull { wastedData[it] } ?: 0 }
    val maxDayName = dayNames.getOrElse(maxIdx) { "N/A" }
    val todayMins = wastedData.getOrElse(todayIndex) { 0 }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            title = "TOTAL WASTED",
            value = "${totalMins / 60}h ${totalMins % 60}m",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "DAILY AVG",
            value = "${avgMins} min",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "MOST WASTED",
            value = maxDayName.take(3),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "TODAY",
            value = "${todayMins} min",
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Daily Wasted Logging Card ───────────────────────────────────────────────
@Composable
private fun WastedLogInputCard(
    todayMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0825))
            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Log End of Day Wasted Time",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Record unproductive time to plot graph accurately",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Time display & quick adjust
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF130B30))
                    .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S WASTED TIME",
                        color = TextGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$todayMinutes",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "min (${todayMinutes / 60}h ${todayMinutes % 60}m)",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                // Steppers [-] [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF20133A))
                            .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(8.dp))
                            .clickable {
                                onMinutesChange((todayMinutes - 15).coerceAtLeast(0))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF20133A))
                            .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(8.dp))
                            .clickable {
                                onMinutesChange(todayMinutes + 15)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick increment chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    -30 to "-30m",
                    -15 to "-15m",
                    15 to "+15m",
                    30 to "+30m",
                    60 to "+1h"
                ).forEach { (delta, lbl) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF130B30))
                            .border(1.dp, Color(0xFF2E1C6A), RoundedCornerShape(8.dp))
                            .clickable {
                                onMinutesChange((todayMinutes + delta).coerceAtLeast(0))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = lbl, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save Today's Wasted Time button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEA580C), Color(0xFFDC2626))
                        )
                    )
                    .clickable { onSave() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Save Today's Wasted Time",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
