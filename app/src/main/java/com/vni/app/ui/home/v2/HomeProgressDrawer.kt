package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.North
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.DailyAssignedPlanTask
import com.vni.app.data.model.Task
import java.util.Calendar
import java.util.Locale

/**
 * Section D:
 * - Stepped top tab on the left housing the sliding pill toggle: `TODAY` vs `WEEK`.
 * - Shows 100% REAL data from current day and week's assigned/spent tasks:
 *   - 7-day Bar chart (M, T, W, T, F, S, S) reflecting real spent hours per day.
 *   - Current day highlighted accurately in purple in TODAY mode; past days white; future days muted.
 *   - Real Consistency % and Real Time logged.
 * - Draggable bottom handle with chevron to expand/collapse detailed breakdown.
 */
@Composable
fun HomeProgressDrawer(
    tasks: List<Task>,
    dailyAssignedTasks: List<DailyAssignedPlanTask>,
    todayProgressPct: Int,
    weekConsistencyPct: Int,
    todayMinutes: Int,
    weekTotalMinutes: Int,
    modifier: Modifier = Modifier
) {
    // Mode: "TODAY" or "WEEK"
    var selectedMode by remember { mutableStateOf("TODAY") }

    // Expanded state
    var isExpanded by remember { mutableStateOf(false) }

    val drawerHeight by animateDpAsState(
        targetValue = if (isExpanded) 400.dp else 230.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "DrawerHeight"
    )

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Format real time text cleanly without fake floors
    fun formatRealTime(mins: Int): String {
        val h = mins / 60
        val m = mins % 60
        return when {
            mins <= 0 -> "0 hr"
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "$h hr"
            else -> "$m min"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(drawerHeight)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    },
                    onDragEnd = {
                        if (dragOffsetY < -40f) {
                            isExpanded = true
                        } else if (dragOffsetY > 40f) {
                            isExpanded = false
                        }
                    }
                )
            }
    ) {
        CustomSteppedDrawerBackground(
            isExpanded = isExpanded,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ── Top Stepped Tab: Sliding Pill Toggle (`⚡ TODAY` vs `📊 WEEK`) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlidingTogglePill(
                    selectedMode = selectedMode,
                    todayProgressPct = todayProgressPct,
                    weekConsistencyPct = weekConsistencyPct,
                    onToggle = { newMode -> selectedMode = newMode }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Main Content: Real 7-Day Bar Chart on Left | Real Metrics on Right ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 7-Day Bar Chart with Y-axis markers from real dailyAssignedTasks
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D0621))
                        .border(1.dp, Color(0xFF261448), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    WeeklyBarChart(
                        dailyAssignedTasks = dailyAssignedTasks,
                        isTodayMode = (selectedMode == "TODAY")
                    )
                }

                // Right: Real Consistency & Real Time Metrics
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Metric 1: Consistency / Today Progress
                    Column {
                        if (selectedMode == "TODAY") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.North,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$todayProgressPct%",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "Today Done",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "$weekConsistencyPct %",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎯 ", fontSize = 10.sp)
                                Text(
                                    text = "Consistency",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Metric 2: Real Time Logged
                    Column {
                        val displayTime = if (selectedMode == "TODAY") {
                            formatRealTime(todayMinutes)
                        } else {
                            formatRealTime(weekTotalMinutes)
                        }
                        Text(
                            text = displayTime,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🕒 ", fontSize = 10.sp)
                            Text(
                                text = "Time",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Extra expanded details if dragged open ──
            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TODAY'S TASK BREAKDOWN",
                        color = Color(0xFFA855F7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks assigned for today",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    } else {
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF140A2E))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(task.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                val pct = if (task.targetMinutes > 0) ((task.todayMinutes.toFloat() / task.targetMinutes) * 100).toInt().coerceIn(0, 100) else 0
                                Text(
                                    text = "${task.todayMinutes}/${task.targetMinutes}m ($pct%)",
                                    color = Color(0xFFC084FC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Bottom Center Handle Notch with Chevron ──
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFF1C0D3A))
                    .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.7f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 22.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Sliding Pill Toggle with REAL Today % and REAL Week %
 */
@Composable
private fun SlidingTogglePill(
    selectedMode: String,
    todayProgressPct: Int,
    weekConsistencyPct: Int,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = (selectedMode == "TODAY")

    Box(
        modifier = modifier
            .width(204.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF13082B))
            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(14.dp))
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFFA855F7))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT: TODAY Button ──
            Box(
                modifier = Modifier
                    .weight(if (isToday) 1.55f else 1.0f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isToday) Color(0xFF241049) else Color.Transparent)
                    .clickable { onToggle("TODAY") }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "⚡", fontSize = 11.sp)
                        Text(
                            text = "TODAY",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$todayProgressPct %",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚡", fontSize = 11.sp)
                        Text(
                            text = "TODAY",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── RIGHT: WEEK Button ──
            Box(
                modifier = Modifier
                    .weight(if (!isToday) 1.55f else 1.0f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isToday) Color(0xFF241049) else Color.Transparent)
                    .clickable { onToggle("WEEK") }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$weekConsistencyPct %",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(text = "📊", fontSize = 11.sp)
                        Text(
                            text = "WEEK",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "📊", fontSize = 11.sp)
                        Text(
                            text = "WEEK",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real 7-Day Bar Chart (Mon -> Sun) for the current week.
 * - Uses real spent minutes from dailyAssignedTasks per day of this week.
 * - Highlights the actual current day in purple; past days white in TODAY mode; empty days show baseline stub.
 */
@Composable
private fun WeeklyBarChart(
    dailyAssignedTasks: List<DailyAssignedPlanTask>,
    isTodayMode: Boolean,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    // Compute Mon..Sun dateKeys of the current week and current day index (0=Mon .. 6=Sun)
    val weekInfo = remember(dailyAssignedTasks) {
        val now = Calendar.getInstance()
        val todayKey = String.format(
            Locale.ENGLISH,
            "%04d-%02d-%02d",
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH)
        )
        // Map Calendar.DAY_OF_WEEK (1=Sun, 2=Mon .. 7=Sat) -> index 0..6 (Mon=0 .. Sun=6)
        val todayMonIndex = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }

        val mondayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -todayMonIndex)
        }

        val daySpentMins = (0..6).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = mondayCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val key = String.format(
                Locale.ENGLISH,
                "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
            )
            if (key > todayKey) 0
            else dailyAssignedTasks.filter { it.dateKey == key }.sumOf { it.spentMinutes }
        }

        val maxMins = (daySpentMins.maxOrNull() ?: 0).coerceAtLeast(120)
        // Round top scale to clean hours (at least 2h, default 8h if > 6h)
        val scaleHours = when {
            maxMins > 360 -> ((maxMins + 59) / 60).coerceAtLeast(8)
            maxMins > 240 -> 6
            maxMins > 120 -> 4
            else -> 2
        }
        val scaleMins = scaleHours * 60f
        val ratios = daySpentMins.map { mins ->
            if (mins <= 0) 0.04f else (mins / scaleMins).coerceIn(0.08f, 1f)
        }
        Triple(todayMonIndex, ratios, scaleHours)
    }

    val (todayMonIndex, barRatios, scaleHours) = weekInfo
    val yLabels = remember(scaleHours) {
        listOf(
            "${scaleHours}h",
            "${(scaleHours * 3) / 4}h",
            "${scaleHours / 2}h",
            "${scaleHours / 4}h",
            "0h"
        )
    }

    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Bars area (7 bars with labels)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                barRatios.forEachIndexed { idx, ratio ->
                    val isCurrentDay = (idx == todayMonIndex)
                    val hasActivity = ratio > 0.05f
                    val barColor = when {
                        !hasActivity -> Color(0xFF261448)
                        isTodayMode -> if (isCurrentDay) Color(0xFFA855F7) else Color(0xFFF1F5F9)
                        else -> Color(0xFFA855F7)
                    }

                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(ratio)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        barColor,
                                        barColor.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Day labels row (M, T, W, T, F, S, S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { idx, label ->
                    val isToday = (idx == todayMonIndex)
                    Box(modifier = Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (isToday) Color(0xFFA855F7) else Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Dynamic Y-axis markers
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach { yl ->
                Text(
                    text = yl,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CustomSteppedDrawerBackground(
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, 22.dp.toPx())
            lineTo(0f, 6.dp.toPx())
            quadraticTo(0f, 0f, 8.dp.toPx(), 0f)
            lineTo(214.dp.toPx(), 0f)
            quadraticTo(222.dp.toPx(), 0f, 226.dp.toPx(), 12.dp.toPx())
            quadraticTo(230.dp.toPx(), 22.dp.toPx(), 242.dp.toPx(), 22.dp.toPx())
            lineTo(w - 18.dp.toPx(), 22.dp.toPx())
            quadraticTo(w, 22.dp.toPx(), w, 40.dp.toPx())
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = path,
            color = Color(0xFF09041A).copy(alpha = 0.95f)
        )

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFA855F7),
                    Color(0xFF7C3AED),
                    Color(0xFF6B21A8)
                )
            ),
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
