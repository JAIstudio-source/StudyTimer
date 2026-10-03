package com.madeby.JAI

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_TIMER = "study_timer_channel"
    const val CHANNEL_COMPLETION = "study_timer_completion"
    const val CHANNEL_GOAL_REMINDER = "study_timer_goal_reminder"

    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_COMPLETION = 1006
    const val NOTIFICATION_ID_GOAL_REMINDER = 1007

    /**
     * Initializes all app notification channels (Android O+).
     */
    fun createAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Ongoing Timer Channel
        if (nm.getNotificationChannel(CHANNEL_TIMER) == null) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Study Timer Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent tray utilities for active sessions"
                setShowBadge(false)
            }
            nm.createNotificationChannel(timerChannel)
        }

        // 2. Completion Channel
        if (nm.getNotificationChannel(CHANNEL_COMPLETION) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION,
                context.getString(R.string.channel_completion_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_completion_desc)
                enableVibration(false)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(completionChannel)
        }

        // 3. Goal Reminder Channel
        if (nm.getNotificationChannel(CHANNEL_GOAL_REMINDER) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val goalChannel = NotificationChannel(
                CHANNEL_GOAL_REMINDER,
                context.getString(R.string.goal_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.goal_channel_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(goalChannel)
        }
    }

    /**
     * Shows a completion notification on CHANNEL_COMPLETION.
     */
    fun showCompletionNotification(context: Context, title: String, text: String) {
        createAllNotificationChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_small_app_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        nm.notify(NOTIFICATION_ID_COMPLETION, builder.build())
    }
}
