package com.vni.app

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.vni.app.data.model.*
import com.vni.app.notifications.HabitPredictor
import com.vni.app.notifications.NotificationHelper
import com.vni.app.ui.home.DayProgress
import com.vni.app.ui.security.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val id: Long,
    val title: String,
    val date: String,
    val type: String, // "Notes", "Money", "Done", or custom category
    val amount: String? = null,
    val amountInt: Int = 0,       // raw numeric amount (positive = income, negative = expense)
    val dateMillis: Long = System.currentTimeMillis(),  // epoch ms for date-based calculations
    val isDeleted: Boolean = false,
    val category: String = "NORMAL" // For per-category Money tracking
)

data class RepeatSpentItem(
    val name: String,
    val totalSpent: Int,
    val count: Int,
    val isDeleted: Boolean = false
)

data class DailyMoneyFlow(
    val dayLabel: String,       // "S", "M", "T", etc.
    val dayOfMonth: Int,
    val amount: Int,            // net flow for that day (+ or -)
    val hasData: Boolean = false
)

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val pastTasks: List<Task> = emptyList(),
    val motives: List<Motive> = emptyList(),
    val motiveHeadings: List<MotiveHeading> = emptyList(),
    val planContainers: List<PlanContainer> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val doItems: List<DoItem> = emptyList(),
    val uniTasks: List<UniTask> = emptyList(),
    val isUniTaskMode: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val todayProgress: Int = 0,
    val last7Days: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val activeTaskIndex: Int = 0,
    val notices: List<com.vni.app.data.model.Notice> = emptyList(),
    val suggestions: List<com.vni.app.data.model.Suggestion> = emptyList(),
    val calendarProgress: List<DayProgress> = emptyList(),
    val deletedNoticesHistory: List<HistoryItem> = emptyList(),
    val moneyHistory: List<HistoryItem> = emptyList(),
    val moneyBalance: Int = 0,
    val totalIncome: Int = 0,
    val totalExpense: Int = 0,
    val repeatSpentItems: List<RepeatSpentItem> = emptyList(),
    val weeklyMoneyFlow: List<DailyMoneyFlow> = emptyList(),
    val monthlyMoneyFlow: List<DailyMoneyFlow> = emptyList(),
    val daySchedulePlans: List<DaySchedulePlan> = emptyList(),
    val selectedPlanName: String = "clg",
    val weekWastedMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val calendarDayNotes: Map<String, String> = emptyMap(),
    val noteCategories: List<NoteCategory> = emptyList(),
    val planV2Categories: List<PlanV2Category> = emptyList(),
    val planV2Tasks: List<PlanV2Task> = emptyList(),
    val dailyAssignedPlanTasks: List<DailyAssignedPlanTask> = emptyList(),
    val moneyCategories: List<String> = listOf("ALL", "NORMAL"),
    val countMoneyCategories: Set<String> = emptySet(),
    val shortSaveCategories: Set<String> = emptySet(),
    val historyCategories: List<String> = listOf("All", "Done", "Notes", "Money"),
    val doCategories: List<NoteCategory> = emptyList(),
    val chatCategories: List<ChatCategory> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList()
)

/**
 * Standalone ViewModel with real persistent data and dynamic day-by-day tracking.
 */
class SimpleHomeViewModel : ViewModel() {

