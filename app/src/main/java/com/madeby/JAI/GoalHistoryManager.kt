package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GoalHistoryEntry(
    val date: String,
    val goalSecs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

object GoalHistoryManager {

    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val HISTORY_KEY = "daily_goal_history_json"

    fun loadHistory(context: Context): List<GoalHistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        return parseHistory(raw)
    }

    fun parseHistory(raw: String): List<GoalHistoryEntry> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<GoalHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val d = obj.optString("date", "")
                val g = obj.optLong("goalSecs", 2700L)
                val ts = obj.optLong("timestamp", 0L)
                if (d.isNotBlank() && g > 0L) {
                    list.add(GoalHistoryEntry(d, g, ts))
                }
            }
            list.sortedBy { it.date }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordGoalChange(context: Context, dateStr: String, goalSecs: Long) {
        if (goalSecs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = loadHistory(context).toMutableList()
        val existingIdx = history.indexOfFirst { it.date == dateStr }
        val newEntry = GoalHistoryEntry(dateStr, goalSecs, System.currentTimeMillis())
        if (existingIdx >= 0) {
            history[existingIdx] = newEntry
        } else {
            history.add(newEntry)
        }
        history.sortBy { it.date }
        val arr = JSONArray()
        for (e in history) {
            arr.put(JSONObject().apply {
                put("date", e.date)
                put("goalSecs", e.goalSecs)
                put("timestamp", e.timestamp)
            })
        }
        prefs.edit()
            .putString(HISTORY_KEY, arr.toString())
            .putLong("${dateStr}_goal_secs", goalSecs)
            .apply()
    }

    fun resolveGoalForDate(context: Context, dateStr: String, fallbackGlobalGoal: Long): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Highest priority: Fetch the saved daily goal on that specific day if present
        if (prefs.contains("${dateStr}_goal_secs")) {
            val explicit = prefs.getLong("${dateStr}_goal_secs", 0L)
            if (explicit > 0L) return explicit
        }

        // 2. Second priority: Check if a planner snapshot exists for this specific date with target minutes
        val snapshotRaw = prefs.getString("${dateStr}_planner_snapshot", null)
        if (!snapshotRaw.isNullOrBlank() && snapshotRaw != "[]") {
            try {
                val array = JSONArray(snapshotRaw)
                var totalTargetMins = 0
                for (i in 0 until array.length()) {
                    totalTargetMins += array.getJSONObject(i).optInt("targetMinutes", 0)
                }
                if (totalTargetMins > 0) {
                    val inferredSecs = (totalTargetMins * 60).toLong()
                    prefs.edit().putLong("${dateStr}_goal_secs", inferredSecs).apply()
                    return inferredSecs
                }
            } catch (_: Exception) {}
        }

        // 3. Third priority: Search Goal History log for the goal active on or immediately before dateStr
        val history = loadHistory(context)
        if (history.isNotEmpty()) {
            val matchingEntry = history.filter { it.date <= dateStr }.maxByOrNull { it.date }
            if (matchingEntry != null && matchingEntry.goalSecs > 0L) {
                prefs.edit().putLong("${dateStr}_goal_secs", matchingEntry.goalSecs).apply()
                return matchingEntry.goalSecs
            }
        }

        // 4. Default fallback: use global goal for today or fallback
        val defaultGoal = if (fallbackGlobalGoal > 0L) fallbackGlobalGoal else 2700L
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (dateStr == todayStr) {
            prefs.edit().putLong("${todayStr}_goal_secs", defaultGoal).apply()
        }
        return defaultGoal
    }

    /**
     * Reconciles all historical dates without overwriting existing saved goals on specific days.
     */
    fun reconcileAllHistoricalGoals(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val globalGoal = prefs.getLong("daily_goal_secs", 2700L)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val history = loadHistory(context)
        if (history.isEmpty()) {
            recordGoalChange(context, todayStr, globalGoal)
        }

        val allKeys = prefs.all.keys.toList()
        val focusKeys = allKeys.filter { it.endsWith("_focus_total") }
        val snapshotKeys = allKeys.filter { it.endsWith("_planner_snapshot") }
        val allDates = (focusKeys.map { it.removeSuffix("_focus_total") } +
                        snapshotKeys.map { it.removeSuffix("_planner_snapshot") } +
                        setOf(todayStr)).toSet()

        val editor = prefs.edit()
        var modified = false

        for (dStr in allDates) {
            val goalKey = "${dStr}_goal_secs"
            // If the date already has a saved goal, DO NOT overwrite it!
            if (prefs.contains(goalKey) && prefs.getLong(goalKey, 0L) > 0L) {
                continue
            }
            val resolved = resolveGoalForDate(context, dStr, globalGoal)
            editor.putLong(goalKey, resolved)
            modified = true
        }
        if (modified) editor.apply()
    }

    fun mergeCloudHistory(localJson: String, cloudJson: String): String {
        val localList = parseHistory(localJson)
        val cloudList = parseHistory(cloudJson)
        val mergedMap = mutableMapOf<String, GoalHistoryEntry>()

        for (e in localList + cloudList) {
            val existing = mergedMap[e.date]
            if (existing == null || e.timestamp >= existing.timestamp) {
                mergedMap[e.date] = e
            }
        }

        val sortedList = mergedMap.values.sortedBy { it.date }
        val arr = JSONArray()
        for (e in sortedList) {
            arr.put(JSONObject().apply {
                put("date", e.date)
                put("goalSecs", e.goalSecs)
                put("timestamp", e.timestamp)
            })
        }
        return arr.toString()
    }
}
