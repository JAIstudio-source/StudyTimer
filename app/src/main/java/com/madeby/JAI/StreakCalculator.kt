package com.madeby.JAI

import java.util.Calendar
import java.util.Date

object StreakCalculator {

    const val QUALIFYING_SECS = 2700L

    fun longestStreak(
        dayFocus: List<Pair<Date, Long>>,
        goalFor: ((Date) -> Long)? = null
    ): Int {
        var longest = 0
        var run = 0
        var prevDayMs: Long? = null
        for ((date, f) in dayFocus.sortedBy { it.first.time }) {
            val consecutive = prevDayMs?.let { prev ->
                val cal = Calendar.getInstance().apply { timeInMillis = prev }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis == date.time
            } ?: false
            val goal = goalFor?.invoke(date) ?: QUALIFYING_SECS
            run = if (f >= goal) (if (consecutive) run + 1 else 1) else 0
            prevDayMs = date.time
            if (run > longest) longest = run
        }
        return longest
    }

    fun currentStreak(
        focusOn: (daysAgo: Int) -> Long,
        todayExtra: Long = 0L,
        goalFor: ((daysAgo: Int) -> Long)? = null
    ): Int {
        var streak = 0
        for (i in 0 until 365) {
            val focusSecs = focusOn(i) + (if (i == 0) todayExtra else 0L)
            val goal = goalFor?.invoke(i) ?: QUALIFYING_SECS
            if (i == 0 && focusSecs < goal) continue
            if (focusSecs >= goal) streak++
            else break
        }
        return streak
    }

    fun calculateBadges(
        context: android.content.Context,
        currentStreak: Int,
        totalStudySeconds: Long
    ): List<AchievementBadge> {
        val totalHours = totalStudySeconds / 3600L
        val logs = TimelineLogger.load(context)

        var hasEarlyBird = false
        var hasMiddayMaster = false
        var hasPrimeTime = false
        var hasNightOwl = false

        val cal = Calendar.getInstance()
        for (item in logs) {
            cal.timeInMillis = item.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour < 7) hasEarlyBird = true
            if (hour in 12..14) hasMiddayMaster = true
            if (hour in 16..20) hasPrimeTime = true
            if (hour >= 23 || hour < 4) hasNightOwl = true
        }

        return listOf(
            AchievementBadge("7_day_streak", "7-Day Streak", "Maintain a study streak for 7 consecutive days", "🔥", currentStreak >= 7),
            AchievementBadge("30_day_streak", "30-Day Streak", "Maintain a study streak for 30 consecutive days", "🏆", currentStreak >= 30),
            AchievementBadge("10_hours_study", "10 Hours Focus", "Complete 10 total hours of focused study", "⏱️", totalHours >= 10),
            AchievementBadge("50_hours_study", "50 Hours Focus", "Complete 50 total hours of focused study", "🎓", totalHours >= 50),
            AchievementBadge("early_bird", "Early Bird", "Complete a study session before 7:00 AM", "🌅", hasEarlyBird),
            AchievementBadge("midday_master", "Midday Master", "Complete a study session between 12 PM - 3 PM", "☀️", hasMiddayMaster),
            AchievementBadge("prime_time", "Prime Time Focus", "Complete a study session between 4 PM - 8 PM", "🎯", hasPrimeTime),
            AchievementBadge("night_owl", "Night Owl", "Complete a study session after 11:00 PM", "🌙", hasNightOwl)
        )
    }
}
