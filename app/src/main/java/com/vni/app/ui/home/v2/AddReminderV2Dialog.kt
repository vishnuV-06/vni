package com.vni.app.ui.home.v2

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import java.util.Locale

/**
 * Add Reminder Panel matching Image 3:
 * - 🔔 Reminder Title
 * - Name input field, time selector dropdown, and confirm checkmark [✓] button
 * - Preset duration pills (5m, 10m, 15m, 20m, 25m, 30m, 45m, 1:00, 1:30, 2:00, 2:30, 3:00)
 * - 4 Category / Type buttons (Compulsary, Important, Normal, Standard)
 * - (i) Info button at bottom-right opening notification intervals panel (e.g. "15, 10, 5, 3, 2, 1"),
 *   persisted in preferences forever.
 */
@Composable
fun AddReminderV2Dialog(
    onDismiss: () -> Unit,
    onSave: (name: String, durationMinutes: Int, categoryType: String, exactTriggerMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vni_reminder_intervals", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("defaults_v5_migrated", false)) {
            prefs.edit()
                .putString("interval_compulsary", prefs.getString("interval_compulsary", null)?.takeIf { it != "15, 10, 5, 3, 2, 1" } ?: "15, 10, 5, 3, 2, 1")
                .putString("interval_important", prefs.getString("interval_important", null)?.takeIf { it != "15, 10, 5" } ?: "15, 10, 3, 1")
                .putString("interval_normal", prefs.getString("interval_normal", null)?.takeIf { it != "10, 5" } ?: "10, 5, 1")
                .putString("interval_standard", prefs.getString("interval_standard", null) ?: "5")
                .putBoolean("defaults_v5_migrated", true)
                .apply()
        }
    }

    var reminderName by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableIntStateOf(120) } // Default 2:00 = 120 mins
    var exactTargetTriggerMillis by remember { mutableStateOf<Long?>(null) }
    var targetClockLabel by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Normal") } // "Compulsary", "Important", "Normal", "Standard"

    var showInfoIntervalsDialog by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }

    val presetPillsRow1 = listOf("5m" to 5, "10m" to 10, "15m" to 15, "20m" to 20, "25m" to 25, "30m" to 30)
    val presetPillsRow2 = listOf("45m" to 45, "1:00" to 60, "1:30" to 90, "2:00" to 120, "2:30" to 150, "3:00" to 180)

    val timeDisplayStr = remember(selectedDurationMinutes, targetClockLabel) {
        if (targetClockLabel != null) {
            targetClockLabel!!
        } else {
            val h = selectedDurationMinutes / 60
            val m = selectedDurationMinutes % 60
            if (h > 0) {
                String.format("%d:%02d", h, m)
            } else {
                "${m}m"
            }
        }
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
                // ── Header: 🔔 Reminder ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Reminder",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                // ── Top Row: Name Input + Time Button + Confirm Checkmark [✓] ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Name input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF160A36))
                            .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Edit, null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            BasicTextField(
                                value = reminderName,
                                onValueChange = { reminderName = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (reminderName.isEmpty()) {
                            Text(
                                text = "Name of Reminder",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 22.dp)
                            )
                        }
                    }

                    // Time display pill button: 🕒 2:00 v
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A0D38))
                            .border(1.dp, Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                            .clickable { showCustomTimeDialog = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFFA855F7), modifier = Modifier.size(15.dp))
                            Text(
                                text = timeDisplayStr,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Confirm [✓] Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF241049))
                            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                if (reminderName.isNotBlank()) {
                                    onSave(reminderName, selectedDurationMinutes, selectedCategory, exactTargetTriggerMillis)
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Reminder",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ── Preset Time Duration Pills (2 rows of 6 pills each) ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        presetPillsRow1.forEach { (label, mins) ->
                            val isSelected = (selectedDurationMinutes == mins)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFA855F7) else Color(0xFF160A36))
                                    .border(1.dp, if (isSelected) Color(0xFFE879F9) else Color(0xFF381B6D), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDurationMinutes = mins
                                        exactTargetTriggerMillis = null
                                        targetClockLabel = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        presetPillsRow2.forEach { (label, mins) ->
                            val isSelected = (selectedDurationMinutes == mins)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFA855F7) else Color(0xFF160A36))
                                    .border(1.dp, if (isSelected) Color(0xFFE879F9) else Color(0xFF381B6D), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDurationMinutes = mins
                                        exactTargetTriggerMillis = null
                                        targetClockLabel = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── 4 Category / Type Buttons (2x2 grid) ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Compulsary, Important
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryButton(
                            title = "Compulsary",
                            isSelected = (selectedCategory == "Compulsary"),
                            onClick = { selectedCategory = "Compulsary" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryButton(
                            title = "Important",
                            isSelected = (selectedCategory == "Important"),
                            onClick = { selectedCategory = "Important" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Normal, Standard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryButton(
                            title = "Normal",
                            isSelected = (selectedCategory == "Normal"),
                            onClick = { selectedCategory = "Normal" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryButton(
                            title = "Standard",
                            isSelected = (selectedCategory == "Standard"),
                            onClick = { selectedCategory = "Standard" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Bottom-Right: (i) Info Button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B0E3C))
                            .border(1.dp, Color(0xFFA855F7), CircleShape)
                            .clickable { showInfoIntervalsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Notification Interval Settings",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Custom Duration or Target Clock Time Picker ──
    if (showCustomTimeDialog) {
        var isClockMode by remember { mutableStateOf(false) } // false = Duration (h & m), true = Target Clock Time (e.g. 7:30)
        var hoursInput by remember { mutableStateOf((selectedDurationMinutes / 60).toString()) }
        var minsInput by remember { mutableStateOf((selectedDurationMinutes % 60).toString()) }

        val calNow = Calendar.getInstance()
        var targetHourInput by remember { mutableStateOf(String.format("%02d", (calNow.get(Calendar.HOUR_OF_DAY) + 1) % 24)) }
        var targetMinInput by remember { mutableStateOf(String.format("%02d", calNow.get(Calendar.MINUTE))) }

        Dialog(onDismissRequest = { showCustomTimeDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F0824))
                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isClockMode) "TARGET TIME (CLOCK)" else "CUSTOM DURATION",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isClockMode) "Switch to Duration" else "Switch to Target Clock",
                            color = Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isClockMode = !isClockMode }
                        )
                    }

                    if (isClockMode) {
                        Text(
                            text = "Enter the exact time (e.g. now 5:00 and you select 7:30 -> 1:30 hrs countdown)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hour (0-23)", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF160A36))
                                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = targetHourInput,
                                        onValueChange = { targetHourInput = it.filter { ch -> ch.isDigit() }.take(2) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Minute (0-59)", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF160A36))
                                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = targetMinInput,
                                        onValueChange = { targetMinInput = it.filter { ch -> ch.isDigit() }.take(2) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hours", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF160A36))
                                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = hoursInput,
                                        onValueChange = { hoursInput = it.filter { ch -> ch.isDigit() } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Minutes", color = Color(0xFFA78BFA), fontSize = 11.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF160A36))
                                        .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = minsInput,
                                        onValueChange = { minsInput = it.filter { ch -> ch.isDigit() } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (isClockMode) {
                                val th = targetHourInput.toIntOrNull()?.coerceIn(0, 23) ?: 12
                                val tm = targetMinInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                val now = System.currentTimeMillis()
                                val targetCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, th)
                                    set(Calendar.MINUTE, tm)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    if (timeInMillis <= now) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }
                                val diffMins = ((targetCal.timeInMillis - now) / 60000L).toInt().coerceAtLeast(1)
                                selectedDurationMinutes = diffMins
                                exactTargetTriggerMillis = targetCal.timeInMillis
                                targetClockLabel = String.format(Locale.ENGLISH, "%02d:%02d", th, tm)
                            } else {
                                val h = hoursInput.toIntOrNull() ?: 0
                                val m = minsInput.toIntOrNull() ?: 0
                                val total = (h * 60 + m).coerceAtLeast(1)
                                selectedDurationMinutes = total
                                exactTargetTriggerMillis = null
                                targetClockLabel = null
                            }
                            showCustomTimeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("SET TIME", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ── (i) Notification Warning Intervals Panel ──
    if (showInfoIntervalsDialog) {
        var compInterval by remember { mutableStateOf(prefs.getString("interval_compulsary", "15, 10, 5, 3, 2, 1") ?: "15, 10, 5, 3, 2, 1") }
        var impInterval by remember { mutableStateOf(prefs.getString("interval_important", "15, 10, 3, 1") ?: "15, 10, 3, 1") }
        var normInterval by remember { mutableStateOf(prefs.getString("interval_normal", "10, 5, 1") ?: "10, 5, 1") }
        var stdInterval by remember { mutableStateOf(prefs.getString("interval_standard", "5") ?: "5") }

        Dialog(onDismissRequest = { showInfoIntervalsDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F0824))
                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTIFICATION INTERVALS",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        IconButton(onClick = { showInfoIntervalsDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    Text(
                        text = "Set countdown warning triggers (minutes before deadline) for each category. Saved permanently!",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )

                    // Compulsary
                    IntervalInputRow(
                        categoryName = "Compulsary",
                        value = compInterval,
                        onValueChange = { compInterval = it }
                    )

                    // Important
                    IntervalInputRow(
                        categoryName = "Important",
                        value = impInterval,
                        onValueChange = { impInterval = it }
                    )

                    // Normal
                    IntervalInputRow(
                        categoryName = "Normal",
                        value = normInterval,
                        onValueChange = { normInterval = it }
                    )

                    // Standard
                    IntervalInputRow(
                        categoryName = "Standard",
                        value = stdInterval,
                        onValueChange = { stdInterval = it }
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("interval_compulsary", compInterval)
                                .putString("interval_important", impInterval)
                                .putString("interval_normal", normInterval)
                                .putString("interval_standard", stdInterval)
                                .apply()
                            showInfoIntervalsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text("SAVE INTERVALS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFA855F7) else Color(0xFF160A36))
            .border(1.dp, if (isSelected) Color(0xFFE879F9) else Color(0xFF381B6D), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun IntervalInputRow(
    categoryName: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categoryName,
            color = Color(0xFFA78BFA),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(90.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF160A36))
                .border(1.dp, Color(0xFF381B6D), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFFA855F7)),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
