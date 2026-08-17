package com.madeby.JAI

import java.text.SimpleDateFormat
import java.util.Date

/**
 * Self-contained JSON serialization for the study timeline.
 *
 * The stored representation is a JSON array of objects: {"t":<long>,"s":"<state>"[,"id":"<string>"]}.
 * This mirrors the format org.json produced for the legacy `focus_timeline` pref value, so existing
 * data remains readable. Implemented without org.json so the format is unit-testable on the JVM.
 */

internal fun timelineToJsonString(entries: List<TimelineEntry>): String {
    val sb = StringBuilder(entries.size * 60 + 2)
    sb.append('[')
    for ((idx, e) in entries.withIndex()) {
        if (idx > 0) sb.append(',')
        sb.append("{\"t\":").append(e.timestamp).append(",\"s\":")
        appendJsonString(sb, e.state)
        if (e.id != null) {
            sb.append(",\"id\":")
            appendJsonString(sb, e.id)
        }
        if (e.subId != null) {
            sb.append(",\"subId\":")
            appendJsonString(sb, e.subId)
        }
        if (e.subName != null) {
            sb.append(",\"subName\":")
            appendJsonString(sb, e.subName)
        }
        if (e.subColor != null) {
            sb.append(",\"subColor\":")
            appendJsonString(sb, e.subColor)
        }
        sb.append('}')
    }
    sb.append(']')
    return sb.toString()
}

internal fun parseTimelineJson(raw: String): List<TimelineEntry> {
    val entries = ArrayList<TimelineEntry>()
    val n = raw.length
    var i = 0

    fun skipWs() {
        while (i < n) {
            val c = raw[i]
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++ else break
        }
    }

    fun parseString(): String {
        val sb = StringBuilder()
        i++
        while (i < n) {
            val c = raw[i]
            if (c == '"') {
                i++
                return sb.toString()
            } else if (c == '\\') {
                i++
                if (i >= n) break
                when (val esc = raw[i]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'u' -> {
                        if (i + 4 < n) {
                            val code = raw.substring(i + 1, i + 5).toIntOrNull(16)
                            if (code != null) sb.append(code.toChar())
                            i += 4
                        }
                    }
                    else -> sb.append(esc)
                }
                i++
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    skipWs()
    if (i >= n || raw[i] != '[') return entries
    i++
    while (i < n) {
        skipWs()
        if (i >= n) break
        when (raw[i]) {
            ']' -> break
            ',' -> { i++; continue }
            '{' -> {}
            else -> { i++; continue }
        }
        i++
        var ts = 0L
        var state = ""
        var id: String? = null
        var subId: String? = null
        var subName: String? = null
        var subColor: String? = null
        while (i < n) {
            skipWs()
            if (i >= n) break
            when (raw[i]) {
                '}' -> { i++; break }
                ',' -> { i++; continue }
                '"' -> {}
                else -> { i++; continue }
            }
            val key = parseString()
            skipWs()
            if (i < n && raw[i] == ':') i++
            skipWs()
            if (i < n && raw[i] == '"') {
                val value = parseString()
                when (key) {
                    "s" -> state = value
                    "id" -> id = value.takeIf { it.isNotEmpty() }
                    "subId" -> subId = value.takeIf { it.isNotEmpty() }
                    "subName" -> subName = value.takeIf { it.isNotEmpty() }
                    "subColor" -> subColor = value.takeIf { it.isNotEmpty() }
                }
            } else {
                val start = i
                while (i < n) {
                    val ch = raw[i]
                    if (ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E' || ch.isDigit()) i++ else break
                }
                if (key == "t") ts = raw.substring(start, i).toLongOrNull() ?: 0L
            }
        }
        entries.add(TimelineEntry(ts, state, id, subId, subName, subColor))
    }
    return entries
}

private fun appendJsonString(sb: StringBuilder, value: String) {
    sb.append('"')
    for (c in value) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (c < ' ') {
                sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
            } else {
                sb.append(c)
            }
        }
    }
    sb.append('"')
}

internal fun insertEntrySorted(entries: List<TimelineEntry>, e: TimelineEntry): List<TimelineEntry> {
    val idx = entries.indexOfFirst { it.timestamp > e.timestamp }
    val out = ArrayList<TimelineEntry>(entries.size + 1)
    if (idx < 0) {
        out.addAll(entries)
        out.add(e)
    } else {
        out.addAll(entries.subList(0, idx))
        out.add(e)
        out.addAll(entries.subList(idx, entries.size))
    }
    return out
}

internal fun removeEntryWithTs(entries: List<TimelineEntry>, ts: Long): List<TimelineEntry> =
    entries.filter { it.timestamp != ts }

internal fun moveEntryInList(entries: List<TimelineEntry>, oldTs: Long, newTs: Long): List<TimelineEntry>? {
    val idx = entries.indexOfFirst { it.timestamp == oldTs }
    if (idx < 0) return null
    val without = ArrayList(entries)
    without.removeAt(idx)
    val moved = entries[idx].copy(timestamp = newTs)
    val insertIdx = without.indexOfFirst { it.timestamp > newTs }
    val out = ArrayList<TimelineEntry>(without.size + 1)
    if (insertIdx < 0) {
        out.addAll(without)
        out.add(moved)
    } else {
        out.addAll(without.subList(0, insertIdx))
        out.add(moved)
        out.addAll(without.subList(insertIdx, without.size))
    }
    return out
}

internal fun removeDayEntries(entries: List<TimelineEntry>, dateStr: String, fmt: SimpleDateFormat): List<TimelineEntry> =
    entries.filter { it.timestamp > 0L && fmt.format(Date(it.timestamp)) != dateStr }
