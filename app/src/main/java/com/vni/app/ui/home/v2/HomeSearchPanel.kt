package com.vni.app.ui.home.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vni.app.data.model.DoItem
import com.vni.app.data.model.MotiveHeading
import com.vni.app.data.model.Notice
import com.vni.app.data.model.PlanContainer
import com.vni.app.data.model.Task

/**
 * Search & Quick Panel matching Image 2:
 * - Triggered by dragging top-to-bottom on the Home page.
 * - Top Motive quote & Section B with Search icon in left square.
 * - Capsule Search Bar: [ 🔍 search ].
 * - 5 Quick Action Blocks:
 *   1. [ 📈 MONEY ]
 *   2. [ 📅 PLANS ]
 *   3. [ ⊞ Categories ]
 *   4. [ ✓ DO ]
 *   5. [ ◷ HISTORY ] (Added per user request)
 * - Suggestions / Activity card with Clock icon:
 *   - "APP DEVELOPMENT , completed 3 days before"
 *   - "you washed cloths 6 DAYS before"
 *   - "PROBLEMS SOLVE is pending since 5 DAYS"
 *   - "your weekly progress is 78%" (purple highlighted)
 * - When searching: blocks and suggestions hide, matching items are shown live.
 * - Dismissed by scrolling up, clicking the sidebar, or pressing back.
 */
