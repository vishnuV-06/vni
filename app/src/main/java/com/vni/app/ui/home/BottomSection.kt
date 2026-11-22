package com.vni.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vni.app.ui.theme.*

/**
 * Bottom section: two cards side by side with perfectly matched height and alignment.
 * Left: today progress segmented bar + % completed.
 * Right: last 7 days bar chart + avg change.
 */
@Composable
fun BottomSection(
    todayProgress: Int,
    last7Days: List<Int>,
    yesterdayProgress: Int = 45,
    onTodayProgressClick: () -> Unit = {},
    onProgressSectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cal = remember { java.util.Calendar.getInstance() }
    val todayDayOfWeekIndex = (cal.get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6) // Sunday = 0

    val diffFromYesterday = todayProgress - yesterdayProgress
    val isTodayIncreased = diffFromYesterday >= 0

    // 7 bars represent Sunday (index 0) to Saturday (index 6)
    // The bar according to the present day of week glows and modifies dynamically!
    // If there is no data for a day it is zero at that day!
    val currentWeekBars: List<Int> = remember(last7Days, todayProgress, todayDayOfWeekIndex) {
        val bars = last7Days.toMutableList()
        while (bars.size < 7) bars.add(0)
        bars[todayDayOfWeekIndex] = todayProgress
        bars.take(7)
    }

    // Today's effect on weekly progress average: compare today against past elapsed days of this week
    val pastElapsedDays = (0 until todayDayOfWeekIndex).map { currentWeekBars.getOrElse(it) { 0 } }
    val otherDaysAvg: Int = if (pastElapsedDays.isNotEmpty()) pastElapsedDays.map { it.toDouble() }.average().toInt() else yesterdayProgress
    val todayDiff = todayProgress - otherDaysAvg
    val isSevenDayIncreased = todayDiff >= 0
    val displayDiffPct = kotlin.math.abs(todayDiff)

    // Sunday to present day weekly average
    val daysElapsed = (todayDayOfWeekIndex + 1).coerceAtLeast(1)
    val totalPct: Int = currentWeekBars.take(daysElapsed).map { it.toDouble() }.average().toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(162.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today Progress card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SurfaceNavy, RoundedCornerShape(16.dp))
                .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                .clickable { onTodayProgressClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TODAY PROGRESS",
                    fontSize = 9.sp,
                    color = TextGrey,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))
                // Segmented gradient bar
                SegmentedProgressBar(progress = todayProgress / 100f)
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$todayProgress%",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isTodayIncreased) Icons.Filled.KeyboardDoubleArrowUp
                        else Icons.Filled.KeyboardDoubleArrowDown,
                        contentDescription = null,
                        tint = if (isTodayIncreased) SuccessGreen else DangerRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "COMPLETED",
                    fontSize = 10.sp,
                    color = TextGrey,
                    letterSpacing = 1.sp
                )
            }
        }

        // PROGRESS card (Modified from "LAST 7 DAYS AVG" -> "PROGRESS")
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SurfaceNavy, RoundedCornerShape(16.dp))
                .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                .clickable { onProgressSectionClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PROGRESS",
                fontSize = 9.sp,
                color = TextGrey,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 7 Bar chart according to progress of the days (0-100%)
                SevenDayBarChart(
                    values = currentWeekBars,
                    todayIndex = todayDayOfWeekIndex,
                    modifier = Modifier.weight(1f).height(50.dp)
                )
                Spacer(Modifier.width(8.dp))
                // Stats column showing today's effect on weekly progress
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$displayDiffPct%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = if (isSevenDayIncreased) Icons.Filled.KeyboardDoubleArrowUp
                            else Icons.Filled.KeyboardDoubleArrowDown,
                            contentDescription = null,
                            tint = if (isSevenDayIncreased) SuccessGreen else DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isSevenDayIncreased) "INCREASED" else "DECREASED",
                        fontSize = 9.sp,
                        color = if (isSevenDayIncreased) SuccessGreen else DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Total badge
            Box(
                modifier = Modifier
                    .background(PurpleFaint, RoundedCornerShape(50))
                    .border(1.dp, PurpleBorder, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Total: $totalPct%",
                    fontSize = 10.sp,
                    color = PurpleLight,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Multicolor segmented progress bar with white thumb marker */
@Composable
fun SegmentedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val cornerRadius = CornerRadius(5.dp.toPx())
        val gradient = Brush.horizontalGradient(
            colors = listOf(DangerRed, Color(0xFFF59E0B), SuccessGreen)
        )
        // Background track
        drawRoundRect(
            color = Color(0xFF1A1A2E),
            size = size,
            cornerRadius = cornerRadius
        )
        // Filled gradient track
        drawRoundRect(
            brush = gradient,
            size = Size(size.width * clamped, size.height),
            cornerRadius = cornerRadius
        )
        // White vertical thumb at progress position
        val thumbX = (size.width * clamped).coerceIn(4f, size.width - 4f)
        drawLine(
            color = Color.White,
            start = Offset(thumbX, -2.dp.toPx()),
            end   = Offset(thumbX, size.height + 2.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** 7 vertical bars for the last 7 days */
@Composable
fun SevenDayBarChart(
    values: List<Int>,
    todayIndex: Int = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6),
    modifier: Modifier = Modifier
) {
    val maxVal = 100f
    val displayValues = if (values.size >= 7) values.take(7) else values
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        displayValues.forEachIndexed { idx, v ->
            val fraction = (v / maxVal).coerceIn(0.08f, 1f)
            val isToday = idx == todayIndex
            val barColor = if (isToday) PurplePrimary else Color(0xFF2C1A5E)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                    .then(
                        if (isToday) Modifier.border(1.dp, Color(0xFFC084FC), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        else Modifier
                    )
            )
        }
    }
}
