package com.vni.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vni.app.MainActivity
import com.vni.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_REMINDERS_ID = "vni_reminders_channel"
    const val CHANNEL_REMINDERS_NAME = "Tasks & Habit Reminders"

    const val CHANNEL_NOTICES_ID = "vni_notices_channel"
    const val CHANNEL_NOTICES_NAME = "Notices & Deadlines"

    const val CHANNEL_MOTIVES_ID = "vni_motives_channel"
    const val CHANNEL_MOTIVES_NAME = "Daily Motivation & Mindset"

    /**
     * Creates notification channels required for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Task & Habit Reminders (High importance for ML timing predictions)
            val taskChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI habit timing analysis and task completion reminders"
                enableVibration(true)
                enableLights(true)
            }

            // Channel 2: Notices & Deadlines (High importance for urgent time-range alerts)
            val noticeChannel = NotificationChannel(
                CHANNEL_NOTICES_ID,
                CHANNEL_NOTICES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Deadline and periodic time-range notice alerts"
                enableVibration(true)
                enableLights(true)
            }

            // Channel 3: Daily Motivation & Mindset Sentences
            val motiveChannel = NotificationChannel(
                CHANNEL_MOTIVES_ID,
                CHANNEL_MOTIVES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic motivational sentences and mindset reminders"
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(noticeChannel)
            notificationManager.createNotificationChannel(motiveChannel)
        }
    }

    /**
     * Sends an immediate standard notification to the user.
     */
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        channelId: String = CHANNEL_REMINDERS_ID
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted yet
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 6: Sends Task Start Reminder with 3 actions:
     * - [ I will do ] -> sends follow-up check-in in 5 mins
     * - [ 10 min after ] -> reminds again in 10 mins
     * - [ Already started ] -> stops sending start reminders for this task today
     */
    fun sendTaskStartNotification(
        context: Context,
        taskId: Long,
        taskName: String,
        notificationId: Int = (20000 + (taskId % 10000)).toInt()
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action a: [ I will do ]
        val iWillDoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_I_WILL_DO
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val iWillDoPending = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            iWillDoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action b: [ 10 min after ]
        val tenMinIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_10_MIN_AFTER
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val tenMinPending = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            tenMinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action c: [ Already started ]
        val startedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_ALREADY_STARTED
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val startedPending = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            startedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "hey boss! , you need to start this task now!"
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎯 Task Alert: $taskName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.mipmap.ic_launcher, "I will do", iWillDoPending)
            .addAction(R.mipmap.ic_launcher, "10 min after", tenMinPending)
            .addAction(R.mipmap.ic_launcher, "Already started", startedPending)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 6: Sends 5-minute Check-in Notification:
     * - Content: "does you started the task name"
     * - Action 1: [ Yes am doing ] -> stops further notifications for this task today
     * - Action 2: [ I will do ] -> snoozes / checks in again
     */
    fun sendTaskCheckInNotification(
        context: Context,
        taskId: Long,
        taskName: String,
        notificationId: Int = (20000 + (taskId % 10000)).toInt()
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val yesIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_CHECKIN_YES
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val yesPending = PendingIntent.getBroadcast(
            context,
            notificationId + 4,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iWillDoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_I_WILL_DO
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val iWillDoPending = PendingIntent.getBroadcast(
            context,
            notificationId + 5,
            iWillDoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "does you started $taskName"
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏳ Task Check-in: $taskName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Yes am doing", yesPending)
            .addAction(R.mipmap.ic_launcher, "I will do", iWillDoPending)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Schedules Task Start Reminder alarm.
     */
    fun scheduleTaskStartReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (20000 + (taskId % 10000)).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TASK_START_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Schedules 5-minute follow-up check-in alarm.
     */
    fun scheduleTaskCheckIn5Min(
        context: Context,
        taskId: Long,
        taskName: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (25000 + (taskId % 10000)).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TASK_CHECKIN_5MIN
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Schedules task start reminders for all tasks that have assigned time ranges.
     */
    fun scheduleTasksWithTimeRanges(context: Context, tasks: List<com.vni.app.data.model.Task>) {
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        for (task in tasks) {
            if (task.completedToday || task.todayMinutes > 0) continue

            val range = task.timeRange ?: continue
            val parsed = parseTimeRange(range) ?: continue
            val startHour = parsed.first.first
            val startMin = parsed.first.second
            val startTotalMin = startHour * 60 + startMin

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (startTotalMin <= currentMinutes) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            scheduleTaskStartReminder(
                context = context,
                taskId = task.id,
                taskName = task.name,
                triggerAtMillis = targetCal.timeInMillis
            )
        }
    }

    /**
     * Sends an actionable Habit Task notification with a [ ✓ Complete ] button directly in the shade.
     */
    fun sendActionableTaskNotification(
        context: Context,
        taskId: Long,
        taskName: String,
        message: String,
        notificationId: Int = (10000 + (taskId % 10000)).toInt()
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "✓ Complete",
            completePendingIntent
        ).build()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎯 Habit AI: $taskName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(completeAction)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission check
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends an actionable Notice notification with a [ ✓ Complete ] button directly in the shade.
     */
    fun sendActionableNoticeNotification(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        timeRange: String? = null,
        notificationId: Int = (30000 + (noticeId % 10000)).toInt()
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_NOTICE
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "✓ Complete",
            completePendingIntent
        ).build()

        val timeRangeText = if (!timeRange.isNullOrEmpty()) " • Active Window: $timeRange" else ""
        val fullBody = "$subtitle$timeRangeText\n(Reminding every 10 mins until completed)"

        val builder = NotificationCompat.Builder(context, CHANNEL_NOTICES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📌 Notice Reminder: $noticeTitle")
            .setContentText(fullBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(completeAction)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            // Permission check
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 5: Sends Notice Alert with [ Okay ] and [ Done ] buttons.
     * Content: "you have a remainder about the notice !"
     */
    fun sendNoticeAlertWithOkayDone(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        notificationId: Int = (30000 + (noticeId % 10000)).toInt(),
        message: String = "you have a remainder about the notice !",
        triggerAtMillis: Long? = null
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: [ Okay ] (Acknowledges reminder, but re-notifies in 5 min if not done)
        val okayIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTICE_OKAY
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(NotificationActionReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val okayPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            okayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val okayAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "Okay",
            okayPendingIntent
        ).build()

        // Action 2: [ Done ] (Marks complete, removes from notice, moves to history, cancels 5-min alarm)
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_NOTICE
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            "Done",
            donePendingIntent
        ).build()

        val fullBody = if (subtitle.isNotBlank()) "$message\n$subtitle" else message

        val builder = NotificationCompat.Builder(context, CHANNEL_NOTICES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📌 $noticeTitle")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(okayAction)
            .addAction(doneAction)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Enable native live countdown chronometer inside the notification itself when target deadline is in the future!
        if (triggerAtMillis != null && triggerAtMillis > System.currentTimeMillis()) {
            builder.setWhen(triggerAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Posts a quiet live-countdown notification immediately when a reminder is created so the
     * user sees a real ticking timer in their notification shade right away.
     */
    fun sendLiveCountdownReminderNotification(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        createNotificationChannels(context)

        val liveNotifId = (30000 + (noticeId % 1000) * 1000 + 999).toInt()
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            liveNotifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_NOTICE
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(NotificationActionReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, liveNotifId)
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            liveNotifId,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneAction = NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Done", donePending).build()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏳ $noticeTitle ($subtitle)")
            .setContentText("Live countdown running...")
            .setWhen(triggerAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .addAction(doneAction)

        try {
            NotificationManagerCompat.from(context).notify(liveNotifId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 5 & 4: Schedules notice duration reminder (e.g. 30 mins) along with
     * user-configured warning notifications (e.g. 15, 10, 5, 3, 2, 1 minutes before).
     */
    fun scheduleNoticeDurationReminder(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        durationMinutes: Int,
        triggerAtMillis: Long = System.currentTimeMillis() + durationMinutes * 60 * 1000L
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Read user-configured intervals from preferences for this category (migrate v5 defaults if needed)
        val prefs = context.getSharedPreferences("vni_reminder_intervals", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("defaults_v5_migrated", false)) {
            prefs.edit()
                .putString("interval_compulsary", prefs.getString("interval_compulsary", null)?.takeIf { it != "15, 10, 5, 3, 2, 1" } ?: "15, 10, 5, 3, 2, 1")
                .putString("interval_important", prefs.getString("interval_important", null)?.takeIf { it != "15, 10, 5" } ?: "15, 10, 3, 1")
                .putString("interval_normal", prefs.getString("interval_normal", null)?.takeIf { it != "10, 5" } ?: "10, 5, 1")
                .putString("interval_standard", prefs.getString("interval_standard", null) ?: "5")
                .putBoolean("defaults_v5_migrated", true)
                .apply()
        }
        val key = when (subtitle.trim().lowercase()) {
            "compulsary", "compulsory" -> "interval_compulsary"
            "important" -> "interval_important"
            "standard" -> "interval_standard"
            else -> "interval_normal"
        }
        val defaultStr = when (subtitle.trim().lowercase()) {
            "compulsary", "compulsory" -> "15, 10, 5, 3, 2, 1"
            "important" -> "15, 10, 3, 1"
            "standard" -> "5"
            else -> "10, 5, 1"
        }
        val intervalStr = prefs.getString(key, defaultStr) ?: defaultStr
        val intervals = intervalStr.split(Regex("[,\\s]+"))
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()

        val now = System.currentTimeMillis()
        val effectiveDurationMinutes = ((triggerAtMillis - now).coerceAtLeast(0L) / 60000L).toInt().coerceAtLeast(durationMinutes)
        val scheduledIds = mutableSetOf<Int>()

        // Post live countdown notification right away in the notification shade!
        sendLiveCountdownReminderNotification(
            context = context,
            noticeId = noticeId,
            noticeTitle = noticeTitle,
            subtitle = subtitle,
            triggerAtMillis = triggerAtMillis
        )

        // 2. Schedule intermediate warning alarms (e.g. 15m, 10m, 5m, 3m, 2m, 1m before deadline)
        for (minsBefore in intervals) {
            val warningAlarmTime = triggerAtMillis - (minsBefore * 60 * 1000L)
            if (minsBefore <= effectiveDurationMinutes && warningAlarmTime >= now - 3000L) {
                val fireAt = maxOf(now + 1000L, warningAlarmTime)
                val warningId = (30000 + (noticeId % 1000) * 1000 + minsBefore).toInt()
                scheduledIds.add(warningId)
                val warningIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_NOTICE_DURATION_REMINDER
                    putExtra(ReminderReceiver.EXTRA_NOTICE_ID, noticeId)
                    putExtra(ReminderReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
                    putExtra(ReminderReceiver.EXTRA_SUBTITLE, subtitle)
                    putExtra(ReminderReceiver.EXTRA_MESSAGE, "do the task: $noticeTitle it has $minsBefore minutes")
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, warningId)
                    putExtra(ReminderReceiver.EXTRA_TRIGGER_MILLIS, triggerAtMillis)
                    putExtra(ReminderReceiver.EXTRA_INTERVAL_MINS, minsBefore)
                }
                val warningPending = PendingIntent.getBroadcast(
                    context,
                    warningId,
                    warningIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setHighPriorityAlarm(context, alarmManager, fireAt, warningPending)
            }
        }

        // 3. Schedule the main deadline alarm (at 0m / duration reached)
        val finalReminderId = (30000 + (noticeId % 1000) * 1000).toInt()
        scheduledIds.add(finalReminderId)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTICE_DURATION_REMINDER
            putExtra(ReminderReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(ReminderReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(ReminderReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "do the task: $noticeTitle - Time is up! Complete it now.")
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, finalReminderId)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_MILLIS, triggerAtMillis)
            putExtra(ReminderReceiver.EXTRA_INTERVAL_MINS, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            finalReminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setHighPriorityAlarm(context, alarmManager, triggerAtMillis, pendingIntent)

        // Save scheduled IDs so cancelNoticeReminders can cancel all of them reliably
        prefs.edit().putString("alarm_ids_$noticeId", scheduledIds.joinToString(",")).apply()
    }

    /**
     * Requirement 5: Re-notify every 5 minutes until user taps [ Done ].
     */
    fun scheduleNoticeFollowUp5Min(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        reminderId: Int = (30000 + (noticeId % 10000)).toInt()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTICE_DURATION_REMINDER
            putExtra(ReminderReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(ReminderReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(ReminderReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Sends a Motive notification with an inspiring quote.
     */
    fun sendMotiveNotification(
        context: Context,
        motiveText: String,
        notificationId: Int = 40001
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MOTIVES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✨ Mindset & Momentum")
            .setContentText("\"$motiveText\"")
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"$motiveText\"\n\nKeep pushing toward your goals today!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates notification in place (e.g. after tapping [ Complete ] or an action).
     */
    fun sendNotificationUpdate(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (notificationId <= 0) return
        createNotificationChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(7000)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Schedules a future actionable task reminder.
     */
    fun scheduleActionableTaskReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        message: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (10000 + (taskId % 10000)).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_HABIT_TASK_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Parses a single time string like "6:00 AM" or "09:30 PM" into (hour, minute).
     */
    fun parseTimeString(timeStr: String): Pair<Int, Int>? {
        return try {
            val clean = timeStr.trim()
            val parts = clean.split(" ")
            val timePart = parts[0]
            val amPm = if (parts.size > 1) parts[1].uppercase(Locale.ENGLISH) else "AM"
            val colonParts = timePart.split(":")
            var hour = colonParts[0].toInt()
            val minute = colonParts[1].toInt()
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a time range string like "6:00 AM - 9:00 AM" into Pair(startHourMinute, endHourMinute).
     */
    fun parseTimeRange(timeRange: String): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val parts = timeRange.split("-")
        if (parts.size != 2) return null
        val start = parseTimeString(parts[0]) ?: return null
        val end = parseTimeString(parts[1]) ?: return null
        return Pair(start, end)
    }

    /**
     * Schedules periodic 10-minute reminders for a notice throughout its time range until complete or deleted.
     */
    fun scheduleNoticeTimeRangeCheck(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        timeRange: String,
        reminderDay: String = "Today"
    ) {
        val parsed = parseTimeRange(timeRange) ?: return
        val (start, end) = parsed

        val calStart = Calendar.getInstance().apply {
            when (reminderDay) {
                "Tomorrow" -> add(Calendar.DAY_OF_YEAR, 1)
                "Today" -> {}
                else -> {
                    try {
                        val parsedDate = SimpleDateFormat("d MMM", Locale.ENGLISH).parse(reminderDay)
                        if (parsedDate != null) {
                            val temp = Calendar.getInstance().apply { time = parsedDate }
                            set(Calendar.MONTH, temp.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, temp.get(Calendar.DAY_OF_MONTH))
                        }
                    } catch (e: Exception) {
                        // Keep today as fallback
                    }
                }
            }
            set(Calendar.HOUR_OF_DAY, start.first)
            set(Calendar.MINUTE, start.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance().apply {
            timeInMillis = calStart.timeInMillis
            set(Calendar.HOUR_OF_DAY, end.first)
            set(Calendar.MINUTE, end.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= calStart.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val now = System.currentTimeMillis()
        val startMillis = calStart.timeInMillis
        val endMillis = calEnd.timeInMillis

        if (now > endMillis) return

        val triggerTime = if (now in startMillis..endMillis) {
            sendActionableNoticeNotification(
                context = context,
                noticeId = noticeId,
                noticeTitle = noticeTitle,
                subtitle = subtitle,
                timeRange = timeRange
            )
            now + 10 * 60 * 1000L
        } else {
            startMillis
        }

        if (triggerTime <= endMillis) {
            scheduleNextNotice10MinCheck(
                context = context,
                noticeId = noticeId,
                noticeTitle = noticeTitle,
                subtitle = subtitle,
                timeRange = timeRange,
                endMillis = endMillis,
                triggerAtMillis = triggerTime
            )
        }
    }

    /**
     * Schedules the next 10-minute alarm for an active notice.
     */
    fun scheduleNextNotice10MinCheck(
        context: Context,
        noticeId: Long,
        noticeTitle: String,
        subtitle: String,
        timeRange: String,
        endMillis: Long,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (30000 + (noticeId % 10000)).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTICE_TIME_RANGE
            putExtra(ReminderReceiver.EXTRA_NOTICE_ID, noticeId)
            putExtra(ReminderReceiver.EXTRA_NOTICE_TITLE, noticeTitle)
            putExtra(ReminderReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(ReminderReceiver.EXTRA_TIME_RANGE, timeRange)
            putExtra(ReminderReceiver.EXTRA_END_MILLIS, endMillis)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Cancels any pending time-range alarms for a specific notice.
     */
    fun cancelNoticeReminders(context: Context, noticeId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (30000 + (noticeId % 10000)).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTICE_TIME_RANGE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        val cancelIds = mutableSetOf(reminderId)
        val baseId = (30000 + (noticeId % 1000) * 1000).toInt()
        cancelIds.add(baseId)
        cancelIds.add(baseId + 999) // live countdown notification ID
        for (m in listOf(1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 45, 60)) {
            cancelIds.add(baseId + m)
        }
        val prefs = context.getSharedPreferences("vni_reminder_intervals", Context.MODE_PRIVATE)
        val savedIdsStr = prefs.getString("alarm_ids_$noticeId", "") ?: ""
        savedIdsStr.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { cancelIds.add(it) }
        prefs.edit().remove("alarm_ids_$noticeId").apply()

        for (id in cancelIds) {
            val durIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_NOTICE_DURATION_REMINDER
            }
            val durPendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                durIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (durPendingIntent != null) {
                alarmManager.cancel(durPendingIntent)
                durPendingIntent.cancel()
            }

            try {
                val manager = NotificationManagerCompat.from(context)
                manager.cancel(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Schedules periodic motivational quotes throughout the day.
     */
    fun schedulePeriodicMotiveNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val times = listOf(
            Pair(11, 30),
            Pair(16, 30),
            Pair(20, 30)
        )

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        times.forEachIndexed { index, (hour, minute) ->
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val reminderId = 40001 + index
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SEND_MOTIVE
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setExactAlarm(alarmManager, target.timeInMillis, pendingIntent)
        }
    }

    /**
     * Schedules a simple reminder using AlarmManager.
     */
    fun scheduleReminder(
        context: Context,
        title: String,
        message: String,
        triggerAtMillis: Long,
        reminderId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Requirement 5: Schedules daily wake-up and sleep schedule reminders.
     */
    fun scheduleSleepWakeSchedule(
        context: Context,
        wakeHour: Int,
        wakeMin: Int,
        sleepHour: Int,
        sleepMin: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Wake up reminder
        val wakeCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeHour)
            set(Calendar.MINUTE, wakeMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val wakeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_WAKE_UP_REMINDER
        }
        val wakePending = PendingIntent.getBroadcast(
            context,
            50001,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(alarmManager, wakeCal.timeInMillis, wakePending)

        // 2. Sleep reminder
        val sleepCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sleepHour)
            set(Calendar.MINUTE, sleepMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val sleepIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SLEEP_REMINDER
        }
        val sleepPending = PendingIntent.getBroadcast(
            context,
            50002,
            sleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(alarmManager, sleepCal.timeInMillis, sleepPending)
    }

    private fun setHighPriorityAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, MainActivity::class.java)
                val showPending = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPending),
                    pendingIntent
                )
            } else {
                setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Requirement 8: Schedules recurring daily reminder for UniTasks.
     */
    fun scheduleDailyUniTaskReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        hour: Int,
        minute: Int,
        notifyBeforeMinutes: Int = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (50000L + (taskId % 10000L)).toInt()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_UNITASK_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra("daily_hour", hour)
            putExtra("daily_minute", minute)
            putExtra("notify_before_minutes", notifyBeforeMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (notifyBeforeMinutes > 0) {
                add(Calendar.MINUTE, -notifyBeforeMinutes)
            }
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        setHighPriorityAlarm(context, alarmManager, cal.timeInMillis, pendingIntent)
    }

    fun cancelDailyUniTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderId = (50000L + (taskId % 10000L)).toInt()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_UNITASK_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
