package com.vni.app.ui.home.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.outlined.Event
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Section B:
 * - Left block: Calendar block (displays Day name e.g. "MONDAY", date number "26", month/year "OCT 2026",
 *   and beside it displays the square violet logo of the present active section e.g. checkmark '✓' for Tasks.
 *   Does NOT open calendar on click, as specified).
 * - Right block: 3 numbers counters block:
 *   - The big number is the count for the active section (Section C = Tasks, so "03 Tasks" is big).
 *   - The two smaller stacked pills display Notice count ("04") and Reminder count ("01").
 */
@Composable
fun HomeDateAndCountersBar(
    tasksCount: Int,
    noticesCount: Int,
    remindersCount: Int,
    eventsCount: Int = 0,
    activeSectionType: String = "TASKS", // "TASKS", "NOTICES", "REMINDERS", "EVENTS", "SEARCH", "CALENDAR"
    onClickDateBlock: (() -> Unit)? = null,
    onClickNoticeBlock: (() -> Unit)? = null,
    onDragNoticeLeft: (() -> Unit)? = null,
    onDragNoticeRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cal = remember { Calendar.getInstance() }
    val dayName = remember {
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date()).uppercase()
    }
    val dayNumber = remember {
        String.format(Locale.ENGLISH, "%02d", cal.get(Calendar.DAY_OF_MONTH))
    }
    val monthShort = remember {
        SimpleDateFormat("MMM", Locale.ENGLISH).format(Date()).uppercase()
    }
    val yearStr = remember {
        cal.get(Calendar.YEAR).toString()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Left Card: Calendar & Active Section Logo ──
        Box(
            modifier = Modifier
                .weight(1.05f)
                .height(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F0724))
                .border(1.2.dp, Color(0xFF6B21A8).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .then(
                    if (onClickDateBlock != null) Modifier.clickable { onClickDateBlock() }
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Day, Date, Month Year
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = dayName,
                        color = Color(0xFFA78BFA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayNumber,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(Modifier.width(6.dp))

                        Column {
                            Text(
                                text = monthShort,
                                color = Color(0xFFE2E8F0),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp
                            )
                            Text(
                                text = yearStr,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }

                // Right: Square violet glowing icon of active section (e.g. '✓' for Tasks, layers for Notices, search icon)
                val activeIcon = when (activeSectionType) {
                    "NOTICES" -> Icons.Outlined.Layers
                    "REMINDERS" -> Icons.Outlined.Notifications
                    "EVENTS" -> Icons.Outlined.Event
                    "CALENDAR" -> Icons.Outlined.CalendarMonth
                    "PLAN" -> Icons.Outlined.TrackChanges
                    "PET" -> Icons.Outlined.Pets
                    "SEARCH" -> Icons.Default.Search
                    else -> Icons.Default.Check
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF241049))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(11.dp))
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(11.dp), spotColor = Color(0xFFA855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeIcon,
                        contentDescription = "Active Section: $activeSectionType",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── 2. Right Card: 3 Counters (Big number for active section, 2 small stacked pills) ──
        Box(
            modifier = Modifier
                .weight(0.95f)
                .height(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F0724))
                .border(1.2.dp, Color(0xFF6B21A8).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX < -30f) {
                                onDragNoticeLeft?.invoke()
                            } else if (totalDragX > 30f) {
                                onDragNoticeRight?.invoke()
                            }
                        }
                    ) { change, dragAmount ->
                        totalDragX += dragAmount
                        if (kotlin.math.abs(totalDragX) > 25f) change.consume()
                    }
                }
                .then(
                    if (onClickNoticeBlock != null) Modifier.clickable { onClickNoticeBlock() }
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big Number & Label for the active section (Section C)
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    val bigCountFormatted = when (activeSectionType) {
                        "NOTICES" -> String.format(Locale.ENGLISH, "%02d", noticesCount)
                        "REMINDERS" -> String.format(Locale.ENGLISH, "%02d", remindersCount)
                        "EVENTS" -> String.format(Locale.ENGLISH, "%02d", eventsCount)
                        else -> String.format(Locale.ENGLISH, "%02d", tasksCount)
                    }
                    val bigLabel = when (activeSectionType) {
                        "NOTICES" -> "Notices"
                        "REMINDERS" -> "Reminders"
                        "EVENTS" -> "Events"
                        else -> "Tasks"
                    }

                    Text(
                        text = bigCountFormatted,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = bigLabel,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Two stacked smaller pill badges
                val topPillCount = when (activeSectionType) {
                    "NOTICES" -> tasksCount
                    else -> noticesCount
                }
                val bottomPillCount = when (activeSectionType) {
                    "REMINDERS" -> noticesCount
                    else -> remindersCount
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Top Pill
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%02d", topPillCount),
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom Pill: Vibrant violet background
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF7C3AED))
                            .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%02d", bottomPillCount),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
