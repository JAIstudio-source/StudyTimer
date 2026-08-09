package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class GoalHistoryStatus { ACHIEVED, DEFICIT, NOT_COMPLETED }

data class GoalInsights(
    val totalDaysTracked: Int,
    val completedDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float, // 0.0f to 1.0f
    val bestWeekday: String
)

data class OverallPlannerInsights(
    val bestStreakGoalTitle: String,
    val bestStreakDays: Int,
    val mostConsistentGoalTitle: String,
    val mostConsistentPct: Int,
    val bestWeekdayName: String,
    val overallCompletionPct: Int,
    val hasEnoughData: Boolean
)

object PlannerHistoryManager {

    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val SNAPSHOT_KEY_SUFFIX = "_planner_snapshot"

    fun snapshotToday(context: Context, goals: List<PlannerGoal>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        snapshotForDate(context, todayStr, goals)
    }

    fun snapshotForDate(context: Context, dateStr: String, goals: List<PlannerGoal>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dayFocusSecs = prefs.getLong("${dateStr}_focus_total", 0L)
        var focusPool = (dayFocusSecs / 60).toInt()

        val sortedGoals = goals.sortedBy { it.targetMinutes }
        val checkedGoals = sortedGoals.filter { it.completed }.sortedBy { it.checkedAt }
        val uncheckedGoals = sortedGoals.filter { !it.completed }

        val achievementMap = mutableMapOf<String, Boolean>()
        for (g in checkedGoals) {
            val target = g.targetMinutes
            if (target > 0) {
                if (focusPool >= target) {
                    focusPool -= target
                    achievementMap[g.id] = true
                } else {
                    focusPool = 0
                    achievementMap[g.id] = false
                }
            } else {
                achievementMap[g.id] = true
            }
        }
        for (g in uncheckedGoals) {
            achievementMap[g.id] = false
        }

        val array = JSONArray()
        for (g in goals) {
            val isAchieved = achievementMap[g.id] ?: false
            val obj = JSONObject().apply {
                put("goalId", g.id)
                put("title", g.title)
                put("targetMinutes", g.targetMinutes)
                put("completed", g.completed)
                put("checkedAt", g.checkedAt)
                put("isAchieved", isAchieved)
            }
            array.put(obj)
        }
        prefs.edit().putString("${dateStr}${SNAPSHOT_KEY_SUFFIX}", array.toString()).apply()
    }