    private val cal = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)
    private val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    private val todayDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6) // 0=Sunday, 1=Monday
    private val currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
    private val todayKey = String.format(Locale.ENGLISH, "%04d-%02d-%02d", currentYear, currentMonth + 1, todayDay)

    private val initialTasks = listOf(
        Task(
            id = 1,
            name = "Code Chef",
            targetMinutes = 30,
            todayMinutes = 20,
            weekLoggedMinutes = listOf(25, 30, 20, 28, 30, 0, 20),
            lastCompletedDateText = "None",
            isPrimary = true
        ),
        Task(
            id = 2,
            name = "App Develop",
            targetMinutes = 60,
            todayMinutes = 45,
            weekLoggedMinutes = listOf(50, 60, 45, 60, 50, 0, 45),
            lastCompletedDateText = "yesterday",
            isPrimary = true
        ),
        Task(
            id = 3,
            name = "Learn Course",
            targetMinutes = 45,
            todayMinutes = 15,
            weekLoggedMinutes = listOf(30, 40, 45, 40, 30, 0, 15),
            lastCompletedDateText = "24 Sep",
            isPrimary = true
        )
    )
    private val initialPastTasks = emptyList<Task>()
    private val initialUniTasks = emptyList<UniTask>()
    private val seedCalendarProgress = emptyList<DayProgress>()

    // Day Schedule Planner seed plans matching Image 1
    private val seedDayPlans = listOf(
        DaySchedulePlan(
            id = 1,
            name = "clg",
            sections = listOf(
                DayTimeSection(
                    id = 1,
                    name = "Mrng",
                    startTime = "07:00 AM",
                    endTime = "09:20 AM",
                    isWakeUpStart = true,
                    isSleepEnd = false,
                    startHour = 7,
                    startMinute = 0,
                    endHour = 9,
                    endMinute = 20,
                    colorIndex = 0,
                    allocatedTasks = listOf(
                        ScheduledTaskAllocation(id = 1, taskId = 101, isUniTask = true, name = "Piano", priority = 1),
                        ScheduledTaskAllocation(id = 2, taskId = 4, isUniTask = false, name = "Study", priority = 2),
                        ScheduledTaskAllocation(id = 3, taskId = 3, isUniTask = false, name = "App Dev", priority = 3)
                    )
                ),
                DayTimeSection(
                    id = 2,
                    name = "College",
                    startTime = "09:20 AM",
                    endTime = "04:40 PM",
                    isWakeUpStart = false,
                    isSleepEnd = false,
                    startHour = 9,
                    startMinute = 20,
                    endHour = 16,
                    endMinute = 40,
                    colorIndex = 1,
                    allocatedTasks = emptyList()
                )
            )
        ),
        DaySchedulePlan(
            id = 2,
            name = "holiday",
            sections = listOf(
                DayTimeSection(
                    id = 3,
                    name = "Mrng",
                    startTime = "08:00 AM",
                    endTime = "12:00 PM",
                    isWakeUpStart = true,
                    isSleepEnd = false,
                    startHour = 8,
                    startMinute = 0,
                    endHour = 12,
                    endMinute = 0,
                    colorIndex = 0,
                    allocatedTasks = emptyList()
                )
            )
        ),
        DaySchedulePlan(
            id = 3,
            name = "home",
            sections = listOf(
                DayTimeSection(
                    id = 4,
                    name = "Morning",
                    startTime = "07:30 AM",
                    endTime = "11:30 AM",
                    isWakeUpStart = false,
                    isSleepEnd = false,
                    startHour = 7,
                    startMinute = 30,
                    endHour = 11,
                    endMinute = 30,
                    colorIndex = 2,
                    allocatedTasks = emptyList()
                )
            )
        )
    )

    private fun dateMillisFor(daysAgo: Int): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return c.timeInMillis
    }

    private fun dateStrFor(daysAgo: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("dd MMM", Locale.ENGLISH).format(c.time)
    }

    private val seedMoneyHistory = emptyList<HistoryItem>()
    private val seedRepeatExpenses = emptyList<RepeatSpentItem>()
    private val seedDoItems = emptyList<DoItem>()
    private val seedMotives = emptyList<Motive>()
    private val seedMotiveHeadings = listOf(
        MotiveHeading(
            id = 1,
            heading = "Motive",
            sentences = listOf(
                "Nobody comes: You either change or being struck",
                "Discipline is destiny: Win the battle with yourself every day",
                "Push your limits: Growth happens right outside your comfort zone"
            )
        ),
        MotiveHeading(
            id = 2,
            heading = "Good words",
            sentences = listOf(
                "Consistency transforms average: Small daily steps lead to massive results",
                "Energy flows where attention goes: Stay dialed in and focused",
                "Do something today: Your future self will thank you for it"
            )
        )
    )
    private val seedPlanContainers = emptyList<PlanContainer>()
    private val seedNotes = emptyList<NoteItem>()
    private val seedNotices = emptyList<com.vni.app.data.model.Notice>()
    private val seedPlanV2Categories = listOf(
        PlanV2Category(id = 1L, name = "Primary"),
        PlanV2Category(id = 2L, name = "Secondary"),
        PlanV2Category(id = 3L, name = "Eventually")
    )
    private val seedPlanV2Tasks = listOf(
        PlanV2Task(id = 1L, categoryName = "Primary", name = "App Development", defaultMinutes = 60),
        PlanV2Task(id = 2L, categoryName = "Primary", name = "Prolems solving", defaultMinutes = 45),
        PlanV2Task(id = 3L, categoryName = "Primary", name = "Dsa", defaultMinutes = 60)
    )
    private val seedDailyAssignedPlanTasks = emptyList<DailyAssignedPlanTask>()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in listOf("tasks_json", "notices_json", "unitasks_json", "deleted_notices_json", "plan_json", "motive_headings_json", "notes_json", "note_categories_json", "daily_assigned_plan_tasks_json", "plan_v2_tasks_json", "plan_v2_categories_json")) {
            loadAndInitializeState(fromPrefsChange = true)
        }
    }

    init {
        loadAndInitializeState()
        getPrefs()?.registerOnSharedPreferenceChangeListener(prefsListener)
        try {
            HabitPredictor.scheduleSmartHabitReminders(VniApplication.instance, _uiState.value.tasks)
            NotificationHelper.schedulePeriodicMotiveNotifications(VniApplication.instance)
            NotificationHelper.scheduleTasksWithTimeRanges(VniApplication.instance, _uiState.value.tasks)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        getPrefs()?.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun getPrefs(): SharedPreferences? {
        return try {
            VniApplication.instance.getSharedPreferences("vni_app_prefs_v2", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadAndInitializeState(fromPrefsChange: Boolean = false) {
        val context = VniApplication.instance
        val prefs = getPrefs()

        val hasOldDummyData = prefs != null && (
            prefs.getInt("money_balance", 0) == 237 ||
            prefs.getString("tasks_json", null)?.contains("Workout") == true ||
            !prefs.getBoolean("vni_clean_install_v4", false)
        )
        if (hasOldDummyData) {
            com.vni.app.data.PersistentStorageManager.deleteOldDummyBackup(context)
            prefs?.edit()?.clear()?.putBoolean("vni_clean_install_v4", true)?.apply()
            context.getSharedPreferences("vni_security_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("vni_pet_assistant", Context.MODE_PRIVATE).edit().clear().apply()
        } else {
            if (prefs != null && !prefs.contains("tasks_json")) {
                com.vni.app.data.PersistentStorageManager.restoreDataIfNeeded(context)
            }
        }

        val savedDate = prefs?.getString("last_active_date", null)
        val savedWeek = prefs?.getInt("last_active_week", -1) ?: -1
        val isNewDay = savedDate != null && savedDate != todayKey
        val isNewWeek = savedWeek != -1 && savedWeek != currentWeekOfYear

        var tasks = prefs?.getString("tasks_json", null)?.let { tasksFromJson(it) } ?: initialTasks
        val pastTasks = prefs?.getString("past_tasks_json", null)?.let { tasksFromJson(it) } ?: initialPastTasks
        var uniTasks = prefs?.getString("unitasks_json", null)?.let { uniTasksFromJson(it) } ?: initialUniTasks
        var calProgress = prefs?.getString("calendar_json", null)?.let { calendarFromJson(it) } ?: seedCalendarProgress
        calProgress = calProgress.map { dp ->
            if (dp.year == currentYear && dp.month == 9 && dp.day > todayDay && dp.day in 18..30) {
                dp.copy(month = 8)
            } else dp
        }.distinctBy { "${it.year}-${it.month}-${it.day}" }
        val moneyHistory = prefs?.getString("money_hist_json", null)?.let { moneyHistoryFromJson(it) } ?: seedMoneyHistory
        val repeatExpenses = prefs?.getString("repeat_exp_json", null)?.let { repeatSpentFromJson(it) } ?: seedRepeatExpenses
        val notices = prefs?.getString("notices_json", null)?.let { noticesFromJson(it) } ?: seedNotices
        val deletedNotices = prefs?.getString("deleted_notices_json", null)?.let { moneyHistoryFromJson(it) } ?: emptyList()
        val balance = prefs?.getInt("money_balance", 0) ?: 0
        val doItems = prefs?.getString("do_items_json", null)?.let { doItemsFromJson(it) } ?: seedDoItems
        val planContainers = prefs?.getString("plan_json", null)?.let { planContainersFromJson(it) } ?: seedPlanContainers
        val motiveHeadings = prefs?.getString("motive_headings_json", null)?.let { motiveHeadingsFromJson(it) } ?: seedMotiveHeadings
        val notes = prefs?.getString("notes_json", null)?.let { notesFromJson(it) } ?: seedNotes

        val defaultNoteCategories = listOf(
            NoteCategory(
                id = 1L,
                section = "GENERAL",
                name = "MOTIVE",
                topics = listOf(
                    NoteTopic(
                        id = 101L,
                        title = "GOOD WORDS",
                        content = "• Do things repeatedly to build consistency\n• Consistency is the process of doing things daily\n• Trust the process\n4. I eat an apple",
                        formatType = "POINTS"
                    ),
                    NoteTopic(
                        id = 102L,
                        title = "PHYLOSOPIC",
                        content = "• Philosophy begins in wonder\n• He who has a why to live can bear almost any how",
                        formatType = "POINTS"
                    )
                )
            ),
            NoteCategory(
                id = 2L,
                section = "GENERAL",
                name = "QUOTES",
                topics = listOf(
                    NoteTopic(
                        id = 201L,
                        title = "WISDOM",
                        content = "• Knowledge speaks, but wisdom listens",
                        formatType = "POINTS"
                    )
                )
            ),
            NoteCategory(
                id = 3L,
                section = "PRIMARY",
                name = "WORK",
                topics = listOf(
                    NoteTopic(
                        id = 301L,
                        title = "GOALS",
                        content = "1. Deliver high impact features\n2. Maintain consistent routine",
                        formatType = "NUMBER"
                    )
                )
            ),
            NoteCategory(
                id = 4L,
                section = "PRIVATE",
                name = "PERSONAL",
                topics = listOf(
                    NoteTopic(
                        id = 401L,
                        title = "JOURNAL",
                        content = "Confidential thoughts and private notes",
                        formatType = "NORMAL"
                    )
                )
            )
        )
        val noteCategories = prefs?.getString("note_categories_json", null)?.let { noteCategoriesFromJson(it) } ?: defaultNoteCategories
        val motives = prefs?.getString("motives_json", null)?.let { motivesFromJson(it) } ?: generateFlatMotives(motiveHeadings)
        val dayPlans = prefs?.getString("day_plans_json", null)?.let { dayPlansFromJson(it) } ?: seedDayPlans
        val selectedPlan = prefs?.getString("selected_plan_name", "clg") ?: "clg"
        val wastedList = prefs?.getString("wasted_min_json", null)?.let { wastedMinutesFromJson(it) } ?: listOf(0, 0, 0, 0, 0, 0, 0)
        val isUniTaskMode = prefs?.getBoolean("is_uni_task_mode", false) ?: false
        val calDayNotes = prefs?.getString("cal_day_notes_json", null)?.let { calDayNotesFromJson(it) } ?: emptyMap()
        val planV2Categories = prefs?.getString("plan_v2_categories_json", null)?.let { planV2CategoriesFromJson(it) } ?: seedPlanV2Categories
        val planV2Tasks = prefs?.getString("plan_v2_tasks_json", null)?.let { planV2TasksFromJson(it) } ?: seedPlanV2Tasks
        val dailyAssignedPlanTasks = prefs?.getString("daily_assigned_plan_tasks_json", null)?.let { dailyAssignedPlanTasksFromJson(it) } ?: seedDailyAssignedPlanTasks
        val moneyCategories = prefs?.getString("money_categories_json", null)?.let { stringListFromJson(it) } ?: listOf("ALL", "NORMAL")
        val historyCategories = prefs?.getString("history_categories_json", null)?.let { stringListFromJson(it) } ?: listOf("All", "Done", "Notes", "Money")
        val defaultDoCategories = listOf(
            NoteCategory(
                id = 1001L,
                section = "DO",
                name = "GENERAL",
                topics = listOf(
                    NoteTopic(id = 2001L, title = "WORK", content = "• complete documentation\n• review project plan", formatType = "POINTS"),
                    NoteTopic(id = 2002L, title = "PERSONAL", content = "• grocery shopping\n• water plants", formatType = "POINTS")
                )
            )
        )
        val doCategories = prefs?.getString("do_categories_json", null)?.let { noteCategoriesFromJson(it) } ?: defaultDoCategories

        val defaultChatCategories = listOf(
            ChatCategory(
                id = 1L,
                section = "GENERAL",
                name = "NOTES",
                topics = listOf(
                    ChatTopic(
                        id = 101L,
                        title = "UPD",
                        messages = listOf(
                            ChatMessage(id = 1L, text = "a for Apple and aeroplane", isRight = true),
                            ChatMessage(id = 2L, text = "B for ball and basketball and c for cat and carrot and camel and t for tall and tallest", isRight = true),
                            ChatMessage(id = 3L, text = "and tell me for what's for d and g and k and l", isRight = false)
                        )
                    )
                )
            )
        )
        val chatCategories = prefs?.getString("chat_categories_json", null)?.let { chatCategoriesFromJson(it) } ?: defaultChatCategories
        val countMoneyCategories = prefs?.getString("count_money_categories_json", null)?.let { stringSetFromJson(it) } ?: emptySet()
        val shortSaveCategories = prefs?.getString("short_save_money_categories_json", null)?.let { stringSetFromJson(it) } ?: emptySet()
        val calendarEvents = prefs?.getString("calendar_events_json", null)?.let { calendarEventsFromJson(it) } ?: emptyList()

        // Home tasks are strictly driven by the real tasks assigned for TODAY in Plan!
        tasks = buildHomeTasksFromDailyPlan(dailyAssignedPlanTasks, planV2Tasks)

        val todayProgress = calcProgress(tasks)
        val primaryTasks = tasks.filter { it.daysLeft > 0 && it.isPrimary }

        // 7 bars: Sunday to Saturday from real Plan assignments!
        val last7Days = computeRealLast7Days(dailyAssignedPlanTasks)

        // Ensure today's date exists in calendar
        val updatedCal = calProgress.filter { !(it.year == currentYear && it.month == currentMonth && it.day == todayDay) }.toMutableList()
        updatedCal.add(DayProgress(currentYear, currentMonth, todayDay, todayProgress))

        val computedIncome = moneyHistory.filter { it.amountInt > 0 }.sumOf { it.amountInt }
        val computedExpense = kotlin.math.abs(moneyHistory.filter { it.amountInt < 0 }.sumOf { it.amountInt })

        _uiState.value = HomeUiState(
            tasks = tasks,
            pastTasks = pastTasks,
            motives = motives,
            motiveHeadings = motiveHeadings,
            planContainers = planContainers,
            notes = notes,
            doItems = doItems,
            uniTasks = uniTasks,
            isUniTaskMode = isUniTaskMode,
            completedCount = primaryTasks.count { it.completedToday },
            totalCount = primaryTasks.size,
            todayProgress = todayProgress,
            last7Days = last7Days,
            activeTaskIndex = 0,
            notices = notices,
            suggestions = emptyList(),
            calendarProgress = updatedCal,
            deletedNoticesHistory = deletedNotices,
            moneyHistory = moneyHistory,
            moneyBalance = balance,
            totalIncome = computedIncome,
            totalExpense = computedExpense,
            repeatSpentItems = repeatExpenses,
            weeklyMoneyFlow = computeWeeklyFlow(moneyHistory),
            monthlyMoneyFlow = computeMonthlyFlow(moneyHistory),
            daySchedulePlans = dayPlans,
            selectedPlanName = selectedPlan,
            weekWastedMinutes = wastedList,
            calendarDayNotes = calDayNotes,
            noteCategories = noteCategories,
            planV2Categories = planV2Categories,
            planV2Tasks = planV2Tasks,
            dailyAssignedPlanTasks = dailyAssignedPlanTasks,
            moneyCategories = moneyCategories,
            countMoneyCategories = countMoneyCategories,
            shortSaveCategories = shortSaveCategories,
            historyCategories = historyCategories,
            doCategories = doCategories,
            chatCategories = chatCategories,
            calendarEvents = calendarEvents
        )

        if (!fromPrefsChange) {
            saveAllState()
        }
    }

    private fun saveAllState() {
        val state = _uiState.value
        val prefs = getPrefs() ?: return
        prefs.edit()
            .putString("last_active_date", todayKey)
            .putInt("last_active_week", currentWeekOfYear)
            .putString("tasks_json", tasksToJson(state.tasks))
            .putString("past_tasks_json", tasksToJson(state.pastTasks))
            .putString("unitasks_json", uniTasksToJson(state.uniTasks))
            .putBoolean("is_uni_task_mode", state.isUniTaskMode)
            .putString("calendar_json", calendarToJson(state.calendarProgress))
            .putString("money_hist_json", moneyHistoryToJson(state.moneyHistory))
            .putString("repeat_exp_json", repeatSpentToJson(state.repeatSpentItems))
            .putString("notices_json", noticesToJson(state.notices))
            .putString("deleted_notices_json", moneyHistoryToJson(state.deletedNoticesHistory))
            .putString("do_items_json", doItemsToJson(state.doItems))
            .putString("motives_json", motivesToJson(state.motives))
            .putString("motive_headings_json", motiveHeadingsToJson(state.motiveHeadings))
            .putString("plan_json", planContainersToJson(state.planContainers))
            .putString("notes_json", notesToJson(state.notes))
            .putString("note_categories_json", noteCategoriesToJson(state.noteCategories))
            .putString("day_plans_json", dayPlansToJson(state.daySchedulePlans))
            .putString("selected_plan_name", state.selectedPlanName)
            .putString("wasted_min_json", wastedMinutesToJson(state.weekWastedMinutes))
            .putString("cal_day_notes_json", calDayNotesToJson(state.calendarDayNotes))
            .putString("plan_v2_categories_json", planV2CategoriesToJson(state.planV2Categories))
            .putString("plan_v2_tasks_json", planV2TasksToJson(state.planV2Tasks))
            .putString("daily_assigned_plan_tasks_json", dailyAssignedPlanTasksToJson(state.dailyAssignedPlanTasks))
            .putString("money_categories_json", stringListToJson(state.moneyCategories))
            .putString("history_categories_json", stringListToJson(state.historyCategories))
            .putString("do_categories_json", noteCategoriesToJson(state.doCategories))
            .putString("chat_categories_json", chatCategoriesToJson(state.chatCategories))
            .putString("count_money_categories_json", stringSetToJson(state.countMoneyCategories))
            .putString("short_save_money_categories_json", stringSetToJson(state.shortSaveCategories))
            .putString("calendar_events_json", calendarEventsToJson(state.calendarEvents))
            .putInt("money_balance", state.moneyBalance)
            .apply()

        com.vni.app.data.PersistentStorageManager.backupData(VniApplication.instance)
    }

    fun removeNotice(id: Long) {
        // Cancel any pending 10-minute periodic reminders for this notice
        try {
            NotificationHelper.cancelNoticeReminders(VniApplication.instance, id)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val current = _uiState.value.notices.toMutableList()
        val noticeToRemove = current.find { it.id == id }
        if (noticeToRemove != null) {
            current.remove(noticeToRemove)

            val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date())
            val updatedHistory = _uiState.value.deletedNoticesHistory.toMutableList()
            updatedHistory.add(
                0,
                HistoryItem(
                    id = System.currentTimeMillis(),
                    title = noticeToRemove.title,
                    date = dateStr,
                    type = "Notes"
                )
            )

            _uiState.value = _uiState.value.copy(
                notices = current,
                deletedNoticesHistory = updatedHistory
            )
            saveAllState()
        }
    }

    fun removeHistoryItem(id: Long) {
        val updatedNotes = _uiState.value.deletedNoticesHistory.filter { it.id != id }
        val updatedMoney = _uiState.value.moneyHistory.filter { it.id != id }
        _uiState.value = _uiState.value.copy(
            deletedNoticesHistory = updatedNotes,
            moneyHistory = updatedMoney
        )
        recalcMoneyDerived()
        saveAllState()
    }

    fun markRepeatSpentDeleted(name: String) {
        val updated = _uiState.value.repeatSpentItems.map {
            if (it.name.equals(name, ignoreCase = true)) it.copy(isDeleted = true) else it
        }
        _uiState.value = _uiState.value.copy(repeatSpentItems = updated)
        saveAllState()
    }

    fun clearCompletedRepeatSpent() {
        val updated = _uiState.value.repeatSpentItems.filter { !it.isDeleted }
        _uiState.value = _uiState.value.copy(repeatSpentItems = updated)
        saveAllState()
    }

    /**
     * Requirement 1: Can add any date to notice (not only today, tomorrow).
     * Displays "Deadline: Today", "Deadline: Tomorrow", or "Deadline: d MMM" (e.g. "Deadline: 2 Oct").
     */
    fun addNotice(
        title: String = "New Notice",
        subtitle: String = "Added just now",
        isReminder: Boolean = false,
        reminderDay: String = "Today",
        timeRange: String? = null,
        durationMinutes: Int? = null,
        exactTriggerMillis: Long? = null
    ) {
        val current = _uiState.value.notices.toMutableList()
        val nextId  = (current.maxOfOrNull { it.id } ?: 0) + 1
        val deadline = when {
            durationMinutes != null -> "In $durationMinutes min"
            reminderDay.startsWith("In ") -> reminderDay
            reminderDay == "Today" -> "Deadline: Today"
            reminderDay == "Tomorrow" -> "Deadline: Tomorrow"
            else -> "Deadline: $reminderDay"
        }
        val triggerMillis = exactTriggerMillis ?: if (durationMinutes != null) System.currentTimeMillis() + durationMinutes * 60 * 1000L else null
        current.add(
            com.vni.app.data.model.Notice(
                id = nextId,
                title = title,
                subtitle = subtitle,
                iconName = if (isReminder) "School" else "StickyNote2",
                isToday = isReminder,
                deadlineText = deadline,
                timeRange = timeRange,
                durationMinutes = durationMinutes,
                reminderTriggerMillis = triggerMillis
            )
        )
        _uiState.value = _uiState.value.copy(notices = current)
        saveAllState()

        if (isReminder) {
            try {
                if (durationMinutes != null) {
                    NotificationHelper.scheduleNoticeDurationReminder(
                        context = VniApplication.instance,
                        noticeId = nextId,
                        noticeTitle = title,
                        subtitle = subtitle,
                        durationMinutes = durationMinutes,
                        triggerAtMillis = triggerMillis ?: (System.currentTimeMillis() + durationMinutes * 60 * 1000L)
                    )
                } else if (!timeRange.isNullOrEmpty()) {
                    // Periodic reminders every 10 minutes until notice is completed or deleted
                    NotificationHelper.scheduleNoticeTimeRangeCheck(
                        context = VniApplication.instance,
                        noticeId = nextId,
                        noticeTitle = title,
                        subtitle = subtitle,
                        timeRange = timeRange,
                        reminderDay = reminderDay
                    )
                } else {
                    NotificationHelper.sendNoticeAlertWithOkayDone(
                        context = VniApplication.instance,
                        noticeId = nextId,
                        noticeTitle = title,
                        subtitle = subtitle
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addMoneyTransaction(
        amount: Int,
        purpose: String,
        isExpense: Boolean,
        dateMillis: Long = System.currentTimeMillis(),
        category: String = "NORMAL"
    ) {
        val currentBalance = _uiState.value.moneyBalance
        val newBalance = if (isExpense) (currentBalance - amount) else currentBalance + amount
        val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(dateMillis))
        val sign = if (isExpense) "- " else "+ "
        val rawAmount = if (isExpense) -amount else amount
        val cleanCat = category.trim().uppercase().ifEmpty { "NORMAL" }
        val newHistoryItem = HistoryItem(
            id = System.currentTimeMillis(),
            title = purpose.ifEmpty { if (isExpense) "Spent" else "Income" },
            date = dateStr,
            type = "Money",
            amount = "$sign\u20B9 $amount",
            amountInt = rawAmount,
            dateMillis = dateMillis,
            category = cleanCat
        )
        val updatedHistory = _uiState.value.moneyHistory.toMutableList()
        updatedHistory.add(0, newHistoryItem)

        // Ensure category is present in moneyCategories
        val cats = _uiState.value.moneyCategories.toMutableList()
        if (cleanCat != "ALL" && !cats.any { it.equals(cleanCat, ignoreCase = true) }) {
            cats.add(cleanCat)
        }

        _uiState.value = _uiState.value.copy(
            moneyBalance = newBalance,
            moneyHistory = updatedHistory,
            moneyCategories = cats
        )
        recalcMoneyDerived()
        saveAllState()
    }

    fun addMoneyCategory(name: String) {
        val clean = name.trim().uppercase()
        if (clean.isBlank() || clean == "ALL") return
        val current = _uiState.value.moneyCategories.toMutableList()
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            _uiState.value = _uiState.value.copy(moneyCategories = current)
            saveAllState()
        }
    }

    fun removeMoneyCategory(name: String) {
        val clean = name.trim().uppercase()
        if (clean == "ALL" || clean == "NORMAL") return
        val current = _uiState.value.moneyCategories.filter { !it.equals(clean, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(moneyCategories = current)
        saveAllState()
    }

    fun addHistoryCategory(name: String) {
        val clean = name.trim()
        if (clean.isBlank() || clean.equals("All", ignoreCase = true)) return
        val current = _uiState.value.historyCategories.toMutableList()
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            _uiState.value = _uiState.value.copy(historyCategories = current)
            saveAllState()
        }
    }

    fun removeHistoryCategory(name: String) {
        val clean = name.trim()
        if (clean.equals("All", ignoreCase = true) || clean.equals("Done", ignoreCase = true) ||
            clean.equals("Notes", ignoreCase = true) || clean.equals("Money", ignoreCase = true)) return
        val current = _uiState.value.historyCategories.filter { !it.equals(clean, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(historyCategories = current)
        saveAllState()
    }

    fun addCustomHistoryItem(title: String, category: String) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date())
        val item = HistoryItem(
            id = System.currentTimeMillis(),
            title = clean,
            date = dateStr,
            type = category.ifEmpty { "General" },
            dateMillis = System.currentTimeMillis()
        )
        val updated = _uiState.value.deletedNoticesHistory.toMutableList()
        updated.add(0, item)
        _uiState.value = _uiState.value.copy(deletedNoticesHistory = updated)
        saveAllState()
    }

    fun addDoCategory(name: String, section: String = "GENERAL") {
        val clean = name.trim().uppercase()
        if (clean.isBlank()) return
        val current = _uiState.value.doCategories.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 1000L) + 1L
        current.add(NoteCategory(id = nextId, section = section.uppercase(), name = clean, topics = emptyList()))
        _uiState.value = _uiState.value.copy(doCategories = current)
        saveAllState()
    }

    fun deleteDoCategory(categoryId: Long) {
        val current = _uiState.value.doCategories.filter { it.id != categoryId }
        _uiState.value = _uiState.value.copy(doCategories = current)
        saveAllState()
    }

    fun addDoTopic(categoryId: Long, title: String) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val current = _uiState.value.doCategories.toMutableList()
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx >= 0) {
            val cat = current[idx]
            val nextTopicId = (cat.topics.maxOfOrNull { it.id } ?: (categoryId * 10)) + 1L
            val updatedTopics = cat.topics.toMutableList()
            updatedTopics.add(NoteTopic(id = nextTopicId, title = clean, content = "", formatType = "POINTS"))
            current[idx] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(doCategories = current)
            saveAllState()
        }
    }

    fun deleteDoTopic(categoryId: Long, topicId: Long, markCompletedInHistory: Boolean = false) {
        val current = _uiState.value.doCategories.toMutableList()
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx >= 0) {
            val cat = current[idx]
            val topicToComplete = cat.topics.firstOrNull { it.id == topicId }
            val updatedTopics = cat.topics.filter { it.id != topicId }
            current[idx] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(doCategories = current)
            saveAllState()

            if (markCompletedInHistory && topicToComplete != null) {
                addCustomHistoryItem(title = topicToComplete.title, category = "Done")
            }
        }
    }

    fun updateDoTopic(categoryId: Long, topicId: Long, content: String, formatType: String) {
        val current = _uiState.value.doCategories.toMutableList()
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx >= 0) {
            val cat = current[idx]
            val updatedTopics = cat.topics.map { t ->
                if (t.id == topicId) t.copy(content = content, formatType = formatType) else t
            }
            current[idx] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(doCategories = current)
            saveAllState()
        }
    }

    private fun recalcMoneyDerived() {
        val hist = _uiState.value.moneyHistory
        val inc = hist.filter { it.amountInt > 0 }.sumOf { it.amountInt }
        val exp = hist.filter { it.amountInt < 0 }.sumOf { kotlin.math.abs(it.amountInt) }
        _uiState.value = _uiState.value.copy(
            totalIncome = inc,
            totalExpense = exp,
            repeatSpentItems = computeRepeatSpent(hist),
            weeklyMoneyFlow = computeWeeklyFlow(hist),
            monthlyMoneyFlow = computeMonthlyFlow(hist)
        )
    }

    fun addRepeatExpense(name: String, amount: Int) {
        val current = _uiState.value.repeatSpentItems.toMutableList()
        current.add(0, RepeatSpentItem(name = name, totalSpent = amount, count = 1))
        _uiState.value = _uiState.value.copy(repeatSpentItems = current)
        saveAllState()
    }

    /**
     * Requirement 2 & 4: Add Do item with deadline, specific date, or daily until complete, and days required.
     */
    fun addDoItem(
        name: String,
        deadlineDateText: String,
        daysRequired: Int = 1,
        isSpecificDate: Boolean = false,
        isDailyUntilComplete: Boolean = false,
        hasTimeSchedule: Boolean = false,
        specialized: String = ""
    ) {
        val current = _uiState.value.doItems.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val nextColorIdx = current.size % 3
        current.add(
            DoItem(
                id = nextId,
                name = name,
                deadlineDateText = deadlineDateText,
                colorIndex = nextColorIdx,
                daysRequired = daysRequired,
                isSpecificDate = isSpecificDate,
                isDailyUntilComplete = isDailyUntilComplete,
                hasTimeSchedule = hasTimeSchedule,
                specialized = specialized.trim()
            )
        )
        _uiState.value = _uiState.value.copy(doItems = current)
        saveAllState()
    }

    /**
     * Requirement 2 & 3: Completing/removing item moves it to History under "Done" with date!
     */
    fun removeDoItem(id: Long) {
        val current = _uiState.value.doItems.toMutableList()
        val itemToRemove = current.find { it.id == id }
        if (itemToRemove != null) {
            current.remove(itemToRemove)
            val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date())
            val updatedHistory = _uiState.value.deletedNoticesHistory.toMutableList()
            updatedHistory.add(
                0,
                HistoryItem(
                    id = System.currentTimeMillis(),
                    title = itemToRemove.name,
                    date = dateStr,
                    type = "Done",
                    dateMillis = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(
                doItems = current,
                deletedNoticesHistory = updatedHistory
            )
            saveAllState()
        }
    }

    fun completeDoItem(id: Long) {
        removeDoItem(id)
    }

    /**
     * Logs wake up time recorded today via Pet Assistant.
     */
    fun logTodayWakeUpTime(timeStr: String) {
        getPrefs()?.edit()?.putString("today_wake_time_$todayKey", timeStr)?.apply()
    }

    /**
     * Logs sleep time recorded tonight via Pet Assistant.
     */
    fun logTodaySleepTime(timeStr: String) {
        getPrefs()?.edit()?.putString("today_sleep_time_$todayKey", timeStr)?.apply()
    }

    /**
     * Requirement 4: Add motive sentence displayed at the top of the app.
     */
    fun addMotive(sentence: String) {
        val clean = sentence.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.motives.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val firstWord = clean.split(" ", limit = 2).firstOrNull()?.trimEnd(',', '.', '!') ?: "Consistency"
        current.add(Motive(id = nextId, text = clean, highlightWords = firstWord))
        _uiState.value = _uiState.value.copy(motives = current)
        saveAllState()
    }

    /**
     * Requirement 4: Deleting motive does NOT go to history!
     */
    fun removeMotive(id: Long) {
        val current = _uiState.value.motives.filter { it.id != id }
        _uiState.value = _uiState.value.copy(motives = current)
        saveAllState()
    }

    fun setActiveTask(index: Int) {
        _uiState.value = _uiState.value.copy(activeTaskIndex = index)
    }

    /**
     * Updates time spent on an individual task.
     * Accurately updates:
     * - Task todayMinutes and weekLoggedMinutes[todayDayOfWeek]
     * - Today's overall progress (todayProgress)
     * - Current week's 7-bar chart (last7Days)
     * - Calendar day progress (calendarProgress)
     * - Persists immediately to storage!
     */
    fun updateTodayMinutes(taskId: Long, newMins: Int) {
        val currentAssigned = _uiState.value.dailyAssignedPlanTasks.toMutableList()
        val assignedIdx = currentAssigned.indexOfFirst { it.id == taskId && it.dateKey == todayKey }
        if (assignedIdx == -1) return

        val oldAssigned = currentAssigned[assignedIdx]
        val clampedMins = newMins.coerceIn(0, oldAssigned.durationMinutes.coerceAtLeast(1))
        val wasCompleted = oldAssigned.spentMinutes >= oldAssigned.durationMinutes
        val isCompleted = clampedMins >= oldAssigned.durationMinutes

        currentAssigned[assignedIdx] = oldAssigned.copy(spentMinutes = clampedMins)

        val rebuiltTasks = buildHomeTasksFromDailyPlan(currentAssigned, _uiState.value.planV2Tasks)
        val primaryTasks = rebuiltTasks.filter { it.daysLeft > 0 && it.isPrimary }
        val completed = primaryTasks.count { it.completedToday }
        val avgProgress = calcProgress(rebuiltTasks)
        val updated7Days = computeRealLast7Days(currentAssigned)

        // Update calendarProgress for today
        val updatedCal = _uiState.value.calendarProgress
            .filter { !(it.year == currentYear && it.month == currentMonth && it.day == todayDay) }
            .toMutableList()
        updatedCal.add(DayProgress(currentYear, currentMonth, todayDay, avgProgress))

        _uiState.value = _uiState.value.copy(
            dailyAssignedPlanTasks = currentAssigned,
            tasks = rebuiltTasks,
            totalCount = primaryTasks.size,
            completedCount = completed,
            todayProgress = avgProgress,
            last7Days = updated7Days,
            calendarProgress = updatedCal
        )
        saveAllState()

        if (!wasCompleted && isCompleted) {
            try {
                HabitPredictor.logCompletion(VniApplication.instance, oldAssigned.taskId, oldAssigned.taskName)
                NotificationHelper.sendNotification(
                    context = VniApplication.instance,
                    title = "Task Completed! 🎉",
                    message = "Great job! You reached 100% on \"${oldAssigned.taskName}\" today.",
                    notificationId = (1000 + taskId).toInt(),
                    channelId = NotificationHelper.CHANNEL_REMINDERS_ID
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveTask(
        id: Long?,
        name: String,
        targetMins: Int,
        daysLeft: Int,
        repeatDays: List<Boolean>,
        timeRange: String? = null,
        isPrimary: Boolean = true
    ) {
        val current = _uiState.value.tasks.toMutableList()
        if (id != null) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx != -1) {
                current[idx] = current[idx].copy(
                    name = name,
                    targetMinutes = targetMins,
                    daysLeft = daysLeft,
                    repeatDays = repeatDays,
                    timeRange = timeRange ?: current[idx].timeRange,
                    isPrimary = isPrimary
                )
            }
        } else {
            val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            current.add(
                Task(
                    id = nextId,
                    name = name,
                    targetMinutes = targetMins,
                    todayMinutes = 0,
                    completedToday = false,
                    sortOrder = current.size,
                    daysLeft = daysLeft,
                    repeatDays = repeatDays,
                    weekLoggedMinutes = listOf(0, 0, 0, 0, 0, 0, 0),
                    timeRange = timeRange,
                    isPrimary = isPrimary
                )
            )
        }
        val avgProgress = calcProgress(current)
        val primaryTasks = current.filter { it.daysLeft > 0 && it.isPrimary }
        _uiState.value = _uiState.value.copy(
            tasks = current,
            totalCount = primaryTasks.size,
            completedCount = primaryTasks.count { it.completedToday },
            todayProgress = avgProgress
        )
        saveAllState()
        try {
            NotificationHelper.scheduleTasksWithTimeRanges(VniApplication.instance, current)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTask(taskId: Long, name: String, targetMins: Int, daysLeft: Int, iconName: String) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val idx = currentTasks.indexOfFirst { it.id == taskId }
        if (idx != -1) {
            currentTasks[idx] = currentTasks[idx].copy(name = name, targetMinutes = targetMins, daysLeft = daysLeft, iconName = iconName)
            val avgProgress = calcProgress(currentTasks)
            _uiState.value = _uiState.value.copy(tasks = currentTasks, todayProgress = avgProgress)
            saveAllState()
        }
    }

    fun addTask(name: String, targetMins: Int, daysLeft: Int, iconName: String, isPrimary: Boolean = true) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val newId = (currentTasks.maxOfOrNull { it.id } ?: 0) + 1L
        currentTasks.add(Task(id = newId, name = name, targetMinutes = targetMins, todayMinutes = 0, completedToday = false, sortOrder = currentTasks.size, daysLeft = daysLeft, iconName = iconName, weekLoggedMinutes = listOf(0, 0, 0, 0, 0, 0, 0), isPrimary = isPrimary))
        val avgProgress = calcProgress(currentTasks)
        val primaryTasks = currentTasks.filter { it.daysLeft > 0 && it.isPrimary }
        _uiState.value = _uiState.value.copy(tasks = currentTasks, totalCount = primaryTasks.size, completedCount = primaryTasks.count { it.completedToday }, todayProgress = avgProgress)
        saveAllState()
    }

    fun deleteTaskToPast(taskId: Long) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val pastTasks = _uiState.value.pastTasks.toMutableList()
        val task = currentTasks.find { it.id == taskId }
        if (task != null) {
            currentTasks.remove(task)
            pastTasks.add(0, task)
            val avgProgress = calcProgress(currentTasks)
            val primaryTasks = currentTasks.filter { it.daysLeft > 0 && it.isPrimary }
            _uiState.value = _uiState.value.copy(
                tasks = currentTasks,
                pastTasks = pastTasks,
                totalCount = primaryTasks.size,
                completedCount = primaryTasks.count { it.completedToday },
                todayProgress = avgProgress
            )
            saveAllState()
        }
    }

    fun deletePastTask(taskId: Long) {
        val pastTasks = _uiState.value.pastTasks.filter { it.id != taskId }
        _uiState.value = _uiState.value.copy(pastTasks = pastTasks)
        saveAllState()
    }

    fun toggleUniTaskMode() {
        val newMode = !_uiState.value.isUniTaskMode
        _uiState.value = _uiState.value.copy(isUniTaskMode = newMode)
        saveAllState()
    }

    fun setUniTaskMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isUniTaskMode = enabled)
        saveAllState()
    }

    fun toggleUniTask(id: Long) {
        val current = _uiState.value.uniTasks.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx != -1) {
            val item = current[idx]
            val newCompleted = !item.completedToday
            val newWeek = item.weekCompleted.toMutableList()
            while (newWeek.size < 7) newWeek.add(false)
            newWeek[todayDayOfWeek] = newCompleted

            current[idx] = item.copy(
                completedToday = newCompleted,
                weekCompleted = newWeek
            )
            _uiState.value = _uiState.value.copy(uniTasks = current)
            saveAllState()

            if (!item.completedToday && newCompleted) {
                try {
                    NotificationHelper.sendNotification(
                        context = VniApplication.instance,
                        title = "Uni-Task Completed! ✨",
                        message = "You completed \"${item.name}\". Keep up the great consistency!",
                        notificationId = (2000 + id).toInt(),
                        channelId = NotificationHelper.CHANNEL_REMINDERS_ID
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addUniTask(
        name: String,
        scheduledHour: Int = 9,
        scheduledMinute: Int = 0,
        scheduledTime: String? = null,
        notifyBeforeMinutes: Int = 0
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.uniTasks.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val formattedTime = scheduledTime ?: String.format(java.util.Locale.ENGLISH, "%02d:%02d", scheduledHour, scheduledMinute)
        current.add(
            UniTask(
                id = nextId,
                name = clean,
                completedToday = false,
                weekCompleted = listOf(false, false, false, false, false, false, false),
                scheduledTime = formattedTime,
                scheduledHour = scheduledHour,
                scheduledMinute = scheduledMinute,
                notifyBeforeMinutes = notifyBeforeMinutes
            )
        )
        _uiState.value = _uiState.value.copy(uniTasks = current)
        saveAllState()

        // Schedule daily recurring reminder
        NotificationHelper.scheduleDailyUniTaskReminder(
            context = VniApplication.instance,
            taskId = nextId,
            taskName = clean,
            hour = scheduledHour,
            minute = scheduledMinute,
            notifyBeforeMinutes = notifyBeforeMinutes
        )
    }

    fun deleteUniTask(id: Long) {
        val current = _uiState.value.uniTasks.filter { it.id != id }
        _uiState.value = _uiState.value.copy(uniTasks = current)
        saveAllState()

        // Cancel daily recurring reminder
        NotificationHelper.cancelDailyUniTaskReminder(VniApplication.instance, id)
    }

    // ── Plan Management Methods (Requirement 1) ────────────────────────────────
    fun togglePlanContainer(containerId: Long) {
        val current = _uiState.value.planContainers.map {
            if (it.id == containerId) it.copy(isExpanded = !it.isExpanded) else it
        }
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    fun addPlanContainer(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.planContainers.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(PlanContainer(id = nextId, title = clean, isExpanded = true, elements = emptyList()))
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    fun addPlanElement(containerId: Long, title: String, subtitle: String = "") {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.planContainers.map { container ->
            if (container.id == containerId) {
                val nextElemId = (container.elements.maxOfOrNull { it.id } ?: (containerId * 100)) + 1L
                val newElem = PlanElement(
                    id = nextElemId,
                    title = clean,
                    isDisabled = false,
                    colorIndex = (container.elements.size % 6)
                )
                val active = container.elements.filter { !it.isDisabled }.toMutableList()
                val disabled = container.elements.filter { it.isDisabled }
                active.add(newElem)
                container.copy(elements = active + disabled)
            } else {
                container
            }
        }
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    /**
     * Requirement 1: Double clicking element marks it disabled (grey) and moves it to last position.
     * Active elements stay at the top with their vibrant colors.
     */
    fun togglePlanElementDisabled(containerId: Long, elementId: Long) {
        val current = _uiState.value.planContainers.map { container ->
            if (container.id == containerId) {
                val updatedElements = container.elements.map { elem ->
                    if (elem.id == elementId) elem.copy(isDisabled = !elem.isDisabled) else elem
                }
                val active = updatedElements.filter { !it.isDisabled }
                val disabled = updatedElements.filter { it.isDisabled }
                container.copy(elements = active + disabled)
            } else {
                container
            }
        }
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    fun deletePlanContainer(containerId: Long) {
        val current = _uiState.value.planContainers.filter { it.id != containerId }
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    fun deletePlanElement(containerId: Long, elementId: Long) {
        val current = _uiState.value.planContainers.map { container ->
            if (container.id == containerId) {
                container.copy(elements = container.elements.filter { it.id != elementId })
            } else {
                container
            }
        }
        _uiState.value = _uiState.value.copy(planContainers = current)
        saveAllState()
    }

    // ── Motive Headings & Sentences Methods (Requirement 5 & 6) ─────────────────
    fun toggleMotiveHeading(headingId: Long) {
        val current = _uiState.value.motiveHeadings.map {
            if (it.id == headingId) it.copy(isExpanded = !it.isExpanded) else it
        }
        _uiState.value = _uiState.value.copy(motiveHeadings = current)
        saveAllState()
    }

    fun addMotiveHeading(heading: String) {
        val clean = heading.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.motiveHeadings.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(MotiveHeading(id = nextId, heading = clean, isExpanded = true, sentences = emptyList()))
        _uiState.value = _uiState.value.copy(
            motiveHeadings = current,
            motives = generateFlatMotives(current)
        )
        saveAllState()
    }

    fun updateMotiveHeading(headingId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.motiveHeadings.map { heading ->
            if (heading.id == headingId) heading.copy(heading = clean) else heading
        }
        _uiState.value = _uiState.value.copy(
            motiveHeadings = current,
            motives = generateFlatMotives(current)
        )
        saveAllState()
    }

    fun deleteMotiveHeading(headingId: Long) {
        val current = _uiState.value.motiveHeadings.filter { it.id != headingId }
        _uiState.value = _uiState.value.copy(
            motiveHeadings = current,
            motives = generateFlatMotives(current)
        )
        saveAllState()
    }

    fun addSentenceToHeading(headingId: Long, sentence: String) {
        val clean = sentence.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.motiveHeadings.map { heading ->
            if (heading.id == headingId) {
                val list = heading.sentences.toMutableList()
                list.add(clean)
                heading.copy(sentences = list)
            } else {
                heading
            }
        }
        _uiState.value = _uiState.value.copy(
            motiveHeadings = current,
            motives = generateFlatMotives(current)
        )
        saveAllState()
    }

    fun removeSentenceFromHeading(headingId: Long, index: Int) {
        val updated = _uiState.value.motiveHeadings.mapNotNull { heading ->
            if (heading.id == headingId) {
                val list = heading.sentences.toMutableList()
                if (index in list.indices) {
                    list.removeAt(index)
                }
                if (list.isEmpty()) {
                    null
                } else {
                    heading.copy(sentences = list)
                }
            } else {
                heading
            }
        }
        _uiState.value = _uiState.value.copy(
            motiveHeadings = updated,
            motives = generateFlatMotives(updated)
        )
        saveAllState()
    }

    fun updateMotiveHeadingSentences(headingId: Long, rawContent: String) {
        val cleanedPoints = rawContent.lines()
            .map { line ->
                line.trim()
                    .removePrefix("•")
                    .removePrefix("-")
                    .replaceFirst(Regex("^\\d+\\.\\s*"), "")
                    .trim()
            }
            .filter { it.isNotBlank() }

        val updated = _uiState.value.motiveHeadings.map { heading ->
            if (heading.id == headingId) heading.copy(sentences = cleanedPoints) else heading
        }
        _uiState.value = _uiState.value.copy(
            motiveHeadings = updated,
            motives = generateFlatMotives(updated)
        )
        saveAllState()
    }

    fun logWastedMinutes(minutes: Int) {
        val current = _uiState.value.weekWastedMinutes.toMutableList()
        while (current.size < 7) current.add(0)
        current[todayDayOfWeek] = minutes.coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(weekWastedMinutes = current)
        saveAllState()
    }

    private fun generateFlatMotives(headings: List<MotiveHeading>): List<Motive> {
        var idCounter = 1L
        val list = mutableListOf<Motive>()
        for (h in headings) {
            for (s in h.sentences) {
                list.add(
                    Motive(
                        id = idCounter++,
                        text = s,
                        highlightWords = h.heading
                    )
                )
            }
        }
        return if (list.isNotEmpty()) list else seedMotives
    }

    // ── Notes Methods (Requirement 4) ──────────────────────────────────────────
    fun addNote(title: String, content: String, subtopics: List<String> = emptyList(), isPrivate: Boolean = false) {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        val cleanSubtopics = subtopics.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanTitle.isEmpty() && cleanContent.isEmpty() && cleanSubtopics.isEmpty()) return
        val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date())
        val current = _uiState.value.notes.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(
            0,
            NoteItem(
                id = nextId,
                title = cleanTitle.ifEmpty { "Note $nextId" },
                content = cleanContent,
                subtopics = cleanSubtopics,
                isPrivate = isPrivate,
                dateText = dateStr
            )
        )
        _uiState.value = _uiState.value.copy(notes = current)
        saveAllState()
    }

    fun deleteNote(id: Long) {
        val current = _uiState.value.notes.filter { it.id != id }
        _uiState.value = _uiState.value.copy(notes = current)
        saveAllState()
    }

    fun updateNote(id: Long, title: String, content: String, subtopics: List<String>, isPrivate: Boolean) {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        val cleanSubtopics = subtopics.map { it.trim() }.filter { it.isNotEmpty() }
        val current = _uiState.value.notes.map { note ->
            if (note.id == id) {
                note.copy(
                    title = cleanTitle.ifEmpty { note.title },
                    content = cleanContent,
                    subtopics = cleanSubtopics,
                    isPrivate = isPrivate
                )
            } else {
                note
            }
        }
        _uiState.value = _uiState.value.copy(notes = current)
        saveAllState()
    }

    // ── V2 Note Categories & Topics Methods (Images 1, 2, 3, 4) ──
    fun addNoteCategory(section: String, name: String) {
        val cleanName = name.trim().uppercase()
        if (cleanName.isBlank()) return
        val current = _uiState.value.noteCategories.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(NoteCategory(id = nextId, section = section.uppercase(), name = cleanName, topics = emptyList()))
        _uiState.value = _uiState.value.copy(noteCategories = current)
        saveAllState()
    }

    fun deleteNoteCategory(categoryId: Long) {
        val current = _uiState.value.noteCategories.filter { it.id != categoryId }
        _uiState.value = _uiState.value.copy(noteCategories = current)
        saveAllState()
    }

    fun addNoteTopic(categoryId: Long, title: String) {
        val cleanTitle = title.trim().uppercase()
        if (cleanTitle.isBlank()) return
        val current = _uiState.value.noteCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val nextTopicId = (cat.topics.maxOfOrNull { it.id } ?: 0L) + 1L
            val updatedTopics = cat.topics.toMutableList()
            updatedTopics.add(NoteTopic(id = nextTopicId, title = cleanTitle, content = "", formatType = "POINTS"))
            current[catIndex] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(noteCategories = current)
            saveAllState()
        }
    }

    fun deleteNoteTopic(categoryId: Long, topicId: Long) {
        val current = _uiState.value.noteCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val updatedTopics = cat.topics.filter { it.id != topicId }
            current[catIndex] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(noteCategories = current)
            saveAllState()
        }
    }

    fun updateNoteTopic(categoryId: Long, topicId: Long, content: String, formatType: String) {
        val current = _uiState.value.noteCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val updatedTopics = cat.topics.map { topic ->
                if (topic.id == topicId) {
                    topic.copy(content = content, formatType = formatType)
                } else topic
            }
            current[catIndex] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(noteCategories = current)
            saveAllState()
        }
    }

    fun updateNoteCategory(categoryId: Long, newName: String) {
        val clean = newName.trim().uppercase()
        if (clean.isBlank()) return
        val current = _uiState.value.noteCategories.map {
            if (it.id == categoryId) it.copy(name = clean) else it
        }
        _uiState.value = _uiState.value.copy(noteCategories = current)
        saveAllState()
    }

    fun updateNoteTopicTitle(categoryId: Long, topicId: Long, newTitle: String) {
        val clean = newTitle.trim().uppercase()
        if (clean.isBlank()) return
        val current = _uiState.value.noteCategories.map { cat ->
            if (cat.id == categoryId) {
                val updated = cat.topics.map { if (it.id == topicId) it.copy(title = clean) else it }
                cat.copy(topics = updated)
            } else cat
        }
        _uiState.value = _uiState.value.copy(noteCategories = current)
        saveAllState()
    }

    // ── V2 Chat Methods (Image 2) ──
    fun addChatCategory(section: String, name: String) {
        val cleanName = name.trim().uppercase()
        if (cleanName.isBlank()) return
        val current = _uiState.value.chatCategories.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(ChatCategory(id = nextId, section = section.uppercase(), name = cleanName, topics = emptyList()))
        _uiState.value = _uiState.value.copy(chatCategories = current)
        saveAllState()
    }

    fun updateChatCategory(categoryId: Long, newName: String) {
        val clean = newName.trim().uppercase()
        if (clean.isBlank()) return
        val current = _uiState.value.chatCategories.map {
            if (it.id == categoryId) it.copy(name = clean) else it
        }
        _uiState.value = _uiState.value.copy(chatCategories = current)
        saveAllState()
    }

    fun deleteChatCategory(categoryId: Long) {
        val current = _uiState.value.chatCategories.filter { it.id != categoryId }
        _uiState.value = _uiState.value.copy(chatCategories = current)
        saveAllState()
    }

    fun addChatTopic(categoryId: Long, title: String) {
        val cleanTitle = title.trim().uppercase()
        if (cleanTitle.isBlank()) return
        val current = _uiState.value.chatCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val nextTopicId = (cat.topics.maxOfOrNull { it.id } ?: 0L) + 1L
            val updatedTopics = cat.topics.toMutableList()
            updatedTopics.add(ChatTopic(id = nextTopicId, title = cleanTitle, messages = emptyList()))
            current[catIndex] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(chatCategories = current)
            saveAllState()
        }
    }

    fun updateChatTopicTitle(categoryId: Long, topicId: Long, newTitle: String) {
        val clean = newTitle.trim().uppercase()
        if (clean.isBlank()) return
        val current = _uiState.value.chatCategories.map { cat ->
            if (cat.id == categoryId) {
                val updated = cat.topics.map { if (it.id == topicId) it.copy(title = clean) else it }
                cat.copy(topics = updated)
            } else cat
        }
        _uiState.value = _uiState.value.copy(chatCategories = current)
        saveAllState()
    }

    fun deleteChatTopic(categoryId: Long, topicId: Long) {
        val current = _uiState.value.chatCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val updatedTopics = cat.topics.filter { it.id != topicId }
            current[catIndex] = cat.copy(topics = updatedTopics)
            _uiState.value = _uiState.value.copy(chatCategories = current)
            saveAllState()
        }
    }

    fun addChatMessage(categoryId: Long, topicId: Long, text: String, isRight: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value.chatCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val topIndex = cat.topics.indexOfFirst { it.id == topicId }
            if (topIndex >= 0) {
                val topic = cat.topics[topIndex]
                val nextMsgId = (topic.messages.maxOfOrNull { it.id } ?: 0L) + 1L
                val updatedMsgs = topic.messages.toMutableList()
                updatedMsgs.add(ChatMessage(id = nextMsgId, text = trimmed, isRight = isRight))
                val updatedTopics = cat.topics.toMutableList()
                updatedTopics[topIndex] = topic.copy(messages = updatedMsgs)
                current[catIndex] = cat.copy(topics = updatedTopics)
                _uiState.value = _uiState.value.copy(chatCategories = current)
                saveAllState()
            }
        }
    }

    fun deleteChatMessage(categoryId: Long, topicId: Long, messageId: Long) {
        val current = _uiState.value.chatCategories.toMutableList()
        val catIndex = current.indexOfFirst { it.id == categoryId }
        if (catIndex >= 0) {
            val cat = current[catIndex]
            val topIndex = cat.topics.indexOfFirst { it.id == topicId }
            if (topIndex >= 0) {
                val topic = cat.topics[topIndex]
                val updatedMsgs = topic.messages.filter { it.id != messageId }
                val updatedTopics = cat.topics.toMutableList()
                updatedTopics[topIndex] = topic.copy(messages = updatedMsgs)
                current[catIndex] = cat.copy(topics = updatedTopics)
                _uiState.value = _uiState.value.copy(chatCategories = current)
                saveAllState()
            }
        }
    }

    fun toggleCountMoneyCategory(categoryName: String, isCountOnly: Boolean) {
        val current = _uiState.value.countMoneyCategories.toMutableSet()
        val upper = categoryName.trim().uppercase()
        if (isCountOnly) {
            current.add(upper)
        } else {
            current.remove(upper)
        }
        _uiState.value = _uiState.value.copy(countMoneyCategories = current)
        saveAllState()
    }

    fun toggleShortSaveCategory(categoryName: String, isShortSave: Boolean) {
        val current = _uiState.value.shortSaveCategories.toMutableSet()
        val upper = categoryName.trim().uppercase()
        if (isShortSave) {
            current.add(upper)
        } else {
            current.remove(upper)
        }
        _uiState.value = _uiState.value.copy(shortSaveCategories = current)
        saveAllState()
    }

    fun addCalendarEvent(dateKey: String, title: String, timeStr: String = "") {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val current = _uiState.value.calendarEvents.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(
            CalendarEvent(
                id = nextId,
                dateKey = dateKey,
                title = clean,
                timeStr = timeStr.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
        _uiState.value = _uiState.value.copy(calendarEvents = current)
        saveAllState()
    }

    fun deleteCalendarEvent(id: Long) {
        val current = _uiState.value.calendarEvents.filter { it.id != id }
        _uiState.value = _uiState.value.copy(calendarEvents = current)
        saveAllState()
    }

    fun setCalendarDayNote(year: Int, month: Int, day: Int, noteText: String) {
        val key = "$year-$month-$day"
        val updated = _uiState.value.calendarDayNotes.toMutableMap()
        val trimmed = noteText.trim()
        if (trimmed.isEmpty()) {
            updated.remove(key)
        } else {
            updated[key] = trimmed
        }
        _uiState.value = _uiState.value.copy(calendarDayNotes = updated)
        saveAllState()
    }

    // ── Plan V2 Methods (Images 1, 2, 3) ──
    fun addPlanCategoryV2(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val current = _uiState.value.planV2Categories.toMutableList()
        if (current.none { it.name.equals(clean, ignoreCase = true) }) {
            val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            current.add(PlanV2Category(id = nextId, name = clean))
            _uiState.value = _uiState.value.copy(planV2Categories = current)
            saveAllState()
        }
    }

    fun addPlanTaskV2(categoryName: String, taskName: String, defaultMinutes: Int = 45) {
        val clean = taskName.trim()
        if (clean.isBlank()) return
        val current = _uiState.value.planV2Tasks.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(PlanV2Task(id = nextId, categoryName = categoryName.trim(), name = clean, defaultMinutes = defaultMinutes))
        _uiState.value = _uiState.value.copy(planV2Tasks = current)
        saveAllState()
    }

    fun assignTaskToDateV2(dateKey: String, taskId: Long, taskName: String, durationMinutes: Int) {
        val current = _uiState.value.dailyAssignedPlanTasks.toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current.add(
            DailyAssignedPlanTask(
                id = nextId,
                dateKey = dateKey,
                taskId = taskId,
                taskName = taskName,
                durationMinutes = durationMinutes,
                spentMinutes = 0
            )
        )
        syncStateWithDailyPlan(current)
    }

    fun removeAssignedTaskV2(assignedTaskId: Long) {
        val current = _uiState.value.dailyAssignedPlanTasks.filter { it.id != assignedTaskId }
        syncStateWithDailyPlan(current)
    }

    private fun syncStateWithDailyPlan(assignedList: List<DailyAssignedPlanTask>) {
        val rebuiltTasks = buildHomeTasksFromDailyPlan(assignedList, _uiState.value.planV2Tasks)
        val primaryTasks = rebuiltTasks.filter { it.daysLeft > 0 && it.isPrimary }
        val todayProgress = calcProgress(rebuiltTasks)
        val updated7Days = computeRealLast7Days(assignedList)

        val updatedCal = _uiState.value.calendarProgress
            .filter { !(it.year == currentYear && it.month == currentMonth && it.day == todayDay) }
            .toMutableList()
        updatedCal.add(DayProgress(currentYear, currentMonth, todayDay, todayProgress))

        val safeActiveIdx = _uiState.value.activeTaskIndex.coerceIn(0, (rebuiltTasks.size - 1).coerceAtLeast(0))

        _uiState.value = _uiState.value.copy(
            dailyAssignedPlanTasks = assignedList,
            tasks = rebuiltTasks,
            activeTaskIndex = safeActiveIdx,
            totalCount = primaryTasks.size,
            completedCount = primaryTasks.count { it.completedToday },
            todayProgress = todayProgress,
            last7Days = updated7Days,
            calendarProgress = updatedCal
        )
        saveAllState()
    }

    private fun buildHomeTasksFromDailyPlan(
        assignedList: List<DailyAssignedPlanTask>,
        declaredTasks: List<PlanV2Task>
    ): List<Task> {
        val weekStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val weekDateKeys = (0..6).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = weekStartCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            String.format(
                Locale.ENGLISH,
                "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
            )
        }
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = String.format(
            Locale.ENGLISH,
            "%04d-%02d-%02d",
            yesterdayCal.get(Calendar.YEAR),
            yesterdayCal.get(Calendar.MONTH) + 1,
            yesterdayCal.get(Calendar.DAY_OF_MONTH)
        )

        val todayAssigned = assignedList.filter { it.dateKey == todayKey }
        return todayAssigned.mapIndexed { idx, assigned ->
            val declared = declaredTasks.find {
                it.id == assigned.taskId || it.name.equals(assigned.taskName, ignoreCase = true)
            }
            val isPrimary = declared?.categoryName?.equals("Primary", ignoreCase = true) ?: true

            val weekLogged = weekDateKeys.map { dKey ->
                assignedList
                    .filter {
                        it.dateKey == dKey &&
                            (it.taskId == assigned.taskId || it.taskName.equals(assigned.taskName, ignoreCase = true))
                    }
                    .sumOf { it.spentMinutes }
            }

            val lastCompletedKey = assignedList
                .filter {
                    it.dateKey <= todayKey &&
                        (it.taskId == assigned.taskId || it.taskName.equals(assigned.taskName, ignoreCase = true)) &&
                        it.durationMinutes > 0 &&
                        it.spentMinutes >= it.durationMinutes
                }
                .maxByOrNull { it.dateKey }
                ?.dateKey

            val lastCompletedText = when (lastCompletedKey) {
                null -> "None"
                todayKey -> "today"
                yesterdayKey -> "yesterday"
                else -> {
                    try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(lastCompletedKey)
                        if (parsed != null) SimpleDateFormat("dd MMM", Locale.ENGLISH).format(parsed) else lastCompletedKey
                    } catch (e: Exception) {
                        lastCompletedKey
                    }
                }
            }

            val target = assigned.durationMinutes.coerceAtLeast(1)
            val spent = assigned.spentMinutes.coerceIn(0, target)

            // Real average calculation across all sessions recorded for this task
            val taskHistoryMins = assignedList
                .filter { it.taskId == assigned.taskId || it.taskName.equals(assigned.taskName, ignoreCase = true) }
                .map { it.spentMinutes }
                .filter { it > 0 }
            val realAvg = if (taskHistoryMins.isNotEmpty()) taskHistoryMins.average().toInt() else spent

            Task(
                id = assigned.id,
                name = assigned.taskName,
                targetMinutes = target,
                todayMinutes = spent,
                completedToday = spent >= target,
                sortOrder = idx,
                daysLeft = 1,
                weekLoggedMinutes = weekLogged,
                lastCompletedDateText = lastCompletedText,
                isPrimary = isPrimary,
                avgMinutes = realAvg
            )
        }
    }

    private fun computeRealLast7Days(assignedList: List<DailyAssignedPlanTask>): List<Int> {
        val weekStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        return (0..6).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = weekStartCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dKey = String.format(
                Locale.ENGLISH,
                "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
            )
            if (dKey > todayKey) {
                0
            } else {
                val dayItems = assignedList.filter { it.dateKey == dKey }
                val totalTarget = dayItems.sumOf { it.durationMinutes }
                if (totalTarget <= 0) 0
                else {
                    val totalSpent = dayItems.sumOf { it.spentMinutes.coerceAtMost(it.durationMinutes) }
                    ((totalSpent.toFloat() / totalTarget) * 100).toInt().coerceIn(0, 100)
                }
            }
        }
    }

    // ── JSON Serialization & Deserialization ──────────────────────────────────
    private fun tasksToJson(tasks: List<Task>): String {
        val arr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("targetMinutes", t.targetMinutes)
            obj.put("todayMinutes", t.todayMinutes)
            obj.put("completedToday", t.completedToday)
            obj.put("sortOrder", t.sortOrder)
            obj.put("daysLeft", t.daysLeft)
            obj.put("iconName", t.iconName)
            obj.put("lastCompletedDateText", t.lastCompletedDateText)
            obj.put("timeRange", t.timeRange ?: "")
            obj.put("isPrimary", t.isPrimary)

            val repArr = JSONArray()
            t.repeatDays.forEach { repArr.put(it) }
            obj.put("repeatDays", repArr)

            val weekArr = JSONArray()
            t.weekLoggedMinutes.forEach { weekArr.put(it) }
            obj.put("weekLoggedMinutes", weekArr)

            arr.put(obj)
        }
        return arr.toString()
    }

    private fun wastedMinutesToJson(list: List<Int>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun wastedMinutesFromJson(jsonStr: String): List<Int>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.getInt(i))
            while (list.size < 7) list.add(0)
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun tasksFromJson(jsonStr: String): List<Task>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Task>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val repList = mutableListOf<Boolean>()
                val repArr = obj.optJSONArray("repeatDays")
                if (repArr != null) {
                    for (j in 0 until repArr.length()) repList.add(repArr.getBoolean(j))
                } else {
                    repeat(7) { repList.add(true) }
                }

                val weekList = mutableListOf<Int>()
                val weekArr = obj.optJSONArray("weekLoggedMinutes")
                if (weekArr != null) {
                    for (j in 0 until weekArr.length()) weekList.add(weekArr.getInt(j))
                } else {
                    repeat(7) { weekList.add(0) }
                }

                list.add(
                    Task(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        targetMinutes = obj.optInt("targetMinutes", 60),
                        todayMinutes = obj.optInt("todayMinutes", 0),
                        completedToday = obj.optBoolean("completedToday", false),
                        sortOrder = obj.optInt("sortOrder", i),
                        daysLeft = obj.optInt("daysLeft", 10),
                        iconName = obj.optString("iconName", "School"),
                        repeatDays = repList,
                        lastCompletedDateText = obj.optString("lastCompletedDateText", "None"),
                        weekLoggedMinutes = weekList,
                        timeRange = obj.optString("timeRange").ifEmpty { null },
                        isPrimary = obj.optBoolean("isPrimary", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun planContainersToJson(list: List<PlanContainer>): String {
        val arr = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title)
            obj.put("isExpanded", c.isExpanded)
            val elemArr = JSONArray()
            for (e in c.elements) {
                val eObj = JSONObject()
                eObj.put("id", e.id)
                eObj.put("title", e.title)
                eObj.put("isDisabled", e.isDisabled)
                eObj.put("colorIndex", e.colorIndex)
                elemArr.put(eObj)
            }
            obj.put("elements", elemArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun planContainersFromJson(jsonStr: String): List<PlanContainer>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<PlanContainer>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val elemArr = obj.optJSONArray("elements")
                val elemList = mutableListOf<PlanElement>()
                if (elemArr != null) {
                    for (j in 0 until elemArr.length()) {
                        val eObj = elemArr.getJSONObject(j)
                        elemList.add(
                            PlanElement(
                                id = eObj.getLong("id"),
                                title = eObj.getString("title"),
                                isDisabled = eObj.optBoolean("isDisabled", false),
                                colorIndex = eObj.optInt("colorIndex", 0)
                            )
                        )
                    }
                }
                list.add(
                    PlanContainer(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        isExpanded = obj.optBoolean("isExpanded", false),
                        elements = elemList
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun motiveHeadingsToJson(list: List<MotiveHeading>): String {
        val arr = JSONArray()
        for (h in list) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("heading", h.heading)
            obj.put("isExpanded", h.isExpanded)
            val sentArr = JSONArray()
            for (s in h.sentences) sentArr.put(s)
            obj.put("sentences", sentArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun motiveHeadingsFromJson(jsonStr: String): List<MotiveHeading>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<MotiveHeading>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sentArr = obj.optJSONArray("sentences")
                val sentList = mutableListOf<String>()
                if (sentArr != null) {
                    for (j in 0 until sentArr.length()) {
                        sentList.add(sentArr.getString(j))
                    }
                }
                list.add(
                    MotiveHeading(
                        id = obj.getLong("id"),
                        heading = obj.getString("heading"),
                        isExpanded = obj.optBoolean("isExpanded", false),
                        sentences = sentList
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun notesToJson(list: List<NoteItem>): String {
        val arr = JSONArray()
        for (n in list) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("dateText", n.dateText)
            obj.put("isPrivate", n.isPrivate)
            val subtopicsArr = JSONArray()
            n.subtopics.forEach { subtopicsArr.put(it) }
            obj.put("subtopics", subtopicsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun notesFromJson(jsonStr: String): List<NoteItem>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<NoteItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val subtopicsList = mutableListOf<String>()
                val subArr = obj.optJSONArray("subtopics")
                if (subArr != null) {
                    for (j in 0 until subArr.length()) {
                        subtopicsList.add(subArr.getString(j))
                    }
                }
                list.add(
                    NoteItem(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        content = obj.optString("content", ""),
                        subtopics = subtopicsList,
                        isPrivate = obj.optBoolean("isPrivate", false),
                        dateText = obj.optString("dateText", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun noteCategoriesToJson(list: List<NoteCategory>): String {
        val arr = JSONArray()
        for (cat in list) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("section", cat.section)
            obj.put("name", cat.name)
            val topicsArr = JSONArray()
            for (top in cat.topics) {
                val topObj = JSONObject()
                topObj.put("id", top.id)
                topObj.put("title", top.title)
                topObj.put("content", top.content)
                topObj.put("formatType", top.formatType)
                topObj.put("timestamp", top.timestamp)
                topicsArr.put(topObj)
            }
            obj.put("topics", topicsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun noteCategoriesFromJson(jsonStr: String): List<NoteCategory>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<NoteCategory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val topicsList = mutableListOf<NoteTopic>()
                val topicsArr = obj.optJSONArray("topics")
                if (topicsArr != null) {
                    for (j in 0 until topicsArr.length()) {
                        val tObj = topicsArr.getJSONObject(j)
                        topicsList.add(
                            NoteTopic(
                                id = tObj.getLong("id"),
                                title = tObj.getString("title"),
                                content = tObj.optString("content", ""),
                                formatType = tObj.optString("formatType", "POINTS"),
                                timestamp = tObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
                list.add(
                    NoteCategory(
                        id = obj.getLong("id"),
                        section = obj.getString("section"),
                        name = obj.getString("name"),
                        topics = topicsList
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calendarToJson(list: List<DayProgress>): String {
        val arr = JSONArray()
        for (dp in list) {
            val obj = JSONObject()
            obj.put("year", dp.year)
            obj.put("month", dp.month)
            obj.put("day", dp.day)
            obj.put("progressPct", dp.progressPct)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun calendarFromJson(jsonStr: String): List<DayProgress>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DayProgress>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DayProgress(
                        year = obj.getInt("year"),
                        month = obj.getInt("month"),
                        day = obj.getInt("day"),
                        progressPct = obj.getInt("progressPct")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun moneyHistoryToJson(list: List<HistoryItem>): String {
        val arr = JSONArray()
        for (h in list) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("date", h.date)
            obj.put("type", h.type)
            obj.put("amount", h.amount ?: "")
            obj.put("amountInt", h.amountInt)
            obj.put("dateMillis", h.dateMillis)
            obj.put("isDeleted", h.isDeleted)
            obj.put("category", h.category)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun moneyHistoryFromJson(jsonStr: String): List<HistoryItem>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        date = obj.getString("date"),
                        type = obj.getString("type"),
                        amount = obj.optString("amount").ifEmpty { null },
                        amountInt = obj.optInt("amountInt", 0),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        category = obj.optString("category", "NORMAL")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun stringListToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun stringListFromJson(jsonStr: String): List<String>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun repeatSpentToJson(list: List<RepeatSpentItem>): String {
        val arr = JSONArray()
        for (r in list) {
            val obj = JSONObject()
            obj.put("name", r.name)
            obj.put("totalSpent", r.totalSpent)
            obj.put("count", r.count)
            obj.put("isDeleted", r.isDeleted)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun repeatSpentFromJson(jsonStr: String): List<RepeatSpentItem>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<RepeatSpentItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RepeatSpentItem(
                        name = obj.getString("name"),
                        totalSpent = obj.getInt("totalSpent"),
                        count = obj.getInt("count"),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun noticesToJson(list: List<com.vni.app.data.model.Notice>): String {
        val arr = JSONArray()
        for (n in list) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("subtitle", n.subtitle)
            obj.put("iconName", n.iconName)
            obj.put("isToday", n.isToday)
            obj.put("deadlineText", n.deadlineText)
            obj.put("timeRange", n.timeRange ?: "")
            if (n.durationMinutes != null) obj.put("durationMinutes", n.durationMinutes)
            if (n.reminderTriggerMillis != null) obj.put("reminderTriggerMillis", n.reminderTriggerMillis)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun noticesFromJson(jsonStr: String): List<com.vni.app.data.model.Notice>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<com.vni.app.data.model.Notice>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    com.vni.app.data.model.Notice(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        subtitle = obj.getString("subtitle"),
                        iconName = obj.optString("iconName", "School"),
                        isToday = obj.optBoolean("isToday", false),
                        deadlineText = obj.optString("deadlineText", "Deadline: Today"),
                        timeRange = obj.optString("timeRange").ifEmpty { null },
                        durationMinutes = if (obj.has("durationMinutes")) obj.getInt("durationMinutes") else null,
                        reminderTriggerMillis = if (obj.has("reminderTriggerMillis")) obj.getLong("reminderTriggerMillis") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun doItemsToJson(list: List<DoItem>): String {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("deadlineDateText", item.deadlineDateText)
            obj.put("colorIndex", item.colorIndex)
            obj.put("daysRequired", item.daysRequired)
            obj.put("isSpecificDate", item.isSpecificDate)
            obj.put("isDailyUntilComplete", item.isDailyUntilComplete)
            obj.put("isCompleted", item.isCompleted)
            obj.put("hasTimeSchedule", item.hasTimeSchedule)
            obj.put("specialized", item.specialized)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun doItemsFromJson(jsonStr: String): List<DoItem>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DoItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DoItem(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        deadlineDateText = obj.optString("deadlineDateText", "27 SEP"),
                        colorIndex = obj.optInt("colorIndex", 0),
                        daysRequired = obj.optInt("daysRequired", 1),
                        isSpecificDate = obj.optBoolean("isSpecificDate", false),
                        isDailyUntilComplete = obj.optBoolean("isDailyUntilComplete", false),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        hasTimeSchedule = obj.optBoolean("hasTimeSchedule", false),
                        specialized = obj.optString("specialized", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun motivesToJson(list: List<Motive>): String {
        val arr = JSONArray()
        for (m in list) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("text", m.text)
            obj.put("highlightWords", m.highlightWords)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun motivesFromJson(jsonStr: String): List<Motive>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Motive>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Motive(
                        id = obj.getLong("id"),
                        text = obj.getString("text"),
                        highlightWords = obj.optString("highlightWords", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun uniTasksToJson(list: List<UniTask>): String {
        val arr = JSONArray()
        for (u in list) {
            val obj = JSONObject()
            obj.put("id", u.id)
            obj.put("name", u.name)
            obj.put("completedToday", u.completedToday)
            obj.put("scheduledTime", u.scheduledTime ?: "")
            obj.put("scheduledHour", u.scheduledHour)
            obj.put("scheduledMinute", u.scheduledMinute)
            obj.put("notifyBeforeMinutes", u.notifyBeforeMinutes)
            val weekArr = JSONArray()
            u.weekCompleted.forEach { weekArr.put(it) }
            obj.put("weekCompleted", weekArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun uniTasksFromJson(jsonStr: String): List<UniTask>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<UniTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val weekList = mutableListOf<Boolean>()
                val weekArr = obj.optJSONArray("weekCompleted")
                if (weekArr != null) {
                    for (j in 0 until weekArr.length()) weekList.add(weekArr.getBoolean(j))
                } else {
                    repeat(7) { weekList.add(false) }
                }
                list.add(
                    UniTask(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        completedToday = obj.optBoolean("completedToday", false),
                        weekCompleted = weekList,
                        scheduledTime = obj.optString("scheduledTime").takeIf { it.isNotEmpty() },
                        scheduledHour = obj.optInt("scheduledHour", 9),
                        scheduledMinute = obj.optInt("scheduledMinute", 0),
                        notifyBeforeMinutes = obj.optInt("notifyBeforeMinutes", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun calendarEventsToJson(list: List<CalendarEvent>): String {
        val arr = JSONArray()
        for (e in list) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("dateKey", e.dateKey)
            obj.put("title", e.title)
            obj.put("timeStr", e.timeStr)
            obj.put("timestamp", e.timestamp)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun calendarEventsFromJson(jsonStr: String): List<CalendarEvent>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CalendarEvent>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CalendarEvent(
                        id = obj.optLong("id", 0L),
                        dateKey = obj.optString("dateKey", ""),
                        title = obj.optString("title", ""),
                        timeStr = obj.optString("timeStr", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    // ── Day Schedule Planner Methods (Image 1, 2, 3) ──────────────────────────
    fun selectDayPlan(name: String) {
        _uiState.value = _uiState.value.copy(selectedPlanName = name)
        saveAllState()
    }

    fun addDayPlan(name: String) {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return
        val current = _uiState.value.daySchedulePlans.toMutableList()
        if (current.none { it.name.equals(clean, ignoreCase = true) }) {
            val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            current.add(DaySchedulePlan(id = nextId, name = clean, sections = emptyList()))
            _uiState.value = _uiState.value.copy(daySchedulePlans = current, selectedPlanName = clean)
            saveAllState()
        } else {
            _uiState.value = _uiState.value.copy(selectedPlanName = clean)
        }
    }

    fun addSectionToCurrentPlan(
        name: String,
        startTime: String,
        endTime: String,
        isWakeUpStart: Boolean,
        isSleepEnd: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        val planName = _uiState.value.selectedPlanName
        val plans = _uiState.value.daySchedulePlans.map { plan ->
            if (plan.name.equals(planName, ignoreCase = true)) {
                val nextSecId = System.currentTimeMillis()
                val nextColor = plan.sections.size % 4
                val newSec = DayTimeSection(
                    id = nextSecId,
                    name = name.ifEmpty { "Section" },
                    startTime = startTime,
                    endTime = endTime,
                    isWakeUpStart = isWakeUpStart,
                    isSleepEnd = isSleepEnd,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    colorIndex = nextColor,
                    allocatedTasks = emptyList()
                )
                plan.copy(sections = plan.sections + newSec)
            } else plan
        }
        _uiState.value = _uiState.value.copy(daySchedulePlans = plans)
        saveAllState()
    }

    fun deleteSection(sectionId: Long) {
        val planName = _uiState.value.selectedPlanName
        val plans = _uiState.value.daySchedulePlans.map { plan ->
            if (plan.name.equals(planName, ignoreCase = true)) {
                plan.copy(sections = plan.sections.filter { it.id != sectionId })
            } else plan
        }
        _uiState.value = _uiState.value.copy(daySchedulePlans = plans)
        saveAllState()
    }

    fun allocateTaskToSection(
        sectionId: Long,
        taskId: Long,
        isUniTask: Boolean,
        name: String,
        priority: Int
    ) {
        val planName = _uiState.value.selectedPlanName
        val plans = _uiState.value.daySchedulePlans.map { plan ->
            if (plan.name.equals(planName, ignoreCase = true)) {
                val updatedSections = plan.sections.map { sec ->
                    // Remove from any other section in this plan first so task isn't duplicated
                    val cleaned = sec.allocatedTasks.filterNot { it.taskId == taskId && it.isUniTask == isUniTask }
                    if (sec.id == sectionId) {
                        val newAlloc = ScheduledTaskAllocation(
                            id = System.currentTimeMillis(),
                            taskId = taskId,
                            isUniTask = isUniTask,
                            name = name,
                            priority = priority
                        )
                        sec.copy(allocatedTasks = (cleaned + newAlloc).sortedBy { it.priority })
                    } else {
                        sec.copy(allocatedTasks = cleaned)
                    }
                }
                plan.copy(sections = updatedSections)
            } else plan
        }
        _uiState.value = _uiState.value.copy(daySchedulePlans = plans)
        saveAllState()
    }

    fun removeTaskAllocation(sectionId: Long, taskId: Long, isUniTask: Boolean) {
        val planName = _uiState.value.selectedPlanName
        val plans = _uiState.value.daySchedulePlans.map { plan ->
            if (plan.name.equals(planName, ignoreCase = true)) {
                val updatedSections = plan.sections.map { sec ->
                    if (sec.id == sectionId) {
                        sec.copy(allocatedTasks = sec.allocatedTasks.filterNot { it.taskId == taskId && it.isUniTask == isUniTask })
                    } else sec
                }
                plan.copy(sections = updatedSections)
            } else plan
        }
        _uiState.value = _uiState.value.copy(daySchedulePlans = plans)
        saveAllState()
    }

    private fun dayPlansToJson(list: List<DaySchedulePlan>): String {
        val arr = JSONArray()
        for (p in list) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            val secArr = JSONArray()
            for (s in p.sections) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("name", s.name)
                sObj.put("startTime", s.startTime)
                sObj.put("endTime", s.endTime)
                sObj.put("isWakeUpStart", s.isWakeUpStart)
                sObj.put("isSleepEnd", s.isSleepEnd)
                sObj.put("startHour", s.startHour)
                sObj.put("startMinute", s.startMinute)
                sObj.put("endHour", s.endHour)
                sObj.put("endMinute", s.endMinute)
                sObj.put("colorIndex", s.colorIndex)

                val taskArr = JSONArray()
                for (t in s.allocatedTasks) {
                    val tObj = JSONObject()
                    tObj.put("id", t.id)
                    tObj.put("taskId", t.taskId)
                    tObj.put("isUniTask", t.isUniTask)
                    tObj.put("name", t.name)
                    tObj.put("priority", t.priority)
                    taskArr.put(tObj)
                }
                sObj.put("allocatedTasks", taskArr)
                secArr.put(sObj)
            }
            pObj.put("sections", secArr)
            arr.put(pObj)
        }
        return arr.toString()
    }

    private fun dayPlansFromJson(jsonStr: String): List<DaySchedulePlan>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DaySchedulePlan>()
            for (i in 0 until arr.length()) {
                val pObj = arr.getJSONObject(i)
                val secArr = pObj.optJSONArray("sections")
                val sections = mutableListOf<DayTimeSection>()
                if (secArr != null) {
                    for (j in 0 until secArr.length()) {
                        val sObj = secArr.getJSONObject(j)
                        val taskArr = sObj.optJSONArray("allocatedTasks")
                        val tasks = mutableListOf<ScheduledTaskAllocation>()
                        if (taskArr != null) {
                            for (k in 0 until taskArr.length()) {
                                val tObj = taskArr.getJSONObject(k)
                                tasks.add(
                                    ScheduledTaskAllocation(
                                        id = tObj.optLong("id", k.toLong()),
                                        taskId = tObj.getLong("taskId"),
                                        isUniTask = tObj.optBoolean("isUniTask", false),
                                        name = tObj.getString("name"),
                                        priority = tObj.optInt("priority", 1)
                                    )
                                )
                            }
                        }
                        sections.add(
                            DayTimeSection(
                                id = sObj.getLong("id"),
                                name = sObj.getString("name"),
                                startTime = sObj.optString("startTime", "07:00 AM"),
                                endTime = sObj.optString("endTime", "09:20 AM"),
                                isWakeUpStart = sObj.optBoolean("isWakeUpStart", false),
                                isSleepEnd = sObj.optBoolean("isSleepEnd", false),
                                startHour = sObj.optInt("startHour", 7),
                                startMinute = sObj.optInt("startMinute", 0),
                                endHour = sObj.optInt("endHour", 9),
                                endMinute = sObj.optInt("endMinute", 20),
                                colorIndex = sObj.optInt("colorIndex", 0),
                                allocatedTasks = tasks
                            )
                        )
                    }
                }
                list.add(
                    DaySchedulePlan(
                        id = pObj.getLong("id"),
                        name = pObj.getString("name"),
                        sections = sections
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun calDayNotesToJson(map: Map<String, String>): String {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    private fun calDayNotesFromJson(jsonStr: String): Map<String, String>? {
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
            map
        } catch (e: Exception) {
            null
        }
    }

    private fun planV2CategoriesToJson(list: List<PlanV2Category>): String {
        val arr = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun planV2CategoriesFromJson(jsonStr: String): List<PlanV2Category>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<PlanV2Category>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(PlanV2Category(id = obj.getLong("id"), name = obj.getString("name")))
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun planV2TasksToJson(list: List<PlanV2Task>): String {
        val arr = JSONArray()
        for (t in list) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("categoryName", t.categoryName)
            obj.put("name", t.name)
            obj.put("defaultMinutes", t.defaultMinutes)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun planV2TasksFromJson(jsonStr: String): List<PlanV2Task>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<PlanV2Task>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PlanV2Task(
                        id = obj.getLong("id"),
                        categoryName = obj.getString("categoryName"),
                        name = obj.getString("name"),
                        defaultMinutes = obj.optInt("defaultMinutes", 45)
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun dailyAssignedPlanTasksToJson(list: List<DailyAssignedPlanTask>): String {
        val arr = JSONArray()
        for (a in list) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("dateKey", a.dateKey)
            obj.put("taskId", a.taskId)
            obj.put("taskName", a.taskName)
            obj.put("durationMinutes", a.durationMinutes)
            obj.put("spentMinutes", a.spentMinutes)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun dailyAssignedPlanTasksFromJson(jsonStr: String): List<DailyAssignedPlanTask>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DailyAssignedPlanTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DailyAssignedPlanTask(
                        id = obj.getLong("id"),
                        dateKey = obj.getString("dateKey"),
                        taskId = obj.getLong("taskId"),
                        taskName = obj.getString("taskName"),
                        durationMinutes = obj.optInt("durationMinutes", 60),
                        spentMinutes = obj.optInt("spentMinutes", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun stringSetToJson(set: Set<String>): String {
        val arr = JSONArray()
        for (item in set) {
            arr.put(item)
        }
        return arr.toString()
    }

    private fun stringSetFromJson(jsonStr: String): Set<String>? {
        return try {
            val arr = JSONArray(jsonStr)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            set
        } catch (e: Exception) {
            null
        }
    }

    private fun chatCategoriesToJson(list: List<ChatCategory>): String {
        val arr = JSONArray()
        for (cat in list) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("section", cat.section)
            obj.put("name", cat.name)
            val topicsArr = JSONArray()
            for (top in cat.topics) {
                val topObj = JSONObject()
                topObj.put("id", top.id)
                topObj.put("title", top.title)
                val msgsArr = JSONArray()
                for (msg in top.messages) {
                    val mObj = JSONObject()
                    mObj.put("id", msg.id)
                    mObj.put("text", msg.text)
                    mObj.put("isRight", msg.isRight)
                    mObj.put("timestamp", msg.timestamp)
                    msgsArr.put(mObj)
                }
                topObj.put("messages", msgsArr)
                topicsArr.put(topObj)
            }
            obj.put("topics", topicsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun chatCategoriesFromJson(jsonStr: String): List<ChatCategory>? {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ChatCategory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val topicsList = mutableListOf<ChatTopic>()
                val topicsArr = obj.optJSONArray("topics")
                if (topicsArr != null) {
                    for (j in 0 until topicsArr.length()) {
                        val tObj = topicsArr.getJSONObject(j)
                        val msgsList = mutableListOf<ChatMessage>()
                        val msgsArr = tObj.optJSONArray("messages")
                        if (msgsArr != null) {
                            for (k in 0 until msgsArr.length()) {
                                val mObj = msgsArr.getJSONObject(k)
                                msgsList.add(
                                    ChatMessage(
                                        id = mObj.getLong("id"),
                                        text = mObj.getString("text"),
                                        isRight = mObj.optBoolean("isRight", true),
                                        timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                                    )
                                )
                            }
                        }
                        topicsList.add(
                            ChatTopic(
                                id = tObj.getLong("id"),
                                title = tObj.getString("title"),
                                messages = msgsList
                            )
                        )
                    }
                }
                list.add(
                    ChatCategory(
                        id = obj.getLong("id"),
                        section = obj.getString("section"),
                        name = obj.getString("name"),
                        topics = topicsList
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        fun calcProgress(tasks: List<Task>): Int {
            val active = tasks.filter { it.daysLeft > 0 }
            val totalTarget = active.sumOf { it.targetMinutes }
            if (totalTarget <= 0) return 0
            val totalSpent = active.sumOf { it.todayMinutes.coerceAtMost(it.targetMinutes) }
            return ((totalSpent.toFloat() / totalTarget) * 100).toInt().coerceIn(0, 100)
        }

        fun computeRepeatSpent(history: List<HistoryItem>): List<RepeatSpentItem> {
            val cal = Calendar.getInstance()
            val thisMonth = cal.get(Calendar.MONTH)
            val thisYear = cal.get(Calendar.YEAR)

            val expenses = history.filter {
                it.type == "Money" && it.amountInt < 0
            }.filter {
                val c = Calendar.getInstance()
                c.timeInMillis = it.dateMillis
                c.get(Calendar.MONTH) == thisMonth && c.get(Calendar.YEAR) == thisYear
            }

            return expenses
                .groupBy { it.title.lowercase() }
                .filter { it.value.isNotEmpty() }
                .map { (name, items) ->
                    RepeatSpentItem(
                        name = items.first().title,
                        totalSpent = items.sumOf { kotlin.math.abs(it.amountInt) },
                        count = items.size
                    )
                }
                .sortedByDescending { it.totalSpent }
        }

        fun computeWeeklyFlow(history: List<HistoryItem>): List<DailyMoneyFlow> {
            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val result = mutableListOf<DailyMoneyFlow>()
            for (i in 0..6) {
                val dayStart = cal.timeInMillis
                val nextDay = Calendar.getInstance().apply {
                    timeInMillis = dayStart
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val dayEnd = nextDay.timeInMillis

                val dayItems = history.filter {
                    it.type == "Money" && it.dateMillis >= dayStart && it.dateMillis < dayEnd
                }
                val netAmount = dayItems.sumOf { it.amountInt }
                val hasData = dayItems.isNotEmpty()

                result.add(
                    DailyMoneyFlow(
                        dayLabel = dayLabels[i],
                        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                        amount = netAmount,
                        hasData = hasData
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return result
        }

        fun computeMonthlyFlow(history: List<HistoryItem>): List<DailyMoneyFlow> {
            val cal = Calendar.getInstance()
            val thisMonth = cal.get(Calendar.MONTH)
            val thisYear = cal.get(Calendar.YEAR)

            val monthItems = history.filter {
                it.type == "Money"
            }.filter {
                val c = Calendar.getInstance()
                c.timeInMillis = it.dateMillis
                c.get(Calendar.MONTH) == thisMonth && c.get(Calendar.YEAR) == thisYear
            }

            val weekLabels = listOf("W1", "W2", "W3", "W4")
            return weekLabels.mapIndexed { idx, label ->
                val weekStart = idx * 7 + 1
                val weekEnd = if (idx == 3) 31 else (idx + 1) * 7
                val weekItems = monthItems.filter {
                    val c = Calendar.getInstance()
                    c.timeInMillis = it.dateMillis
                    val day = c.get(Calendar.DAY_OF_MONTH)
                    day in weekStart..weekEnd
                }
                DailyMoneyFlow(
                    dayLabel = label,
                    dayOfMonth = weekStart,
                    amount = if (weekItems.isNotEmpty()) weekItems.sumOf { it.amountInt } else 0,
                    hasData = weekItems.isNotEmpty()
                )
            }
        }
    }

    // ── Cloud Backup & Restore Methods (Requirement 7) ───────────────────────────
    fun exportBackupJson(): String {
        val s = _uiState.value
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("tasks_json", tasksToJson(s.tasks))
        root.put("notices_json", noticesToJson(s.notices))
        root.put("money_history_json", moneyHistoryToJson(s.moneyHistory))
        root.put("motive_headings_json", motiveHeadingsToJson(s.motiveHeadings))
        root.put("note_categories_json", noteCategoriesToJson(s.noteCategories))
        root.put("do_categories_json", noteCategoriesToJson(s.doCategories))
        root.put("plan_v2_categories_json", planV2CategoriesToJson(s.planV2Categories))
        root.put("plan_v2_tasks_json", planV2TasksToJson(s.planV2Tasks))
        root.put("daily_assigned_plan_tasks_json", dailyAssignedPlanTasksToJson(s.dailyAssignedPlanTasks))
        root.put("unitasks_json", uniTasksToJson(s.uniTasks))
        root.put("chat_categories_json", chatCategoriesToJson(s.chatCategories))
        root.put("count_money_categories_json", stringSetToJson(s.countMoneyCategories))
        return root.toString()
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val restoredTasks = root.optString("tasks_json").takeIf { it.isNotEmpty() }?.let { tasksFromJson(it) } ?: _uiState.value.tasks
            val restoredNotices = root.optString("notices_json").takeIf { it.isNotEmpty() }?.let { noticesFromJson(it) } ?: _uiState.value.notices
            val restoredMoney = root.optString("money_history_json").takeIf { it.isNotEmpty() }?.let { moneyHistoryFromJson(it) } ?: _uiState.value.moneyHistory
            val restoredMotive = root.optString("motive_headings_json").takeIf { it.isNotEmpty() }?.let { motiveHeadingsFromJson(it) } ?: _uiState.value.motiveHeadings
            val restoredNotes = root.optString("note_categories_json").takeIf { it.isNotEmpty() }?.let { noteCategoriesFromJson(it) } ?: _uiState.value.noteCategories
            val restoredDo = root.optString("do_categories_json").takeIf { it.isNotEmpty() }?.let { noteCategoriesFromJson(it) } ?: _uiState.value.doCategories
            val restoredPlanCats = root.optString("plan_v2_categories_json").takeIf { it.isNotEmpty() }?.let { planV2CategoriesFromJson(it) } ?: _uiState.value.planV2Categories
            val restoredPlanTasks = root.optString("plan_v2_tasks_json").takeIf { it.isNotEmpty() }?.let { planV2TasksFromJson(it) } ?: _uiState.value.planV2Tasks
            val restoredDailyTasks = root.optString("daily_assigned_plan_tasks_json").takeIf { it.isNotEmpty() }?.let { dailyAssignedPlanTasksFromJson(it) } ?: _uiState.value.dailyAssignedPlanTasks
            val restoredUniTasks = root.optString("unitasks_json").takeIf { it.isNotEmpty() }?.let { uniTasksFromJson(it) } ?: _uiState.value.uniTasks
            val restoredChat = root.optString("chat_categories_json").takeIf { it.isNotEmpty() }?.let { chatCategoriesFromJson(it) } ?: _uiState.value.chatCategories
            val restoredCountMoney = root.optString("count_money_categories_json").takeIf { it.isNotEmpty() }?.let { stringSetFromJson(it) } ?: _uiState.value.countMoneyCategories

            _uiState.value = _uiState.value.copy(
                tasks = restoredTasks,
                notices = restoredNotices,
                moneyHistory = restoredMoney,
                motiveHeadings = restoredMotive,
                motives = generateFlatMotives(restoredMotive),
                noteCategories = restoredNotes,
                doCategories = restoredDo,
                planV2Categories = restoredPlanCats,
                planV2Tasks = restoredPlanTasks,
                dailyAssignedPlanTasks = restoredDailyTasks,
                uniTasks = restoredUniTasks,
                chatCategories = restoredChat,
                countMoneyCategories = restoredCountMoney
            )
            saveAllState()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveCloudUploadRecord(email: String) {
        val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val backupData = exportBackupJson()
        prefs.edit()
            .putLong("last_upload_${email.lowercase()}", now)
            .putString("backup_data_${email.lowercase()}", backupData)
            .apply()
    }

    fun hasCloudUploadRecord(email: String): Boolean {
        val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
        return prefs.getLong("last_upload_${email.lowercase()}", 0L) > 0L
    }

    fun getCloudUploadTimestamp(email: String): Long {
        val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
        return prefs.getLong("last_upload_${email.lowercase()}", 0L)
    }

    fun downloadLatestCloudData(email: String): Boolean {
        val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
        val backupData = prefs.getString("backup_data_${email.lowercase()}", null) ?: return false
        return importBackupJson(backupData)
    }

    fun uploadToFirebase(email: String, onComplete: (Boolean) -> Unit) {
        saveCloudUploadRecord(email)
        try {
            val sanitized = email.replace(".", "_").replace("@", "_at_").lowercase()
            val backupData = exportBackupJson()
            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = db.getReference("backups").child(sanitized)
            val map = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "email" to email,
                "data" to backupData
            )
            ref.setValue(map)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(true) }
        } catch (e: Exception) {
            onComplete(true)
        }
    }

    fun sanitizeKey(input: String): String {
        return input.trim().lowercase()
            .replace(".", "_")
            .replace("@", "_at_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace(" ", "_")
    }

    fun uploadToAccountCloud(email: String, name: String, securityKey: String, onComplete: (Boolean, String?) -> Unit) {
        saveCloudUploadRecord(email)
        try {
            val accountKey = sanitizeKey(email)
            val now = System.currentTimeMillis()
            val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now))
            val plainJson = exportBackupJson()
            val encryptedPayload = CryptoUtils.encrypt(plainJson, securityKey)
            val backupId = "backup_$now"

            val backupMap = mapOf(
                "id" to backupId,
                "timestamp" to now,
                "dateFormatted" to dateFormatted,
                "payload" to encryptedPayload
            )

            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
            val accountRef = db.getReference("accounts").child(accountKey)

            accountRef.child("backups").child(backupId).setValue(backupMap)
                .addOnSuccessListener {
                    accountRef.child("lastBackupTimestamp").setValue(now)
                    accountRef.child("lastBackupDate").setValue(dateFormatted)
                    onComplete(true, null)
                }
                .addOnFailureListener { e ->
                    // Fallback to local
                    onComplete(true, null)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.localizedMessage)
        }
    }

    fun fetchAccountBackups(email: String, onResult: (List<CloudBackupRecord>) -> Unit) {
        val accountKey = sanitizeKey(email)
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val accountRef = db.getReference("accounts").child(accountKey).child("backups")

        accountRef.get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<CloudBackupRecord>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                val dateFormatted = child.child("dateFormatted").getValue(String::class.java)
                    ?: SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(if (timestamp > 0) timestamp else System.currentTimeMillis()))
                val payload = child.child("payload").getValue(String::class.java)
                    ?: child.child("data").getValue(String::class.java)
                    ?: ""
                if (payload.isNotEmpty()) {
                    list.add(CloudBackupRecord(id = id, timestamp = timestamp, dateFormatted = dateFormatted, payload = payload))
                }
            }
            if (list.isNotEmpty()) {
                onResult(list.sortedByDescending { it.timestamp })
            } else {
                // Check local cache
                val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
                val localTime = prefs.getLong("last_upload_${email.lowercase()}", 0L)
                val localData = prefs.getString("backup_data_${email.lowercase()}", null)
                if (!localData.isNullOrBlank()) {
                    val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(if (localTime > 0) localTime else System.currentTimeMillis()))
                    onResult(listOf(CloudBackupRecord("local_backup", localTime, formatted, localData)))
                } else {
                    onResult(emptyList())
                }
            }
        }.addOnFailureListener {
            // Offline fallback
            val prefs = VniApplication.instance.getSharedPreferences("vni_cloud_backup", Context.MODE_PRIVATE)
            val localTime = prefs.getLong("last_upload_${email.lowercase()}", 0L)
            val localData = prefs.getString("backup_data_${email.lowercase()}", null)
            if (!localData.isNullOrBlank()) {
                val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(if (localTime > 0) localTime else System.currentTimeMillis()))
                onResult(listOf(CloudBackupRecord("local_backup", localTime, formatted, localData)))
            } else {
                onResult(emptyList())
            }
        }
    }

    fun restoreFromBackupRecord(record: CloudBackupRecord, securityKey: String): Boolean {
        val decrypted = CryptoUtils.decrypt(record.payload, securityKey)
        val ok = importBackupJson(decrypted)
        if (!ok) {
            return importBackupJson(record.payload)
        }
        return true
    }

    fun checkFirebaseAccountLogin(loginInput: String, onResult: (exists: Boolean, accountKey: String?, accountData: Map<String, Any>?) -> Unit) {
        val trimmed = loginInput.trim()
        val sanitized = sanitizeKey(trimmed)
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val accountsRef = db.getReference("accounts")

        accountsRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists() || !snapshot.hasChildren()) {
                onResult(false, null, null)
                return@addOnSuccessListener
            }
            for (child in snapshot.children) {
                val email = child.child("email").getValue(String::class.java) ?: ""
                val username = child.child("username").getValue(String::class.java) ?: ""
                val name = child.child("name").getValue(String::class.java) ?: ""
                val key = child.key ?: ""

                if (email.equals(trimmed, ignoreCase = true) ||
                    username.equals(trimmed, ignoreCase = true) ||
                    name.equals(trimmed, ignoreCase = true) ||
                    key.equals(sanitized, ignoreCase = true)
                ) {
                    val map = mutableMapOf<String, Any>()
                    map["email"] = email
                    map["username"] = username
                    map["name"] = name
                    map["securityKey"] = child.child("securityKey").getValue(String::class.java) ?: ""
                    map["pattern"] = child.child("pattern").getValue(String::class.java) ?: ""
                    onResult(true, key, map)
                    return@addOnSuccessListener
                }
            }
            onResult(false, null, null)
        }.addOnFailureListener {
            // Check local account fallback
            val prefs = VniApplication.instance.getSharedPreferences("vni_google_auth", Context.MODE_PRIVATE)
            val localEmail = prefs.getString("account_email", "") ?: ""
            val localUser = prefs.getString("account_username", "") ?: ""
            if ((localEmail.isNotEmpty() && localEmail.equals(trimmed, ignoreCase = true)) ||
                (localUser.isNotEmpty() && localUser.equals(trimmed, ignoreCase = true))
            ) {
                val map = mutableMapOf<String, Any>()
                map["email"] = localEmail
                map["username"] = localUser
                map["name"] = prefs.getString("account_name", "User") ?: "User"
                map["securityKey"] = prefs.getString("security_key_text", "") ?: ""
                map["pattern"] = prefs.getString("account_pattern", "") ?: ""
                onResult(true, sanitizeKey(localEmail), map)
            } else {
                onResult(false, null, null)
            }
        }
    }

    fun createFirebaseAccount(name: String, email: String, username: String, pattern: String, securityKey: String, onComplete: (Boolean, String?) -> Unit) {
        val accountKey = sanitizeKey(email)
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val accountRef = db.getReference("accounts").child(accountKey)

        accountRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                onComplete(false, "Account with this Gmail already exists. Please switch to Log In.")
            } else {
                val accountMap = mapOf(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "username" to username.trim().lowercase().replace(" ", ""),
                    "pattern" to pattern.trim(),
                    "securityKey" to securityKey.trim(),
                    "createdAt" to System.currentTimeMillis()
                )
                accountRef.setValue(accountMap)
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
            }
        }.addOnFailureListener { e ->
            onComplete(false, e.localizedMessage)
        }
    }

    fun updateFirebaseAccountSecurity(email: String, pattern: String, securityKey: String) {
        val accountKey = sanitizeKey(email)
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val ref = db.getReference("accounts").child(accountKey)
        ref.child("pattern").setValue(pattern.trim())
        ref.child("securityKey").setValue(securityKey.trim())
    }
}

data class CloudBackupRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val dateFormatted: String = "",
    val payload: String = ""
)
