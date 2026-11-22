package com.vni.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.ui.theme.*
import java.util.Calendar

@Composable
fun AddNoticePanel(
    isReminder: Boolean = false,
    onSave: (title: String, subtitle: String, isReminder: Boolean, reminderDay: String, timeRange: String?, durationMinutes: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    // Calendar state for Image 1 (Notice only)
    var isDateActive by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    // Date display
    val cal = Calendar.getInstance()
    var selectedDayNum by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var selectedNoticeDay by remember { mutableStateOf("Today") }

    // Reminder Duration state (Requirement 5)
    var durationMinutes by remember { mutableIntStateOf(30) }
    var durationMinutesText by remember { mutableStateOf("30") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically focus title and show keyboard on open
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
            .imePadding()
            .zIndex(60f)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C071E))
                    .border(1.5.dp, PurplePrimary.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top handle pill (for Reminder panel Image 2)
                    if (isReminder) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary)
                            )
                        }
                    }

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF130B30))
                                    .border(1.dp, PurplePrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isReminder) "Add Reminder" else "Add Notice",
                                    color = TextWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Capture what matters",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Top-Right Checkmark "Tick" Button (both Image 1 & Image 2)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                                .background(Color(0xFF130B30))
                                .clickable {
                                    val finalTitle = title.ifEmpty { if (isReminder) "Reminder" else "New Notice" }
                                    val finalDuration = if (isReminder) (durationMinutesText.toIntOrNull() ?: durationMinutes).coerceAtLeast(1) else null
                                    val reminderDayStr = if (isReminder) "In $finalDuration min" else selectedNoticeDay
                                    onSave(
                                        finalTitle,
                                        details.ifEmpty { if (isReminder) "Stay alert" else "Important note" },
                                        isReminder,
                                        reminderDayStr,
                                        null,
                                        finalDuration
                                    )
                                    keyboardController?.hide()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Row 1: Title Input (Left) + Calendar Box or Time Range (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Title Input Box
                        Column(modifier = Modifier.weight(if (isReminder) 1.1f else 1.5f)) {
                            Text("Title", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF130B30))
                                    .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    textStyle = TextStyle(color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    decorationBox = { inner ->
                                        if (title.isEmpty()) {
                                            Text("Enter notice...", color = TextMuted, fontSize = 14.sp)
                                        }
                                        inner()
                                    }
                                )
                            }
                        }

                        // Right: Calendar Box (Image 1) OR Time Range Reminder (Image 2)
                        if (!isReminder) {
                            // Calendar Box matching Image 1
                            Column(modifier = Modifier.width(115.dp)) {
                                val calBorderColor = if (isDateActive) PurplePrimary else Color(0xFF26194E)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF130B30))
                                        .border(if (isDateActive) 2.dp else 1.dp, calBorderColor, RoundedCornerShape(14.dp))
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = { isDateActive = !isDateActive },
                                                onTap = { if (isDateActive) showCalendarPicker = true }
                                            )
                                        }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (isDateActive) PurplePrimary else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = selectedDayNum.toString(),
                                                color = if (isDateActive) TextWhite else TextMuted,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                lineHeight = 17.sp
                                            )
                                            Text(
                                                text = "SEP",
                                                color = if (isDateActive) TextWhite else TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 10.sp
                                            )
                                            Text(
                                                text = "2026",
                                                color = TextMuted,
                                                fontSize = 8.sp,
                                                lineHeight = 9.sp
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = if (isDateActive) PurplePrimary else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Countdown Duration Input (Requirement 5)
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = "Countdown Duration",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF130B30))
                                        .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = durationMinutesText,
                                        onValueChange = {
                                            if (it.all { ch -> ch.isDigit() } && it.length <= 4) {
                                                durationMinutesText = it
                                                it.toIntOrNull()?.let { num -> durationMinutes = num }
                                            }
                                        },
                                        textStyle = TextStyle(
                                            color = TextWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "min",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Row 2: Details Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Details", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${details.length}/${if (isReminder) "300" else "30"}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF130B30))
                            .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notes,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = details,
                            onValueChange = {
                                val max = if (isReminder) 300 else 30
                                if (it.length <= max) details = it
                            },
                            textStyle = TextStyle(color = TextWhite, fontSize = 14.sp),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { inner ->
                                if (details.isEmpty()) {
                                    Text("Enter more info...", color = TextMuted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        )
                    }

                    // Row 3 (Reminders Only): Quick duration presets with clean rectangular buttons (Requirement 5)
                    if (isReminder) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Quick Presets",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                                val isSelected = durationMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PurplePrimary else Color(0xFF130B30))
                                        .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFF26194E), RoundedCornerShape(8.dp))
                                        .clickable {
                                            durationMinutes = mins
                                            durationMinutesText = mins.toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (isSelected) Color.White else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF150C33))
                                .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Alarm fires in $durationMinutes min • Repeats every 5 min until Done",
                                color = TextWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Calendar Picker Modal (can pick ANY date for notice or reminder)
        if (showCalendarPicker) {
            CalendarPanel(
                initialYear = cal.get(Calendar.YEAR),
                initialMonth = cal.get(Calendar.MONTH),
                initialDay = selectedDayNum,
                onDateSelected = { year, month, day ->
                    selectedDayNum = day
                    val todayCal = Calendar.getInstance()
                    val isToday = todayCal.get(Calendar.YEAR) == year &&
                            todayCal.get(Calendar.MONTH) == month &&
                            todayCal.get(Calendar.DAY_OF_MONTH) == day

                    val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val isTomorrow = tomCal.get(Calendar.YEAR) == year &&
                            tomCal.get(Calendar.MONTH) == month &&
                            tomCal.get(Calendar.DAY_OF_MONTH) == day

                    val targetCal = Calendar.getInstance().apply { set(year, month, day) }
                    selectedNoticeDay = when {
                        isToday -> "Today"
                        isTomorrow -> "Tomorrow"
                        else -> java.text.SimpleDateFormat("d MMM", java.util.Locale.ENGLISH).format(targetCal.time)
                    }
                    showCalendarPicker = false
                },
                onDismiss = {
                    showCalendarPicker = false
                }
            )
        }
    }
}
