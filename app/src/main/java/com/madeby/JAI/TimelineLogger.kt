package com.madeby.JAI

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineEntry(
    val timestamp: Long,
    val state: String,
    val id: String? = null,
    val subId: String? = null,
    val subName: String? = null,
    val subColor: String? = null
)

object TimelineLogger {

    private const val KEY = "focus_timeline"
    private const val FILE_NAME = "focus_timeline.json"

    @Volatile
    private var cache: List<TimelineEntry>? = null

    fun invalidate() {
        cache = null
    }

    fun record(
        context: Context,
        state: TimerState,
        id: String? = null,
        subId: String? = null,
        subName: String? = null,
        subColor: String? = null
    ) {
        synchronized(this) {
            val entries = load(context).toMutableList()
            entries.add(TimelineEntry(System.currentTimeMillis(), state.name, id, subId, subName, subColor))
            persist(context, entries)
        }
    }

    fun recordRaw(
        context: Context,
        state: String,
        timestamp: Long = System.currentTimeMillis(),
        id: String? = null,
        subId: String? = null,
        subName: String? = null,
        subColor: String? = null
    ) {
        synchronized(this) {
            val entries = insertEntrySorted(load(context), TimelineEntry(timestamp, state, id, subId, subName, subColor))
            persist(context, entries)
        }
    }

