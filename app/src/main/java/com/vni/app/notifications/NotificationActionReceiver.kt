package com.vni.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vni.app.SimpleHomeViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Handles actionable clicks directly from the Android notification shade:
 * - [ ✓ Complete ] on Habit Task notifications: marks task 100% complete and feeds completion into HabitPredictor ML model.
 * - [ ✓ Complete ] on Notice notifications: marks notice complete, moves it to History, and stops further 10-minute periodic reminders.
 * - [ ✓ Complete ] on UniTask notifications: marks uni-task complete.
 * - [ I will do ], [ 10 min after ], [ Already started ] on Task Start Alert notifications ("hey boss! , you need to start this task now! ")
 * - [ Yes am doing ], [ I will do ] on 5-minute Check-in notifications ("does you started the task name")
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.vni.app.action.COMPLETE_TASK"
        const val ACTION_COMPLETE_NOTICE = "com.vni.app.action.COMPLETE_NOTICE"
        const val ACTION_COMPLETE_UNI_TASK = "com.vni.app.action.COMPLETE_UNI_TASK"
        const val ACTION_NOTICE_OKAY = "com.vni.app.action.NOTICE_OKAY"

        // Interactive Task Start Notification Actions (Requirement 6)
        const val ACTION_TASK_I_WILL_DO = "com.vni.app.action.TASK_I_WILL_DO"
        const val ACTION_TASK_10_MIN_AFTER = "com.vni.app.action.TASK_10_MIN_AFTER"
        const val ACTION_TASK_ALREADY_STARTED = "com.vni.app.action.TASK_ALREADY_STARTED"
        const val ACTION_TASK_CHECKIN_YES = "com.vni.app.action.TASK_CHECKIN_YES"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_NOTICE_ID = "extra_notice_id"
        const val EXTRA_NOTICE_TITLE = "extra_notice_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_UNI_TASK_ID = "extra_uni_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private const val PREFS_NAME = "vni_app_prefs_v2"

        fun getTodayKey(): String {
            val cal = Calendar.getInstance()
            return String.format(
                Locale.ENGLISH,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
                if (taskId != -1L) {
                    completeTask(context, taskId, taskName, notifId)
                }
            }

            ACTION_COMPLETE_NOTICE -> {
                val noticeId = intent.getLongExtra(EXTRA_NOTICE_ID, -1L)
                val noticeTitle = intent.getStringExtra(EXTRA_NOTICE_TITLE) ?: "Notice"
                if (noticeId != -1L) {
                    completeNotice(context, noticeId, noticeTitle, notifId)
                }
            }

            ACTION_COMPLETE_UNI_TASK -> {
                val uniTaskId = intent.getLongExtra(EXTRA_UNI_TASK_ID, -1L)
                if (uniTaskId != -1L) {
                    completeUniTask(context, uniTaskId, notifId)
                }
            }

            ACTION_NOTICE_OKAY -> {
                val noticeId = intent.getLongExtra(EXTRA_NOTICE_ID, -1L)
                val noticeTitle = intent.getStringExtra(EXTRA_NOTICE_TITLE) ?: "Notice"
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
                handleNoticeOkay(context, noticeId, noticeTitle, subtitle, notifId)
            }

            // Requirement 6: Option c - Already started
            ACTION_TASK_ALREADY_STARTED, ACTION_TASK_CHECKIN_YES -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
                handleTaskAlreadyStarted(context, taskId, taskName, notifId)
            }

            // Requirement 6: Option b - 10 min after (reminds again in 10 minutes)
            ACTION_TASK_10_MIN_AFTER -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
                handleTask10MinAfter(context, taskId, taskName, notifId)
            }

            // Requirement 6: Option a - I will do (sends check-in after 5 minutes)
            ACTION_TASK_I_WILL_DO -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
                handleTaskIWillDo(context, taskId, taskName, notifId)
            }
        }
    }

    private fun handleNoticeOkay(context: Context, noticeId: Long, noticeTitle: String, subtitle: String, notifId: Int) {
        NotificationHelper.sendNotificationUpdate(
            context = context,
            notificationId = notifId,
            title = "Notice Acknowledged ⏰",
            message = "Will remind you again in 5 minutes until marked Done!"
        )
        // Keep 5-minute repeating follow-up active
        NotificationHelper.scheduleNoticeFollowUp5Min(
            context = context,
            noticeId = noticeId,
            noticeTitle = noticeTitle,
            subtitle = subtitle
        )
    }

    private fun handleTaskAlreadyStarted(context: Context, taskId: Long, taskName: String, notifId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayKey()
        // Record that task has started today so app won't send start notifications about this task next time for that day!
        prefs.edit().putBoolean("task_started_${today}_$taskId", true).apply()

        NotificationHelper.sendNotificationUpdate(
            context = context,
            notificationId = notifId,
            title = "Awesome! 🚀",
            message = "\"$taskName\" marked in progress. No more start alerts today!"
        )
    }

    private fun handleTask10MinAfter(context: Context, taskId: Long, taskName: String, notifId: Int) {
        NotificationHelper.sendNotificationUpdate(
            context = context,
            notificationId = notifId,
            title = "Reminder Snoozed ⏰",
            message = "Will remind you in 10 minutes to start \"$taskName\"."
        )

        // Schedule start reminder in 10 minutes
        NotificationHelper.scheduleTaskStartReminder(
            context = context,
            taskId = taskId,
            taskName = taskName,
            triggerAtMillis = System.currentTimeMillis() + 10 * 60 * 1000L
        )
    }

    private fun handleTaskIWillDo(context: Context, taskId: Long, taskName: String, notifId: Int) {
        NotificationHelper.sendNotificationUpdate(
            context = context,
            notificationId = notifId,
            title = "Got it! 💪",
            message = "Checking in with you in 5 minutes about \"$taskName\"."
        )

        // Schedule check-in notification in 5 minutes: "does you started the task name"
        NotificationHelper.scheduleTaskCheckIn5Min(
            context = context,
            taskId = taskId,
            taskName = taskName,
            triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L
        )
    }

    private fun completeTask(context: Context, taskId: Long, taskName: String, notifId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksJson = prefs.getString("tasks_json", null) ?: return

        try {
            val arr = JSONArray(tasksJson)
            var targetMins = 60
            val cal = Calendar.getInstance()
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getLong("id") == taskId) {
                    targetMins = obj.optInt("targetMinutes", 60)
                    obj.put("todayMinutes", targetMins)
                    obj.put("completedToday", true)
                    obj.put("lastCompletedDateText", "today")

                    val weekArr = obj.optJSONArray("weekLoggedMinutes") ?: JSONArray()
                    while (weekArr.length() < 7) weekArr.put(0)
                    weekArr.put(dayOfWeek, targetMins)
                    obj.put("weekLoggedMinutes", weekArr)
                    break
                }
            }

            prefs.edit().putString("tasks_json", arr.toString()).apply()

            // Also mark started today so no further start reminders
            prefs.edit().putBoolean("task_started_${getTodayKey()}_$taskId", true).apply()

            // Feed completion into the ML Habit Predictor model for continuous learning
            HabitPredictor.logCompletion(context, taskId, taskName)

            // Update notification in shade to show completion confirmation
            NotificationHelper.sendNotificationUpdate(
                context = context,
                notificationId = notifId,
                title = "✓ Task Completed! 🎉",
                message = "\"$taskName\" marked as 100% completed. Habit streak updated!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeNotice(context: Context, noticeId: Long, noticeTitle: String, notifId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticesJson = prefs.getString("notices_json", null) ?: return

        try {
            val arr = JSONArray(noticesJson)
            val remaining = JSONArray()
            var removedTitle = noticeTitle

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getLong("id") == noticeId) {
                    removedTitle = obj.optString("title", noticeTitle)
                } else {
                    remaining.put(obj)
                }
            }

            // Move completed notice to History
            val deletedJson = prefs.getString("deleted_notices_json", null)
            val deletedArr = if (deletedJson != null) JSONArray(deletedJson) else JSONArray()
            val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date())

            val histObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", removedTitle)
                put("date", dateStr)
                put("type", "Notes")
                put("amount", "")
                put("amountInt", 0)
                put("dateMillis", System.currentTimeMillis())
                put("isDeleted", false)
            }

            val newDeleted = JSONArray()
            newDeleted.put(histObj)
            for (i in 0 until deletedArr.length()) {
                newDeleted.put(deletedArr.getJSONObject(i))
            }

            prefs.edit()
                .putString("notices_json", remaining.toString())
                .putString("deleted_notices_json", newDeleted.toString())
                .apply()

            NotificationHelper.cancelNoticeReminders(context, noticeId)

            NotificationHelper.sendNotificationUpdate(
                context = context,
                notificationId = notifId,
                title = "✓ Notice Completed! 📌",
                message = "\"$removedTitle\" completed and moved to History."
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeUniTask(context: Context, uniTaskId: Long, notifId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uniTasksJson = prefs.getString("unitasks_json", null) ?: return

        try {
            val arr = JSONArray(uniTasksJson)
            val cal = Calendar.getInstance()
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            var uniTaskName = "Uni-Task"

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getLong("id") == uniTaskId) {
                    uniTaskName = obj.optString("name", "Uni-Task")
                    obj.put("completedToday", true)
                    val weekArr = obj.optJSONArray("weekCompleted") ?: JSONArray()
                    while (weekArr.length() < 7) weekArr.put(false)
                    weekArr.put(dayOfWeek, true)
                    obj.put("weekCompleted", weekArr)
                    break
                }
            }

            prefs.edit().putString("unitasks_json", arr.toString()).apply()

            NotificationHelper.sendNotificationUpdate(
                context = context,
                notificationId = notifId,
                title = "✓ Uni-Task Completed! ✨",
                message = "\"$uniTaskName\" marked as completed."
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
