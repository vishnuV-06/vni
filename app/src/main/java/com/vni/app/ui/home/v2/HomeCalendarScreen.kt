package com.vni.app.ui.home.v2

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Event
import com.vni.app.data.model.CalendarEvent
import com.vni.app.HistoryItem
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.vni.app.data.model.DailyAssignedPlanTask
import com.vni.app.data.model.MotiveHeading
import com.vni.app.data.model.Notice
import com.vni.app.data.model.Task
import com.vni.app.ui.home.DayProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Calendar Screen V2 with 100% REAL per-day data:
 * - Each day calculates real progress from that day's assigned Plan tasks (spent / target time).
 * - Top card counter shows "01 / 02" (completed tasks / total assigned tasks on that day).
 * - Right card shows ONLY really completed items on that day (completed tasks, Do items, Notices/Reminders) - blank if none!
 * - Bottom day note is individual per day and completely blank if nothing was written.
 * - Clicking the Date block in Section B toggles Calendar closed back to Home.
 */
@Composable
fun HomeCalendarScreen(
    tasks: List<Task>,
    dailyAssignedTasks: List<DailyAssignedPlanTask>,
    completedHistory: List<HistoryItem>,
    notices: List<Notice>,
    motiveHeadings: List<MotiveHeading>,
    dayProgressList: List<DayProgress>,
    dayNotesMap: Map<String, String>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveDayNote: (year: Int, month: Int, day: Int, note: String) -> Unit,
    calendarEvents: List<CalendarEvent> = emptyList(),
    onAddEvent: (dateKey: String, title: String, timeStr: String) -> Unit = { _, _, _ -> },
    onDeleteEvent: (Long) -> Unit = {},
    onOpenNotices: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val actualNow = remember { Calendar.getInstance() }
    val todayYear = actualNow.get(Calendar.YEAR)
    val todayMonth = actualNow.get(Calendar.MONTH) // 0-indexed
    val todayDay = actualNow.get(Calendar.DAY_OF_MONTH)
    val todayDateKey = remember {
        String.format(Locale.ENGLISH, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)
    }

    var displayYear by remember { mutableIntStateOf(todayYear) }
    var displayMonth by remember { mutableIntStateOf(todayMonth) }

    var selectedDay by remember { mutableIntStateOf(todayDay) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedYear by remember { mutableIntStateOf(todayYear) }

    val selectedNoteKey = "$selectedYear-$selectedMonth-$selectedDay"
    val selectedDateKey = String.format(
        Locale.ENGLISH,
        "%04d-%02d-%02d",
        selectedYear,
        selectedMonth + 1,
        selectedDay
    )

    var dayNoteText by remember(selectedNoteKey, dayNotesMap) {
        mutableStateOf(dayNotesMap[selectedNoteKey] ?: "")
    }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var targetEventDateKey by remember { mutableStateOf("") }

    val monthCal = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
    }
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthHeaderStr = remember(displayYear, displayMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(monthCal.time).uppercase()
    }

    val prevCal = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply { set(displayYear, displayMonth, 1); add(Calendar.MONTH, -1) }
    }
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Pre-group Plan tasks and Completed History by yyyy-MM-dd for fast real per-day lookup
    val planTasksByDate = remember(dailyAssignedTasks) {
        dailyAssignedTasks.groupBy { it.dateKey }
    }
    val historyByDate = remember(completedHistory) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        completedHistory.groupBy { item ->
            val time = if (item.dateMillis > 0L) item.dateMillis else item.id
            fmt.format(Date(time))
        }
    }

    // Real metrics for the currently selected day
    val dayPlannedTasks = remember(selectedDateKey, planTasksByDate) {
        planTasksByDate[selectedDateKey].orEmpty()
    }
    val totalTasksCount = dayPlannedTasks.size
    val completedTasksCount = dayPlannedTasks.count { it.durationMinutes > 0 && it.spentMinutes >= it.durationMinutes }
    val totalTargetMins = dayPlannedTasks.sumOf { it.durationMinutes }
    val totalSpentMins = dayPlannedTasks.sumOf { it.spentMinutes.coerceAtMost(it.durationMinutes) }

    val progressMap = remember(dayProgressList) {
        dayProgressList.associateBy { "${it.year}-${it.month}-${it.day}" }
    }
    val donePct = remember(totalTargetMins, totalSpentMins, progressMap, selectedNoteKey) {
        if (totalTargetMins > 0) {
            ((totalSpentMins.toFloat() / totalTargetMins) * 100).toInt().coerceIn(0, 100)
        } else {
            progressMap[selectedNoteKey]?.progressPct ?: 0
        }
    }

    // Real completed items for the selected day (completed tasks, Do items, Notices/Reminders)
    val completedWorkList = remember(selectedDateKey, dayPlannedTasks, historyByDate) {
        val list = mutableListOf<String>()
        // 1. Completed tasks from Plan for this day
        dayPlannedTasks
            .filter { it.durationMinutes > 0 && it.spentMinutes >= it.durationMinutes }
            .forEach { t ->
                list.add("${t.taskName} (${t.spentMinutes}m)")
            }
        // 2. Completed Do items and Notices/Reminders for this day
        historyByDate[selectedDateKey].orEmpty().forEach { h ->
            val tag = if (h.type == "Done") "Do" else "Notice"
            list.add("${h.title} ($tag)")
        }
        list
    }

    val actualNoticesCount = notices.count { it.iconName != "School" && it.durationMinutes == null && it.reminderTriggerMillis == null }
    val actualRemindersCount = notices.count { it.iconName == "School" || it.durationMinutes != null || it.reminderTriggerMillis != null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Section A: Top Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            // ── Section B: Date Block (tapping again closes Calendar -> Home) & Real Counters ──
            HomeDateAndCountersBar(
                tasksCount = tasks.size,
                noticesCount = actualNoticesCount,
                remindersCount = actualRemindersCount,
                activeSectionType = "CALENDAR",
                onClickDateBlock = onDismiss,
                onClickNoticeBlock = {
                    onDismiss()
                    onOpenNotices()
                }
            )

            Spacer(Modifier.height(8.dp))

            // ── Calendar Container ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month Header Navigation (< NOVEMBER 2026 >)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A0D38))
                                .clickable {
                                    if (displayMonth == 0) {
                                        displayMonth = 11
                                        displayYear -= 1
                                    } else {
                                        displayMonth -= 1
                                    }
                                    selectedYear = displayYear
                                    selectedMonth = displayMonth
                                    selectedDay = 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev Month", tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = monthHeaderStr,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A0D38))
                                .clickable {
                                    if (displayMonth == 11) {
                                        displayMonth = 0
                                        displayYear += 1
                                    } else {
                                        displayMonth += 1
                                    }
                                    selectedYear = displayYear
                                    selectedMonth = displayMonth
                                    selectedDay = 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month", tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                        }
                    }

                    // 1. Day of Week Header Row: SU, MO, TU, WE, TH, FR, SA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEachIndexed { idx, dayName ->
                            if (idx == 0) {
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2E1065))
                                        .border(1.2.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayName,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayName,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // 2. Real Calendar Grid (5 or 6 rows depending on month length & start offset)
                    val leadingSlots = firstDayOfWeek
                    val rowCount = if (leadingSlots + daysInMonth > 35) 6 else 5
                    var dayCounter = 1

                    for (row in 0 until rowCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0 until 7) {
                                val cellIdx = row * 7 + col
                                when {
                                    cellIdx < leadingSlots -> {
                                        val prevDayNum = daysInPrevMonth - (leadingSlots - 1 - cellIdx)
                                        DayCellBox(
                                            text = prevDayNum.toString(),
                                            bgColor = Color(0xFF13092A).copy(alpha = 0.5f),
                                            textColor = Color(0xFF475569),
                                            isSelected = false,
                                            onClick = {}
                                        )
                                    }
                                    dayCounter <= daysInMonth -> {
                                        val thisDay = dayCounter
                                        val cellDateKey = String.format(
                                            Locale.ENGLISH,
                                            "%04d-%02d-%02d",
                                            displayYear,
                                            displayMonth + 1,
                                            thisDay
                                        )
                                        val isSelected = (displayYear == selectedYear && displayMonth == selectedMonth && thisDay == selectedDay)
                                        val cellPlanned = planTasksByDate[cellDateKey].orEmpty()
                                        val cellTarget = cellPlanned.sumOf { it.durationMinutes }
                                        val cellSpent = cellPlanned.sumOf { it.spentMinutes.coerceAtMost(it.durationMinutes) }
                                        val cellPct = if (cellTarget > 0) ((cellSpent.toFloat() / cellTarget) * 100).toInt() else -1
                                        val isTodayCell = (cellDateKey == todayDateKey)

                                        val cellBg = when {
                                            cellPct >= 80 -> Color(0xFFEA580C) // Completed high %
                                            cellPct in 1..79 -> Color(0xFF4C1D95) // Partial progress
                                            cellPct == 0 && cellDateKey < todayDateKey -> Color(0xFFE11D48) // Missed past tasks
                                            cellPlanned.isNotEmpty() -> Color(0xFF241049) // Assigned upcoming/today
                                            isTodayCell -> Color(0xFF2A1459) // Today highlight
                                            else -> Color(0xFF160A33) // Clean neutral day
                                        }

                                        DayCellBox(
                                            text = thisDay.toString(),
                                            bgColor = cellBg,
                                            textColor = Color.White,
                                            isSelected = isSelected,
                                            hasBorderGlow = isSelected,
                                            onClick = {
                                                selectedYear = displayYear
                                                selectedMonth = displayMonth
                                                selectedDay = thisDay
                                            },
                                            onLongClick = {
                                                selectedYear = displayYear
                                                selectedMonth = displayMonth
                                                selectedDay = thisDay
                                                targetEventDateKey = cellDateKey
                                                showAddEventDialog = true
                                            }
                                        )
                                        dayCounter++
                                    }
                                    else -> {
                                        Spacer(Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Day Details Section (Real Per-Day Metrics & Completed Items) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Left Card: Tasks Done Ratio (01 / 02), Time Spent/Total, & Done %
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(155.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.3.dp, Color(0xFF6B21A8), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Circular Ring + "01 / 02 TASKS DONE"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Canvas(modifier = Modifier.size(46.dp)) {
                                val strokeWidth = 5.dp.toPx()
                                drawCircle(
                                    color = Color(0xFF2A1058),
                                    style = Stroke(strokeWidth)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFFA855F7))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = (donePct / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.ENGLISH, "%02d", completedTasksCount),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = String.format(Locale.ENGLISH, " / %02d", totalTasksCount),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${totalSpentMins}m / ${totalTargetMins}m",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }

                        // Bottom Row: "$donePct% DONE" + Progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$donePct%",
                                    color = Color(0xFFE879F9),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "DONE",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF200C45))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(donePct / 100f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // 2. Right Card: Day's Events & Real Completed Work (Requirement 7)
                val dayEvents = remember(selectedDateKey, calendarEvents) {
                    calendarEvents.filter { it.dateKey == selectedDateKey }
                }

                Box(
                    modifier = Modifier
                        .weight(1.05f)
                        .height(155.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.3.dp, Color(0xFF6B21A8), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (dayEvents.isNotEmpty() || completedWorkList.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Events first with calendar badge
                            dayEvents.forEach { ev ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Event,
                                            contentDescription = null,
                                            tint = Color(0xFFE879F9),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${ev.title}${if (ev.timeStr.isNotBlank()) " (${ev.timeStr})" else ""}",
                                            color = Color(0xFFF0ABFC),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete Event",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteEvent(ev.id) }
                                    )
                                }
                            }

                            // Completed Tasks
                            completedWorkList.forEach { itemTitle ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFA855F7))
                                    )
                                    Text(
                                        text = itemTitle,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No events or work.\nHold a day 1s to add event",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Bottom Card: Individual Day Note Block (Blank if none written!) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.3.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0xFFA855F7))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF220E49))
                            .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Day Note",
                            tint = Color(0xFFE879F9),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    BasicTextField(
                        value = dayNoteText,
                        onValueChange = { newText ->
                            dayNoteText = newText
                            onSaveDayNote(selectedYear, selectedMonth, selectedDay, newText)
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                        decorationBox = { innerTextField ->
                            if (dayNoteText.isEmpty()) {
                                Text(
                                    text = "",
                                    color = Color.Transparent,
                                    fontSize = 13.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        // ── Add Event Dialog (Triggered by holding a day for 1s per Requirement 7) ──
        if (showAddEventDialog) {
            var eventTitleInput by remember { mutableStateOf("") }
            var eventTimeInput by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showAddEventDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Add Calendar Event", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                        Text("Date: $targetEventDateKey", color = Color(0xFFC084FC), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)

                        Text("Event Title", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = eventTitleInput,
                                onValueChange = { eventTitleInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp),
                                decorationBox = { inner ->
                                    if (eventTitleInput.isEmpty()) {
                                        Text("e.g. Exam, Birthday, Doctor Appointment", color = Color(0xFF64748B), fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )
                        }

                        Text("Time (Optional)", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = eventTimeInput,
                                onValueChange = { eventTimeInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp),
                                decorationBox = { inner ->
                                    if (eventTimeInput.isEmpty()) {
                                        Text("e.g. 10:00 AM", color = Color(0xFF64748B), fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddEventDialog = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val clean = eventTitleInput.trim()
                                    if (clean.isNotBlank()) {
                                        onAddEvent(targetEventDateKey, clean, eventTimeInput.trim())
                                        showAddEventDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("Save Event", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCellBox(
    text: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    hasBorderGlow: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (hasBorderGlow) {
                    Modifier
                        .border(2.dp, Color(0xFFA855F7), RoundedCornerShape(10.dp))
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp), spotColor = Color(0xFFA855F7))
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
        )
    }
}
