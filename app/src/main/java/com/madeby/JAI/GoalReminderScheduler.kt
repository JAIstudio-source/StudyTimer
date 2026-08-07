package com.madeby.JAI

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

object GoalReminderScheduler {

    const val CHANNEL_ID = "goal_reminders"
    private const val REQUEST_CODE = 2001
    private const val REMIND_HOUR = 21
    private const val REMIND_MINUTE = 0

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.goal_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.goal_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", true)) {
            cancel(context)
            return
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_DAILY_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hour = prefs.safeInt("reminder_hour", REMIND_HOUR)
        val minute = prefs.safeInt("reminder_minute", REMIND_MINUTE)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val exact = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true
        }
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_DAILY_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
