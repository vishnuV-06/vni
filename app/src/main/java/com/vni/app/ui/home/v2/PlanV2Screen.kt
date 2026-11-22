package com.vni.app.ui.home.v2

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.vni.app.data.model.DailyAssignedPlanTask
import com.vni.app.data.model.MotiveHeading
import com.vni.app.data.model.Notice
import com.vni.app.data.model.PlanV2Category
import com.vni.app.data.model.PlanV2Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PlanDayItem(
    val dayName: String, // "Mon", "Tue"
    val dayNum: Int,     // 8, 9, 10
    val dateKey: String, // "yyyy-MM-dd"
    val isPast: Boolean,
    val isToday: Boolean
)

/**
 * Plan Section V2 matching Images 1, 2, and 3:
 * - Top Motive quote, logo, bell, settings (identical across V2).
 * - Section B: Monday 09 NOV 2026, calendar icon (Image 1) or layers icon (Image 2), 04 Notices.
 * - HorizontalPager between:
 *   - Page 0 (Image 1): Date Carousel & Assigned Tasks on selected date + Declared Tasks Selector below.
 *   - Page 1 (Image 2): Categories with (+) to add, Declared Tasks list, and (+ New) button on bottom.
 * - Image 3 (Time Selector Dialog):
 *   - Appears when user selects a task on Page 0 and clicks the [ ✓ ] tick button.
 *   - Preset duration chips (5 to 60 min, 1:15 to 4:00 hr).
 *   - Clicking [ ✓ ] assigns the task to the selected date.
 * - Long-press for 1 second on assigned tasks removes it from the date!
 * - Past dates are read-only (cannot add or modify tasks).
 */
