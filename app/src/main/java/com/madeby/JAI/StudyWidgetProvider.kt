package com.madeby.JAI

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class StudyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.madeby.JAI.WIDGET_TOGGLE"
        const val ACTION_STOP = "com.madeby.JAI.WIDGET_STOP"
        private val handler = Handler(Looper.getMainLooper())
        private var refreshScheduled = false

        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, StudyWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                render(context, appWidgetManager, ids)
            }
        }

        private fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val accumulated = prefs.getLong("accumulatedStudy", 0L)
            val focusToday = prefs.getLong("${todayStr}_focus_total", 0L) + accumulated
            val streak = prefs.safeInt("current_streak", 0)
            val state = TimerState.valueOf(prefs.getString("timerState", "IDLE") ?: "IDLE")

            val oled = prefs.getString("activeBgMode", "OLED") == "OLED"
            val light = prefs.getString("activeBgMode", "OLED") == "LIGHT"
            val bgColor = when {
                light -> 0xFFFFFFFF.toInt()
                oled -> 0xFF121212.toInt()
                else -> 0xFF1E293B.toInt()
            }
            val textColor = if (light) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
            val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.getBoolean("dynamic_color", false)) {
                context.getColor(android.R.color.system_accent1_500)
            } else {
                prefs.safeInt("customPrimary", 0xFF7DD3FC.toInt())
            }

            val views = RemoteViews(context.packageName, R.layout.study_widget_layout)
            views.setTextViewText(R.id.widToday, context.getString(R.string.widget_today, focusToday / 3600, (focusToday % 3600) / 60))
            views.setTextViewText(R.id.widStreak, context.getString(R.string.widget_streak, streak))
            views.setTextViewText(R.id.widToggle, when (state) {
                TimerState.STUDYING -> context.getString(R.string.take_a_break)
                TimerState.BREAK -> context.getString(R.string.notif_action_resume_focus)
                TimerState.PAUSED -> context.getString(R.string.resume)
                else -> context.getString(R.string.start_focus)
            })
            views.setInt(R.id.widRoot, "setBackgroundResource", if (light) R.drawable.bg_widget_light else R.drawable.bg_widget_dark)
            views.setTextColor(R.id.widTitle, primary)
            views.setTextColor(R.id.widToday, textColor)
            views.setTextColor(R.id.widStreak, textColor)
            views.setTextColor(R.id.widToggle, bgColor)
            views.setInt(R.id.widToggle, "setBackgroundColor", primary)
            views.setTextColor(R.id.widStop, textColor)
            views.setInt(R.id.widStop, "setBackgroundColor", 0x33EF4444.toInt())
            views.setViewVisibility(R.id.widStop, if (state == TimerState.IDLE) android.view.View.GONE else android.view.View.VISIBLE)

            val openIntent = Intent(context, MainActivity::class.java)
            val openPi = PendingIntent.getActivity(
                context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widRoot, openPi)

            val toggleIntent = Intent(context, StudyWidgetProvider::class.java).apply { action = ACTION_TOGGLE }
            val togglePi = PendingIntent.getBroadcast(
                context, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widToggle, togglePi)

            val stopIntent = Intent(context, StudyWidgetProvider::class.java).apply { action = ACTION_STOP }
            val stopPi = PendingIntent.getBroadcast(
                context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widStop, stopPi)

            for (id in appWidgetIds) {
                appWidgetManager.updateAppWidget(id, views)
            }

            schedulePeriodicRefresh(context)
        }

        private fun schedulePeriodicRefresh(context: Context) {
            if (refreshScheduled) return
            refreshScheduled = true
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val state = TimerState.valueOf(prefs.getString("timerState", "IDLE") ?: "IDLE")
            val delay = if (state != TimerState.IDLE) 30_000L else 5 * 60 * 1000L
            handler.postDelayed({
                refreshScheduled = false
                refresh(context.applicationContext)
            }, delay)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val receivedAction = intent?.action
        if (context == null) return
        when (receivedAction) {
            ACTION_TOGGLE -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_TOGGLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                handler.postDelayed({ refresh(context.applicationContext) }, 1200)
            }
            ACTION_STOP -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_STOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                handler.postDelayed({ refresh(context.applicationContext) }, 1200)
            }
        }
    }
}