    fun loadDaySnapshot(context: Context, dateStr: String): List<PlannerGoalSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("${dateStr}${SNAPSHOT_KEY_SUFFIX}", null) ?: return emptyList()
        return parseSnapshots(jsonStr)
    }

    private fun parseSnapshots(jsonStr: String): List<PlannerGoalSnapshot> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PlannerGoalSnapshot>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val completed = obj.optBoolean("completed", false)
                val isAchieved = obj.optBoolean("isAchieved", completed)
                list.add(
                    PlannerGoalSnapshot(
                        goalId = obj.optString("goalId", ""),
                        title = obj.optString("title", ""),
                        targetMinutes = obj.optInt("targetMinutes", 0),
                        completed = completed,
                        checkedAt = obj.optLong("checkedAt", 0L),
                        isAchieved = isAchieved
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Loads detailed completion status per date ("yyyy-MM-dd" -> GoalHistoryStatus)
     * for a specific goalId across all saved daily snapshots.
     */
    fun loadGoalHistoryDetailed(context: Context, goalId: String): Map<String, GoalHistoryStatus> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyMap = mutableMapOf<String, GoalHistoryStatus>()
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.endsWith(SNAPSHOT_KEY_SUFFIX)) {
                val dateStr = key.removeSuffix(SNAPSHOT_KEY_SUFFIX)
                val rawJson = prefs.getString(key, null) ?: continue
                val snapshots = parseSnapshots(rawJson)
                val match = snapshots.firstOrNull { it.goalId == goalId }
                if (match != null) {
                    val status = when {
                        !match.completed -> GoalHistoryStatus.NOT_COMPLETED
                        match.isAchieved -> GoalHistoryStatus.ACHIEVED
                        else -> GoalHistoryStatus.DEFICIT
                    }
                    historyMap[dateStr] = status
                }
            }
        }
        return historyMap
    }

    fun loadGoalHistory(context: Context, goalId: String): Map<String, Boolean> {
        return loadGoalHistoryDetailed(context, goalId).mapValues { it.value == GoalHistoryStatus.ACHIEVED }
    }

    fun computeGoalInsights(context: Context, goalId: String): GoalInsights {
        val history = loadGoalHistory(context, goalId)
        if (history.isEmpty()) {
            return GoalInsights(0, 0, 0, 0, 0f, "N/A")
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sortedDates = history.keys.mapNotNull {
            runCatching { sdf.parse(it) }.getOrNull()
        }.sorted()

        val totalDays = sortedDates.size
        val completedDays = history.values.count { it }
        val completionRate = if (totalDays > 0) completedDays.toFloat() / totalDays.toFloat() else 0f

        var maxStreak = 0
        var tempStreak = 0

        for (d in sortedDates) {
            val dStr = sdf.format(d)
            if (history[dStr] == true) {
                tempStreak++
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        var currentStreak = 0
        var checkCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var checkStr = sdf.format(checkCal.time)
        if (history[checkStr] != true) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(checkCal.time)
        }

        while (history[checkStr] == true) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(checkCal.time)
        }

        val weekdayCounts = IntArray(7)
        val weekdayCompletions = IntArray(7)
        for ((dStr, completed) in history) {
            val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: continue
            val cal = Calendar.getInstance().apply { time = parsed }
            val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
            weekdayCounts[idx]++
            if (completed) weekdayCompletions[idx]++
        }

        val bestIdx = (0 until 7).maxByOrNull {
            if (weekdayCounts[it] > 0) weekdayCompletions[it].toFloat() / weekdayCounts[it].toFloat() else 0f
        } ?: 0

        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestWeekdayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestIdx + 1) }.time)

        return GoalInsights(
            totalDaysTracked = totalDays,
            completedDays = completedDays,
            currentStreak = currentStreak,
            longestStreak = maxStreak,
            completionRate = completionRate,
            bestWeekday = bestWeekdayName
        )
    }

    fun computeOverallPlannerInsights(context: Context, currentGoals: List<PlannerGoal>): OverallPlannerInsights {
        if (currentGoals.isEmpty()) {
            return OverallPlannerInsights("", 0, "", 0, "N/A", 0, false)
        }

        var maxStreakVal = 0
        var maxStreakGoalTitle = ""

        var maxRateVal = 0f
        var maxRateGoalTitle = ""

        var totalGoalDays = 0
        var totalCompletedDays = 0

        val weekdayTotals = IntArray(7)

        for (goal in currentGoals) {
            val insights = computeGoalInsights(context, goal.id)
            if (insights.totalDaysTracked > 0) {
                totalGoalDays += insights.totalDaysTracked
                totalCompletedDays += insights.completedDays

                if (insights.longestStreak > maxStreakVal) {
                    maxStreakVal = insights.longestStreak
                    maxStreakGoalTitle = goal.title
                }

                if (insights.completionRate > maxRateVal) {
                    maxRateVal = insights.completionRate
                    maxRateGoalTitle = goal.title
                }
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (key in prefs.all.keys) {
            if (key.endsWith(SNAPSHOT_KEY_SUFFIX)) {
                val dStr = key.removeSuffix(SNAPSHOT_KEY_SUFFIX)
                val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
                val rawJson = prefs.getString(key, null) ?: continue
                val snapshots = parseSnapshots(rawJson)
                val completedCount = snapshots.count { it.completed }
                weekdayTotals[idx] += completedCount
            }
        }

        val bestDayIdx = (0 until 7).maxByOrNull { weekdayTotals[it] } ?: 0
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestDayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestDayIdx + 1) }.time)

        val overallPct = if (totalGoalDays > 0) (totalCompletedDays * 100) / totalGoalDays else 0
        val hasEnoughData = totalGoalDays >= 3 || currentGoals.any { it.completed }

        return OverallPlannerInsights(
            bestStreakGoalTitle = if (maxStreakGoalTitle.isNotBlank()) maxStreakGoalTitle else currentGoals.first().title,
            bestStreakDays = maxStreakVal,
            mostConsistentGoalTitle = if (maxRateGoalTitle.isNotBlank()) maxRateGoalTitle else currentGoals.first().title,
            mostConsistentPct = (maxRateVal * 100).toInt(),
            bestWeekdayName = bestDayName,
            overallCompletionPct = overallPct,
            hasEnoughData = hasEnoughData
        )
    }
}