@Composable
fun PlanV2Screen(
    planCategories: List<PlanV2Category>,
    planTasks: List<PlanV2Task>,
    dailyAssignedTasks: List<DailyAssignedPlanTask>,
    notices: List<Notice>,
    motiveHeadings: List<MotiveHeading>,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddCategory: (String) -> Unit,
    onAddTask: (categoryName: String, taskName: String, defaultMinutes: Int) -> Unit,
    onAssignTaskToDate: (dateKey: String, taskId: Long, taskName: String, durationMinutes: Int) -> Unit,
    onRemoveAssignedTask: (assignedTaskId: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Date calculations: 3 days in the past up to 14 days in the future
    val cal = remember { Calendar.getInstance() }
    val todayYear = cal.get(Calendar.YEAR)
    val todayMonth = cal.get(Calendar.MONTH)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayKey = remember {
        String.format(Locale.ENGLISH, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)
    }

    val daysList = remember {
        val list = mutableListOf<PlanDayItem>()
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2) // Show 2 past days for review
        }
        for (i in 0..16) {
            val yr = startCal.get(Calendar.YEAR)
            val mo = startCal.get(Calendar.MONTH)
            val d = startCal.get(Calendar.DAY_OF_MONTH)
            val key = String.format(Locale.ENGLISH, "%04d-%02d-%02d", yr, mo + 1, d)
            val dName = SimpleDateFormat("EEE", Locale.ENGLISH).format(startCal.time)
            val isPast = key < todayKey
            val isToday = key == todayKey
            list.add(PlanDayItem(dName, d, key, isPast, isToday))
            startCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Default selected date: today (or tomorrow if user prefers)
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    val selectedDayItem = remember(selectedDateKey, daysList) {
        daysList.find { it.dateKey == selectedDateKey } ?: daysList.firstOrNull { it.isToday } ?: daysList[2]
    }
    val isPastDate = selectedDayItem.isPast

    // Selected task for assignment on Page 0
    var selectedTaskToAssign by remember { mutableStateOf<PlanV2Task?>(null) }
    var showTimeSelectorDialog by remember { mutableStateOf(false) }

    // Dialogs on Page 1 (Add category / Add task)
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // ── Section A: Top Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            Spacer(Modifier.height(4.dp))

            // ── Section B: Date & Counters Bar ──
            val activeNoticesCount = notices.count { it.iconName != "School" && it.durationMinutes == null && it.reminderTriggerMillis == null }
            val activeRemindersCount = notices.count { it.iconName == "School" || it.durationMinutes != null || it.reminderTriggerMillis != null }

            HomeDateAndCountersBar(
                tasksCount = dailyAssignedTasks.filter { it.dateKey == selectedDateKey }.size,
                noticesCount = activeNoticesCount,
                remindersCount = activeRemindersCount,
                activeSectionType = if (pagerState.currentPage == 0) "CALENDAR" else "PLAN"
            )

            Spacer(Modifier.height(8.dp))

            // ── Horizontal Pager: Page 0 (Daily Assignment) & Page 1 (Task Catalog) ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) {
                    // ── PAGE 0: Daily Task Assignment (Image 1) ──
                    DailyAssignmentPage(
                        daysList = daysList,
                        selectedDateKey = selectedDateKey,
                        onSelectDate = { key ->
                            selectedDateKey = key
                            selectedTaskToAssign = null
                        },
                        isPastDate = isPastDate,
                        assignedTasksForDay = dailyAssignedTasks.filter { it.dateKey == selectedDateKey },
                        onRemoveAssignedTask = onRemoveAssignedTask,
                        categories = planCategories,
                        tasks = planTasks,
                        selectedTask = selectedTaskToAssign,
                        onSelectTask = { task ->
                            if (!isPastDate) {
                                selectedTaskToAssign = if (selectedTaskToAssign?.id == task.id) null else task
                            } else {
                                Toast.makeText(context, "Past dates cannot be modified", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenTimeSelector = {
                            if (selectedTaskToAssign != null && !isPastDate) {
                                showTimeSelectorDialog = true
                            } else if (isPastDate) {
                                Toast.makeText(context, "Past dates cannot be modified", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    // ── PAGE 1: Task Catalog & Categories Management (Image 2) ──
                    TaskCatalogPage(
                        categories = planCategories,
                        tasks = planTasks,
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onAddTaskClick = { showAddTaskDialog = true }
                    )
                }
            }

            // ── Bottom Pagination Dots (Page 0 vs Page 1) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..1) {
                    val isActive = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFFA855F7) else Color(0xFF4C1D95).copy(alpha = 0.5f))
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                    )
                }
            }
        }

        // ── Image 3: Time Selector Dialog Overlay ──
        if (showTimeSelectorDialog && selectedTaskToAssign != null) {
            val task = selectedTaskToAssign!!
            PlanTimeSelectorDialog(
                taskName = task.name,
                initialMinutes = task.defaultMinutes,
                onDismiss = { showTimeSelectorDialog = false },
                onConfirm = { minutes ->
                    onAssignTaskToDate(selectedDateKey, task.id, task.name, minutes)
                    selectedTaskToAssign = null
                    showTimeSelectorDialog = false
                    Toast.makeText(context, "\"${task.name}\" assigned for $minutes min", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ── Add Category Dialog (Page 1) ──
        if (showAddCategoryDialog) {
            AddSimpleInputDialog(
                title = "New Category",
                placeholder = "Category name (e.g. Eventually)",
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name ->
                    onAddCategory(name)
                    showAddCategoryDialog = false
                }
            )
        }

        // ── Add Task Dialog (Page 1 - Bottom +New) ──
        if (showAddTaskDialog) {
            val defaultCategory = planCategories.firstOrNull()?.name ?: "Primary"
            var chosenCat by remember { mutableStateOf(defaultCategory) }

            AddSimpleInputDialog(
                title = "New Plan Task",
                placeholder = "Task name (e.g. DSA, App Dev)",
                secondaryPicker = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        planCategories.forEach { cat ->
                            val isSel = cat.name.equals(chosenCat, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFA855F7) else Color(0xFF160833))
                                    .clickable { chosenCat = cat.name }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { taskName ->
                    onAddTask(chosenCat, taskName, 45)
                    showAddTaskDialog = false
                    Toast.makeText(context, "Added \"$taskName\" to $chosenCat", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * Page 0 (Image 1):
 * - Middle Card: Date horizontal carousel + Assigned Tasks for that day.
 * - Bottom Card: Search bar with [✓] button + Category tabs + Declared task pills.
 */
@Composable
private fun DailyAssignmentPage(
    daysList: List<PlanDayItem>,
    selectedDateKey: String,
    onSelectDate: (String) -> Unit,
    isPastDate: Boolean,
    assignedTasksForDay: List<DailyAssignedPlanTask>,
    onRemoveAssignedTask: (Long) -> Unit,
    categories: List<PlanV2Category>,
    tasks: List<PlanV2Task>,
    selectedTask: PlanV2Task?,
    onSelectTask: (PlanV2Task) -> Unit,
    onOpenTimeSelector: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryName by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Primary") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Middle Section Card: Dates Row + Assigned Tasks List ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Horizontal Dates Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    daysList.forEach { item ->
                        val isSelected = (item.dateKey == selectedDateKey)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF3B1578)
                                        else if (item.isPast) Color(0xFF13092A).copy(alpha = 0.6f)
                                        else Color(0xFF180A38)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFA855F7) else Color(0xFF381A6B),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectDate(item.dateKey) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.dayName,
                                        color = if (isSelected) Color(0xFFE9D5FF) else if (item.isPast) Color(0xFF6B7280) else Color(0xFFC084FC),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${item.dayNum}",
                                        color = if (isSelected) Color.White else if (item.isPast) Color(0xFF9CA3AF) else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Glowing white/purple dot underneath selected day
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            } else {
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Status tag if past date
                if (isPastDate) {
                    Text(
                        text = "🔒 Past date (Viewing only - cannot modify)",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Assigned Tasks for Selected Date
                if (assignedTasksForDay.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPastDate) "No tasks recorded on this date" else "No tasks assigned yet. Select a task below and tap ✓ to assign",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        assignedTasksForDay.forEach { assigned ->
                            key(assigned.id) {
                                AssignedTaskCardItem(
                                    task = assigned,
                                    isPastDate = isPastDate,
                                    onHoldToRemove = {
                                        onRemoveAssignedTask(assigned.id)
                                        Toast.makeText(context, "Removed \"${assigned.taskName}\"", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Bottom Section Card: Search + Tick + Categories + Declared Task Pills ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row: Search Bar [ 🔍 search ] + [ ✓ ] Checkmark Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF160A36))
                            .border(1.2.dp, Color(0xFFA855F7).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "search",
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                        }
                    }

                    // Tick Button [ ✓ ]
                    val isTickEnabled = (selectedTask != null && !isPastDate)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTickEnabled) Color(0xFFA855F7) else Color(0xFF1E0D3D))
                            .border(1.2.dp, if (isTickEnabled) Color(0xFFE879F9) else Color(0xFF4C1D95), RoundedCornerShape(12.dp))
                            .clickable(enabled = isTickEnabled) { onOpenTimeSelector() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm Assign",
                            tint = if (isTickEnabled) Color.White else Color(0xFF6B7280),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Category Tabs (Primary, Secondary, General / Eventually)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isCatActive = cat.name.equals(activeCategoryName, ignoreCase = true)
                        Column(
                            modifier = Modifier
                                .clickable { activeCategoryName = cat.name }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isCatActive) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            if (isCatActive) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFA855F7))
                                )
                            } else {
                                Spacer(Modifier.height(2.5.dp))
                            }
                        }
                    }
                }

                // Declared Task Pills (Filter by Category + Search Query)
                val filteredTasks = tasks.filter {
                    (it.categoryName.equals(activeCategoryName, ignoreCase = true) || searchQuery.isNotBlank()) &&
                    (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
                }

                if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No declared tasks in $activeCategoryName. Swipe left to add tasks",
                            color = Color(0xFF64748B),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Task Pills in Flow / Wrapped layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chunked = filteredTasks.chunked(2)
                        chunked.forEach { rowTasks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTasks.forEach { taskItem ->
                                    val isSelected = (selectedTask?.id == taskItem.id)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF3B1578) else Color(0xFF160A36))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFA855F7) else Color(0xFF381A6B),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .then(
                                                if (isSelected) Modifier.shadow(elevation = 10.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7))
                                                else Modifier
                                            )
                                            .clickable { onSelectTask(taskItem) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = taskItem.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (rowTasks.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Assigned Task Card Item in Middle Section:
 * - Left vertical glowing violet bar
 * - Task Name
 * - Right: 🕒 60 m
 * - Long-press for 1 second to remove!
 */
@Composable
private fun AssignedTaskCardItem(
    task: DailyAssignedPlanTask,
    isPastDate: Boolean,
    onHoldToRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF160A36))
            .border(1.dp, Color(0xFF381A6B), RoundedCornerShape(12.dp))
            .pointerInput(task.id, isPastDate) {
                if (!isPastDate) {
                    detectTapGestures(
                        onLongPress = {
                            onHoldToRemove()
                        }
                    )
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glowing bar + Task Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE879F9), Color(0xFF8B5CF6))
                            )
                        )
                )

                Text(
                    text = task.taskName,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right: Time (e.g. 🕒 60 m)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${task.durationMinutes} m",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Page 1 (Image 2):
 * - Search bar: [ 🔍 Search...   → ]
 * - Category Tabs with (+) button on the left to add category!
 * - Declared tasks grid
 * - Bottom capsule button: [ (+) New ] to add a new task
 */
@Composable
private fun TaskCatalogPage(
    categories: List<PlanV2Category>,
    tasks: List<PlanV2Task>,
    onAddCategoryClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Primary") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Capsule Search Bar: [ 🔍 Search...   → ] ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFF0F0724))
                .border(1.2.dp, Color(0xFFA855F7).copy(alpha = 0.7f), RoundedCornerShape(23.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Arrow circle button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Search...",
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }
        }

        // ── 2. Category Row with (+) Button on the Left ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // (+) Add category button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E0B3E))
                    .border(1.dp, Color(0xFFA855F7), CircleShape)
                    .clickable { onAddCategoryClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add Category", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            categories.forEach { cat ->
                val isSel = cat.name.equals(selectedCat, ignoreCase = true)
                Text(
                    text = cat.name,
                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                    fontSize = 13.5.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clickable { selectedCat = cat.name }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // ── 3. Declared Tasks in this Category (Matching Image 2 large cards) ──
        val filtered = tasks.filter {
            (it.categoryName.equals(selectedCat, ignoreCase = true) || searchQuery.isNotBlank()) &&
            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val chunked = filtered.chunked(2)
            chunked.forEach { rowTasks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTasks.forEach { task ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF160A36))
                                .border(1.2.dp, Color(0xFFA855F7).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = task.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowTasks.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // ── 4. Bottom (+ New) Capsule Button (Image 2) ──
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF4C1D95))
                    .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(22.dp))
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                    .clickable { onAddTaskClick() }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "New",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Image 3: Time Selector Overlay Dialog
 * - Card with purple glow
 * - Header: 🕒 45 min, Recommended ✨, [ Dsa ] black pill, and [ ✓ ] button
 * - Grid of duration chips (5..60 mins, 1:15..4:00 hrs)
 */
@Composable
private fun PlanTimeSelectorDialog(
    taskName: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }

    // Chips data
    val chipsRow1 = listOf(5 to "5", 10 to "10", 15 to "15", 20 to "20", 30 to "30", 45 to "45", 60 to "60")
    val chipsRow2 = listOf(75 to "1:15", 90 to "1:30", 105 to "1:45", 120 to "2:00")
    val chipsRow3 = listOf(150 to "2:30", 180 to "3:00", 210 to "3:30", 240 to "4:00")

    val formattedDisplay = remember(selectedMinutes) {
        val h = selectedMinutes / 60
        val m = selectedMinutes % 60
        if (h > 0 && m > 0) "$h hr $m m"
        else if (h > 0) "$h hr"
        else "$m min"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(22.dp))
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: [🕒 45 min Recommended ✨] + [ Dsa ] + [ ✓ ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Time & Recommended
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF26104E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = formattedDisplay,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recommended ✨",
                                color = Color(0xFFC084FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Center: Black pill with Task Name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF000000))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = taskName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Right: [ ✓ ] Confirm Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFA855F7))
                            .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(10.dp))
                            .clickable { onConfirm(selectedMinutes) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Chips Grid: Row 1 (5..60)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chipsRow1.forEach { (mins, label) ->
                        val isSel = (selectedMinutes == mins)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFA855F7) else Color(0xFF1B0E3C))
                                .border(1.dp, if (isSel) Color(0xFFE879F9) else Color(0xFF381A6B), RoundedCornerShape(8.dp))
                                .clickable { selectedMinutes = mins },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Chips Grid: Row 2 (1:15..2:00)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chipsRow2.forEach { (mins, label) ->
                        val isSel = (selectedMinutes == mins)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFA855F7) else Color(0xFF1B0E3C))
                                .border(1.dp, if (isSel) Color(0xFFE879F9) else Color(0xFF381A6B), RoundedCornerShape(8.dp))
                                .clickable { selectedMinutes = mins },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Chips Grid: Row 3 (2:30..4:00)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chipsRow3.forEach { (mins, label) ->
                        val isSel = (selectedMinutes == mins)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFA855F7) else Color(0xFF1B0E3C))
                                .border(1.dp, if (isSel) Color(0xFFE879F9) else Color(0xFF381A6B), RoundedCornerShape(8.dp))
                                .clickable { selectedMinutes = mins },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Simple Dialog for entering single text item (Category name or Task name) */
@Composable
private fun AddSimpleInputDialog(
    title: String,
    placeholder: String,
    secondaryPicker: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textVal by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0724))
                .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (secondaryPicker != null) {
                    secondaryPicker()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF160A36))
                        .border(1.dp, Color(0xFF381A6B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = textVal,
                        onValueChange = { textVal = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (textVal.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF6B7280),
                            fontSize = 12.5.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (textVal.isNotBlank()) {
                                onConfirm(textVal.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
