package com.vni.app.data.model

/** A single consistency task (e.g., "Study", "Workout"). */
data class Task(
    val id: Long = 0,
    val name: String,
    /** Target duration in minutes per day */
    val targetMinutes: Int = 60,
    /** Total minutes logged today */
    val todayMinutes: Int = 0,
    /** Whether task is marked complete today */
    val completedToday: Boolean = false,
    /** Display order index */
    val sortOrder: Int = 0,
    /** Days left for task */
    val daysLeft: Int = 12,
    /** Name of the icon */
    val iconName: String = "School",
    /** Active days of week: Sunday (index 0) to Saturday (index 6) */
    val repeatDays: List<Boolean> = listOf(true, true, true, true, true, true, true),
    /** Day when last fully completed: "today", "yesterday", "24 Sep", or "None" */
    val lastCompletedDateText: String = "None",
    /** Past minutes logged per day for this week (Sunday to Saturday, index 0 to 6) */
    val weekLoggedMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    /** Daily active time range, e.g. "03:00 PM - 05:00 PM" */
    val timeRange: String? = null,
    /** Whether task is Primary (included in daily 100% progress) or General (not in progress %) */
    val isPrimary: Boolean = true,
    /** Real average minutes logged across all recorded sessions for this task */
    val avgMinutes: Int = 0
) {
    /**
     * Sunday to present day weekly average comparison with 50%.
     * Returns the percentage difference (e.g. 38% -> -12%).
     */
    fun getAvgTimeDiff(currentDayOfWeekIndex: Int = 0): Int {
        val daysElapsed = (currentDayOfWeekIndex + 1).coerceAtLeast(1)
        val totalTarget = daysElapsed * targetMinutes.coerceAtLeast(1)
        var totalSpent = 0
        for (i in 0..currentDayOfWeekIndex) {
            totalSpent += if (i == currentDayOfWeekIndex) todayMinutes else weekLoggedMinutes.getOrElse(i) { 0 }
        }
        val progressPct = ((totalSpent.toFloat() / totalTarget) * 100).toInt()
        return progressPct - 50
    }

    /**
     * Sunday to present day weekly progress percentage (0..100%).
     */
    fun getWeekProgressPct(currentDayOfWeekIndex: Int = 0): Int {
        val daysElapsed = (currentDayOfWeekIndex + 1).coerceAtLeast(1)
        val totalTarget = daysElapsed * targetMinutes.coerceAtLeast(1)
        var totalSpent = 0
        for (i in 0..currentDayOfWeekIndex) {
            totalSpent += if (i == currentDayOfWeekIndex) todayMinutes else weekLoggedMinutes.getOrElse(i) { 0 }
        }
        return if (totalTarget > 0) ((totalSpent.toFloat() / totalTarget) * 100).toInt().coerceIn(0, 100) else 0
    }

    fun getAvgTimeText(currentDayOfWeekIndex: Int = 0): String {
        val diff = getAvgTimeDiff(currentDayOfWeekIndex)
        return when {
            diff < 0 -> "$diff% ↓"
            diff > 0 -> "+$diff% ↑"
            else -> "0%"
        }
    }

    fun isAvgTimeDecreased(currentDayOfWeekIndex: Int = 0): Boolean = getAvgTimeDiff(currentDayOfWeekIndex) < 0

    fun isScheduledToday(currentDayOfWeekIndex: Int = 0): Boolean =
        repeatDays.getOrElse(currentDayOfWeekIndex) { true }

    fun getFullyCompletedDisplay(): String {
        return if (todayMinutes >= targetMinutes && targetMinutes > 0) {
            "today"
        } else {
            lastCompletedDateText.ifEmpty { "None" }
        }
    }
}

/** A timed session for a task */
data class Session(
    val id: Long = 0,
    val taskId: Long,
    val durationMinutes: Int,
    /** Epoch millis when session ended */
    val endedAt: Long = System.currentTimeMillis(),
    /** Day offset from today: 0=today, 1=yesterday, etc. */
    val dayOffset: Int = 0
)

/** A motivational phrase shown in the motive ticker */
data class Motive(
    val id: Long = 0,
    val text: String,
    /** Purple-highlighted word(s) - comma-separated */
    val highlightWords: String = ""
)

data class Notice(
    val id: Long,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val isToday: Boolean = false,
    val deadlineText: String = "Deadline: Today",
    val timeRange: String? = null,
    val durationMinutes: Int? = null,
    val reminderTriggerMillis: Long? = null
)

data class Suggestion(
    val id: Long,
    val text: String,
    val highlightText: String
)

data class DoItem(
    val id: Long = 0,
    val name: String,
    val deadlineDateText: String = "27 SEP",
    val colorIndex: Int = 0,
    val daysRequired: Int = 1,
    val isSpecificDate: Boolean = false,
    val isDailyUntilComplete: Boolean = false,
    val isCompleted: Boolean = false,
    val hasTimeSchedule: Boolean = false,
    val specialized: String = ""
)

