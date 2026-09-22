package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ExamCountdown(
    val id: String,
    val title: String,
    val targetDateStr: String, // "yyyy-MM-dd"
    val targetTimestampMs: Long,
    val subjectId: String? = null,
    val colorHex: String? = null,
    val notes: String? = null
)

data class ExamCountdownBreakdown(
    val isPast: Boolean,
    val isToday: Boolean,
    val daysAgo: Long,
    val totalDays: Long,
    val totalWeeks: Double,
    val totalMonths: Double,
    val monthsPart: Long,
    val daysPart: Long,
    val hoursPart: Long,
    val minsPart: Long,
    val mainHeadline: String,
    val readableSubtitle: String
)

object ExamCountdownManager {
    private const val PREFS_NAME = "studytimer_exam_countdowns"
    private const val KEY_EXAMS_JSON = "exams_list_json"

    fun getExams(context: Context): List<ExamCountdown> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_EXAMS_JSON, "[]") ?: "[]"
        val list = mutableListOf<ExamCountdown>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExamCountdown(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        targetDateStr = obj.optString("targetDateStr", ""),
                        targetTimestampMs = obj.getLong("targetTimestampMs"),
                        subjectId = obj.optString("subjectId", "").takeIf { it.isNotBlank() },
                        colorHex = obj.optString("colorHex", "").takeIf { it.isNotBlank() },
                        notes = obj.optString("notes", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedBy { it.targetTimestampMs }
    }

    fun saveExams(context: Context, list: List<ExamCountdown>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("targetDateStr", item.targetDateStr)
                    put("targetTimestampMs", item.targetTimestampMs)
                    item.subjectId?.let { put("subjectId", it) }
                    item.colorHex?.let { put("colorHex", it) }
                    item.notes?.let { put("notes", it) }
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_EXAMS_JSON, arr.toString()).commit()

            // Trigger silent backup & cloud sync
            BackupManager(context).markDataModified()
            BackupManager(context).runSilentAutoBackup()
            if (AuthManager.isLoggedIn(context)) {
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(context, force = true)
                    }
                }.start()
            }
        } catch (_: Exception) {}
    }

    fun addExam(
        context: Context,
        title: String,
        targetTimestampMs: Long,
        subjectId: String? = null,
        colorHex: String? = null,
        notes: String? = null
    ): ExamCountdown {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date(targetTimestampMs))
        val newExam = ExamCountdown(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifEmpty { "Upcoming Exam" },
            targetDateStr = dateStr,
            targetTimestampMs = targetTimestampMs,
            subjectId = subjectId,
            colorHex = colorHex,
            notes = notes?.trim()?.ifEmpty { null }
        )
        val current = getExams(context).toMutableList()
        current.add(newExam)
        saveExams(context, current)
        return newExam
    }

    fun updateExam(context: Context, updated: ExamCountdown) {
        val current = getExams(context).toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            current[idx] = updated
            saveExams(context, current)
        }
    }

    fun deleteExam(context: Context, examId: String) {
        val current = getExams(context).filter { it.id != examId }
        saveExams(context, current)
    }

    /**
     * Complete breakdown with mathematically decomposed parts.
     */
    fun getCountdownBreakdown(targetMs: Long): ExamCountdownBreakdown {
        val now = System.currentTimeMillis()
        val diffMs = targetMs - now
        if (diffMs <= 0) {
            val daysAgo = -diffMs / (24L * 3600_000L)
            val isToday = daysAgo == 0L
            return ExamCountdownBreakdown(
                isPast = !isToday,
                isToday = isToday,
                daysAgo = daysAgo,
                totalDays = 0,
                totalWeeks = 0.0,
                totalMonths = 0.0,
                monthsPart = 0,
                daysPart = 0,
                hoursPart = 0,
                minsPart = 0,
                mainHeadline = if (isToday) "Exam is Today" else "$daysAgo days ago",
                readableSubtitle = if (isToday) "Scheduled for today" else "Exam date has passed"
            )
        }

        val totalSecs = diffMs / 1000L
        val totalDays = totalSecs / 86400L
        val remSecsAfterDays = totalSecs % 86400L
        val hours = remSecsAfterDays / 3600L
        val mins = (remSecsAfterDays % 3600L) / 60L

        val months = totalDays / 30L
        val daysRem = totalDays % 30L
        val totalWeeks = ((totalDays / 7.0) * 10).toInt() / 10.0
        val totalMonths = ((totalDays / 30.0) * 10).toInt() / 10.0

        val headline = when {
            totalDays >= 2 -> "$totalDays Days Left"
            totalDays == 1L -> "1 Day Left"
            hours > 0 -> "$hours Hours Left"
            else -> "$mins Minutes Left"
        }

        val subtitleParts = mutableListOf<String>()
        if (months > 0) {
            val mStr = "$months ${if (months == 1L) "Month" else "Months"}"
            val dStr = if (daysRem > 0) ", $daysRem ${if (daysRem == 1L) "Day" else "Days"}" else ""
            subtitleParts.add("$mStr$dStr")
        }
        if (totalWeeks >= 1.0) {
            subtitleParts.add("${totalWeeks} Weeks Total")
        }

        return ExamCountdownBreakdown(
            isPast = false,
            isToday = false,
            daysAgo = 0,
            totalDays = totalDays,
            totalWeeks = totalWeeks,
            totalMonths = totalMonths,
            monthsPart = months,
            daysPart = daysRem,
            hoursPart = hours,
            minsPart = mins,
            mainHeadline = headline,
            readableSubtitle = subtitleParts.joinToString(" • ")
        )
    }

    /**
     * Compact display string for small card badge.
     */
    fun formatShortCountdown(targetMs: Long): String {
        val breakdown = getCountdownBreakdown(targetMs)
        return if (breakdown.isToday) {
            "Today"
        } else if (breakdown.isPast) {
            "${breakdown.daysAgo}d ago"
        } else {
            breakdown.mainHeadline
        }
    }

    /**
     * Clean detailed decomposition string (e.g. "4 Months, 1 Day, 6 Hours").
     */
    fun formatDetailedCountdown(targetMs: Long): String {
        val breakdown = getCountdownBreakdown(targetMs)
        if (breakdown.isToday) return "Exam is scheduled for today"
        if (breakdown.isPast) return "Exam date passed ${breakdown.daysAgo} days ago"

        val parts = mutableListOf<String>()
        if (breakdown.monthsPart > 0) parts.add("${breakdown.monthsPart} ${if (breakdown.monthsPart == 1L) "Month" else "Months"}")
        if (breakdown.daysPart > 0) parts.add("${breakdown.daysPart} ${if (breakdown.daysPart == 1L) "Day" else "Days"}")
        if (breakdown.hoursPart > 0 && breakdown.monthsPart == 0L) parts.add("${breakdown.hoursPart} ${if (breakdown.hoursPart == 1L) "Hour" else "Hours"}")
        if (breakdown.minsPart > 0 && breakdown.monthsPart == 0L) parts.add("${breakdown.minsPart} ${if (breakdown.minsPart == 1L) "Min" else "Mins"}")

        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Less than 1 minute left"
    }
}