@Composable
fun HomeSearchPanel(
    tasks: List<Task>,
    notices: List<Notice>,
    doItems: List<DoItem>,
    planContainers: List<PlanContainer>,
    motiveHeadings: List<MotiveHeading>,
    weeklyProgressPct: Int = 78,
    weekWastedMinutes: List<Int> = emptyList(),
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenSentence: () -> Unit,
    onOpenDo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDaily: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onNavigateToTask: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Search Results Match Model
    data class GlobalSearchResult(
        val type: String, // "TASK", "PLAN", "NOTICE", "DO"
        val title: String,
        val subtitle: String,
        val id: Long
    )

    val searchResults = remember(searchQuery, tasks, notices, doItems, planContainers) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            val list = mutableListOf<GlobalSearchResult>()

            tasks.forEach { t ->
                if (t.name.lowercase().contains(q)) {
                    list.add(GlobalSearchResult("TASK", t.name, "Task • ${t.daysLeft} days left", t.id))
                }
            }
            notices.forEach { n ->
                if (n.title.lowercase().contains(q) || n.subtitle.lowercase().contains(q)) {
                    list.add(GlobalSearchResult("NOTICE", n.title, "Notice • ${n.subtitle}", n.id))
                }
            }
            doItems.forEach { d ->
                if (d.name.lowercase().contains(q) || d.specialized.lowercase().contains(q)) {
                    list.add(GlobalSearchResult("DO", d.name, "Do • ${d.specialized.ifEmpty { "General" }}", d.id))
                }
            }
            planContainers.forEach { p ->
                if (p.title.lowercase().contains(q)) {
                    list.add(GlobalSearchResult("PLAN", p.title, "Plan Chapter", p.id))
                }
            }
            list
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
            .zIndex(65f)
            .statusBarsPadding()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                ) { change, dragAmount ->
                    totalDrag += dragAmount
                    if (totalDrag < -20f) {
                        change.consume()
                        keyboardController?.hide()
                        onDismiss()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Section A: Top Motive Bar ──
            HomeTopMotiveBar(
                motiveHeadings = motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = onOpenSettings
            )

            // ── Section B: Date Card with Search Icon inside purple square (Image 2) ──
            HomeDateAndCountersBar(
                tasksCount = tasks.size,
                noticesCount = notices.count { it.iconName != "School" && it.durationMinutes == null && it.reminderTriggerMillis == null },
                remindersCount = notices.count { it.iconName == "School" || it.durationMinutes != null || it.reminderTriggerMillis != null },
                activeSectionType = "SEARCH"
            )

            Spacer(Modifier.height(14.dp))

            // ── Search Bar Pill (Image 2) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF0F0724))
                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(26.dp))
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFFA855F7))
                    .clickable {
                        isSearchFocused = true
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotEmpty()) isSearchFocused = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFA855F7)),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "search",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchQuery = ""
                                    isSearchFocused = false
                                    keyboardController?.hide()
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Content Switch: Live Search Results VS Quick Blocks & Suggestions ──
            if (isSearchFocused && searchQuery.isNotBlank()) {
                // Live Search Results List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching items found",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        searchResults.forEach { res ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF14082D))
                                    .border(1.2.dp, Color(0xFF581C87), RoundedCornerShape(16.dp))
                                    .clickable {
                                        when (res.type) {
                                            "TASK" -> onNavigateToTask(res.id)
                                            "NOTICE" -> onDismiss()
                                            "DO" -> onOpenDo()
                                            "PLAN" -> onOpenPlans()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = res.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = res.subtitle,
                                        color = Color(0xFFA78BFA),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Default View: Horizontally Scrollable Categories (Image 1) + Suggestions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val pagerState = rememberPagerState { 2 }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        if (page == 0) {
                            // Page 1: 6 Core Categories
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SearchNavCard(
                                        title = "MONEY",
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenMoney
                                    )
                                    SearchNavCard(
                                        title = "PLANS",
                                        icon = Icons.Outlined.CalendarMonth,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenPlans
                                    )
                                    SearchNavCard(
                                        title = "SENTENCE",
                                        icon = Icons.Outlined.GridView,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenSentence
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SearchNavCard(
                                        title = "DO",
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenDo
                                    )
                                    SearchNavCard(
                                        title = "HISTORY",
                                        icon = Icons.Outlined.History,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenHistory
                                    )
                                    SearchNavCard(
                                        title = "DAILY",
                                        icon = Icons.Outlined.Schedule,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenDaily
                                    )
                                }
                            }
                        } else {
                            // Page 2: Chat Card on the left matching Image 1
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SearchNavCard(
                                        title = "chat",
                                        icon = Icons.Outlined.Mail,
                                        modifier = Modifier.weight(1f),
                                        onClick = onOpenChat
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Spacer(Modifier.weight(1f))
                                }
                                // Spacer to maintain consistent card row height
                                Spacer(Modifier.height(84.dp))
                            }
                        }
                    }

                    // Pager Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(2) { idx ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(if (pagerState.currentPage == idx) 16.dp else 6.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (pagerState.currentPage == idx) Color(0xFFA855F7)
                                        else Color(0xFF4C1D95)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Suggestions & Pet Analytics Card (Requirements 2 & 6)
                    var suggestionCycleIndex by remember { mutableIntStateOf(0) }

                    val completedTask = remember(tasks) { tasks.firstOrNull { it.completedToday || it.lastCompletedDateText != "None" } }
                    val pendingTask = remember(tasks) { tasks.firstOrNull { !it.completedToday } }
                    val activeNotice = remember(notices) { notices.firstOrNull() }
                    val wastedToday = remember(weekWastedMinutes) { weekWastedMinutes.firstOrNull() ?: 0 }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF0F0724))
                            .border(1.3.dp, Color(0xFF6B21A8), RoundedCornerShape(22.dp))
                            .shadow(elevation = 14.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFA855F7))
                            .clickable {
                                suggestionCycleIndex = (suggestionCycleIndex + 1) % 7
                            }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Clock Icon in circle [ 🕒 ] - Clicking cycles insights dynamically
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF220E49))
                                    .border(1.5.dp, Color(0xFFA855F7), CircleShape)
                                    .clickable {
                                        suggestionCycleIndex = (suggestionCycleIndex + 1) % 7
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = "Cycle Insights",
                                    tint = Color(0xFFE879F9),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            // Right: Real Data & Pet Non-linear Analytics Cycling (7 dynamic variations)
                            AnimatedContent(
                                targetState = suggestionCycleIndex,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                                },
                                label = "SuggestionCycleAnimation",
                                modifier = Modifier.weight(1f)
                            ) { cycle ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (cycle) {
                                        0 -> {
                                            // Cycle 0: Task Progress & Notice Summary
                                            if (completedTask != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${completedTask.name.uppercase()} ",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (completedTask.completedToday) ", completed today" else ", completed ${completedTask.lastCompletedDateText}",
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "DAILY HABITS , start your first habit today",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp
                                                )
                                            }

                                            if (activeNotice != null) {
                                                Text(
                                                    text = "${activeNotice.title} is scheduled (${activeNotice.deadlineText.replace("Deadline: ", "")})",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (doItems.isNotEmpty()) {
                                                Text(
                                                    text = "DO: ${doItems.first().name} is in progress",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "all notices & works up to date",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            if (pendingTask != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${pendingTask.name.uppercase()} ",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val remainingMins = (pendingTask.targetMinutes - pendingTask.todayMinutes).coerceAtLeast(0)
                                                    Text(
                                                        text = if (remainingMins > 0) "is pending (${remainingMins}m target left)" else "ready to log",
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "All scheduled tasks completed for today! 🎉",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "your weekly progress is ",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "$weeklyProgressPct%",
                                                    color = Color(0xFFE879F9),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        1 -> {
                                            // Cycle 1: Pet Focus & Wasted Minutes Analytics
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "PET FOCUS , ",
                                                    color = Color(0xFFE879F9),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "weekly discipline at $weeklyProgressPct%",
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (wastedToday > 0) {
                                                Text(
                                                    text = "distraction alert: ${wastedToday}m wasted time recorded today",
                                                    color = Color(0xFFF87171),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            } else {
                                                Text(
                                                    text = "zero wasted time detected today — pristine focus flow",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            val topTask = tasks.maxByOrNull { it.todayMinutes }
                                            if (topTask != null && topTask.todayMinutes > 0) {
                                                Text(
                                                    text = "${topTask.name.uppercase()} leads today with ${topTask.todayMinutes}m focused time",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            } else {
                                                Text(
                                                    text = "ready for your next deep focus session",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }

                                        2 -> {
                                            // Cycle 2: Critical Deadlines & Action Items
                                            if (activeNotice != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "DEADLINE , ",
                                                        color = Color(0xFFF43F5E),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = "${activeNotice.title} (${activeNotice.deadlineText.replace("Deadline: ", "")})",
                                                        color = Color.White,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "deadlines clear — excellent proactive planning",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            if (doItems.isNotEmpty()) {
                                                Text(
                                                    text = "DO: ${doItems.first().name} is queued for immediate action",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "all quick DO items cleared out",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            val doneCount = tasks.count { it.completedToday }
                                            Text(
                                                text = "$doneCount of ${tasks.size} active daily goals checked off",
                                                color = Color(0xFFC084FC),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        3 -> {
                                            // Cycle 3: Pet Daily Rhythm & Sleep Insights
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "PET RHYTHM , ",
                                                    color = Color(0xFFA855F7),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "peak mental window between 8 AM - 1 PM",
                                                    color = Color.White,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            Text(
                                                text = "consistent sleep timing unlocks 2x stamina tomorrow",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (pendingTask != null) {
                                                Text(
                                                    text = "quick sprint: spend 15m on ${pendingTask.name} before rest",
                                                    color = Color(0xFFE879F9),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            } else {
                                                Text(
                                                    text = "daily targets fully achieved — rest well tonight! 🌙",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        4 -> {
                                            // Cycle 4: Habit Consistency & Resource Flow
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "HABIT STREAK , ",
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "consistency rating is $weeklyProgressPct%",
                                                    color = Color.White,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            Text(
                                                text = "financial flow timeline synced with current week",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (planContainers.isNotEmpty()) {
                                                Text(
                                                    text = "plan module: ${planContainers.size} categories actively configured",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "set up tomorrow's timeline in Plans",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }

                                        5 -> {
                                            // Cycle 5: Pet Motive & Direct Action Push
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "PET MOTIVE , ",
                                                    color = Color(0xFFE879F9),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "\"Nobody comes. You either change or get struck.\"",
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (pendingTask != null) {
                                                Text(
                                                    text = "${pendingTask.name.uppercase()} is waiting — tackle it now!",
                                                    color = Color(0xFFF59E0B),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = "unstoppable momentum maintained today!",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "small disciplines repeated daily compound into mastery",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp
                                            )
                                        }

                                        else -> {
                                            // Cycle 6: Strategic Tomorrow Plan & Overall Index
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "TOMORROW PREP , ",
                                                    color = Color(0xFFA855F7),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "assign upcoming tasks today for instant clarity",
                                                    color = Color.White,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            Text(
                                                text = "active trajectory: $weeklyProgressPct% efficiency across all goals",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "end of day check-in: review your achievements and recharge",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
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
 * Quick Action Block matching Image 2:
 * Rounded card with icon, title, and glowing violet bottom highlight line.
 */
@Composable
private fun SearchNavCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF220E49), Color(0xFF14082D))
                )
            )
            .border(1.2.dp, Color(0xFF6B21A8), RoundedCornerShape(18.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0xFFA855F7))
            .clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFE879F9),
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Glowing bottom accent line (matching Image 2)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(42.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFA855F7), Color(0xFFE879F9), Color(0xFFA855F7))
                    )
                )
        )
    }
}