data class UniTask(
    val id: Long = 0,
    val name: String,
    val completedToday: Boolean = false,
    /** Completed status per day of this week (Sunday = index 0 to Saturday = index 6) */
    val weekCompleted: List<Boolean> = listOf(false, false, false, false, false, false, false),
    val scheduledTime: String? = null, // e.g. "08:30 AM"
    val scheduledHour: Int = 9,
    val scheduledMinute: Int = 0,
    val notifyBeforeMinutes: Int = 0
) {
    fun getWeekCompletedCount(): Int = weekCompleted.count { it }
}

data class PlanElement(
    val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val colorIndex: Int = 0,
    val isDisabled: Boolean = false
)

data class PlanContainer(
    val id: Long = 0,
    val title: String,
    val elements: List<PlanElement> = emptyList(),
    val isExpanded: Boolean = false
)

data class MotiveHeading(
    val id: Long = 0,
    val heading: String,
    val sentences: List<String> = emptyList(),
    val isExpanded: Boolean = true
)

data class NoteItem(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val subtopics: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val dateText: String = "Today",
    val timestamp: Long = System.currentTimeMillis()
)

/** Note Topic inside a category (Image 2 & 3) */
data class NoteTopic(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val formatType: String = "POINTS", // "POINTS", "NUMBER", "NORMAL"
    val timestamp: Long = System.currentTimeMillis()
)

/** Note Category within a Section (Image 1 & 2) e.g. "MOTIVE", "QUOTES" in "GENERAL" */
data class NoteCategory(
    val id: Long = 0,
    val section: String = "GENERAL", // "GENERAL", "PRIMARY", "PRIVATE"
    val name: String,
    val topics: List<NoteTopic> = emptyList()
)

/** Scheduled task or unitask allocation within a section of a day plan (Image 1 & 3) */
data class ScheduledTaskAllocation(
    val id: Long = 0,
    val taskId: Long,
    val isUniTask: Boolean = false,
    val name: String,
    val priority: Int = 1
)

/** A custom time section of the day (Image 1 & 2) e.g. "Mrng" 7:00 AM - 9:20 AM */
data class DayTimeSection(
    val id: Long = 0,
    val name: String,
    val startTime: String = "07:00 AM",     // e.g. "07:00 AM" or "Wake up"
    val endTime: String = "09:20 AM",       // e.g. "09:20 AM" or "Sleep"
    val isWakeUpStart: Boolean = false,
    val isSleepEnd: Boolean = false,
    val startHour: Int = 7,
    val startMinute: Int = 0,
    val endHour: Int = 9,
    val endMinute: Int = 20,
    val colorIndex: Int = 0,
    val allocatedTasks: List<ScheduledTaskAllocation> = emptyList()
)

/** A complete day plan template (Image 1) e.g. "clg", "holiday", "home" */
data class DaySchedulePlan(
    val id: Long = 0,
    val name: String, // e.g. "clg", "holiday", "home"
    val sections: List<DayTimeSection> = emptyList()
)

/** Plan Category V2 matching Images 1 & 2 (e.g. Primary, Secondary, Eventually) */
data class PlanV2Category(
    val id: Long = 0,
    val name: String
)

/** Plan Declared Task V2 matching Images 1 & 2 (e.g. "App Development", "Prolems solving", "Dsa") */
data class PlanV2Task(
    val id: Long = 0,
    val categoryName: String,
    val name: String,
    val defaultMinutes: Int = 45
)

/** Daily Assigned Task V2 matching Images 1 & 3 */
data class DailyAssignedPlanTask(
    val id: Long = 0,
    val dateKey: String, // "yyyy-MM-dd" e.g. "2026-11-10"
    val taskId: Long,
    val taskName: String,
    val durationMinutes: Int = 60,
    val spentMinutes: Int = 0
)

/** App Version Item for In-App Updates matching the updates panel */
data class AppVersionItem(
    val versionKey: String = "v1",
    val versionName: String = "Vni V1",
    val versionCode: Int = 1,
    val lastUpdatedDate: String = "22/07/26",
    val sizeText: String = "4.4 MB",
    val downloadUrl: String = "",
    val hasUpload: Boolean = true,
    val isCurrent: Boolean = false
)

/** Money Category (e.g. "ALL", "NORMAL", "FOOD", "TRAVEL") */
data class MoneyCategory(
    val id: Long = 0,
    val name: String,
    val isCountOnly: Boolean = false,
    val isShortSave: Boolean = false
)

/** Calendar Event created on holding a day cell for 1 second */
data class CalendarEvent(
    val id: Long = 0,
    val dateKey: String = "", // "yyyy-MM-dd"
    val title: String,
    val timeStr: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** Chat message with left/right alignment matching Image 2 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isRight: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/** Chat topic inside a category (Level 2) */
data class ChatTopic(
    val id: Long = 0,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/** Chat category within a section (Level 1) e.g. "GENERAL", "PRIMARY", "PRIVATE" */
data class ChatCategory(
    val id: Long = 0,
    val section: String = "GENERAL",
    val name: String,
    val topics: List<ChatTopic> = emptyList()
)