    fun load(context: Context): List<TimelineEntry> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val result = loadEntries(context)
            cache = result
            return result
        }
    }

    fun addBlock(
        context: Context,
        startMs: Long,
        endMs: Long,
        state: String,
        subId: String? = null,
        subName: String? = null,
        subColor: String? = null
    ) {
        if (endMs <= startMs) return
        synchronized(this) {
            val list = load(context).toMutableList()
            list.removeAll { it.timestamp == startMs || it.timestamp == endMs }
            var updated = insertEntrySorted(list, TimelineEntry(startMs, state, subId = subId, subName = subName, subColor = subColor))
            updated = insertEntrySorted(updated, TimelineEntry(endMs, "IDLE"))
            persist(context, updated)
        }
    }

    fun replaceBlock(
        context: Context,
        oldStartMs: Long,
        oldEndMs: Long,
        newStartMs: Long,
        newEndMs: Long,
        state: String,
        subId: String? = null,
        subName: String? = null,
        subColor: String? = null
    ) {
        if (newEndMs <= newStartMs) return
        synchronized(this) {
            val list = load(context).toMutableList()
            val oldEntry = list.firstOrNull { it.timestamp == oldStartMs }
            list.removeAll { it.timestamp == oldStartMs }
            val atOldEnd = list.firstOrNull { it.timestamp == oldEndMs }
            if (atOldEnd != null && atOldEnd.state == "IDLE") {
                list.remove(atOldEnd)
            }
            list.removeAll { it.timestamp == newStartMs || it.timestamp == newEndMs }
            val finalSubId = subId ?: oldEntry?.subId
            val finalSubName = subName ?: oldEntry?.subName
            val finalSubColor = subColor ?: oldEntry?.subColor
            var updated = insertEntrySorted(list, TimelineEntry(newStartMs, state, subId = finalSubId, subName = finalSubName, subColor = finalSubColor))
            updated = insertEntrySorted(updated, TimelineEntry(newEndMs, "IDLE"))
            persist(context, updated)
        }
    }

    fun deleteBlock(context: Context, startMs: Long, endMs: Long) {
        synchronized(this) {
            val list = load(context).toMutableList()
            list.removeAll { it.timestamp == startMs }
            val atEnd = list.firstOrNull { it.timestamp == endMs }
            if (atEnd != null && atEnd.state == "IDLE") {
                list.remove(atEnd)
            }
            persist(context, list)
        }
    }

    fun appendBlockForDay(
        context: Context,
        dateStr: String,
        durationSecs: Long,
        state: String,
        subId: String? = null,
        subName: String? = null,
        subColor: String? = null
    ): Pair<Long, Long> {
        if (durationSecs <= 0) return 0L to 0L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = runCatching { sdf.parse(dateStr) }.getOrNull() ?: Date()
        val startCal = java.util.Calendar.getInstance().apply {
            time = parsed
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStartMs = startCal.timeInMillis
        val dayEndMs = dayStartMs + 24L * 3600_000L - 1000L
        val durationMs = durationSecs * 1000L

        synchronized(this) {
            val entries = load(context)
            val dayEntries = entries.filter { it.timestamp in dayStartMs..dayEndMs }.sortedBy { it.timestamp }
            val maxTs = dayEntries.maxOfOrNull { it.timestamp }
            val isToday = dateStr == sdf.format(Date())
            val nowMs = System.currentTimeMillis()

            var candidateStart = if (maxTs != null) {
                maxOf(maxTs + 1000L, dayStartMs)
            } else {
                if (isToday) {
                    val defaultStart = nowMs - durationMs
                    defaultStart.coerceIn(dayStartMs, dayEndMs - durationMs)
                } else {
                    dayStartMs + 9L * 3600_000L
                }
            }

            if (maxTs != null && candidateStart <= maxTs) {
                candidateStart = maxTs + 1000L
            }

            if (candidateStart + durationMs > dayEndMs) {
                candidateStart = (dayEndMs - durationMs).coerceAtLeast(dayStartMs)
                if (maxTs != null && candidateStart <= maxTs) {
                    candidateStart = maxTs + 1000L
                }
            }

            val candidateEnd = candidateStart + durationMs
            addBlock(context, candidateStart, candidateEnd, state, subId = subId, subName = subName, subColor = subColor)
            return candidateStart to candidateEnd
        }
    }

    fun deleteEntry(context: Context, startMs: Long) {
        synchronized(this) {
            persist(context, removeEntryWithTs(load(context), startMs))
        }
    }

    fun moveEntry(context: Context, oldTs: Long, newTs: Long) {
        synchronized(this) {
            val entries = moveEntryInList(load(context), oldTs, newTs) ?: return
            persist(context, entries)
        }
    }

    fun deleteDay(context: Context, dateStr: String) {
        synchronized(this) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            persist(context, removeDayEntries(load(context), dateStr, fmt))
        }
    }

    /**
     * Replaces the entire timeline from an external source (backup restore).
     * Passing null (or empty) clears the timeline.
     */
    fun importRaw(context: Context, raw: String?) {
        synchronized(this) {
            if (raw.isNullOrEmpty()) {
                deleteStaleFile(context)
                removeLegacyPref(context)
                cache = emptyList()
            } else {
                val parsed = parseTimelineJson(raw)
                if (writeFile(context, raw)) {
                    removeLegacyPref(context)
                } else {
                    deleteStaleFile(context)
                    writeLegacyPref(context, raw)
                }
                cache = parsed
            }
        }
    }

    private fun timelineFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun readFile(context: Context): String? {
        val f = timelineFile(context)
        if (!f.exists()) return null
        return try { f.readText().takeIf { it.isNotEmpty() } } catch (_: Exception) { null }
    }

    private fun writeFile(context: Context, content: String): Boolean {
        return try {
            val f = timelineFile(context)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(content)
            if (tmp.renameTo(f)) {
                true
            } else {
                f.writeText(content)
                tmp.delete()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun deleteStaleFile(context: Context) {
        try { timelineFile(context).delete() } catch (_: Exception) {}
    }

    private fun removeLegacyPref(context: Context) {
        try {
            context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                .edit().remove(KEY).apply()
        } catch (_: Exception) {}
    }

    private fun writeLegacyPref(context: Context, content: String) {
        try {
            context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                .edit().putString(KEY, content).apply()
        } catch (_: Exception) {}
    }

    private fun loadEntries(context: Context): List<TimelineEntry> {
        val fileRaw = readFile(context)
        if (fileRaw != null) {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            if (prefs.contains(KEY)) removeLegacyPref(context)
            return parseTimelineJson(fileRaw)
        }
        val legacy = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString(KEY, null)
        if (!legacy.isNullOrEmpty()) {
            if (writeFile(context, legacy)) removeLegacyPref(context)
            return parseTimelineJson(legacy)
        }
        return emptyList()
    }

    private fun persist(context: Context, entries: List<TimelineEntry>) {
        val content = timelineToJsonString(entries)
        if (writeFile(context, content)) {
            removeLegacyPref(context)
        } else {
            deleteStaleFile(context)
            writeLegacyPref(context, content)
        }
        cache = entries
    }
}
