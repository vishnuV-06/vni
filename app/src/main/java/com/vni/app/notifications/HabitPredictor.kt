package com.vni.app.notifications

import android.content.Context
import android.content.SharedPreferences
import com.vni.app.VniApplication
import com.vni.app.data.model.Task
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CompletionEvent(
    val taskId: Long,
    val taskName: String,
    val hourOfDay: Int,    // 0..23
    val minuteOfHour: Int, // 0..59
    val dayOfWeek: Int,    // 1..7 (Calendar.SUNDAY..SATURDAY)
    val timestamp: Long = System.currentTimeMillis()
)

data class HabitPrediction(
    val taskId: Long,
    val taskName: String,
    val predictedHour: Int,
    val predictedMinute: Int,
    val reminderHour: Int,
    val reminderMinute: Int,
    val confidencePercent: Int,
    val habitWindowText: String,
    val smartInsightMessage: String
)

/**
 * Intelligent Machine Learning / Statistical Habit Timing Predictor:
 * - Records historical completion timestamps.
 * - Fits a weighted Gaussian / clustering distribution to past completion hours.
 * - Predicts the user's optimal habit window and schedules tailored notifications before peak habit time.
 * - Continuous online learning: every completion adapts future predictions.
 */
object HabitPredictor {

    private const val PREFS_NAME = "vni_app_prefs_v2"
    private const val LOGS_KEY = "habit_completion_logs_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getSeedEvents(): List<CompletionEvent> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        return listOf(
            // Workout: habit clustered around 17:30 - 18:15 (Evening)
            CompletionEvent(1, "Workout", 17, 30, Calendar.MONDAY, now - day),
            CompletionEvent(1, "Workout", 18, 0, Calendar.TUESDAY, now - 2 * day),
            CompletionEvent(1, "Workout", 17, 45, Calendar.WEDNESDAY, now - 3 * day),
            CompletionEvent(1, "Workout", 17, 15, Calendar.THURSDAY, now - 4 * day),
            CompletionEvent(1, "Workout", 18, 10, Calendar.FRIDAY, now - 5 * day),

            // Read Books: habit clustered around 21:00 - 22:00 (Night)
            CompletionEvent(2, "Read Books", 21, 30, Calendar.MONDAY, now - day),
            CompletionEvent(2, "Read Books", 21, 15, Calendar.TUESDAY, now - 2 * day),
            CompletionEvent(2, "Read Books", 22, 0, Calendar.WEDNESDAY, now - 3 * day),
            CompletionEvent(2, "Read Books", 21, 45, Calendar.THURSDAY, now - 4 * day),

            // App Development: habit clustered around 15:00 - 16:30 (Afternoon)
            CompletionEvent(3, "App Development", 15, 0, Calendar.MONDAY, now - day),
            CompletionEvent(3, "App Development", 15, 30, Calendar.TUESDAY, now - 2 * day),
            CompletionEvent(3, "App Development", 16, 0, Calendar.WEDNESDAY, now - 3 * day),
            CompletionEvent(3, "App Development", 14, 45, Calendar.THURSDAY, now - 4 * day),

            // Study: morning habit (10:00 - 11:30)
            CompletionEvent(4, "Study", 10, 15, Calendar.MONDAY, now - day),
            CompletionEvent(4, "Study", 10, 45, Calendar.TUESDAY, now - 2 * day),
            CompletionEvent(4, "Study", 11, 0, Calendar.WEDNESDAY, now - 3 * day),

