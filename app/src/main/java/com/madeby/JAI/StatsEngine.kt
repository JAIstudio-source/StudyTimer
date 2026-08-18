package com.madeby.JAI

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class StatsEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

    fun dailyGoalSecs(): Long {
        return prefs.getLong("daily_goal_secs", 2700L)
    }

    fun resolveGoalFor(dateStr: String): Long {
        return prefs.getLong("${dateStr}_goal_secs", dailyGoalSecs())
    }

    fun recalculateStreak(todayExtra: Long = 0L) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val streakGoalBased = prefs.getBoolean("streak_uses_daily_goal", false)

        val streak = StreakCalculator.currentStreak(
            todayExtra = todayExtra,
            focusOn = { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                prefs.getLong("${sdf.format(cal.time)}_focus_total", 0L)
            },
            goalFor = if (streakGoalBased) ({ daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                resolveGoalFor(sdf.format(cal.time))
            }) else null
        )

        if (streak == prefs.safeInt("current_streak", 0) && prefs.getString("streak_last_calculated", null) == todayStr) return
        prefs.edit()
            .putInt("current_streak", streak)
            .putString("streak_last_calculated", todayStr)
            .apply()
    }

    fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> {
        val entries = TimelineLogger.load(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedStart = runCatching { sdf.parse(dateStr)?.time }.getOrNull() ?: 0L
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis
        val endMs = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val dayEntries = entries.filter { it.timestamp in startMs until endMs }.sortedBy { it.timestamp }
        return dayBlocks(dateStr, dayEntries)
    }

    fun dayBlocks(dateStr: String, entries: List<TimelineEntry>): Pair<List<BlockInfo>, List<BlockInfo>> {
        val parsed = parseDayBlocks(entries)
        val sessions = ArrayList(parsed.sessions)
        val breaks = ArrayList(parsed.breaks)
        if (parsed.openFocusStart != null || parsed.openBreakStart != null) {
            val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val isToday = dateStr == dateSdf.format(Date())
            val timerState = prefs.getString("timerState", "IDLE") ?: "IDLE"
            val timerRunning = timerState == "STUDYING" || timerState == "BREAK"
            val endTs = if (isToday && timerRunning) {
                System.currentTimeMillis()
            } else {
                entries.lastOrNull()?.timestamp ?: 0L
            }
            if (endTs > 0L) {
                val fs = parsed.openFocusStart
                if (fs != null) {
                    val gapMs = endTs - fs
                    if (gapMs <= 24L * 3600_000) {
                        sessions.add(
                            BlockInfo(
                                fs,
                                endTs,
                                gapMs / 1000L,
                                isToday && timerRunning,
                                parsed.openFocusManual,
                                subjectId = parsed.openFocusSubId,
                                subjectName = parsed.openFocusSubName,
                                subjectColor = parsed.openFocusSubColor
                            )
                        )
                    }
                }
                val bs = parsed.openBreakStart
                if (bs != null) breaks.add(BlockInfo(bs, endTs, (endTs - bs) / 1000L, manual = parsed.openBreakManual))
            }
        }
        val focusManual = prefs.getLong("${dateStr}_focus_manual", 0L)
        val breakManual = prefs.getLong("${dateStr}_break_manual", 0L)
        val hasRawManualFocus = entries.any { it.state == "MANUAL_FOCUS" }
        val hasRawManualBreak = entries.any { it.state == "MANUAL_BREAK" }

        if (focusManual != 0L && !hasRawManualFocus) {
            if (sessions.isNotEmpty()) {
                val last = sessions.last()
                val adj = max(0L, last.secs + focusManual)
                sessions[sessions.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (focusManual > 0L) {
                val lastEnd = entries.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                sessions.add(BlockInfo(lastEnd, lastEnd + focusManual * 1000L, focusManual, manual = true))
            }
        }
        if (breakManual != 0L && !hasRawManualBreak) {
            if (breaks.isNotEmpty()) {
                val last = breaks.last()
                val adj = max(0L, last.secs + breakManual)
                breaks[breaks.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (breakManual > 0L) {
                val lastEnd = entries.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                breaks.add(BlockInfo(lastEnd, lastEnd + breakManual * 1000L, breakManual, manual = true))
            }
        }
        return sessions to breaks
    }

    fun reconcileDayTotals(dateStr: String) {
        val timerActive = prefs.getString("timerState", "IDLE") != "IDLE"
        if (timerActive && dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) return
        val (sessions, breaks) = dayBlocks(dateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        val breakSum = breaks.filter { !it.running }.sumOf { it.secs }
        val focusKey = "${dateStr}_focus_total"
        val breakKey = "${dateStr}_break_total"
        if (prefs.getLong(focusKey, 0L) == focusSum && prefs.getLong(breakKey, 0L) == breakSum) return
        prefs.edit()
            .putLong(focusKey, focusSum)
            .putLong(breakKey, breakSum)
            .apply()
    }

    fun forceReconcileDayTotals(dateStr: String) {
        val (sessions, breaks) = dayBlocks(dateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        val breakSum = breaks.filter { !it.running }.sumOf { it.secs }
        val focusKey = "${dateStr}_focus_total"
        val breakKey = "${dateStr}_break_total"
        prefs.edit()
            .putLong(focusKey, focusSum)
            .putLong(breakKey, breakSum)
            .apply()
    }

    fun computeStatsSnapshot(currentBreakSecs: Long = 0L): StatsSnapshot {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        reconcileDayTotals(todayStr)
        val currentSessionSecs = prefs.getLong("accumulatedStudy", 0L)

        recalculateStreak(todayExtra = currentSessionSecs)
        val streak = prefs.safeInt("current_streak", 0)

        val todayFocus = prefs.getLong("${todayStr}_focus_total", 0L) + currentSessionSecs
        val todayBreak = prefs.getLong("${todayStr}_break_total", 0L) + currentBreakSecs

        var total7 = 0L
        for (i in 0 until 7) {
            val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i)
            val d = sdf.format(cal.time)
            total7 += prefs.getLong("${d}_focus_total", 0L)
            if (d == todayStr) total7 += currentSessionSecs
        }
        val avg7 = total7 / 7

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdaySecs = prefs.getLong("${sdf.format(yesterdayCal.time)}_focus_total", 0L)

        val heroGoalSecs = resolveGoalFor(todayStr)

        val allKeys = prefs.all.keys
        val allFocusKeys = allKeys.filter { it.endsWith("_focus_total") }

        val dayFocus = HashMap<String, Long>()
        val dayBreak = HashMap<String, Long>()
        var totalLifeFocus = 0L
        var totalLifeBreak = 0L
        for (key in allFocusKeys) {
            val dStr = key.removeSuffix("_focus_total")
            val f = prefs.getLong(key, 0L)
            val b = prefs.getLong("${dStr}_break_total", 0L)
            dayFocus[dStr] = f
            dayBreak[dStr] = b
            totalLifeFocus += f
            totalLifeBreak += b
        }
        dayFocus[todayStr] = (dayFocus[todayStr] ?: 0L) + currentSessionSecs
        dayBreak[todayStr] = (dayBreak[todayStr] ?: 0L) + currentBreakSecs
        totalLifeFocus += currentSessionSecs
        totalLifeBreak += currentBreakSecs
        val totalLife = totalLifeFocus + totalLifeBreak

        val streakGoalBased = prefs.getBoolean("streak_uses_daily_goal", false)

        var longestStreak = StreakCalculator.longestStreak(
            allFocusKeys.mapNotNull { key ->
                val dStr = key.removeSuffix("_focus_total")
                val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: return@mapNotNull null
                Pair(parsed, dayFocus[dStr] ?: 0L)
            },
            goalFor = if (streakGoalBased) ({ date -> resolveGoalFor(sdf.format(date)) }) else null
        )

        val activeDays = allFocusKeys.count { k ->
            val d = k.removeSuffix("_focus_total")
            val f = dayFocus[d] ?: 0L
            val b = dayBreak[d] ?: 0L
            if (f > 0L || b > 0L) true
            else d == todayStr && (currentSessionSecs > 0L || currentBreakSecs > 0L)
        }

        val weekdayTotals = LongArray(7)
        val weekdayCounts = IntArray(7)
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            if (dStr == todayStr) continue
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val f = dayFocus[dStr] ?: 0L
            if (f > 0L) {
                val cal = Calendar.getInstance().apply { time = parsed }
                val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
                weekdayTotals[idx] += f
                weekdayCounts[idx]++
            }
        }
        val todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        if (todayFocus > 0L) {
            weekdayTotals[todayIdx] += todayFocus
            weekdayCounts[todayIdx]++
        }
        val bestWeekdayIdx = (0 until 7).maxByOrNull { idx ->
            if (weekdayCounts[idx] > 0) weekdayTotals[idx] / weekdayCounts[idx] else 0L
        } ?: 0
        val bestWeekdaySecs = if (weekdayCounts[bestWeekdayIdx] > 0) weekdayTotals[bestWeekdayIdx] / weekdayCounts[bestWeekdayIdx] else 0L
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestWeekdayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestWeekdayIdx + 1) }.time)

        val weekTotals = HashMap<String, Long>()
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val f = dayFocus[dStr] ?: 0L
            if (f <= 0L) continue
            val cal = Calendar.getInstance().apply { time = parsed }
            val weekKey = sdf.format(WeekHelper.mondayOf(cal).time)
            weekTotals[weekKey] = (weekTotals[weekKey] ?: 0L) + f
        }
        var bestWeekSecs = 0L
        var bestWeekLabel = ""
        val shortDateSdf = SimpleDateFormat("d MMM", Locale.getDefault())
        weekTotals.maxByOrNull { it.value }?.let { best ->
            if (best.value > 0L) {
                val weekStart = try { sdf.parse(best.key) } catch (_: Exception) { null } ?: Date()
                val weekEnd = Calendar.getInstance().apply { time = weekStart; add(Calendar.DAY_OF_YEAR, 6) }.time
                bestWeekSecs = best.value
                bestWeekLabel = "${shortDateSdf.format(weekStart)} \u2013 ${shortDateSdf.format(weekEnd)}"
            }
        }

        fun mondayOffset(): Int = WeekHelper.mondayOffset(Calendar.getInstance())
        fun weekFocus(weekOffset: Int): Long {
            var sum = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset() + i + weekOffset * 7) }
                sum += dayFocus[sdf.format(c.time)] ?: 0L
            }
            return sum
        }
        val thisWeek = weekFocus(0)
        val prevWeek = weekFocus(-1)

        var goalHits = 0
        for (i in 0 until 14) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(c.time)
            if ((dayFocus[dStr] ?: 0L) >= resolveGoalFor(dStr)) goalHits++
        }

        val heatmapData = HashMap<String, Long>()
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            heatmapData[dStr] = prefs.getLong(k, 0L)
        }
        if (currentSessionSecs > 0L) {
            heatmapData[todayStr] = (heatmapData[todayStr] ?: 0L) + currentSessionSecs
        }

        val showHeatmap = prefs.getBoolean("show_focus_heatmap", true)
        val showPattern = prefs.getBoolean("show_focus_pattern", true)
        val showPieChart = prefs.getBoolean("show_subject_pie_chart", true)

        val timeline = TimelineLogger.load(context)
        val nowMs = System.currentTimeMillis()
        val blockSecs7 = buildFocusBlockArray(timeline, 7, nowMs, todayStr, currentSessionSecs)
        val blockSecs30 = buildFocusBlockArray(timeline, 30, nowMs, todayStr, currentSessionSecs)
        val maxBlock7 = blockSecs7.maxOrNull() ?: 0L
        val maxBlock30 = blockSecs30.maxOrNull() ?: 0L
        val patternTotal7 = blockSecs7.sum()
        val patternTotal30 = blockSecs30.sum()

        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthAgg = LinkedHashMap<String, MonthBucket>()
        for ((dStr, f) in dayFocus) {
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val b = dayBreak[dStr] ?: 0L
            if (f <= 0L && b <= 0L) continue
            val mKey = monthSdf.format(parsed)
            val existing = monthAgg[mKey]
            monthAgg[mKey] = MonthBucket(mKey, (existing?.focus ?: 0L) + f, (existing?.breakSecs ?: 0L) + b)
        }
        val monthBuckets = monthAgg.values.toList().sortedBy { runCatching { monthSdf.parse(it.label)?.time }.getOrNull() ?: 0L }

        val allHistKeys = allFocusKeys.sortedDescending()
        val entriesByDay = timeline.groupBy { sdf.format(Date(it.timestamp)) }

        val hasAnySessions = totalLifeFocus > 0L || totalLifeBreak > 0L || currentSessionSecs > 0L || currentBreakSecs > 0L

        return StatsSnapshot(
            todayFocus, todayBreak, streak, avg7, yesterdaySecs, heroGoalSecs,
            totalLifeFocus, totalLifeBreak, totalLife, longestStreak, activeDays,
            bestWeekdayName, bestWeekdaySecs, bestWeekLabel, bestWeekSecs,
            thisWeek, prevWeek, goalHits, hasAnySessions,
            showHeatmap, showPattern, showPieChart, heatmapData, blockSecs7, maxBlock7, blockSecs30, maxBlock30,
            patternTotal7, patternTotal30,
            dayFocus, monthBuckets, allHistKeys, entriesByDay
        )
    }

    private fun buildFocusBlockArray(
        timeline: List<TimelineEntry>,
        windowDays: Int,
        nowMs: Long,
        todayStr: String,
        currentSessionSecs: Long
    ): LongArray {
        val arr = LongArray(12)
        val windowStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -windowDays)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        fun addFocusInterval(startMs: Long, endMs: Long) {
            var s = max(startMs, windowStart)
            val e = min(endMs, nowMs)
            while (s < e) {
                val cal = Calendar.getInstance().apply { timeInMillis = s }
                val block = cal.get(Calendar.HOUR_OF_DAY) / 2
                val nextHour = Calendar.getInstance().apply {
                    timeInMillis = s
                    add(Calendar.HOUR_OF_DAY, 1)
                    set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val segEnd = min(e, nextHour)
                arr[block] += (segEnd - s) / 1000L
                s = segEnd
            }
        }
        for (i in 1 until timeline.size) {
            if (timeline[i - 1].state != "STUDYING" && timeline[i - 1].state != "MANUAL_FOCUS") continue
            addFocusInterval(timeline[i - 1].timestamp, timeline[i].timestamp)
        }
        timeline.lastOrNull()?.let { last ->
            if (last.state == "STUDYING" || last.state == "MANUAL_FOCUS") addFocusInterval(last.timestamp, nowMs)
        }
        val manualFocusToday = prefs.getLong("${todayStr}_focus_manual", 0L)
        if (manualFocusToday != 0L) {
            if (manualFocusToday > 0L) {
                addFocusInterval(nowMs - manualFocusToday * 1000L, nowMs)
            } else {
                val calNow = Calendar.getInstance()
                val block = calNow.get(Calendar.HOUR_OF_DAY) / 2
                arr[block] = max(0L, arr[block] + manualFocusToday)
            }
        }
        return arr
    }

    fun buildHeatmapData(): Map<String, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val data = HashMap<String, Long>()
        for (k in prefs.all.keys) {
            if (k.endsWith("_focus_total")) {
                data[k.removeSuffix("_focus_total")] = prefs.getLong(k, 0L)
            }
        }
        val currentSessionSecs = prefs.getLong("accumulatedStudy", 0L)
        if (currentSessionSecs > 0L) {
            data[todayStr] = (data[todayStr] ?: 0L) + currentSessionSecs
        }
        return data
    }

    fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val base = try { sdf.parse(dateStr) } catch (_: Exception) { null } ?: Date()
        val c = Calendar.getInstance().apply { time = base }
        c.set(Calendar.HOUR_OF_DAY, h)
        c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) {
        val startState = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
        TimelineLogger.replaceBlock(context, block.startMs, block.endMs, newStartMs, newEndMs, startState)

        if (!isBreak) {
            val oldSecs = block.secs
            val newSecs = (newEndMs - newStartMs) / 1000L
            val delta = newSecs - oldSecs
            val selectedSubject = SubjectTagManager.getSelectedSubject(context)
            if (delta != 0L) {
                SubjectTagManager.recordSubjectStudyTime(context, selectedSubject.id, delta, dateStr)
            }
        }

        prefs.edit()
            .putLong("${dateStr}_focus_manual", 0L)
            .putLong("${dateStr}_break_manual", 0L)
            .apply()
        forceReconcileDayTotals(dateStr)
        recalculateStreak()
    }
}

