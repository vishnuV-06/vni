package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.Task
import com.vni.app.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import java.util.*

// Month-aware DayProgress ensuring dates in different months are strictly separate!
data class DayProgress(
    val year: Int,
    val month: Int, // 0-11 Calendar.MONTH
    val day: Int,
    val progressPct: Int // 0-100
)

/**
 * Interactive Calendar Panel matching Image 1:
 * - Dynamic red-to-green heatmap colors for every day.
 * - Highlights selected day in vibrant green.
 * - Displays attached bottom "Day Summary" card with 3 circular metric rings:
 *   1. Tasks Completed (e.g. 4 / 8, 50%)
 *   2. Progress (e.g. 48%)
 *   3. Spent Time (e.g. 38 min, 63%)
 * - "View Details >" pill button navigates to detailed breakdown.
 * - Individual daily description / journal text block below day summary.
 */
@Composable
fun TaskCalendarPanel(
    dayProgressList: List<DayProgress> = emptyList(),
    tasks: List<Task> = emptyList(),
    todayProgress: Int = 0,
    dayNotesMap: Map<String, String> = emptyMap(),
    onSaveDayNote: (year: Int, month: Int, day: Int, note: String) -> Unit = { _, _, _, _ -> },
    onViewDetails: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val actualNow = Calendar.getInstance()
    var currentYear  by remember { mutableIntStateOf(actualNow.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(actualNow.get(Calendar.MONTH)) }
    val isViewingActualMonth = (actualNow.get(Calendar.MONTH) == currentMonth && actualNow.get(Calendar.YEAR) == currentYear)
    val todayNumber = if (isViewingActualMonth) actualNow.get(Calendar.DAY_OF_MONTH) else -1

    var selectedYear by remember { mutableIntStateOf(actualNow.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(actualNow.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(actualNow.get(Calendar.DAY_OF_MONTH)) }

    val selectedKey = "$selectedYear-$selectedMonth-$selectedDay"
    var noteInputText by remember(selectedKey, dayNotesMap) {
        mutableStateOf(dayNotesMap[selectedKey] ?: "")
    }

    val monthNames = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

    // Accurate mapping: keyed by "year-month-day" so NO OTHER MONTH ever shares progress!
    val progressMap = remember(dayProgressList) {
        dayProgressList.associateBy { "${it.year}-${it.month}-${it.day}" }
    }

    // Calendar math
    val cal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevCal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1); add(Calendar.MONTH, -1) }
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 42 cells (6 rows x 7 cols)
    val cells = buildList {
        repeat(firstDayOfWeek) { add(daysInPrevMonth - firstDayOfWeek + 1 + it to false) }
        repeat(daysInMonth) { add(it + 1 to true) }
        val remaining = 42 - size
        repeat(remaining) { add(it + 1 to false) }
    }

    // Red-to-Green Heatmap curve matching Image 1
    fun getCalendarProgressColor(pct: Int): Color = when {
        pct <= 0   -> Color.Transparent
        pct <= 25  -> Color(0xFFDC2626) // Deep Red
        pct <= 50  -> Color(0xFFEA580C) // Orange
        pct <= 75  -> Color(0xFFCA8A04) // Amber / Yellow
        else       -> Color(0xFF16A34A) // Green
    }

    // Calculate metrics for selected day
    val selectedCellKey = "$selectedYear-$selectedMonth-$selectedDay"
    val savedProg = progressMap[selectedCellKey]?.progressPct ?: 0
    val isTodaySelected = selectedDay == actualNow.get(Calendar.DAY_OF_MONTH) && selectedMonth == actualNow.get(Calendar.MONTH) && selectedYear == actualNow.get(Calendar.YEAR)

    val primaryTasks = tasks.filter { it.daysLeft > 0 && it.isPrimary }
    val liveCompletedCount = primaryTasks.count { it.completedToday }
    val liveTotalTasks = primaryTasks.size
    val liveSpentMins = primaryTasks.sumOf { it.todayMinutes }
    val liveTargetMins = primaryTasks.sumOf { it.targetMinutes }.coerceAtLeast(1)

    val completedCount: Int
    val totalTasks: Int
    val completedPct: Int
    val displayProgressPct: Int
    val spentMinutes: Int
    val spentPct: Int

    if (isTodaySelected) {
        completedCount = liveCompletedCount
        totalTasks = liveTotalTasks
        completedPct = if (totalTasks > 0) ((completedCount.toFloat() / totalTasks) * 100).toInt().coerceIn(0, 100) else 0
        displayProgressPct = todayProgress
        spentMinutes = liveSpentMins
        spentPct = ((spentMinutes.toFloat() / liveTargetMins) * 100).toInt().coerceIn(0, 100)
    } else if (savedProg > 0) {
        displayProgressPct = savedProg
        totalTasks = primaryTasks.size.takeIf { it > 0 } ?: 6
        completedCount = ((savedProg / 100f) * totalTasks).toInt().coerceIn(0, totalTasks)
        completedPct = if (totalTasks > 0) ((completedCount.toFloat() / totalTasks) * 100).toInt() else 0
        spentMinutes = (savedProg * 0.75f).toInt()
        spentPct = savedProg.coerceIn(0, 100)
    } else {
        completedCount = 0
        totalTasks = primaryTasks.size
        completedPct = 0
        displayProgressPct = 0
        spentMinutes = 0
        spentPct = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f))
            .clickable(onClick = onDismiss)
            .zIndex(65f)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0C071E))
                .border(2.dp, PurplePrimary, RoundedCornerShape(26.dp))
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header Row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Month
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C1040))
                            .border(1.dp, PurplePrimary, RoundedCornerShape(10.dp))
                            .clickable {
                                if (currentMonth == 0) {
                                    currentMonth = 11; currentYear--
                                } else currentMonth--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ChevronLeft, null, tint = TextWhite, modifier = Modifier.size(20.dp))
                    }

                    Text(
                        text = monthNames[currentMonth],
                        color = TextWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    // Next Month
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C1040))
                            .border(1.dp, PurplePrimary, RoundedCornerShape(10.dp))
                            .clickable {
                                if (currentMonth == 11) {
                                    currentMonth = 0; currentYear++
                                } else currentMonth++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ChevronRight, null, tint = TextWhite, modifier = Modifier.size(20.dp))
                    }

                    Text(
                        text = currentYear.toString(),
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Close
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, TextMuted.copy(0.3f), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Day of Week Headers ──────────────────────────────────────
                val dayHeaders = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
                Row(Modifier.fillMaxWidth()) {
                    dayHeaders.forEachIndexed { idx, d ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = d,
                                    color = if (idx == 0) PurplePrimary else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (idx == 0) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(Modifier.width(14.dp).height(2.dp).background(PurplePrimary))
                                } else {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Calendar Grid (Heatmap & Selection) ──────────────────────
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { (day, isCurrentMonthDay) ->
                            val cellKey = "$currentYear-$currentMonth-$day"
                            val isToday = isCurrentMonthDay && day == todayNumber && isViewingActualMonth
                            val prog = if (isToday) todayProgress else if (isCurrentMonthDay) progressMap[cellKey]?.progressPct ?: 0 else 0
                            val isSelected = isCurrentMonthDay && day == selectedDay && currentMonth == selectedMonth && currentYear == selectedYear
                            val heatColor = getCalendarProgressColor(prog)

                            val cellBg = when {
                                isToday    -> Color.Transparent // Violet border with no colour
                                isSelected -> Color(0xFF22C55E) // Bright green pill only when selected in this month
                                prog > 0   -> heatColor.copy(alpha = 0.85f)
                                else       -> Color(0xFF130B30)
                            }

                            val cellBorderColor = when {
                                isToday    -> Color(0xFFA855F7) // Violet border
                                isSelected -> Color.White.copy(alpha = 0.9f)
                                prog > 0   -> heatColor
                                else       -> Color(0xFF26194E)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.5.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(cellBg)
                                    .border(
                                        width = if (isToday || isSelected) 2.dp else 0.5.dp,
                                        color = cellBorderColor,
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                    .clickable(enabled = isCurrentMonthDay) {
                                        selectedDay = day
                                        selectedMonth = currentMonth
                                        selectedYear = currentYear
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        !isCurrentMonthDay -> TextMuted.copy(0.25f)
                                        isToday           -> Color.White
                                        isSelected        -> Color.White
                                        prog > 0           -> Color.White
                                        else               -> TextWhite
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isToday || isSelected || prog > 0) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Attached Day Summary Card (Image 1) ──────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F092B))
                        .border(1.dp, Color(0xFF2E1A5C), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        // Title row: Date, "Day Summary" on left | "View Details >" on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val monthNameCapitalized = monthNames[currentMonth].lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                Text(
                                    text = "$selectedDay $monthNameCapitalized $currentYear",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Day Summary",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // "View Details >" pill button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF25134F))
                                    .border(1.dp, PurplePrimary, RoundedCornerShape(16.dp))
                                    .clickable { onViewDetails() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View Details >",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // 3 Metric Cards side by side matching Image 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Tasks Completed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF090518))
                                    .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Tasks\nCompleted",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "$completedCount",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " / $totalTasks",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                        MetricCircularRing(
                                            progressPct = completedPct,
                                            strokeColor = Color(0xFF22C55E)
                                        )
                                    }
                                }
                            }

                            // 2. Progress
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF090518))
                                    .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PurplePrimary))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Progress",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$displayProgressPct%",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        MetricCircularRing(
                                            progressPct = displayProgressPct,
                                            strokeColor = PurplePrimary
                                        )
                                    }
                                }
                            }

                            // 3. Spent Time
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF090518))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Spent\nTime",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "$spentMinutes",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " min",
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp
                                            )
                                        }
                                        MetricCircularRing(
                                            progressPct = spentPct,
                                            strokeColor = Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Bottom Handle Line
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38236B))
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Individual Daily Journal / Activity Text Block (Requirement 3) ──
                val selectedMonthNameCap = monthNames[selectedMonth].lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F092B))
                        .border(1.dp, Color(0xFF2E1A5C), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "DAY JOURNAL / LOG",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "$selectedDay $selectedMonthNameCap $selectedYear",
                                color = Color(0xFFA78BFA),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Describe what you did on this day:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 76.dp, max = 130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF160C3B))
                                .border(1.dp, Color(0xFF38236B), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = noteInputText,
                                onValueChange = {
                                    noteInputText = it
                                    onSaveDayNote(selectedYear, selectedMonth, selectedDay, it)
                                },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                cursorBrush = SolidColor(PurplePrimary),
                                decorationBox = { innerTextField ->
                                    if (noteInputText.isEmpty()) {
                                        Text(
                                            text = "Enter what you did on $selectedDay $selectedMonthNameCap (e.g. solved practice questions, workout, completed assignments)...",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCircularRing(
    progressPct: Int,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(34.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            // Track
            drawArc(
                color = Color(0xFF1E143E),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke)
            )
            // Progress
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = 360f * (progressPct.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
            )
        }
        Text(
            text = "$progressPct%",
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