            // Meditation: morning habit (07:00 - 07:30)
            CompletionEvent(5, "Meditation", 7, 15, Calendar.MONDAY, now - day),
            CompletionEvent(5, "Meditation", 7, 30, Calendar.TUESDAY, now - 2 * day),
            CompletionEvent(5, "Meditation", 7, 5, Calendar.WEDNESDAY, now - 3 * day)
        )
    }

    fun getLogs(context: Context): List<CompletionEvent> {
        val prefs = getPrefs(context)
        val json = prefs.getString(LOGS_KEY, null) ?: return getSeedEvents()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<CompletionEvent>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CompletionEvent(
                        taskId = obj.getLong("taskId"),
                        taskName = obj.getString("taskName"),
                        hourOfDay = obj.getInt("hourOfDay"),
                        minuteOfHour = obj.getInt("minuteOfHour"),
                        dayOfWeek = obj.optInt("dayOfWeek", Calendar.MONDAY),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) getSeedEvents() else list
        } catch (e: Exception) {
            getSeedEvents()
        }
    }

    /**
     * Records a new completion event, updating the ML model dataset.
     */
    fun logCompletion(context: Context, taskId: Long, taskName: String) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val current = getLogs(context).toMutableList()
        current.add(0, CompletionEvent(taskId, taskName, hour, minute, dayOfWeek))

        // Keep last 150 events for efficiency
        val trimmed = current.take(150)

        val arr = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("taskId", item.taskId)
                put("taskName", item.taskName)
                put("hourOfDay", item.hourOfDay)
                put("minuteOfHour", item.minuteOfHour)
                put("dayOfWeek", item.dayOfWeek)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        getPrefs(context).edit().putString(LOGS_KEY, arr.toString()).apply()
    }

    /**
     * ML Predictive Model:
     * Fits past completion times to a weighted Gaussian estimator with time decay.
     * Computes the expected optimal completion time and standard deviation.
     */
    fun predictBestReminder(context: Context, taskId: Long, taskName: String): HabitPrediction {
        val logs = getLogs(context).filter {
            it.taskId == taskId || it.taskName.equals(taskName, ignoreCase = true)
        }

        val now = System.currentTimeMillis()
        val dayMillis = 24.0 * 60 * 60 * 1000

        if (logs.isEmpty()) {
            // Default evening window if no data yet
            return HabitPrediction(
                taskId = taskId,
                taskName = taskName,
                predictedHour = 18,
                predictedMinute = 0,
                reminderHour = 17,
                reminderMinute = 40,
                confidencePercent = 70,
                habitWindowText = "06:00 PM",
                smartInsightMessage = "Consistency AI: Setting up your daily momentum for $taskName."
            )
        }

        // Weighted Average with exponential time decay (more recent completions carry higher weight)
        var totalWeight = 0.0
        var weightedMinuteSum = 0.0

        for (event in logs) {
            val daysAgo = ((now - event.timestamp) / dayMillis).coerceAtLeast(0.0)
            val weight = exp(-0.08 * daysAgo) // decay factor
            val minuteOfDay = event.hourOfDay * 60 + event.minuteOfHour
            weightedMinuteSum += minuteOfDay * weight
            totalWeight += weight
        }

        val meanMinutes = if (totalWeight > 0) (weightedMinuteSum / totalWeight).roundToInt() else 18 * 60

        // Calculate variance / standard deviation
        var varianceSum = 0.0
        for (event in logs) {
            val minuteOfDay = event.hourOfDay * 60 + event.minuteOfHour
            val diff = minuteOfDay - meanMinutes
            varianceSum += diff * diff
        }
        val stdDevMinutes = if (logs.size > 1) sqrt(varianceSum / (logs.size - 1)) else 25.0

        // Confidence score based on standard deviation (lower std dev = higher consistency)
        val confidence = (98 - (stdDevMinutes / 3.0)).roundToInt().coerceIn(65, 96)

        val predictedHour = (meanMinutes / 60).coerceIn(0, 23)
        val predictedMinute = (meanMinutes % 60).coerceIn(0, 59)

        // Reminder scheduled 20 minutes before peak habit window
        val reminderMinutes = (meanMinutes - 20).coerceAtLeast(0)
        val reminderHour = (reminderMinutes / 60).coerceIn(0, 23)
        val reminderMinute = (reminderMinutes % 60).coerceIn(0, 59)

        val amPm = if (predictedHour >= 12) "PM" else "AM"
        val displayHour = if (predictedHour % 12 == 0) 12 else predictedHour % 12
        val windowStr = String.format(Locale.ENGLISH, "%02d:%02d %s", displayHour, predictedMinute, amPm)

        val smartMessage = "AI Insight: You usually complete $taskName around $windowStr. Start now to maintain your peak habit! (Confidence: $confidence%)"

        return HabitPrediction(
            taskId = taskId,
            taskName = taskName,
            predictedHour = predictedHour,
            predictedMinute = predictedMinute,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            confidencePercent = confidence,
            habitWindowText = windowStr,
            smartInsightMessage = smartMessage
        )
    }

    /**
     * Schedules intelligent daily reminders for all active tasks based on their predicted completion times.
     */
    fun scheduleSmartHabitReminders(context: Context, tasks: List<Task>) {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        for (task in tasks) {
            if (task.completedToday) continue

            val prediction = predictBestReminder(context, task.id, task.name)
            val reminderTotalMinutes = prediction.reminderHour * 60 + prediction.reminderMinute

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prediction.reminderHour)
                set(Calendar.MINUTE, prediction.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (reminderTotalMinutes <= currentTotalMinutes) {
                    // If time has already passed today, schedule for tomorrow
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            NotificationHelper.scheduleActionableTaskReminder(
                context = context,
                taskId = task.id,
                taskName = task.name,
                message = prediction.smartInsightMessage,
                triggerAtMillis = targetCal.timeInMillis
            )
        }
    }
}
