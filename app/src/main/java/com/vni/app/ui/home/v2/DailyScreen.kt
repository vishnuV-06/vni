package com.vni.app.ui.home.v2

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.MotiveHeading
import com.vni.app.data.model.UniTask
import java.util.Calendar
import java.util.Locale

/**
 * Daily Section (Requirement 8):
 * - Contains UniTasks / Daily Tasks.
 * - Each item can be assigned with an exact time (e.g. 08:30 AM).
 * - Sends recurring daily notification reminders at the mentioned time until removed from Daily!
 * - Holding a card for 1 second prompts deletion and cancels the scheduled alarm.
 */
@Composable
fun DailyScreen(
    uniTasks: List<UniTask>,
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    tasksCount: Int,
    noticesCount: Int,
    remindersCount: Int,
    onAddDailyTask: (name: String, hour: Int, minute: Int, timeLabel: String, notifyBeforeMins: Int) -> Unit,
    onDeleteDailyTask: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<UniTask?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Section A: Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            // ── Section B: Date & Counters Bar ──
            HomeDateAndCountersBar(
                tasksCount = tasksCount,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                activeSectionType = "DAILY",
                onClickDateBlock = onDismiss,
                onClickNoticeBlock = onDismiss
            )

            Spacer(Modifier.height(12.dp))

            // Subheader banner: DAILY TASKS (Recurring every day)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7))))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("DAILY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Recurring Unitasks", color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Text(
                    text = "${uniTasks.size} tasks active",
                    color = Color(0xFFA855F7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Content List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (uniTasks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Today, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(42.dp))
                            Text(
                                text = "No daily unitasks yet.\nTap + New below to set recurring daily tasks & reminders.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uniTasks, key = { it.id }) { task ->
                            DailyUniTaskCard(
                                task = task,
                                onLongPress = { taskToDelete = task },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Add Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7))))
                    .border(1.2.dp, Color(0xFFE879F9), RoundedCornerShape(24.dp))
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0xFFA855F7))
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 28.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("New Daily Task", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add Daily Task Dialog with Time Picker
        if (showAddDialog) {
            var taskNameInput by remember { mutableStateOf("") }
            val nowCal = remember { Calendar.getInstance() }
            var hourInput by remember { mutableStateOf(String.format(Locale.ENGLISH, "%02d", (nowCal.get(Calendar.HOUR_OF_DAY) + 1) % 24)) }
            var minInput by remember { mutableStateOf(String.format(Locale.ENGLISH, "%02d", nowCal.get(Calendar.MINUTE))) }
            var notifyBeforeInput by remember { mutableStateOf("5") }

            Dialog(onDismissRequest = { showAddDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Add Daily Recurring Task", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)

                        Text("Task Name", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = taskNameInput,
                                onValueChange = { taskNameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontSize = 14.5.sp),
                                decorationBox = { inner ->
                                    if (taskNameInput.isEmpty()) {
                                        Text("e.g. Piano Practice, DSA, Exercise", color = Color(0xFF64748B), fontSize = 13.5.sp)
                                    }
                                    inner()
                                }
                            )
                        }

                        Text("Scheduled Reminder Time (Daily)", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Box
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hour (0-23)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1045))
                                        .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = hourInput,
                                        onValueChange = { hourInput = it.take(2) },
                                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    )
                                }
                            }

                            Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))

                            // Minute Box
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Min (0-59)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1045))
                                        .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = minInput,
                                        onValueChange = { minInput = it.take(2) },
                                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    )
                                }
                            }
                        }

                        // Requirement 5: Option to select time to notify before (e.g. 5 minutes before assigning time)
                        Text("Select time (Notify before assigning time in mins)", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1045))
                                .border(1.dp, Color(0xFF7C3AED), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BasicTextField(
                                    value = notifyBeforeInput,
                                    onValueChange = { notifyBeforeInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    decorationBox = { inner ->
                                        if (notifyBeforeInput.isEmpty()) {
                                            Text("e.g. 5 (notifies 5 min before)", color = Color(0xFF64748B), fontSize = 13.sp)
                                        }
                                        inner()
                                    }
                                )
                                Text("mins before", color = Color(0xFFC084FC), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cleanName = taskNameInput.trim()
                                    val h = hourInput.toIntOrNull()?.coerceIn(0, 23) ?: 9
                                    val m = minInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                    val notifyBeforeMins = notifyBeforeInput.toIntOrNull() ?: 0
                                    if (cleanName.isNotBlank()) {
                                        val ampm = if (h >= 12) "PM" else "AM"
                                        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                                        val timeStr = String.format(Locale.ENGLISH, "%02d:%02d %s", h12, m, ampm)
                                        onAddDailyTask(cleanName, h, m, timeStr, notifyBeforeMins)
                                        val alertMsg = if (notifyBeforeMins > 0) {
                                            "Added! Will notify $notifyBeforeMins min before $timeStr"
                                        } else {
                                            "Added! Daily reminder set for $timeStr"
                                        }
                                        Toast.makeText(context, alertMsg, Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (taskToDelete != null) {
            Dialog(onDismissRequest = { taskToDelete = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F0724))
                        .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Remove Daily Task?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Remove \"${taskToDelete?.name}\" from daily tasks and cancel its recurring reminders?", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { taskToDelete = null }) { Text("Cancel", color = Color(0xFF94A3B8)) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDeleteDailyTask(taskToDelete!!.id)
                                    taskToDelete = null
                                    Toast.makeText(context, "Removed from daily tasks", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyUniTaskCard(
    task: UniTask,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF14082D))
            .border(1.2.dp, Color(0xFF7C3AED), RoundedCornerShape(18.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0xFFA855F7))
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Every day at ${task.scheduledTime ?: "09:00 AM"}",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF220E49))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
