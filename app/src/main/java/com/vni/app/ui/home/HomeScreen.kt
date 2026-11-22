package com.vni.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vni.app.SimpleHomeViewModel
import com.vni.app.data.model.Task
import com.vni.app.ui.home.v2.AddNoticeV2Dialog
import com.vni.app.ui.home.v2.AddReminderV2Dialog
import com.vni.app.ui.home.v2.DailyScreen
import com.vni.app.ui.home.v2.DoV2Screen
import com.vni.app.ui.home.v2.HomeCalendarScreen
import com.vni.app.ui.home.v2.HomeDateAndCountersBar
import com.vni.app.ui.home.v2.HomeEventsSection
import com.vni.app.ui.home.v2.HomeNoticeSection
import com.vni.app.ui.home.v2.HomeProgressDrawer
import com.vni.app.ui.home.v2.HomeSearchPanel
import com.vni.app.ui.home.v2.HomeSidebarDock
import com.vni.app.ui.home.v2.HomeTasksSection
import com.vni.app.ui.home.v2.HomeTopMotiveBar
import com.vni.app.ui.home.v2.ChatV2Screen
import com.vni.app.ui.home.v2.NotesV2Screen
import com.vni.app.ui.home.v2.PlanV2Screen
import com.vni.app.ui.home.v2.SentenceV2Screen
import com.vni.app.ui.home.v2.SettingsV2Screen
import com.vni.app.ui.focus.FocusScreen
import com.vni.app.ui.focus.LiveFocusModeScreen
import com.vni.app.ui.security.SecuritySettingsDialog
import com.vni.app.ui.theme.DeepNavy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Root home screen V2:
 * - Shared top Section A (HomeTopMotiveBar) stays fixed while scrolling smoothly between:
 *   - Page 0: Notes Section (NotesV2Screen embedded) - swipe left-to-right from Home!
 *   - Page 1: Home Section (Date/Counters, Tasks or Notices, Real Progress Drawer) - swipe right-to-left from Notes!
 * - Clicking segmented time bars opens TimeSliderBottomCard ONLY (Edit Task page removed).
 * - Clicking Date block toggles Calendar; clicking Notice counter toggles Notices <-> Tasks.
 * - Single persistent HomeSidebarDock visible in the exact same position & UI across the entire app.
 */
