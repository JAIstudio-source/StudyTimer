package com.madeby.JAI

import android.content.Context
import android.text.format.DateFormat
import java.util.*

object TimeFormat {

    enum class Mode { SYSTEM, H24, H12 }

    private const val PREF_KEY = "time_format"

    fun currentMode(context: Context): Mode {
        val v = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString(PREF_KEY, "SYSTEM")
        return runCatching { Mode.valueOf(v ?: "SYSTEM") }.getOrDefault(Mode.SYSTEM)
    }

    fun setMode(context: Context, mode: Mode) {
        context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, mode.name).apply()
    }

    fun is24Hour(context: Context): Boolean {
        return when (currentMode(context)) {
            Mode.H24 -> true
            Mode.H12 -> false
            Mode.SYSTEM -> DateFormat.is24HourFormat(context)
        }
    }

    fun formatHourMinute(context: Context, hourOfDay: Int, minute: Int): String {
        return if (is24Hour(context)) {
            String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        } else {
            val h = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val ap = if (hourOfDay < 12) "AM" else "PM"
            String.format(Locale.getDefault(), "%d:%02d %s", h, minute, ap)
        }
    }

    fun formatWallClock(context: Context, millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return formatHourMinute(context, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}

fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0m"
    val hours = totalSeconds / 3600L
    val mins = (totalSeconds % 3600L) / 60L
    val secs = totalSeconds % 60L
    return when {
        hours > 0L && mins > 0L -> "${hours}h ${mins}m"
        hours > 0L -> "${hours}h"
        mins > 0L -> "${mins}m"
        else -> "${secs}s"
    }
}

fun formatDuration(totalSeconds: Int): String = formatDuration(totalSeconds.toLong())