internal data class ParsedDay(
    val sessions: List<BlockInfo>,
    val breaks: List<BlockInfo>,
    val openFocusStart: Long?,
    val openFocusManual: Boolean,
    val openFocusSubId: String?,
    val openFocusSubName: String?,
    val openFocusSubColor: String?,
    val openBreakStart: Long?,
    val openBreakManual: Boolean
)

internal fun parseDayBlocks(entries: List<TimelineEntry>): ParsedDay {
    val sessions = ArrayList<BlockInfo>()
    val breaks = ArrayList<BlockInfo>()
    var fs: Long? = null
    var fsManual = false
    var fsSubId: String? = null
    var fsSubName: String? = null
    var fsSubColor: String? = null
    var bs: Long? = null
    var bsManual = false
    for (e in entries) {
        when (e.state) {
            "STUDYING", "MANUAL_FOCUS" -> {
                if (fs != null) {
                    val gapMs = e.timestamp - fs
                    if (gapMs <= 24L * 3600_000) {
                        sessions.add(BlockInfo(fs, e.timestamp, gapMs / 1000L, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor))
                    }
                }
                if (bs != null) breaks.add(BlockInfo(bs, e.timestamp, (e.timestamp - bs) / 1000L, manual = bsManual))
                bs = null
                bsManual = false
                fs = e.timestamp
                fsManual = e.state == "MANUAL_FOCUS"
                fsSubId = e.subId
                fsSubName = e.subName
                fsSubColor = e.subColor
            }
            "BREAK", "MANUAL_BREAK" -> {
                if (fs != null) sessions.add(BlockInfo(fs, e.timestamp, (e.timestamp - fs) / 1000L, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor))
                fs = null
                fsManual = false
                fsSubId = null
                fsSubName = null
                fsSubColor = null
                if (bs == null) {
                    bs = e.timestamp
                    bsManual = e.state == "MANUAL_BREAK"
                }
            }
            "IDLE" -> {
                if (fs != null) sessions.add(BlockInfo(fs, e.timestamp, (e.timestamp - fs) / 1000L, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor))
                fs = null
                fsManual = false
                fsSubId = null
                fsSubName = null
                fsSubColor = null
                if (bs != null) breaks.add(BlockInfo(bs, e.timestamp, (e.timestamp - bs) / 1000L, manual = bsManual))
                bs = null
                bsManual = false
            }
        }
    }
    return ParsedDay(sessions, breaks, fs, fsManual, fsSubId, fsSubName, fsSubColor, bs, bsManual)
}
