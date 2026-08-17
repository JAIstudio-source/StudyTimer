package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubjectTag(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val colorHex: String,
    val isCustom: Boolean = false
)

object SubjectTagManager {

    private const val PREFS_NAME = "studytimer_subject_tags"
    private const val KEY_SELECTED_SUBJECT = "selected_subject_id"
    private const val KEY_CUSTOM_SUBJECTS_JSON = "custom_subjects_json"
    private const val KEY_HIDDEN_SUBJECTS = "hidden_subjects_set"
    private const val KEY_SUBJECT_DURATIONS = "subject_durations_json"

    val DEFAULT_SUBJECTS = listOf(
        SubjectTag("general", "General", "📖", "#6366F1"),
        SubjectTag("math", "Math", "📐", "#10B981"),
        SubjectTag("coding", "Coding", "💻", "#3B82F6"),
        SubjectTag("physics", "Physics", "⚛️", "#8B5CF6"),
        SubjectTag("history", "History", "📜", "#F59E0B"),
        SubjectTag("science", "Science", "🧪", "#EC4899")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllSubjects(context: Context): List<SubjectTag> {
        val hiddenSet = getPrefs(context).getStringSet(KEY_HIDDEN_SUBJECTS, emptySet()) ?: emptySet()
        val list = ArrayList<SubjectTag>()

        for (d in DEFAULT_SUBJECTS) {
            if (!hiddenSet.contains(d.id)) {
                list.add(d)
            }
        }

        val customJson = getPrefs(context).getString(KEY_CUSTOM_SUBJECTS_JSON, "[]") ?: "[]"
        try {
            val arr = JSONArray(customJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                if (!hiddenSet.contains(id)) {
                    list.add(
                        SubjectTag(
                            id = id,
                            name = obj.getString("name"),
                            iconEmoji = obj.optString("iconEmoji", "📚"),
                            colorHex = obj.optString("colorHex", "#6366F1"),
                            isCustom = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            list.add(DEFAULT_SUBJECTS[0])
        }
        return list
    }

    fun getSelectedSubject(context: Context): SubjectTag {
        val all = getAllSubjects(context)
        val id = getPrefs(context).getString(KEY_SELECTED_SUBJECT, "general") ?: "general"
        return all.find { it.id == id } ?: all[0]
    }

    fun setSelectedSubject(context: Context, subjectId: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_SUBJECT, subjectId).apply()
    }

    val COLOR_PALETTE = listOf(
        "#EC4899", "#F97316", "#06B6D4", "#14B8A6", "#EAB308",
        "#8B5CF6", "#EF4444", "#3B82F6", "#10B981", "#A855F7",
        "#6366F1", "#F43F5E", "#84CC16", "#E11D48", "#0284C7",
        "#7C3AED", "#D97706", "#059669", "#DB2777", "#2563EB"
    )

    fun generateUniqueColor(context: Context): String {
        val existing = getAllSubjects(context)
        val usedColors = existing.map { it.colorHex.uppercase(Locale.US) }.toSet()

        for (c in COLOR_PALETTE) {
            if (!usedColors.contains(c.uppercase(Locale.US))) {
                return c
            }
        }

        val count = usedColors.size
        val hue = (count * 137.508f) % 360f
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.78f, 0.62f))
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }

    fun addCustomSubject(context: Context, name: String, emoji: String = "📚", colorHex: String? = null): SubjectTag {
        val cleanName = name.trim().take(25)
        val id = "custom_" + System.currentTimeMillis()
        val finalColor = colorHex?.takeIf { it.isNotBlank() } ?: generateUniqueColor(context)
        val newSub = SubjectTag(id, cleanName, emoji, finalColor, isCustom = true)
        val prefs = getPrefs(context)
        val customJson = prefs.getString(KEY_CUSTOM_SUBJECTS_JSON, "[]") ?: "[]"
        try {
            val arr = JSONArray(customJson)
            val obj = JSONObject().apply {
                put("id", newSub.id)
                put("name", newSub.name)
                put("iconEmoji", newSub.iconEmoji)
                put("colorHex", newSub.colorHex)
            }
            arr.put(obj)
            prefs.edit().putString(KEY_CUSTOM_SUBJECTS_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
        return newSub
    }

    fun removeSubject(context: Context, subjectId: String) {
        if (subjectId == "general") return // Keep General as fallback

        val prefs = getPrefs(context)
        val hiddenSet = HashSet(prefs.getStringSet(KEY_HIDDEN_SUBJECTS, emptySet()) ?: emptySet())
        hiddenSet.add(subjectId)
        prefs.edit().putStringSet(KEY_HIDDEN_SUBJECTS, hiddenSet).apply()

        // Also clean custom JSON if it was custom
        val customJson = prefs.getString(KEY_CUSTOM_SUBJECTS_JSON, "[]") ?: "[]"
        try {
            val arr = JSONArray(customJson)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") != subjectId) {
                    newArr.put(obj)
                }
            }
            prefs.edit().putString(KEY_CUSTOM_SUBJECTS_JSON, newArr.toString()).apply()
        } catch (_: Exception) {}

        if (getSelectedSubject(context).id == subjectId) {
            setSelectedSubject(context, "general")
        }
    }

    private const val KEY_DAILY_SUBJECT_DURATIONS = "daily_subject_durations_json"
    private const val KEY_SUBJECT_BREAK_DURATIONS = "subject_break_durations_json"
    private const val KEY_DAILY_SUBJECT_BREAK_DURATIONS = "daily_subject_break_durations_json"

    fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun recordSubjectStudyTime(context: Context, subjectId: String, durationSecs: Long, dateKey: String = getTodayKey()) {
        if (durationSecs <= 0) return
        val prefs = getPrefs(context)
        
        // 1. Update Global Total
        val jsonStr = prefs.getString(KEY_SUBJECT_DURATIONS, "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            val current = json.optLong(subjectId, 0L)
            json.put(subjectId, current + durationSecs)
            prefs.edit().putString(KEY_SUBJECT_DURATIONS, json.toString()).apply()
        } catch (_: Exception) {}

        // 2. Update Daily Map
        val dailyStr = prefs.getString(KEY_DAILY_SUBJECT_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey) ?: JSONObject()
            val curDaySecs = dayObj.optLong(subjectId, 0L)
            dayObj.put(subjectId, curDaySecs + durationSecs)
            dailyJson.put(dateKey, dayObj)
            prefs.edit().putString(KEY_DAILY_SUBJECT_DURATIONS, dailyJson.toString()).apply()
        } catch (_: Exception) {}
    }

    fun recordSubjectBreakTime(context: Context, subjectId: String, durationSecs: Long, dateKey: String = getTodayKey()) {
        if (durationSecs <= 0) return
        val prefs = getPrefs(context)

        val jsonStr = prefs.getString(KEY_SUBJECT_BREAK_DURATIONS, "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            val current = json.optLong(subjectId, 0L)
            json.put(subjectId, current + durationSecs)
            prefs.edit().putString(KEY_SUBJECT_BREAK_DURATIONS, json.toString()).apply()
        } catch (_: Exception) {}

        val dailyStr = prefs.getString(KEY_DAILY_SUBJECT_BREAK_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey) ?: JSONObject()
            val curDaySecs = dayObj.optLong(subjectId, 0L)
            dayObj.put(subjectId, curDaySecs + durationSecs)
            dailyJson.put(dateKey, dayObj)
            prefs.edit().putString(KEY_DAILY_SUBJECT_BREAK_DURATIONS, dailyJson.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getSubjectBreakDurationsForDate(context: Context, dateKey: String): Map<String, Long> {
        val map = HashMap<String, Long>()
        val prefs = getPrefs(context)
        val dailyStr = prefs.getString(KEY_DAILY_SUBJECT_BREAK_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey)
            if (dayObj != null) {
                val keys = dayObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = dayObj.optLong(k, 0L)
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun getSubjectDurations(context: Context): Map<String, Long> {
        val map = HashMap<String, Long>()
        val jsonStr = getPrefs(context).getString(KEY_SUBJECT_DURATIONS, "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.optLong(k, 0L)
            }
        } catch (_: Exception) {}
        return map
    }

    fun getSubjectDurationsForDate(context: Context, dateKey: String): Map<String, Long> {
        val map = HashMap<String, Long>()
        val prefs = getPrefs(context)
        val dailyStr = prefs.getString(KEY_DAILY_SUBJECT_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey)
            if (dayObj != null) {
                val keys = dayObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = dayObj.optLong(k, 0L)
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun clearTodaySubjectDurations(context: Context, dateKey: String = getTodayKey()) {
        val prefs = getPrefs(context)
        val todayMap = getSubjectDurationsForDate(context, dateKey)

        if (todayMap.isNotEmpty()) {
            val jsonStr = prefs.getString(KEY_SUBJECT_DURATIONS, "{}") ?: "{}"
            try {
                val json = JSONObject(jsonStr)
                for ((subId, secs) in todayMap) {
                    val current = json.optLong(subId, 0L)
                    val updated = (current - secs).coerceAtLeast(0L)
                    if (updated == 0L) {
                        json.remove(subId)
                    } else {
                        json.put(subId, updated)
                    }
                }
                prefs.edit().putString(KEY_SUBJECT_DURATIONS, json.toString()).apply()
            } catch (_: Exception) {}
        }

        val dailyStr = prefs.getString(KEY_DAILY_SUBJECT_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            dailyJson.remove(dateKey)
            prefs.edit().putString(KEY_DAILY_SUBJECT_DURATIONS, dailyJson.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearAllSubjectDurations(context: Context) {
        getPrefs(context).edit().remove(KEY_SUBJECT_DURATIONS).remove(KEY_DAILY_SUBJECT_DURATIONS).apply()
    }
}
