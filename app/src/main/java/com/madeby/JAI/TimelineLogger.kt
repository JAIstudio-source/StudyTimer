package com.madeby.JAI

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineEntry(val timestamp: Long, val state: String, val id: String? = null)

object TimelineLogger {

    private const val KEY = "focus_timeline"
    private const val FILE_NAME = "focus_timeline.json"

    @Volatile
    private var cache: List<TimelineEntry>? = null

    fun invalidate() {
        cache = null
    }

    fun record(context: Context, state: TimerState, id: String? = null) {
        synchronized(this) {
            val entries = load(context).toMutableList()
            entries.add(TimelineEntry(System.currentTimeMillis(), state.name, id))
            persist(context, entries)
        }
    }

    fun recordRaw(context: Context, state: String, timestamp: Long = System.currentTimeMillis(), id: String? = null) {
        synchronized(this) {
            val entries = insertEntrySorted(load(context), TimelineEntry(timestamp, state, id))
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
