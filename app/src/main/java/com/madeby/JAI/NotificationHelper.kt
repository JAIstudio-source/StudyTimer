package com.madeby.JAI

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_TIMER = "study_timer_channels"
    const val CHANNEL_COMPLETION = "study_timer_completion_v4"
    const val CHANNEL_GOAL_REMINDER = "goal_reminders_v4"

    // Backward-compatible alias channels in case cached intents or older components reference them
    private const val CHANNEL_TIMER_LEGACY = "study_timer_channel"
    private const val CHANNEL_COMPLETION_LEGACY = "study_timer_completion"
    private const val CHANNEL_GOAL_REMINDER_LEGACY = "study_timer_goal_reminder"

    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_COMPLETION = 1002
    const val NOTIFICATION_ID_GOAL_REMINDER = 1007
    const val NOTIFICATION_ID_INACTIVITY_CHECK = 1005

    /**
     * Initializes all app notification channels (Android O+).
     */
    fun createAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Ongoing Timer Channel (and legacy alias)
        listOf(CHANNEL_TIMER, CHANNEL_TIMER_LEGACY).forEach { chId ->
            if (nm.getNotificationChannel(chId) == null) {
                val timerChannel = NotificationChannel(
                    chId,
                    "Study Timer Control",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Persistent tray utilities for active sessions"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(timerChannel)
            }
        }

        // 2. Completion Channel (and legacy alias)
        listOf(CHANNEL_COMPLETION, CHANNEL_COMPLETION_LEGACY).forEach { chId ->
            if (nm.getNotificationChannel(chId) == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val completionChannel = NotificationChannel(
                    chId,
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
        }

        // 3. Goal Reminder Channel (and legacy alias)
        listOf(CHANNEL_GOAL_REMINDER, CHANNEL_GOAL_REMINDER_LEGACY).forEach { chId ->
            if (nm.getNotificationChannel(chId) == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val goalChannel = NotificationChannel(
                    chId,
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
