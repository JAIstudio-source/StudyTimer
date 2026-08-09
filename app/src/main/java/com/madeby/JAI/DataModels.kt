package com.madeby.JAI

enum class AppPanel { FOCUS, STATS, SETTINGS, HEATMAP }
enum class TimerState { IDLE, STUDYING, BREAK, PAUSED, LECTURE_ENDED }
enum class AppStatsTab { OVERVIEW, TIMELINE, PLANNER }
enum class AppSettingsTab { SIMPLE, THEME }

data class PlannerGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val targetMinutes: Int = 0,
    val completed: Boolean = false,
    val checkedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

typealias SessionGoal = PlannerGoal

data class PlannerGoalSnapshot(
    val goalId: String,
    val title: String,
    val targetMinutes: Int,
    val completed: Boolean,
    val checkedAt: Long = 0L,
    val isAchieved: Boolean = true
)

data class PlannerDayRecord(
    val date: String,
    val goalSnapshots: List<PlannerGoalSnapshot>
)

data class LectureScheduleItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val startTime: String, // "HH:mm" e.g. "10:00"
    val endTime: String,   // "HH:mm" e.g. "11:00"
    val enabled: Boolean = true
)



