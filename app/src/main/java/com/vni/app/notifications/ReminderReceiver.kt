package com.vni.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HABIT_TASK_REMINDER = "com.vni.app.action.HABIT_TASK_REMINDER"
        const val ACTION_NOTICE_TIME_RANGE = "com.vni.app.action.NOTICE_TIME_RANGE"
        const val ACTION_SEND_MOTIVE = "com.vni.app.action.SEND_MOTIVE"

        // Requirement 6: Task Start Reminder and 5-min Check-in
        const val ACTION_TASK_START_REMINDER = "com.vni.app.action.TASK_START_REMINDER"
        const val ACTION_TASK_CHECKIN_5MIN = "com.vni.app.action.TASK_CHECKIN_5MIN"

        // Requirement 5: Notice Duration Reminder and 5-min repeating follow-up
        const val ACTION_NOTICE_DURATION_REMINDER = "com.vni.app.action.NOTICE_DURATION_REMINDER"

        // Requirement 8: Recurring Daily UniTask Reminder
        const val ACTION_DAILY_UNITASK_REMINDER = "com.vni.app.action.DAILY_UNITASK_REMINDER"

        // Requirement 5 (Sleep): Wake Up and Sleep Reminders
        const val ACTION_WAKE_UP_REMINDER = "com.vni.app.action.WAKE_UP_REMINDER"
        const val ACTION_SLEEP_REMINDER = "com.vni.app.action.SLEEP_REMINDER"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTICE_ID = "extra_notice_id"
        const val EXTRA_NOTICE_TITLE = "extra_notice_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_TIME_RANGE = "extra_time_range"
        const val EXTRA_END_MILLIS = "extra_end_millis"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
        const val EXTRA_INTERVAL_MINS = "extra_interval_mins"

        private const val PREFS_NAME = "vni_app_prefs_v2"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled && intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        when (intent.action) {
            ACTION_HABIT_TASK_REMINDER -> handleHabitTaskReminder(context, intent)
            ACTION_DAILY_UNITASK_REMINDER -> handleDailyUniTaskReminder(context, intent)
            ACTION_NOTICE_TIME_RANGE -> handleNoticeTimeRangeReminder(context, intent)
            ACTION_NOTICE_DURATION_REMINDER -> handleNoticeDurationReminder(context, intent)
            ACTION_SEND_MOTIVE -> handleMotiveNotification(context)
            ACTION_TASK_START_REMINDER -> handleTaskStartReminder(context, intent)
            ACTION_TASK_CHECKIN_5MIN -> handleTaskCheckIn5Min(context, intent)
            ACTION_WAKE_UP_REMINDER -> handleWakeUpReminder(context)
            ACTION_SLEEP_REMINDER -> handleSleepReminder(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
            else -> handleDefaultReminder(context, intent)
        }
    }

    /**
     * Requirement 6: Fires when the task's time range begins.
     * If the task is still not started, sends: "hey boss! , you need to start this task now!"
     */
    private fun handleTaskStartReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (20000 + (taskId % 10000)).toInt())

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = NotificationActionReceiver.getTodayKey()

        // Check if user already started this task today or completed it
        val alreadyStarted = prefs.getBoolean("task_started_${today}_$taskId", false)
        if (alreadyStarted) return

        val tasksJson = prefs.getString("tasks_json", null)
        var taskFound = false
        var isAlreadyCompletedOrInProgress = false
        if (tasksJson != null) {
            try {
                val arr = JSONArray(tasksJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == taskId) {
                        val daysLeft = obj.optInt("daysLeft", 1)
                        if (daysLeft <= 0) break // Task expired or deleted!
                        taskFound = true
                        val completedToday = obj.optBoolean("completedToday", false)
                        val todayMins = obj.optInt("todayMinutes", 0)
                        if (completedToday || todayMins > 0) {
                            isAlreadyCompletedOrInProgress = true
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!taskFound || isAlreadyCompletedOrInProgress) return

        NotificationHelper.sendTaskStartNotification(
            context = context,
            taskId = taskId,
            taskName = taskName,
            notificationId = reminderId
        )
    }

    /**
     * Requirement 6: Fires 5 minutes after user clicked "I will do".
     * Asks: "does you started the task name"
     */
    private fun handleTaskCheckIn5Min(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (25000 + (taskId % 10000)).toInt())

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = NotificationActionReceiver.getTodayKey()

        val alreadyStarted = prefs.getBoolean("task_started_${today}_$taskId", false)
        if (alreadyStarted) return

        val tasksJson = prefs.getString("tasks_json", null)
        var isAlreadyDone = false
        if (tasksJson != null) {
            try {
                val arr = JSONArray(tasksJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == taskId) {
                        val completedToday = obj.optBoolean("completedToday", false)
                        val todayMins = obj.optInt("todayMinutes", 0)
                        if (completedToday || todayMins > 0) isAlreadyDone = true
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!isAlreadyDone) {
            NotificationHelper.sendTaskCheckInNotification(
                context = context,
                taskId = taskId,
                taskName = taskName,
                notificationId = reminderId
            )
        }
    }

    private fun handleHabitTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Time to complete your habit!"
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (10000 + (taskId % 10000)).toInt())

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tasksJson = prefs.getString("tasks_json", null)
        var taskFound = false
        var isAlreadyCompleted = false

        if (tasksJson != null) {
            try {
                val arr = JSONArray(tasksJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == taskId) {
                        val daysLeft = obj.optInt("daysLeft", 1)
                        if (daysLeft <= 0) break // Task expired or deleted!
                        taskFound = true
                        isAlreadyCompleted = obj.optBoolean("completedToday", false)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!taskFound || isAlreadyCompleted) return

        NotificationHelper.sendActionableTaskNotification(
            context = context,
            taskId = taskId,
            taskName = taskName,
            message = message,
            notificationId = reminderId
        )
    }

    private fun handleNoticeTimeRangeReminder(context: Context, intent: Intent) {
        val noticeId = intent.getLongExtra(EXTRA_NOTICE_ID, -1L)
        val noticeTitle = intent.getStringExtra(EXTRA_NOTICE_TITLE) ?: "Notice"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
        val timeRange = intent.getStringExtra(EXTRA_TIME_RANGE) ?: ""
        val endMillis = intent.getLongExtra(EXTRA_END_MILLIS, 0L)
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (30000 + (noticeId % 10000)).toInt())
        val triggerMillis = intent.getLongExtra(EXTRA_TRIGGER_MILLIS, -1L).takeIf { it > 0 }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticesJson = prefs.getString("notices_json", null)
        var noticeStillExists = false

        if (noticesJson != null) {
            try {
                val arr = JSONArray(noticesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == noticeId) {
                        noticeStillExists = true
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!noticeStillExists) return

        val now = System.currentTimeMillis()
        if (now <= endMillis) {
            NotificationHelper.sendActionableNoticeNotification(
                context = context,
                noticeId = noticeId,
                noticeTitle = noticeTitle,
                subtitle = subtitle,
                timeRange = timeRange,
                notificationId = reminderId
            )

            val nextCheck = now + 10 * 60 * 1000L
            if (nextCheck <= endMillis) {
                NotificationHelper.scheduleNextNotice10MinCheck(
                    context = context,
                    noticeId = noticeId,
                    noticeTitle = noticeTitle,
                    subtitle = subtitle,
                    timeRange = timeRange,
                    endMillis = endMillis,
                    triggerAtMillis = nextCheck
                )
            }
        }
    }

    /**
     * Requirement 5: Notice Duration Alarm handler.
     * Fires when the duration countdown expires.
     * Sends: "you have a remainder about the notice !" with [ Okay ] and [ Done ] buttons.
     * Automatically schedules a follow-up in 5 minutes unless [ Done ] is clicked!
     */
    private fun handleNoticeDurationReminder(context: Context, intent: Intent) {
        val noticeId = intent.getLongExtra(EXTRA_NOTICE_ID, -1L)
        val noticeTitle = intent.getStringExtra(EXTRA_NOTICE_TITLE) ?: "Notice"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "you have a remainder about the notice !"
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (30000 + (noticeId % 10000)).toInt())
        val triggerMillis = intent.getLongExtra(EXTRA_TRIGGER_MILLIS, -1L).takeIf { it > 0 }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticesJson = prefs.getString("notices_json", null)
        var noticeStillExists = false

        if (noticesJson != null) {
            try {
                val arr = JSONArray(noticesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == noticeId) {
                        noticeStillExists = true
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!noticeStillExists) return

        val intervalMins = intent.getIntExtra(EXTRA_INTERVAL_MINS, -1)
        if (intervalMins >= 0) {
            val intPrefs = context.getSharedPreferences("vni_reminder_intervals", Context.MODE_PRIVATE)
            if (intPrefs.getBoolean("fired_notif_${noticeId}_${intervalMins}", false)) {
                return
            }
            intPrefs.edit().putBoolean("fired_notif_${noticeId}_${intervalMins}", true).apply()
        }

        // Send alert with [ Okay ] and [ Done ] buttons & live countdown chronometer
        NotificationHelper.sendNoticeAlertWithOkayDone(
            context = context,
            noticeId = noticeId,
            noticeTitle = noticeTitle,
            subtitle = subtitle,
            notificationId = reminderId,
            message = message,
            triggerAtMillis = triggerMillis
        )

        // Only schedule 5-minute repeating follow-up if this is the final deadline alarm (not intermediate warnings)
        if (message.contains("Time is up")) {
            NotificationHelper.scheduleNoticeFollowUp5Min(
                context = context,
                noticeId = noticeId,
                noticeTitle = noticeTitle,
                subtitle = subtitle,
                reminderId = reminderId
            )
        }
    }

    private fun handleMotiveNotification(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val quotes = mutableListOf<String>()

        // Check headings json first
        val headingsJson = prefs.getString("motive_headings_json", null)
        if (headingsJson != null) {
            try {
                val arr = JSONArray(headingsJson)
                for (i in 0 until arr.length()) {
                    val sArr = arr.getJSONObject(i).optJSONArray("sentences")
                    if (sArr != null) {
                        for (j in 0 until sArr.length()) {
                            val str = sArr.getString(j)
                            if (str.isNotEmpty()) quotes.add(str)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to legacy motives
        if (quotes.isEmpty()) {
            val motivesJson = prefs.getString("motives_json", null)
            if (motivesJson != null) {
                try {
                    val arr = JSONArray(motivesJson)
                    for (i in 0 until arr.length()) {
                        val text = arr.getJSONObject(i).optString("text")
                        if (text.isNotEmpty()) quotes.add(text)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (quotes.isEmpty()) {
            quotes.addAll(
                listOf(
                    "Consistency, is the only key for success",
                    "Consistency is what transforms average into excellence",
                    "Success is not a destination, it is daily discipline",
                    "Focus, on the step in front of you, not the whole staircase",
                    "Momentum, is built one small win at a time"
                )
            )
        }

        val selectedQuote = quotes.random()
        NotificationHelper.sendMotiveNotification(context, selectedQuote)
    }

    private fun handleBootCompleted(context: Context) {
        NotificationHelper.schedulePeriodicMotiveNotifications(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticesJson = prefs.getString("notices_json", null)
        if (noticesJson != null) {
            try {
                val arr = JSONArray(noticesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val timeRange = obj.optString("timeRange").ifEmpty { null }
                    if (timeRange != null) {
                        NotificationHelper.scheduleNoticeTimeRangeCheck(
                            context = context,
                            noticeId = obj.getLong("id"),
                            noticeTitle = obj.getString("title"),
                            subtitle = obj.optString("subtitle", ""),
                            timeRange = timeRange,
                            reminderDay = obj.optString("deadlineText", "Today").removePrefix("Deadline: ")
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleWakeUpReminder(context: Context) {
        val prefs = context.getSharedPreferences("vni_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("day_planner_enabled", true)) return

        NotificationHelper.sendNotification(
            context = context,
            title = "🌅 Good Morning! Wake Up Time",
            message = "Time to rise and shine! Check your day planner and start your tasks.",
            notificationId = 50001,
            channelId = NotificationHelper.CHANNEL_REMINDERS_ID
        )
    }

    private fun handleSleepReminder(context: Context) {
        val prefs = context.getSharedPreferences("vni_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("day_planner_enabled", true)) return

        NotificationHelper.sendNotification(
            context = context,
            title = "🌙 Sleep Reminder",
            message = "Time to wind down and go to sleep. Rest well for a productive tomorrow!",
            notificationId = 50002,
            channelId = NotificationHelper.CHANNEL_REMINDERS_ID
        )
    }

    private fun handleDefaultReminder(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: "Time to focus on your consistency task!"
        val id = intent.getIntExtra("id", (System.currentTimeMillis() % 100000).toInt())

        NotificationHelper.sendNotification(
            context = context,
            title = title,
            message = message,
            notificationId = id,
            channelId = NotificationHelper.CHANNEL_REMINDERS_ID
        )
    }

    private fun handleDailyUniTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Daily Task"
        val hour = intent.getIntExtra("daily_hour", 9)
        val minute = intent.getIntExtra("daily_minute", 0)
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, (50000 + (taskId % 10000)).toInt())

        // Check if unitask still exists
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unitasksJson = prefs.getString("unitasks_json", null)
        var stillExists = false
        if (unitasksJson != null) {
            try {
                val arr = JSONArray(unitasksJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getLong("id") == taskId) {
                        stillExists = true
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!stillExists) return

        val notifyBeforeMinutes = intent.getIntExtra("notify_before_minutes", 0)

        NotificationHelper.sendNoticeAlertWithOkayDone(
            context = context,
            noticeId = reminderId.toLong(),
            noticeTitle = "Daily Task: $taskName",
            subtitle = if (notifyBeforeMinutes > 0) "Upcoming daily task in $notifyBeforeMinutes minutes!" else "Time to do your scheduled daily task!",
            notificationId = reminderId
        )

        // Reschedule for tomorrow at the same hour and minute
        NotificationHelper.scheduleDailyUniTaskReminder(
            context = context,
            taskId = taskId,
            taskName = taskName,
            hour = hour,
            minute = minute,
            notifyBeforeMinutes = notifyBeforeMinutes
        )
    }
}
