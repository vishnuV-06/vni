package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.data.model.Task
import com.vni.app.ui.theme.*

/**
 * The active (expanded) task card shown in the scroll list.
 * Single click on 4 time bars opens the TimeSliderBottomCard!
 */
@Composable
fun ActiveTaskCard(
    task: Task,
    onOpenTimeSlider: () -> Unit,
    showGlobalTaskProgress: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cal = remember { java.util.Calendar.getInstance() }
    val dayOfWeekIndex = (cal.get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6) // Sunday = 0
    val isScheduledToday = task.isScheduledToday(dayOfWeekIndex)

    val changeText  = task.getAvgTimeText(dayOfWeekIndex)
    val isDecreased = task.isAvgTimeDecreased(dayOfWeekIndex)
    val changeColor = if (isDecreased) DangerRed else SuccessGreen
    val fullyCompletedDisplay = task.getFullyCompletedDisplay()

    val currentProgress = (task.todayMinutes.toFloat() / task.targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val currentMins     = task.todayMinutes

    // Red-to-green progress color for easter egg / today progress toggle
    val progressColor = getTaskProgressColor(currentProgress)

    val borderColor = when {
        showGlobalTaskProgress -> progressColor
        !isScheduledToday -> Color(0xFF261947)
        else -> PurplePrimary
    }

    val fillColor = when {
        showGlobalTaskProgress -> progressColor.copy(alpha = 0.28f)
        !isScheduledToday -> Color(0xFF110B22)
        else -> PurplePrimary.copy(alpha = 0.18f)
    }

    val cardBackgroundColors = if (!task.isPrimary) {
        listOf(Color(0xFF0C1322), Color(0xFF131D33))
    } else if (!isScheduledToday) {
        listOf(Color(0xFF080512), Color(0xFF0F0A1D)) // Black/dark appearance on inactive days
    } else {
        listOf(Color(0xFF1C1040), Color(0xFF221555))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
            .clip(RoundedCornerShape(16.dp))
            .background(brush = Brush.horizontalGradient(cardBackgroundColors))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = if (showGlobalTaskProgress) {
                        listOf(progressColor, progressColor.copy(alpha = 0.7f))
                    } else if (!task.isPrimary) {
                        listOf(Color(0xFF384B66), Color(0xFF253347))
                    } else if (!isScheduledToday) {
                        listOf(Color(0xFF2E1C55), Color(0xFF1E1438))
                    } else {
                        listOf(PurplePrimary, PurpleGlow)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Progress fill inside the card
        if (currentProgress > 0f) {
            Row(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(currentProgress.coerceIn(0.001f, 1f))
                        .background(fillColor)
                )
                if (currentProgress < 1f) {
                    Spacer(modifier = Modifier.weight((1f - currentProgress).coerceAtLeast(0.001f)))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left – task name + duration
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    if (!task.isPrimary) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(0.8.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GENERAL",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextGrey,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$currentMins min / ${task.targetMinutes} min",
                        fontSize = 13.sp,
                        color = if (showGlobalTaskProgress) progressColor else TextGrey,
                        fontWeight = if (showGlobalTaskProgress) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Right – avg stats + 4 bars
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "fully completed: $fullyCompletedDisplay",
                    fontSize = 10.sp,
                    color = TextGrey,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Avg time: $changeText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
                Spacer(Modifier.height(6.dp))

                // 4 time-segment bars — SINGLE CLICK opens time slider panel!
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onOpenTimeSlider()
                        }
                        .padding(2.dp)
                ) {
                    FourTimeBars(
                        progress = currentProgress,
                        overrideColor = if (showGlobalTaskProgress) progressColor else null
                    )
                }
            }
        }
    }
}

/**
 * 4 purple filled bars showing time completion segments (each = 25% of target).
 */
@Composable
fun FourTimeBars(
    progress: Float,
    overrideColor: Color? = null
) {
    val filledBars = (progress * 4).toInt().coerceIn(0, 4)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { i ->
            val isFilled = i < filledBars
            val barColor = if (isFilled) {
                overrideColor ?: PurplePrimary
            } else {
                PurpleFaint
            }
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(18.dp)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (isFilled) (overrideColor ?: PurpleBorder) else PurpleBorder.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * Helper to compute red-to-green color across 0-100% completion
 */
fun getTaskProgressColor(progress: Float): Color {
    return when {
        progress <= 0.20f -> Color(0xFFEF4444) // Red
        progress <= 0.40f -> Color(0xFFF97316) // Orange
        progress <= 0.60f -> Color(0xFFEAB308) // Yellow
        progress <= 0.80f -> Color(0xFF84CC16) // Light Green
        else              -> Color(0xFF22C55E) // Full Green
    }
}