@Composable
fun HomeScreen(
    viewModel: SimpleHomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var sidebarOpen              by remember { mutableStateOf(false) }
    var currentActiveSection     by remember { mutableStateOf("TASKS") }
    var showAddNotice            by remember { mutableStateOf(false) }
    var showAddRemind            by remember { mutableStateOf(false) }
    var showHistoryPanel         by remember { mutableStateOf(false) }
    var timeSliderTask           by remember { mutableStateOf<Task?>(null) }
    var showCalendarPanel        by remember { mutableStateOf(false) }
    var showSearchPanel          by remember { mutableStateOf(false) }
    var showMoneyInput           by remember { mutableStateOf(false) }
    var isMoneyExpense           by remember { mutableStateOf(true) }
    var showMoneyScreen          by remember { mutableStateOf(false) }
    var showProgressDetailScreen by remember { mutableStateOf(false) }
    var showDoSection            by remember { mutableStateOf(false) }
    var showMotiveScreen         by remember { mutableStateOf(false) }
    var showPlanScreen           by remember { mutableStateOf(false) }
    var showDailyScreen          by remember { mutableStateOf(false) }
    var showDayTimelineScreen    by remember { mutableStateOf(false) }
    var showSecuritySettings     by remember { mutableStateOf(false) }
    var showChatScreen           by remember { mutableStateOf(false) }
    var returnToSearchOnBack     by remember { mutableStateOf(false) }
    var chatBackAction           by remember { mutableStateOf<(() -> Boolean)?>(null) }
    var notesBackAction          by remember { mutableStateOf<(() -> Boolean)?>(null) }
    var showFocusScreen          by remember { mutableStateOf(false) }
    var isLiveFocusRunning       by remember { mutableStateOf(false) }
    var liveFocusSessionName     by remember { mutableStateOf("STUDY") }
    var liveFocusMode            by remember { mutableStateOf("TIMER") }
    var liveFocusDurationSec     by remember { mutableStateOf(25 * 60L) }
    var liveFocusBreakSec        by remember { mutableStateOf(5 * 60L) }
    var liveFocusIntervalWorkSec by remember { mutableStateOf(30 * 60L) }
    var liveFocusIntervalBreakSec by remember { mutableStateOf(5 * 60L) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appPrefs = remember(context) { context.getSharedPreferences("vni_app_prefs_v2", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(appPrefs.getBoolean("notifications_enabled", true)) }

    val onToggleNotifications = {
        val newState = !notificationsEnabled
        notificationsEnabled = newState
        appPrefs.edit().putBoolean("notifications_enabled", newState).apply()
    }

    // Pet assistant is opened manually by user from the sidebar dock (no auto-popup on app open)

    val coroutineScope = rememberCoroutineScope()
    // Page 0 = Notes, Page 1 = Home (Tasks/Notices & Progress)
    val homePagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })

    // Real today's tasks synced directly from Plan
    val activeTasks = remember(uiState.tasks) {
        uiState.tasks.filter { it.daysLeft > 0 }
    }
    val noticesCount = uiState.notices.count { it.iconName != "School" && it.durationMinutes == null && it.reminderTriggerMillis == null }
    val remindersCount = uiState.notices.count { it.iconName == "School" || it.durationMinutes != null || it.reminderTriggerMillis != null }
    val totalLoggedMinutes = activeTasks.sumOf { it.todayMinutes }

    // Real current week stats calculated from dailyAssignedPlanTasks (Sunday..Today)
    val weekStats = remember(uiState.dailyAssignedPlanTasks) {
        val now = Calendar.getInstance()
        val sundayCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(now.time)
        val weekKeys = (0..6).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = sundayCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.time)
        }.filter { it <= todayStr }

        val weekTasks = uiState.dailyAssignedPlanTasks.filter { it.dateKey in weekKeys }
        val weekTarget = weekTasks.sumOf { it.durationMinutes }
        val weekSpent = weekTasks.sumOf { it.spentMinutes.coerceAtMost(it.durationMinutes) }
        val pct = if (weekTarget > 0) ((weekSpent.toFloat() / weekTarget) * 100).toInt().coerceIn(0, 100) else 0
        Pair(pct, weekTasks.sumOf { it.spentMinutes })
    }
    val weekConsistencyPct = weekStats.first
    val weekTotalLoggedMinutes = weekStats.second

    val isFullScreenOverlayOpen = showProgressDetailScreen || showMoneyScreen || showDoSection ||
        showMotiveScreen || showPlanScreen || showDailyScreen || showDayTimelineScreen ||
        showCalendarPanel || showSearchPanel || showHistoryPanel || showSecuritySettings

    val closeAllOverlays = {
        showChatScreen = false
        returnToSearchOnBack = false
        showCalendarPanel = false
        showSearchPanel = false
        showPlanScreen = false
        showDailyScreen = false
        showMoneyScreen = false
        showDoSection = false
        showHistoryPanel = false
        showMotiveScreen = false
        showProgressDetailScreen = false
        showDayTimelineScreen = false
        showSecuritySettings = false
        showFocusScreen = false
        timeSliderTask = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070314))
    ) {

        // Hardware Back from Notes (Page 0) returns smoothly to Home (Page 1)
        BackHandler(enabled = !isFullScreenOverlayOpen && homePagerState.currentPage == 0) {
            coroutineScope.launch {
                homePagerState.animateScrollToPage(1)
            }
        }

        // ── Main Shared Top Bar + Smooth HorizontalPager (Page 0: Notes | Page 1: Home) ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Fixed Section A: Top Bar & Motive Sentences (stays same while scrolling between Notes and Home!)
            HomeTopMotiveBar(
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true }
            )

            // Smooth horizontal scroll between Notes (Page 0) and Home (Page 1)
            HorizontalPager(
                state = homePagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) {
                    // ── PAGE 0: Notes UI attached smoothly to the left of Home ──
                    NotesV2Screen(
                        noteCategories = uiState.noteCategories,
                        onAddCategory = viewModel::addNoteCategory,
                        onDeleteCategory = viewModel::deleteNoteCategory,
                        onAddTopic = viewModel::addNoteTopic,
                        onDeleteTopic = viewModel::deleteNoteTopic,
                        onUpdateTopic = viewModel::updateNoteTopic,
                        onUpdateCategory = viewModel::updateNoteCategory,
                        onUpdateTopicTitle = viewModel::updateNoteTopicTitle,
                        onDismiss = {
                            coroutineScope.launch { homePagerState.animateScrollToPage(1) }
                        },
                        onRegisterBackAction = { notesBackAction = it },
                        isEmbeddedInPager = true
                    )
                } else {
                    // ── PAGE 1: Home (Date/Counters + Tasks/Notices + Real Progress Drawer) ──
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Drag down anywhere in Section B to open Search accurately
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    var totalDrag = 0f
                                    detectVerticalDragGestures(
                                        onDragStart = { totalDrag = 0f },
                                        onDragEnd = { totalDrag = 0f },
                                        onDragCancel = { totalDrag = 0f }
                                    ) { change, dragAmount ->
                                        totalDrag += dragAmount
                                        if (totalDrag > 20f) {
                                            change.consume()
                                            showSearchPanel = true
                                        }
                                    }
                                }
                        ) {
                            HomeDateAndCountersBar(
                                tasksCount = activeTasks.size,
                                noticesCount = noticesCount,
                                remindersCount = remindersCount,
                                eventsCount = uiState.calendarEvents.size,
                                activeSectionType = currentActiveSection,
                                onClickDateBlock = { showCalendarPanel = !showCalendarPanel },
                                onClickNoticeBlock = {
                                    currentActiveSection = if (currentActiveSection == "NOTICES") "TASKS" else "NOTICES"
                                },
                                onDragNoticeLeft = {
                                    currentActiveSection = "EVENTS"
                                },
                                onDragNoticeRight = {
                                    if (currentActiveSection != "NOTICES") {
                                        currentActiveSection = "NOTICES"
                                    } else {
                                        showAddNotice = true
                                    }
                                }
                            )
                        }

                        // Section C: Tasks List or Notices/Reminders or All Events (Requirement 7)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (currentActiveSection) {
                                "NOTICES" -> {
                                    HomeNoticeSection(
                                        notices = uiState.notices,
                                        onAddReminder = { showAddRemind = true },
                                        onAddNotice = { showAddNotice = true },
                                        onCompleteReminder = { id -> viewModel.removeNotice(id) },
                                        onCompleteNotice = { id -> viewModel.removeNotice(id) },
                                        onSidebarOpen = { sidebarOpen = true }
                                    )
                                }
                                "EVENTS" -> {
                                    HomeEventsSection(
                                        events = uiState.calendarEvents,
                                        onDeleteEvent = viewModel::deleteCalendarEvent,
                                        onOpenCalendar = { showCalendarPanel = true },
                                        onSidebarOpen = { sidebarOpen = true }
                                    )
                                }
                                else -> {
                                    HomeTasksSection(
                                        tasks = activeTasks,
                                        activeIndex = uiState.activeTaskIndex,
                                        onSelectActiveTask = viewModel::setActiveTask,
                                        onSidebarOpen = { sidebarOpen = true },
                                        onOpenTaskEdit = {
                                            val selected = activeTasks.getOrNull(uiState.activeTaskIndex) ?: activeTasks.firstOrNull()
                                            if (selected != null) {
                                                timeSliderTask = selected
                                            }
                                        },
                                        onOpenSearch = { showSearchPanel = true },
                                        onOpenNotes = {
                                            coroutineScope.launch { homePagerState.animateScrollToPage(0) }
                                        }
                                    )
                                }
                            }
                        }

                        // Section D: Bottom Consistency & Progress Drawer (100% Real Data!)
                        if (currentActiveSection == "TASKS" && !isFullScreenOverlayOpen) {
                            HomeProgressDrawer(
                                tasks = activeTasks,
                                dailyAssignedTasks = uiState.dailyAssignedPlanTasks,
                                todayProgressPct = uiState.todayProgress,
                                weekConsistencyPct = weekConsistencyPct,
                                todayMinutes = totalLoggedMinutes,
                                weekTotalMinutes = weekTotalLoggedMinutes
                            )
                        }
                    }
                }
            }
        }

        // ── Unified Persistent Sidebar Dock (Requirement 1) ──
        val isAnyPageOpen = showChatScreen || showFocusScreen || showDailyScreen || showPlanScreen || showMoneyScreen || showHistoryPanel ||
                showMotiveScreen || showDoSection || showCalendarPanel || showSearchPanel ||
                showSecuritySettings || (homePagerState.currentPage == 0)

        val handleGoBack = {
            when {
                // Requirement 2: Sidebar back steps back a single page/level at a time
                showChatScreen -> {
                    if (chatBackAction?.invoke() != true) {
                        showChatScreen = false
                        if (returnToSearchOnBack) {
                            returnToSearchOnBack = false
                            showSearchPanel = true
                        }
                    }
                }
                homePagerState.currentPage == 0 -> {
                    if (notesBackAction?.invoke() != true) {
                        coroutineScope.launch { homePagerState.animateScrollToPage(1) }
                    }
                }
                showFocusScreen -> showFocusScreen = false
                showDailyScreen -> {
                    showDailyScreen = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showPlanScreen -> {
                    showPlanScreen = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showMoneyScreen -> {
                    showMoneyScreen = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showHistoryPanel -> {
                    showHistoryPanel = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showMotiveScreen -> {
                    showMotiveScreen = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showDoSection -> {
                    showDoSection = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
                showCalendarPanel -> showCalendarPanel = false
                showSearchPanel -> showSearchPanel = false
                showSecuritySettings -> showSecuritySettings = false
            }
        }

        val sidebarActiveSection = when {
            showFocusScreen || isLiveFocusRunning -> "FOCUS"
            showDailyScreen -> "DAILY"
            showPlanScreen -> "PLAN"
            else -> "HOME"
        }

        HomeSidebarDock(
            isOpen          = sidebarOpen,
            activeSection   = sidebarActiveSection,
            isBackAvailable = isAnyPageOpen,
            onBack          = handleGoBack,
            onOpen          = { sidebarOpen = true },
            onDismiss       = { sidebarOpen = false },
            onSelectSection = { section ->
                sidebarOpen = false
                when (section) {
                    "HOME" -> {
                        closeAllOverlays()
                        currentActiveSection = "TASKS"
                        coroutineScope.launch { homePagerState.animateScrollToPage(1) }
                    }
                    "PLAN" -> {
                        closeAllOverlays()
                        showPlanScreen = true
                    }
                    "FOCUS" -> {
                        closeAllOverlays()
                        showFocusScreen = true
                    }
                    "DAILY" -> {
                        closeAllOverlays()
                        showDailyScreen = true
                    }
                }
            }
        )

        // ── Time Slider Bar Overlay (Opens ONLY when clicking time bars in task!) ──
        if (timeSliderTask != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .zIndex(75f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { timeSliderTask = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                TimeSliderBottomCard(
                    task = timeSliderTask!!,
                    onConfirm = { newMins ->
                        viewModel.updateTodayMinutes(timeSliderTask!!.id, newMins)
                        timeSliderTask = null
                    },
                    onDismiss = { timeSliderTask = null },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                )
            }
        }

        // ── Calendar Screen with 100% REAL per-day data ──
        if (showCalendarPanel) {
            HomeCalendarScreen(
                tasks = activeTasks,
                dailyAssignedTasks = uiState.dailyAssignedPlanTasks,
                completedHistory = uiState.deletedNoticesHistory,
                notices = uiState.notices,
                motiveHeadings = uiState.motiveHeadings,
                dayProgressList = uiState.calendarProgress,
                dayNotesMap = uiState.calendarDayNotes,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onSaveDayNote = { year, month, day, note ->
                    viewModel.setCalendarDayNote(year, month, day, note)
                },
                calendarEvents = uiState.calendarEvents,
                onAddEvent = viewModel::addCalendarEvent,
                onDeleteEvent = viewModel::deleteCalendarEvent,
                onOpenNotices = {
                    showCalendarPanel = false
                    currentActiveSection = "NOTICES"
                    coroutineScope.launch { homePagerState.animateScrollToPage(1) }
                },
                onDismiss = { showCalendarPanel = false }
            )
        }

        // ── Search & Quick Action Panel ──
        if (showSearchPanel) {
            HomeSearchPanel(
                tasks = activeTasks,
                notices = uiState.notices,
                doItems = uiState.doItems,
                planContainers = uiState.planContainers,
                motiveHeadings = uiState.motiveHeadings,
                weeklyProgressPct = weekConsistencyPct,
                weekWastedMinutes = uiState.weekWastedMinutes,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onOpenMoney = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showMoneyScreen = true
                },
                onOpenPlans = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showPlanScreen = true
                },
                onOpenSentence = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showMotiveScreen = true
                },
                onOpenDo = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showDoSection = true
                },
                onOpenHistory = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showHistoryPanel = true
                },
                onOpenDaily = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showDailyScreen = true
                },
                onOpenChat = {
                    returnToSearchOnBack = true
                    showSearchPanel = false
                    showChatScreen = true
                },
                onNavigateToTask = { taskId ->
                    showSearchPanel = false
                    val taskIndex = activeTasks.indexOfFirst { it.id == taskId }
                    if (taskIndex >= 0) {
                        viewModel.setActiveTask(taskIndex)
                        timeSliderTask = activeTasks[taskIndex]
                    }
                    coroutineScope.launch { homePagerState.animateScrollToPage(1) }
                },
                onDismiss = { showSearchPanel = false }
            )
        }

        // ── Add Notice Dialog ──
        if (showAddNotice) {
            AddNoticeV2Dialog(
                onDismiss = { showAddNotice = false },
                onSave = { title, subtitle, deadlineText ->
                    viewModel.addNotice(
                        title = title,
                        subtitle = subtitle,
                        isReminder = false,
                        reminderDay = deadlineText
                    )
                    showAddNotice = false
                }
            )
        }

        // ── Add Reminder Dialog ──
        if (showAddRemind) {
            AddReminderV2Dialog(
                onDismiss = { showAddRemind = false },
                onSave = { name, durationMinutes, categoryType, exactTriggerMillis ->
                    viewModel.addNotice(
                        title = name,
                        subtitle = categoryType,
                        isReminder = true,
                        reminderDay = "Today",
                        durationMinutes = durationMinutes,
                        exactTriggerMillis = exactTriggerMillis
                    )
                    showAddRemind = false
                }
            )
        }

        // ── Chat Screen (Image 2) ──
        if (showChatScreen) {
            ChatV2Screen(
                chatCategories = uiState.chatCategories,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onAddCategory = viewModel::addChatCategory,
                onUpdateCategory = viewModel::updateChatCategory,
                onDeleteCategory = viewModel::deleteChatCategory,
                onAddTopic = viewModel::addChatTopic,
                onUpdateTopicTitle = viewModel::updateChatTopicTitle,
                onDeleteTopic = viewModel::deleteChatTopic,
                onAddMessage = viewModel::addChatMessage,
                onDeleteMessage = viewModel::deleteChatMessage,
                onRegisterBackAction = { chatBackAction = it },
                onDismiss = {
                    showChatScreen = false
                    if (returnToSearchOnBack) {
                        returnToSearchOnBack = false
                        showSearchPanel = true
                    }
                }
            )
        }

        // ── History Panel ──
        if (showHistoryPanel) {
            HistoryPanel(
                deletedNotices = uiState.deletedNoticesHistory,
                moneyHistory   = uiState.moneyHistory,
                historyCategories = uiState.historyCategories,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onRemoveItem   = viewModel::removeHistoryItem,
                onAddCategory  = viewModel::addHistoryCategory,
                onRemoveCategory = viewModel::removeHistoryCategory,
                onAddHistoryItem = viewModel::addCustomHistoryItem,
                onDismiss      = { showHistoryPanel = false }
            )
        }

        // ── Money Input Panel ──
        if (showMoneyInput) {
            MoneyInputPanel(
                isExpense = isMoneyExpense,
                onConfirm = { amount, purpose, dateMillis ->
                    viewModel.addMoneyTransaction(amount, purpose, isMoneyExpense, dateMillis)
                    showMoneyInput = false
                },
                onDismiss = { showMoneyInput = false }
            )
        }

        // ── Money Screen Page Overlay ──
        if (showMoneyScreen) {
            MoneyScreen(
                moneyHistory = uiState.moneyHistory,
                moneyCategories = uiState.moneyCategories,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onAddTransaction = { amount, purpose, isExpense, category ->
                    viewModel.addMoneyTransaction(amount, purpose, isExpense, category = category)
                },
                onAddCategory = viewModel::addMoneyCategory,
                onRemoveCategory = viewModel::removeMoneyCategory,
                onDismiss = { showMoneyScreen = false }
            )
        }

        // ── Plan Screen V2 Overlay ──
        if (showPlanScreen) {
            PlanV2Screen(
                planCategories = uiState.planV2Categories,
                planTasks = uiState.planV2Tasks,
                dailyAssignedTasks = uiState.dailyAssignedPlanTasks,
                notices = uiState.notices,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onAddCategory = viewModel::addPlanCategoryV2,
                onAddTask = viewModel::addPlanTaskV2,
                onAssignTaskToDate = viewModel::assignTaskToDateV2,
                onRemoveAssignedTask = viewModel::removeAssignedTaskV2,
                onDismiss = { showPlanScreen = false },
                modifier = Modifier.zIndex(65f)
            )
        }

        // ── Categories / Motive Screen Page Overlay ──
        if (showMotiveScreen) {
            SentenceV2Screen(
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onAddHeading = viewModel::addMotiveHeading,
                onDeleteHeading = viewModel::deleteMotiveHeading,
                onUpdateHeadingSentences = viewModel::updateMotiveHeadingSentences,
                onDismiss = { showMotiveScreen = false }
            )
        }

        // ── Do Section Page Overlay ──
        if (showDoSection) {
            DoV2Screen(
                doCategories = uiState.doCategories,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                onAddCategory = viewModel::addDoCategory,
                onDeleteCategory = viewModel::deleteDoCategory,
                onAddPoint = viewModel::addDoTopic,
                onDeletePoint = viewModel::deleteDoTopic,
                onDismiss = { showDoSection = false }
            )
        }

        // ── Daily Section Overlay (Requirement 8) ──
        if (showDailyScreen) {
            DailyScreen(
                uniTasks = uiState.uniTasks,
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onOpenSettings = { showSecuritySettings = true },
                tasksCount = activeTasks.size,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                onAddDailyTask = { name, hour, minute, timeLabel, notifyBeforeMins ->
                    viewModel.addUniTask(name, hour, minute, timeLabel, notifyBeforeMins)
                },
                onDeleteDailyTask = viewModel::deleteUniTask,
                onDismiss = { showDailyScreen = false }
            )
        }

        // ── Progress Detail Screen Overlay ──
        if (showProgressDetailScreen) {
            ProgressDetailScreen(
                tasks = activeTasks,
                pastTasks = uiState.pastTasks,
                completedCount = uiState.completedCount,
                totalCount = uiState.totalCount,
                uniTasks = uiState.uniTasks,
                isUniTaskModeInitial = uiState.isUniTaskMode,
                weekWastedMinutes = uiState.weekWastedMinutes,
                onLogWastedMinutes = viewModel::logWastedMinutes,
                onDeletePastTask = viewModel::deletePastTask,
                onDismiss = { showProgressDetailScreen = false }
            )
        }

        // ── Day Schedule Planner Screen Overlay ──
        if (showDayTimelineScreen) {
            DayTimelineScreen(
                plans = uiState.daySchedulePlans,
                selectedPlanName = uiState.selectedPlanName,
                tasks = activeTasks,
                uniTasks = uiState.uniTasks,
                onSelectPlan = viewModel::selectDayPlan,
                onAddPlan = viewModel::addDayPlan,
                onAddSection = { name, startTime, endTime, isWakeUp, isSleep, startHour, startMin, endHour, endMin ->
                    viewModel.addSectionToCurrentPlan(
                        name = name,
                        startTime = startTime,
                        endTime = endTime,
                        isWakeUpStart = isWakeUp,
                        isSleepEnd = isSleep,
                        startHour = startHour,
                        startMinute = startMin,
                        endHour = endHour,
                        endMinute = endMin
                    )
                },
                onDeleteSection = viewModel::deleteSection,
                onAllocateTask = { sectionId, taskId, isUniTask, name, priority ->
                    viewModel.allocateTaskToSection(sectionId, taskId, isUniTask, name, priority)
                },
                onRemoveAllocation = { sectionId, taskId, isUniTask ->
                    viewModel.removeTaskAllocation(sectionId, taskId, isUniTask)
                },
                onDismiss = { showDayTimelineScreen = false }
            )
        }

        // ── Settings Page Overlay (Matching Image) ──
        if (showSecuritySettings) {
            SettingsV2Screen(
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                tasksCount = activeTasks.size,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                viewModel = viewModel,
                onDismiss = { showSecuritySettings = false }
            )
        }

        // ── Focus Section Overlay (Images 1, 2, 3, 4) ──
        if (showFocusScreen && !isLiveFocusRunning) {
            FocusScreen(
                motiveHeadings = uiState.motiveHeadings,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                tasksCount = activeTasks.size,
                noticesCount = noticesCount,
                remindersCount = remindersCount,
                viewModel = viewModel,
                onLaunchLiveFocus = { name, mode, durSec, breakSec, intWorkSec, intBreakSec ->
                    liveFocusSessionName = name
                    liveFocusMode = mode
                    liveFocusDurationSec = durSec
                    liveFocusBreakSec = breakSec
                    liveFocusIntervalWorkSec = intWorkSec
                    liveFocusIntervalBreakSec = intBreakSec
                    isLiveFocusRunning = true
                },
                onDismiss = { showFocusScreen = false }
            )
        }

        // ── Live Fullscreen Focus Mode Overlay (Image 5) ──
        if (isLiveFocusRunning) {
            val focusQuotes = remember(uiState.noteCategories) {
                val focusCat = uiState.noteCategories.find { it.name.equals("Focus", ignoreCase = true) }
                focusCat?.topics?.flatMap { topic ->
                    if (topic.content.isNotBlank()) topic.content.split("\n").map { it.trim() }
                    else listOf(topic.title)
                }?.filter { it.isNotBlank() } ?: emptyList<String>()
            }

            LiveFocusModeScreen(
                sessionName = liveFocusSessionName,
                mode = liveFocusMode,
                targetDurationSeconds = liveFocusDurationSec,
                breakDurationSeconds = liveFocusBreakSec,
                intervalFocusSeconds = liveFocusIntervalWorkSec,
                intervalBreakSeconds = liveFocusIntervalBreakSec,
                motiveQuotes = focusQuotes,
                onFinishSession = { elapsedSec ->
                    isLiveFocusRunning = false
                    val focusPrefs = context.getSharedPreferences("vni_focus_prefs", android.content.Context.MODE_PRIVATE)
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val currentMins = focusPrefs.getInt("today_focused_mins_${todayKey}", 39)
                    val addedMins = (elapsedSec / 60).toInt()
                    focusPrefs.edit().putInt("today_focused_mins_${todayKey}", currentMins + addedMins).apply()
                }
            )
        }
    }
}
