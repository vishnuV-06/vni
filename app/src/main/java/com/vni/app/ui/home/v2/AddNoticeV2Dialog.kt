package com.vni.app.ui.home.v2

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Add Notice Panel matching Image 2:
 * - Name input field with confirm checkmark [✓] button on the right
 * - "Apply On" selector with [📅 Date] and [📅 Day] toggle
 * - Date picker selector button (opens calendar to select a day)
 * - Details input area
 */
@Composable
fun AddNoticeV2Dialog(
    onDismiss: () -> Unit,
    onSave: (title: String, subtitle: String, deadlineText: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }
    var applyOnMode by remember { mutableStateOf("DATE") } // "DATE" or "DAY"

    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

    var selectedDayLabel by remember { mutableStateOf("Today") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val formattedDateText = remember(selectedYear, selectedMonth, selectedDay) {
        val c = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(c.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0824))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Top Row: Name Input + Confirm Checkmark Button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Name",
                            color = Color(0xFFA78BFA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF160A36))
                                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = nameText,
                                onValueChange = { nameText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (nameText.isEmpty()) {
                                Text(
                                    text = "Enter notice name",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Confirm [✓] Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF241049))
                            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                if (nameText.isNotBlank()) {
                                    val deadline = if (applyOnMode == "DATE") formattedDateText else selectedDayLabel
                                    onSave(nameText, detailsText, deadline)
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Notice",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ── Middle Row: "Apply On" with Date / Day Toggle ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apply On",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Toggle Tabs [📅 Date] vs [📅 Day]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF160A36))
                            .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Date Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (applyOnMode == "DATE") Color(0xFF7C3AED) else Color.Transparent)
                                .clickable { applyOnMode = "DATE" }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Date",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Day Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (applyOnMode == "DAY") Color(0xFF7C3AED) else Color.Transparent)
                                .clickable { applyOnMode = "DAY" }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Day",
                                    color = if (applyOnMode == "DAY") Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── Date Selector or Day Selector Row ──
                if (applyOnMode == "DATE") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF160A36))
                            .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                            .clickable { showDatePickerDialog = true }
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                                Text(
                                    text = formattedDateText,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // Quick Day Options (Today, Tomorrow, 2 Days, 3 Days)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dayOptions = listOf("Today", "Tomorrow", "In 2 Days", "This Week")
                        dayOptions.forEach { opt ->
                            val isSelected = (selectedDayLabel == opt)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF7C3AED) else Color(0xFF160A36))
                                    .border(1.dp, if (isSelected) Color(0xFFA855F7) else Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                    .clickable { selectedDayLabel = opt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ── Details Box ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF160A36))
                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Details",
                                color = Color(0xFFA78BFA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = detailsText,
                            onValueChange = { detailsText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp, lineHeight = 16.sp),
                            cursorBrush = SolidColor(Color(0xFFA855F7)),
                            modifier = Modifier.fillMaxSize()
                        )
                        if (detailsText.isEmpty()) {
                            Text(
                                text = "Write note / description...",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Compact Calendar Date Picker Modal ──
    if (showDatePickerDialog) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        var calMonth by remember { mutableIntStateOf(selectedMonth) }
        var calYear by remember { mutableIntStateOf(selectedYear) }

        val monthCal = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1

        Dialog(onDismissRequest = { showDatePickerDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F0824))
                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Header Month & Year
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth--
                        }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                        }

                        Text(
                            text = "${monthNames[calMonth]} $calYear",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++
                        }) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Days Grid
                    val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        dayHeaders.forEach { d ->
                            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                Text(d, color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (row in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (col in 0..6) {
                                val cellIdx = row * 7 + col
                                val dayNum = cellIdx - firstDayOfWeek + 1
                                if (dayNum in 1..daysInMonth) {
                                    val isSelected = (dayNum == selectedDay && calMonth == selectedMonth && calYear == selectedYear)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFA855F7) else Color.Transparent)
                                            .clickable {
                                                selectedDay = dayNum
                                                selectedMonth = calMonth
                                                selectedYear = calYear
                                                showDatePickerDialog = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                            fontSize = 13.sp,
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
        }
    }
}
