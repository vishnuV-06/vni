package com.vni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.*
import java.util.Calendar

@Composable
fun CalendarPanel(
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    initialDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    var currentYear by remember { mutableIntStateOf(initialYear) }
    var currentMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    val monthNames = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

    // Calendar calculations
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -1)
    }
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = buildList {
        repeat(firstDayOfWeek) { add(daysInPrevMonth - firstDayOfWeek + 1 + it to false) }
        repeat(daysInMonth) { add(it + 1 to true) }
        val remaining = 42 - size
        repeat(remaining) { add(it + 1 to false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
            .zIndex(90f)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C071E))
                .border(2.dp, PurplePrimary.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Previous Month
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF130B30))
                                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev Month", tint = TextWhite)
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = monthNames.getOrElse(currentMonth) { "MONTH" },
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(12.dp))

                        // Next Month
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF130B30))
                                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month", tint = TextWhite)
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = currentYear.toString(),
                            color = TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save / Confirm Checkmark
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C3AED))
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                onDateSelected(currentYear, currentMonth, selectedDay)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm Date", tint = TextWhite, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Days of week headers
                val daysOfWeek = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEachIndexed { index, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = day,
                                color = if (index == 0) PurplePrimary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (index == 0) {
                                Spacer(Modifier.height(4.dp))
                                Box(modifier = Modifier.width(16.dp).height(2.dp).background(PurplePrimary))
                            } else {
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Calendar Grid cells (6 rows x 7 cols)
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { (day, isCurrentMonth) ->
                            val isSelected = isCurrentMonth && day == selectedDay

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> PurplePrimary
                                            isCurrentMonth -> Color(0xFF130B30)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color.White else Color(0xFF26194E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = isCurrentMonth) {
                                        if (isCurrentMonth) {
                                            selectedDay = day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isCurrentMonth -> TextWhite
                                        else -> TextMuted.copy(alpha = 0.25f)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
