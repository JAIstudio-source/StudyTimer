package com.madeby.JAI

data class MonthBucket(val label: String, val focus: Long, val breakSecs: Long)

data class BlockInfo(
    val startMs: Long,
    val endMs: Long,
    val secs: Long,
    val running: Boolean = false,
    val manual: Boolean = false,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val subjectColor: String? = null
)

data class StatsSnapshot(
    val todayFocus: Long,
    val todayBreak: Long,
    val streak: Int,
    val avg7: Long,
    val yesterdaySecs: Long,
    val heroGoalSecs: Long,
    val totalLifeFocus: Long,
    val totalLifeBreak: Long,
    val totalLife: Long,
    val longestStreak: Int,
    val activeDays: Int,
    val bestWeekdayName: String,
    val bestWeekdaySecs: Long,
    val bestWeekLabel: String,
    val bestWeekSecs: Long,
    val thisWeek: Long,
    val prevWeek: Long,
    val goalHits: Int,
    val hasAnySessions: Boolean,
    val showHeatmap: Boolean,
    val showPattern: Boolean,
    val showPieChart: Boolean,
    val heatmapData: Map<String, Long>,
    val blockSecs7: LongArray,
    val maxBlock7: Long,
    val blockSecs30: LongArray,
    val maxBlock30: Long,
    val patternTotal7: Long,
    val patternTotal30: Long,
    val dayFocus: Map<String, Long>,
    val monthBuckets: List<MonthBucket>,
    val allHistKeys: List<String>,
    val entriesByDay: Map<String, List<TimelineEntry>>
)
