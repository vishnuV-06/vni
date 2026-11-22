package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Task

/**
 * Section C:
 * - Active task card (expanded, glowing violet border, vertical accent, 7 dots for 7 days,
 *   logged/target time, last completed, avg time, and 4 segmented progress bars).
 * - Inactive tasks (compact sleek card with 6-dot drag handle, square box filled with violet liquid
 *   matching completion %, task name, and chevron right).
 * - Right screen edge sidebar pull tab.
 */
@Composable
fun HomeTasksSection(
    tasks: List<Task>,
    activeIndex: Int,
    onSelectActiveTask: (Int) -> Unit,
    onSidebarOpen: () -> Unit,
    onOpenTaskEdit: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTask = tasks.getOrNull(activeIndex) ?: tasks.firstOrNull()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount > 25f && scrollState.value == 0) {
                        change.consume()
                        onOpenSearch()
                    }
                }
            }
    ) {
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF110829))
                    .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No tasks assigned for today",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open Plan from the sidebar to assign tasks",
                        color = Color(0xFFA78BFA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val safeActiveIndex = activeIndex.coerceIn(0, (tasks.size - 1).coerceAtLeast(0))
                tasks.forEachIndexed { index, task ->
                    if (index == safeActiveIndex) {
                        ActiveTaskItemCard(
                            task = task,
                            onClick = onOpenTaskEdit
                        )
                    } else {
                        InactiveTaskItemCard(
                            task = task,
                            onClick = { onSelectActiveTask(index) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top active task card matching Image 1 & 2:
 * - Neon purple glowing border & soft inner shadow
 * - Vertical accent bar
 * - Task Title
 * - 7 dots for last 7 days (glows bright purple if logged > 0 min)
 * - 🕒 20 min / 30 min
 * - Last completed: None
 * - Avg time: 28 m
 * - 4 segmented progress block bars
 */
@Composable
private fun ActiveTaskItemCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressPct = if (task.targetMinutes > 0) {
        ((task.todayMinutes.toFloat() / task.targetMinutes) * 100).toInt().coerceIn(0, 100)
    } else 0

    // 4 quarters: 25%, 50%, 75%, 100%
    val filledBars = when {
        progressPct >= 90 -> 4
        progressPct >= 65 -> 3
        progressPct >= 35 -> 2
        progressPct >= 10 -> 1
        else -> 0
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF110829))
            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0xFFA855F7))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content: Accent bar + Title with 7 dots + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Vertical glowing accent bar
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE879F9), Color(0xFF8B5CF6))
                            )
                        )
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    // Task Name + 7 Dots
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = task.name,
                            color = Color.White,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )

                        // 7 Dots for last 7 days (glows bright purple if logged > 0 min)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val loggedDays = task.weekLoggedMinutes
                            val todayDow = remember {
                                (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
                            }
                            for (dayIdx in 0..6) {
                                val minutesLogged = loggedDays.getOrElse(dayIdx) { 0 }
                                val isGlow = minutesLogged > 0 || (dayIdx == todayDow && task.todayMinutes > 0)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isGlow) Color(0xFFA855F7) else Color(0xFF2E1A52))
                                        .then(
                                            if (isGlow) {
                                                Modifier.shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0xFFA855F7))
                                            } else Modifier
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Time logged vs target: 🕒 20 min / 30 min
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🕒 ",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${task.todayMinutes} min / ${task.targetMinutes} min",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right Content: Last completed, Avg time, 4 Segmented progress bars
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Last completed: ${task.lastCompletedDateText.ifBlank { "None" }}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Avg time: ",
                        color = Color(0xFFC084FC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val displayAvg = if (task.avgMinutes > 0) task.avgMinutes
                    else {
                        val pos = task.weekLoggedMinutes.filter { it > 0 }
                        if (pos.isNotEmpty()) pos.average().toInt() else task.todayMinutes
                    }
                    Text(
                        text = "$displayAvg m",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 4 Segmented Progress Bars (filled with violet) - only this opens the time bars panel!
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClick() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val isFilled = i <= filledBars
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isFilled) Color(0xFFA855F7) else Color(0xFF261548))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inactive task card matching Image 1 & 2:
 * - 6-dot drag handle ⠿
 * - Square icon filled with violet liquid up to completion %
 * - Task Name
 * - Chevron right >
 */
@Composable
private fun InactiveTaskItemCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (task.targetMinutes > 0) {
        (task.todayMinutes.toFloat() / task.targetMinutes).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F0824))
            .border(1.dp, Color(0xFF261548), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 6-dot drag handle ⠿
                DragHandle6Dots()

                // Square Liquid Fill Indicator
                LiquidSquareProgress(progressFraction = progressFraction)

                // Task Name
                Text(
                    text = task.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }

            // Right Chevron >
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Details",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 6-dot drag handle (2 columns x 3 dots)
 */
@Composable
private fun DragHandle6Dots(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 10.dp, height = 16.dp)) {
        val radius = 1.6.dp.toPx()
        val color = Color(0xFF64748B)

        val col1X = size.width * 0.25f
        val col2X = size.width * 0.75f

        val row1Y = size.height * 0.20f
        val row2Y = size.height * 0.50f
        val row3Y = size.height * 0.80f

        drawCircle(color, radius, Offset(col1X, row1Y))
        drawCircle(color, radius, Offset(col2X, row1Y))

        drawCircle(color, radius, Offset(col1X, row2Y))
        drawCircle(color, radius, Offset(col2X, row2Y))

        drawCircle(color, radius, Offset(col1X, row3Y))
        drawCircle(color, radius, Offset(col2X, row3Y))
    }
}

/**
 * Square box with rounded corners, filled from the bottom up with violet liquid
 * according to progress percentage (as seen in Image 1: App Develop ~75%, Learn Course ~35%).
 */
@Composable
private fun LiquidSquareProgress(
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF160B30))
            .border(1.dp, Color(0xFF3B1E6D), RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height

        // Calculate fill height based on progress (minimum slight glow if > 0)
        val fillH = if (progressFraction > 0f) {
            (h * progressFraction).coerceAtLeast(4.dp.toPx())
        } else 0f

        if (fillH > 0f) {
            val fillTop = h - fillH
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFA855F7), // Bright top meniscus
                        Color(0xFF7C3AED)  // Solid violet base
                    ),
                    startY = fillTop,
                    endY = h
                ),
                topLeft = Offset(0f, fillTop),
                size = Size(w, fillH)
            )

            // Meniscus line
            drawLine(
                color = Color(0xFFE879F9),
                start = Offset(0f, fillTop),
                end = Offset(w, fillTop),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
