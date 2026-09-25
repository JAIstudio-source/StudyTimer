package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class PlannerPanelBuilder(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val statsEngine get() = host.statsEngine
    private val dateKeyFmt get() = host.dateKeyFmt
    private val cachedTodayStr get() = host.cachedTodayStr
    private var statsSnapshotCache
        get() = host.statsSnapshotCache
        set(value) { host.statsSnapshotCache = value }

    private fun dp(v: Int): Int = host.dp(v)
    private fun dp(v: Float): Int = host.dp(v.toInt())
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)
    private fun rippleBackground(color: Int) = host.rippleBackground(color)
    private fun computeStatsSnapshot() = host.computeStatsSnapshot()
    private fun resolvePlannerColors() = host.resolvePlannerColors()
    private fun resolveGoalFor(dateStr: String) = host.resolveGoalFor(dateStr)
    private fun loadSessionGoalsFromJson(jsonStr: String) = host.loadSessionGoalsFromJson(jsonStr)
    private fun saveSessionGoalsToJson(goals: List<SessionGoal>) = host.saveSessionGoalsToJson(goals)
    private fun getStatusBarHeight() = host.getStatusBarHeight()
    private fun showSubjectPickerDialog() = host.showSubjectPickerDialog()
    private fun showAddCustomSubjectDialog(onCreated: ((SubjectTag) -> Unit)? = null) = host.showAddCustomSubjectDialog(onCreated)

    internal fun renderPlannerTabContent(parent: LinearLayout, snap: StatsSnapshot) {
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val goalsJson = prefs.host.getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }
        val dailySubjectDurations = SubjectTagManager.(host, todayStr)
        val todayFocusSecs = snap.todayFocus

        val progressMap = PlannerHistoryManager.calculateGoalProgress(goalsList, todayFocusSecs, dailySubjectDurations)

        fun reloadPlanner() {
            val curSnap = statsSnapshotCache ?: computeStatsSnapshot().also { statsSnapshotCache = it }
            parent.removeAllViews()
            renderPlannerTabContent(parent, curSnap)
        }

        // Auto-complete goals (subject-tagged or untagged) whose target time has reached
        var goalsUpdated = false
        val activeGoalsList = goalsList.map { goal ->
            val prog = progressMap[goal.id]
            if (goal.targetMinutes > 0 && prog != null && prog.isAchieved && !goal.completed) {
                goalsUpdated = true
                goal.copy(completed = true, checkedAt = System.currentTimeMillis())
            } else {
                goal
            }
        }
        if (goalsUpdated) {
            saveSessionGoalsToJson(activeGoalsList)
            PlannerHistoryManager.snapshotToday(this, activeGoalsList)
        }

        val trulyAchievedIds = mutableSetOf<String>()
        for (goal in activeGoalsList) {
            val prog = progressMap[goal.id]
            val targetMins = goal.targetMinutes
            val actualMins = prog?.actualMinutes ?: 0
            if (targetMins > 0) {
                if (actualMins >= targetMins) {
                    trulyAchievedIds.add(goal.id)
                }
            } else if (goal.completed) {
                trulyAchievedIds.add(goal.id)
            }
        }

        val completedCount = trulyAchievedIds.size
        val totalCount = activeGoalsList.size
        val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        val summaryCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
        }

        val topRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(host).apply {
            text = "DAILY GOALS"
            setTextColor(plannerPrimary)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.15f
        })
        topRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })

        val addBtn = TextView(host).apply {
            text = "+ Add Goal"
            setTextColor(plannerPrimary)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 12f)
            setPadding(dp(14), dp(7), dp(14), dp(7))
            setOnClickListener { showAddSessionGoalDialog() }
        }
        topRow.addView(addBtn)
        summaryCard.addView(topRow)

        val isGoalReached = progressPct >= 100 && totalCount > 0
        val lineProgressColor = if (isGoalReached) 0xFF43D36E.toInt() else plannerPrimary

        val progressRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(12), 0, dp(6)) }
        progressRow.addView(TextView(host).apply {
            tag = "planner_completed_text"
            text = "$completedCount of $totalCount completed"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        progressRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        progressRow.addView(TextView(host).apply {
            tag = "planner_pct_text"
            text = "$progressPct%"
            setTextColor(lineProgressColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        summaryCard.addView(progressRow)

        val progressBar = ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal).apply {
            tag = "planner_progress_bar"
            max = 100
            progress = progressPct
            progressTintList = android.content.res.ColorStateList.valueOf(lineProgressColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(if (themeCoordinator.isDarkMode()) 0xFF222430.toInt() else 0xFFE2E8F0.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { setMargins(0, 0, 0, dp(14)) }
        }
        summaryCard.addView(progressBar)

        val actionsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 0)
        }
        val yesterdayActionBtn = TextView(host).apply {
            text = "Yesterday's Goals"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 35), 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(4), 0)
            }
            setOnClickListener { showEditYesterdayGoalsDialog() }
        }
        val gridBtn = TextView(host).apply {
            text = "Goal History Grid"
            setTextColor(plannerPrimary)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, 0, 0)
            }
            setOnClickListener { showPlannerMatrixDialog() }
        }
        actionsRow.addView(yesterdayActionBtn)
        actionsRow.addView(gridBtn)
        summaryCard.addView(actionsRow)
        parent.addView(summaryCard)

        if (activeGoalsList.isEmpty()) {
            val emptyCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = themeCoordinator.createCardBackground()
                setPadding(dp(20), dp(32), dp(20), dp(32))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            emptyCard.addView(TextView(host).apply {
                text = "No Goals Set for Today"
                setTextColor(themeCoordinator.textColor)
                textSize = 16f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
            })
            emptyCard.addView(TextView(host).apply {
                text = "Set target study times for your subjects or add daily tasks to stay on track."
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                textSize = 13.5f
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(4), dp(16), dp(16))
            })

            val addFirstBtn = TextView(host).apply {
                text = "+ Add Daily Goal"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(plannerPrimary)
                }
                setPadding(dp(18), dp(10), dp(18), dp(10))
                gravity = Gravity.CENTER
                setOnClickListener { showAddSessionGoalDialog() }
            }
            emptyCard.addView(addFirstBtn)
            parent.addView(emptyCard)
        } else {
            val goalsContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
            }

            class GoalDisplayInfo(
                val isChecked: Boolean,
                val progressText: String,
                val chipColor: Int,
                val isDeficit: Boolean
            )

            val infoMap = mutableMapOf<String, GoalDisplayInfo>()
            val greenColor = 0xFF22C55E.toInt()
            val redColor = 0xFFEF4444.toInt()

            for (goal in activeGoalsList) {
                val targetMins = goal.targetMinutes
                val prog = progressMap[goal.id]
                val actualMins = prog?.actualMinutes ?: 0

                if (targetMins > 0) {
                    val isDone = actualMins >= targetMins
                    if (isDone) {
                        infoMap[goal.id] = GoalDisplayInfo(
                            isChecked = true,
                            progressText = "${actualMins}/${targetMins}m",
                            chipColor = greenColor,
                            isDeficit = false
                        )
                    } else if (goal.completed) {
                        val deficit = targetMins - actualMins
                        infoMap[goal.id] = GoalDisplayInfo(
                            isChecked = true,
                            progressText = "${actualMins}/${targetMins}m (${deficit}m left)",
                            chipColor = redColor,
                            isDeficit = true
                        )
                    } else {
                        infoMap[goal.id] = GoalDisplayInfo(
                            isChecked = false,
                            progressText = "${actualMins}/${targetMins}m",
                            chipColor = themeCoordinator.primaryColor,
                            isDeficit = false
                        )
                    }
                } else {
                    infoMap[goal.id] = GoalDisplayInfo(
                        isChecked = goal.completed,
                        progressText = if (goal.completed) "Done" else "Mark Done",
                        chipColor = if (goal.completed) greenColor else themeCoordinator.primaryColor,
                        isDeficit = false
                    )
                }
            }

            for (goal in activeGoalsList) {
                val info = infoMap[goal.id] ?: continue
                val isChecked = info.isChecked
                val targetMins = goal.targetMinutes

                val goalCard = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }

                val checkBtn = TextView(host).apply {
                    tag = "goal_check_${goal.id}"
                    text = if (isChecked) (if (info.isDeficit) "\u2715" else "\u2713") else ""
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isChecked) (if (info.isDeficit) redColor else greenColor) else Color.TRANSPARENT)
                        setStroke(dp(2), if (isChecked) (if (info.isDeficit) redColor else greenColor) else themeCoordinator.textColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { setMargins(0, 0, dp(12), 0) }
                    setOnClickListener {
                        val now = System.currentTimeMillis()
                        val updated = activeGoalsList.map {
                            if (it.id == goal.id) it.copy(completed = !it.completed, checkedAt = if (!it.completed) now else 0L) else it
                        }
                        saveSessionGoalsToJson(updated)
                        PlannerHistoryManager.snapshotToday(host, updated)
                        reloadPlanner()
                    }
                }
                goalCard.addView(checkBtn)

                val textCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { showGoalHistoryDialog(goal) }
                }
                textCol.addView(TextView(host).apply {
                    text = goal.title
                    setTextColor(if (isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    if (isChecked) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                })
                if (!goal.subjectId.isNullOrBlank() && goal.subjectId != "all") {
                    val sub = SubjectTagManager.(host, goal.subjectId)
                    val subBadge = TextView(host).apply {
                        text = "${sub.iconEmoji} ${sub.name}"
                        setTextColor(try { Color.parseColor(sub.colorHex) } catch (_: Exception) { plannerPrimary })
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(0, dp(2), 0, 0)
                    }
                    textCol.addView(subBadge)
                }
                if (goal.note.isNotBlank()) {
                    textCol.addView(TextView(host).apply {
                        text = goal.note
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.7f
                        textSize = 13.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                goalCard.addView(textCol)

                if (targetMins > 0) {
                    val chipView = TextView(host).apply {
                        tag = "goal_chip_${goal.id}"
                        text = info.progressText
                        setTextColor(info.chipColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(info.chipColor, 100), 10f)
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6), 0, dp(6), 0) }
                        setOnClickListener { showGoalHistoryDialog(goal) }
                    }
                    goalCard.addView(chipView)
                }

                // Hold & Drag Reorder Handle
                if (activeGoalsList.size > 1) {
                    val gripBtn = TextView(host).apply {
                        text = "⠿"
                        textSize = 17f
                        setTextColor(tintedColor(themeCoordinator.textColor, 70))
                        setPadding(dp(6), dp(4), dp(4), dp(4))
                    }
                    goalCard.addView(gripBtn)

                    // Enable Hold & Drag on the goal card
                    goalCard.setOnLongClickListener { v ->
                        val clipData = android.content.ClipData.newPlainText("goal_id", goal.id)
                        val shadow = View.DragShadowBuilder(v)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            v.startDragAndDrop(clipData, shadow, goal.id, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            v.startDrag(clipData, shadow, goal.id, 0)
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        true
                    }

                    goalCard.setOnDragListener { v, event ->
                        when (event.action) {
                            android.view.DragEvent.ACTION_DRAG_STARTED -> true
                            android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                                v.alpha = 0.55f
                                true
                            }
                            android.view.DragEvent.ACTION_DRAG_EXITED -> {
                                v.alpha = 1.0f
                                true
                            }
                            android.view.DragEvent.ACTION_DROP -> {
                                v.alpha = 1.0f
                                val draggedGoalId = event.localState as? String
                                if (!draggedGoalId.isNullOrEmpty() && draggedGoalId != goal.id) {
                                    val fromIdx = activeGoalsList.indexOfFirst { it.id == draggedGoalId }
                                    val toIdx = activeGoalsList.indexOfFirst { it.id == goal.id }
                                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                        val mutable = activeGoalsList.toMutableList()
                                        val moved = mutable.removeAt(fromIdx)
                                        mutable.add(toIdx, moved)
                                        saveSessionGoalsToJson(mutable)
                                        PlannerHistoryManager.snapshotToday(host, mutable)
                                        reloadPlanner()
                                    }
                                }
                                true
                            }
                            android.view.DragEvent.ACTION_DRAG_ENDED -> {
                                v.alpha = 1.0f
                                true
                            }
                            else -> true
                        }
                    }
                }

                val deleteBtn = TextView(host).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 16f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        showConfirmDialog(
                            title = "Delete Goal?",
                            message = "Are you sure you want to remove '${goal.title}' from your planner?"
                        ) {
                            val updated = activeGoalsList.filter { it.id != goal.id }
                            saveSessionGoalsToJson(updated)
                            PlannerHistoryManager.snapshotToday(host, updated)
                            reloadPlanner()
                        }
                    }
                }
                goalCard.addView(deleteBtn)
                goalsContainer.addView(goalCard)
            }
            parent.addView(goalsContainer)
        }

        // Planner Insights Section (always computed)
        val overallInsights = PlannerHistoryManager.computeOverallPlannerInsights(this, activeGoalsList)

        parent.addView(createSectionLabel("Planner Insights"))

        val insightsCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(12)) }
        }

        fun addInsightRow(icon: String, title: String, value: String, sub: String) {
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(TextView(host).apply {
                text = icon
                textSize = 20f
                setPadding(0, 0, dp(12), 0)
            })
            val textCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(host).apply {
                text = title
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            textCol.addView(TextView(host).apply {
                text = sub
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                textSize = 13f
                setPadding(0, dp(2), 0, 0)
            })
            row.addView(textCol)
            row.addView(TextView(host).apply {
                text = value
                setTextColor(plannerSecondary)
                textSize = 16f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            insightsCard.addView(row)
        }

        addInsightRow("🔥", "Best Streak", if (overallInsights.bestStreakDays > 0) "${overallInsights.bestStreakDays}d" else "0d", if (overallInsights.bestStreakDays > 0) overallInsights.bestStreakGoalTitle else "No streak yet")
        insightsCard.addView(createDivider())
        addInsightRow("📈", "Most Consistent", if (overallInsights.mostConsistentPct > 0) "${overallInsights.mostConsistentPct}%" else "0%", if (overallInsights.mostConsistentPct > 0) overallInsights.mostConsistentGoalTitle else "No track yet")
        insightsCard.addView(createDivider())

        val isDark = themeCoordinator.isDarkMode()
        val matrixBtn = TextView(host).apply {
            text = "📊 Goal & Habit Grid"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 14f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            setOnClickListener { showPlannerMatrixDialog() }
        }
        insightsCard.addView(matrixBtn)

        val themeBtn = TextView(host).apply {
            text = "🎨 Planner Theme"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 14f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            setOnClickListener { showPlannerThemePickerDialog() }
        }
        insightsCard.addView(themeBtn)

        parent.addView(insightsCard)
    }

    internal fun showGoalHistoryDialog(goal: PlannerGoal, displayMonthOffset: Int = 0) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val historyDetailedMap = PlannerHistoryManager.loadGoalHistoryDetailed(this, goal.id)
        val insights = PlannerHistoryManager.computeGoalInsights(this, goal.id)
        var currentOffset = displayMonthOffset

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // 1. Scrollable Header Box for Goal Title and Optional Note ONLY
        val maxHeaderHeight = dp(110)
        val headerScroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(4))
            }
            isVerticalScrollBarEnabled = true
        }

        val headerBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        headerBox.addView(TextView(host).apply {
            text = goal.title
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        if (goal.note.isNotBlank()) {
            headerBox.addView(TextView(host).apply {
                text = goal.note
                setTextColor(themeCoordinator.textColor)
                alpha = 0.8f
                textSize = 13f
                setLineSpacing(dp(2).toFloat(), 1.1f)
                setPadding(0, dp(4), 0, 0)
            })
        }

        headerScroll.addView(headerBox)

        // Restrict headerScroll height to maxHeaderHeight if content exceeds it
        headerScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                headerScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (headerScroll.height > maxHeaderHeight) {
                    headerScroll.layoutParams = (headerScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxHeaderHeight
                    }
                }
            }
        })
        content.addView(headerScroll)

        // 2. FIXED Stats Summary Chips Row
        val statsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(10))
        }
        fun makeChip(icon: String, textVal: String) {
            val chip = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isDark) {
                    themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 100), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
            }
            if (icon.isNotBlank()) {
                chip.addView(TextView(host).apply { text = icon; textSize = 13f; setPadding(0, 0, dp(4), 0) })
            }
            chip.addView(TextView(host).apply {
                text = textVal
                setTextColor(if (isDark) themeCoordinator.textColor else 0xFF0F172A.toInt())
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            statsRow.addView(chip)
        }
        makeChip("", "${insights.currentStreak}d streak")
        makeChip("", "${(insights.completionRate * 100).toInt()}% complete")
        makeChip("", "${insights.completedDays}/${insights.totalDaysTracked} days")
        content.addView(statsRow)

        content.addView(createDivider())

        // 3. FIXED Calendar Grid View Container
        val calendarContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateKeys = historyDetailedMap.keys.mapNotNull { runCatching { sdf.parse(it) }.getOrNull() }
        val minCal = Calendar.getInstance()
        if (dateKeys.isNotEmpty()) {
            minCal.time = dateKeys.minOrNull()!!
        } else {
            minCal.timeInMillis = goal.createdAt
        }
        val todayCal = Calendar.getInstance()
        val minOffset = (minCal.get(Calendar.YEAR) - todayCal.get(Calendar.YEAR)) * 12 + (minCal.get(Calendar.MONTH) - todayCal.get(Calendar.MONTH))
        val maxOffset = 0

        fun updateCalendarView(offset: Int) {
            calendarContainer.removeAllViews()
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, offset)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthLabelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            // Calendar header with navigation
            val calNavRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(6))
            }
            calNavRow.addView(TextView(host).apply {
                text = "‹"
                textSize = 24f
                setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                alpha = if (currentOffset <= minOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset > minOffset) {
                        currentOffset--
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calNavRow.addView(TextView(host).apply {
                text = monthLabelSdf.format(cal.time)
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            calNavRow.addView(TextView(host).apply {
                text = "›"
                textSize = 24f
                setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                alpha = if (currentOffset >= maxOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset < maxOffset) {
                        currentOffset++
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calendarContainer.addView(calNavRow)

            // Day of week labels
            val weekdaysRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(6)) }
            val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
            for (day in daysOfWeek) {
                weekdaysRow.addView(TextView(host).apply {
                    text = day
                    setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            calendarContainer.addView(weekdaysRow)

            // Days Grid
            val gridCal = cal.clone() as Calendar
            gridCal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) - 1
            val maxDaysInMonth = gridCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val todayStr = sdf.format(Date())

            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > maxDaysInMonth) break
                val weekRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(3), 0, dp(3)) }
                for (col in 0..6) {
                    if ((week == 0 && col < firstDayOfWeek) || dayCounter > maxDaysInMonth) {
                        weekRow.addView(View(host).apply { layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f) })
                    } else {
                        val currCal = cal.clone() as Calendar
                        currCal.set(Calendar.DAY_OF_MONTH, dayCounter)
                        val dateKey = sdf.format(currCal.time)
                        val status = historyDetailedMap[dateKey]
                        val isToday = dateKey == todayStr

                        val dayCell = FrameLayout(host).apply {
                            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
                            val circle = View(host).apply {
                                layoutParams = FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER)
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    when (status) {
                                        GoalHistoryStatus.ACHIEVED -> {
                                            setColor(if (isDark) 0xFF22C55E.toInt() else 0xFF10B981.toInt())
                                        }
                                        GoalHistoryStatus.DEFICIT -> {
                                            setColor(if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt())
                                        }
                                        GoalHistoryStatus.NOT_COMPLETED -> {
                                            setColor(if (isDark) 0x33EF4444.toInt() else 0x22EF4444.toInt())
                                            setStroke(dp(1), if (isDark) 0x66EF4444.toInt() else 0x4DEF4444.toInt())
                                        }
                                        null -> {
                                            if (isToday) {
                                                setColor(if (isDark) tintedColor(plannerSecondary, 40) else 0x1A0284C7.toInt())
                                                setStroke(dp(2), if (isDark) plannerSecondary else 0xFF0284C7.toInt())
                                            } else {
                                                setColor(Color.TRANSPARENT)
                                            }
                                        }
                                    }
                                }
                            }
                            addView(circle)
                            addView(TextView(host).apply {
                                text = when (status) {
                                    GoalHistoryStatus.ACHIEVED -> "\u2713"
                                    GoalHistoryStatus.DEFICIT -> "\u2715"
                                    else -> dayCounter.toString()
                                }
                                textSize = 12f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                gravity = Gravity.CENTER
                                setTextColor(
                                    when (status) {
                                        GoalHistoryStatus.ACHIEVED, GoalHistoryStatus.DEFICIT -> 0xFFFFFFFF.toInt()
                                        GoalHistoryStatus.NOT_COMPLETED -> if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()
                                        null -> if (isToday) (if (isDark) plannerSecondary else 0xFF0284C7.toInt()) else (if (isDark) 0xCCFFFFFF.toInt() else 0xFF334155.toInt())
                                    }
                                )
                                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            })
                        }
                        weekRow.addView(dayCell)
                        dayCounter++
                    }
                }
                calendarContainer.addView(weekRow)
            }
        }

        updateCalendarView(currentOffset)
        content.addView(calendarContainer)

        // 4. FIXED Legend Row
        val legendRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(8))
        }
        legendRow.addView(TextView(host).apply {
            text = "● Goal Met   ✕ Incomplete   ○ Missed"
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            textSize = 11f
        })
        content.addView(legendRow)

        // 5. FIXED Action Buttons Row at Bottom (Edit Goal + Close)
        val buttonRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }

        val editBtn = TextView(host).apply {
            text = "Edit Goal"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(plannerPrimary)
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener {
                dialog.dismiss()
                showEditSessionGoalDialog(goal)
            }
        }
        buttonRow.addView(editBtn)

        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonRow.addView(closeBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showEditYesterdayGoalsDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val yesterdayDisplayStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(cal.time)

        val (plannerPrimary, _) = resolvePlannerColors()
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dailySubjectDurations = SubjectTagManager.(host, yesterdayStr)
        val dayFocusSecs = prefs.getLong("${yesterdayStr}_focus_total", 0L)
        val yesterdayFocusMins = (dayFocusSecs / 60).toInt()

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        // Header Title
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        headerRow.addView(TextView(host).apply {
            text = "Yesterday's Goals"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val closeBtn = TextView(host).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        root.addView(TextView(host).apply {
            text = "$yesterdayDisplayStr • ${yesterdayFocusMins / 60}h ${yesterdayFocusMins % 60}m studied"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setPadding(0, 0, 0, dp(12))
        })

        data class YesterdayGoalItem(
            var id: String,
            var title: String,
            var targetMinutes: Int,
            var completed: Boolean,
            var isAchieved: Boolean,
            var subjectId: String?
        )

        val itemsList = mutableListOf<YesterdayGoalItem>()
        val existingSnapshots = PlannerHistoryManager.loadDaySnapshot(this, yesterdayStr)
        if (existingSnapshots.isNotEmpty()) {
            for (s in existingSnapshots) {
                itemsList.add(YesterdayGoalItem(s.goalId, s.title, s.targetMinutes, s.completed, s.isAchieved, s.subjectId))
            }
        } else {
            val currentGoals = loadSessionGoalsFromJson(prefs.host.getString("session_goals_json", "[]") ?: "[]")
            val progressMap = PlannerHistoryManager.calculateGoalProgress(currentGoals, yesterdayFocusMins * 60L, dailySubjectDurations)
            for (g in currentGoals) {
                val prog = progressMap[g.id]
                val autoDone = if (g.targetMinutes > 0) (prog?.isAchieved ?: false) else g.completed
                itemsList.add(YesterdayGoalItem(g.id, g.title, g.targetMinutes, autoDone, autoDone, g.subjectId))
            }
        }

        val scroll = ScrollView(host).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (host.resources.displayMetrics.heightPixels * 0.40f).toInt()
            )
        }

        val goalsContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(goalsContainer)

        fun renderGoals() {
            goalsContainer.removeAllViews()
            if (itemsList.isEmpty()) {
                goalsContainer.addView(TextView(host).apply {
                    text = "No goals recorded for yesterday. Tap below to add any goals you completed."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(24), dp(16), dp(24))
                })
            }
            for ((idx, item) in itemsList.withIndex()) {
                val card = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                }

                val toggleBadge = TextView(host).apply {
                    text = if (item.completed) "Done" else "Not Done"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (item.completed) Color.parseColor("#10B981") else Color.parseColor("#475569"))
                    }
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener {
                        item.completed = !item.completed
                        item.isAchieved = item.completed
                        renderGoals()
                    }
                }
                card.addView(toggleBadge)

                val textCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(10), 0, dp(8), 0)
                    }
                }
                textCol.addView(TextView(host).apply {
                    text = item.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val target = item.targetMinutes
                val actual = if (!item.subjectId.isNullOrBlank() && item.subjectId != "all") {
                    ((dailySubjectDurations[item.subjectId] ?: 0L) / 60).toInt()
                } else {
                    yesterdayFocusMins
                }
                val subText = if (target > 0) "Target: ${target}m • Studied: ${actual}m" else "Daily Goal"
                textCol.addView(TextView(host).apply {
                    text = subText
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })
                card.addView(textCol)

                val removeBtn = TextView(host).apply {
                    text = "✕"
                    textSize = 14f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        itemsList.removeAt(idx)
                        renderGoals()
                    }
                }
                card.addView(removeBtn)
                goalsContainer.addView(card)
            }
        }
        renderGoals()

        root.addView(scroll)

        val addRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(12))
        }

        val addHabitBtn = TextView(host).apply {
            text = "+ Add Yesterday's Goal"
            setTextColor(plannerPrimary)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 35), 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val inputDialog = Dialog(host)
                inputDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val inputRoot = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(24f)
                    setPadding(dp(20), dp(18), dp(20), dp(18))
                }
                val titleView = TextView(host).apply {
                    text = "Add Yesterday's Goal"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, 0, 0, dp(12))
                }
                val inputEdit = EditText(host).apply {
                    hint = "Goal name (e.g. Math homework)"
                    setTextColor(themeCoordinator.textColor)
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                }
                val btnRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }
                val cancelBtn = Button(host).apply {
                    text = "Cancel"
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    textSize = 12f
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                    setOnClickListener { inputDialog.dismiss() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                val addBtn = Button(host).apply {
                    text = "Add Goal"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(themeCoordinator.primaryColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38))
                    setOnClickListener {
                        val t = inputEdit.text.toString().trim()
                        if (t.isNotEmpty()) {
                            itemsList.add(YesterdayGoalItem("custom_${System.currentTimeMillis()}", t, 30, true, true, null))
                            renderGoals()
                        }
                        inputDialog.dismiss()
                    }
                }
                btnRow.addView(cancelBtn)
                btnRow.addView(addBtn)
                inputRoot.addView(titleView)
                inputRoot.addView(inputEdit)
                inputRoot.addView(btnRow)
                inputDialog.setContentView(inputRoot)
                inputDialog.window?.apply {
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                inputDialog.show()
            }
        }
        addRow.addView(addHabitBtn)
        root.addView(addRow)

        val saveBtn = Button(host).apply {
            text = "SAVE YESTERDAY'S GOALS"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(plannerPrimary)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val array = org.json.JSONArray()
                for (item in itemsList) {
                    val target = item.targetMinutes
                    val actual = if (!item.subjectId.isNullOrBlank() && item.subjectId != "all") {
                        ((dailySubjectDurations[item.subjectId] ?: 0L) / 60).toInt()
                    } else {
                        yesterdayFocusMins
                    }
                    val isAchieved = if (item.completed) (target == 0 || actual >= target) else false
                    array.put(org.json.JSONObject().apply {
                        put("goalId", item.id)
                        put("title", item.title)
                        put("targetMinutes", item.targetMinutes)
                        put("completed", item.completed)
                        put("checkedAt", if (item.completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L)
                        put("isAchieved", isAchieved)
                        if (item.subjectId != null) put("subjectId", item.subjectId)
                    })
                }

                prefs.edit().putString("${yesterdayStr}_planner_snapshot", array.toString()).apply()
                statsDirty = true
                tabPageCache.clear()
                recalculateStreak()
                refreshStatsPanel()

                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(host)
                    }
                }.start()

                Toast.makeText(host, "Yesterday's goals updated!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        root.addView(saveBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun migrateHistoricalDailyGoals(context: Context) {
        val prefs = context.host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val globalGoal = (prefs.all["daily_goal_secs"] as? Number)?.toLong() ?: 2700L
        val editor = prefs.edit()
        var modified = false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (key in prefs.all.keys) {
            if (key.endsWith("_focus_total")) {
                val dStr = key.removeSuffix("_focus_total")
                val goalKey = "${dStr}_goal_secs"
                if (!prefs.contains(goalKey)) {
                    editor.putLong(goalKey, globalGoal)
                    modified = true
                }
            }
        }
        if (!prefs.contains("${todayStr}_goal_secs")) {
            editor.putLong("${todayStr}_goal_secs", globalGoal)
            modified = true
        }
        if (modified) editor.apply()
    }

    internal data class MatrixGoalItem(val id: String, val title: String, val note: String = "", val targetMinutes: Int = 0, val isDeleted: Boolean = false)

    internal fun showPlannerMatrixDialog(startFullscreen: Boolean = false) {
        val isFullscreen = startFullscreen
        val dialog = Dialog(
            this,
            if (isFullscreen) android.R.style.Theme_Black_NoTitleBar_Fullscreen else android.R.style.Theme_Dialog
        )
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val goalsJson = host.getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).host.getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        var selectedRangeDays = 14

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = if (isFullscreen) {
                android.graphics.drawable.ColorDrawable(themeCoordinator.bgColor)
            } else {
                themeCoordinator.createDialogBackground(28f)
            }
            setPadding(
                dp(16),
                if (isFullscreen) getStatusBarHeight() + dp(12) else dp(18),
                dp(16),
                if (isFullscreen) dp(16) else dp(18)
            )
        }

        val topHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topHeaderRow.addView(TextView(host).apply {
            text = "GOAL COMPLETION GRID"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val fsBtn = TextView(host).apply {
            text = if (isFullscreen) "Exit Fullscreen" else "Fullscreen"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(plannerPrimary, 12f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener {
                dialog.dismiss()
                showPlannerMatrixDialog(startFullscreen = !isFullscreen)
            }
        }
        topHeaderRow.addView(fsBtn)
        content.addView(topHeaderRow)

        content.addView(TextView(host).apply {
            text = "Track your daily completion history across all goals:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(10))
        })

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHeaderFmt = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Collect all goals (both active and historically deleted)
        val allGoalItemsMap = mutableMapOf<String, MatrixGoalItem>()

        // Add current active goals first
        for (g in goalsList) {
            allGoalItemsMap[g.id] = MatrixGoalItem(g.id, g.title, note = g.note, targetMinutes = g.targetMinutes, isDeleted = false)
        }

        // Add past deleted goals from daily snapshots
        val prefs = host.getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        for (key in prefs.all.keys) {
            if (key.endsWith("_planner_snapshot")) {
                if (!prefs.contains(key)) continue
                val snapshots = PlannerHistoryManager.loadDaySnapshot(this, key.removeSuffix("_planner_snapshot"))
                for (s in snapshots) {
                    if (s.goalId.isNotBlank() && !allGoalItemsMap.containsKey(s.goalId)) {
                        allGoalItemsMap[s.goalId] = MatrixGoalItem(s.goalId, if (s.title.isNotBlank()) s.title else "Deleted Goal", isDeleted = true)
                    }
                }
            }
        }

        val allGoalItems = allGoalItemsMap.values.toList()
        val allDates = mutableSetOf<String>()

        val goalHistories = allGoalItems.associate { item ->
            val h = PlannerHistoryManager.loadGoalHistoryDetailed(this, item.id)
            allDates.addAll(h.keys)
            item.id to h
        }

        val allCandidateDates = mutableListOf<Date>()
        for (dStr in allDates) {
            runCatching { sdf.parse(dStr) }.getOrNull()?.let { allCandidateDates.add(it) }
        }
        for (key in prefs.all.keys) {
            if (key.endsWith("_planner_snapshot")) {
                runCatching { sdf.parse(key.removeSuffix("_planner_snapshot")) }.getOrNull()?.let { allCandidateDates.add(it) }
            }
            if (key.endsWith("_focus_total")) {
                val total = (prefs.all[key] as? Number)?.toLong() ?: 0L
                if (total > 0) {
                    runCatching { sdf.parse(key.removeSuffix("_focus_total")) }.getOrNull()?.let { allCandidateDates.add(it) }
                }
            }
        }
        for (g in goalsList) {
            if (g.createdAt > 0) {
                allCandidateDates.add(Date(g.createdAt))
            }
        }
        runCatching {
            TimelineLogger.load(host).forEach { entry ->
                if (entry.timestamp > 0) allCandidateDates.add(Date(entry.timestamp))
            }
        }

        val earliestDate = allCandidateDates.minOrNull() ?: Date()
        val earliestCal = Calendar.getInstance().apply {
            time = earliestDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var currentAllTimeDays = 30

        val matrixContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun renderMatrix(rangeDays: Int) {
            matrixContainer.removeAllViews()
            val calendarList = mutableListOf<Date>()
            val futureOffset = if (rangeDays == 0) 0 else 3
            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, futureOffset) }
            val count = if (rangeDays > 0) rangeDays else currentAllTimeDays
            var reachedEarliest = false

            for (i in 0 until count) {
                val c = startCal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, -i)
                val dayCal = Calendar.getInstance().apply {
                    time = c.time
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (rangeDays == 0 && dayCal.before(earliestCal)) {
                    reachedEarliest = true
                    break
                }
                calendarList.add(c.time)
            }
            val filteredDates = calendarList

            if (filteredDates.isEmpty()) {
                matrixContainer.addView(TextView(host).apply {
                    text = "No goal history yet. Completed goals will appear here."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(20), 0, dp(20))
                })
                return
            }

            val splitLayout = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // Left Fixed Column for Goal Titles
            val leftColumn = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            leftColumn.addView(TextView(host).apply {
                text = "Goal"
                setTextColor(plannerPrimary)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(dp(110), dp(36))
            })
            leftColumn.addView(createDivider())

            for (item in allGoalItems) {
                leftColumn.addView(TextView(host).apply {
                    text = if (item.isDeleted) "${item.title} (Old)" else item.title
                    setTextColor(if (item.isDeleted) (if (isDark) tintedColor(themeCoordinator.textColor, 120) else 0xFF94A3B8.toInt()) else (if (isDark) themeCoordinator.textColor else 0xFF0F172A.toInt()))
                    textSize = 13f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(110), dp(38))
                    setOnClickListener {
                        showGoalDetailFloatingCard(item)
                    }
                    setOnLongClickListener {
                        showDeletePlannerGoalMatrixDialog(item.id, item.title) {
                            dialog.dismiss()
                            showPlannerMatrixDialog(startFullscreen = isFullscreen)
                        }
                        true
                    }
                })
                leftColumn.addView(createDivider())
            }
            splitLayout.addView(leftColumn)

            // Right Scrollable Dates Container
            val rightScroll = HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val rightTable = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
            }

            val todayStr = sdf.format(Date())

            val allGoalsTodayCompleted = allGoalItems.isNotEmpty() && allGoalItems.all { item ->
                val history = goalHistories[item.id] ?: emptyMap()
                history[todayStr] == GoalHistoryStatus.ACHIEVED
            }

            val todayAccent = if (allGoalsTodayCompleted) (if (isDark) 0xFF22C55E.toInt() else 0xFF10B981.toInt()) else (if (isDark) plannerSecondary else 0xFF0284C7.toInt())
            val todayBgColor = if (isDark) {
                if (allGoalsTodayCompleted) Color.argb(35, 34, 197, 94) else Color.argb(22, Color.red(plannerSecondary), Color.green(plannerSecondary), Color.blue(plannerSecondary))
            } else {
                if (allGoalsTodayCompleted) 0x2210B981.toInt() else 0x1A0284C7.toInt()
            }

            val headerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36))
            }
            for (d in filteredDates) {
                val dateKey = sdf.format(d)
                val isToday = (dateKey == todayStr)
                headerRow.addView(TextView(host).apply {
                    text = if (isToday) "TODAY\n${dateHeaderFmt.format(d)}" else dateHeaderFmt.format(d)
                    setTextColor(
                        if (isToday) {
                            if (isDark) 0xFFFFFFFF.toInt() else 0xFF0369A1.toInt()
                        } else {
                            if (isDark) plannerPrimary else 0xFF475569.toInt()
                        }
                    )
                    textSize = if (isToday) 10f else 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    if (isToday) {
                        background = if (isDark) {
                            themeCoordinator.createGlassChip(todayAccent, 10f)
                        } else {
                            GradientDrawable().apply {
                                cornerRadius = dp(10).toFloat()
                                setColor(0xFFE0F2FE.toInt())
                                setStroke(dp(1), 0xFF0284C7.toInt())
                            }
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
            rightTable.addView(headerRow)
            rightTable.addView(createDivider())

            val greenColor = if (isDark) 0xFF22C55E.toInt() else 0xFF047857.toInt()
            val redColor = if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()

            for (item in allGoalItems) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38))
                }
                val history = goalHistories[item.id] ?: emptyMap()
                for (d in filteredDates) {
                    val dateKey = sdf.format(d)
                    val status = history[dateKey]
                    val isToday = (dateKey == todayStr)
                    row.addView(TextView(host).apply {
                        text = when (status) {
                            GoalHistoryStatus.ACHIEVED -> "\u2713"
                            GoalHistoryStatus.DEFICIT -> "\u2715"
                            GoalHistoryStatus.NOT_COMPLETED, null -> "-"
                        }
                        setTextColor(when (status) {
                            GoalHistoryStatus.ACHIEVED -> greenColor
                            GoalHistoryStatus.DEFICIT -> redColor
                            GoalHistoryStatus.NOT_COMPLETED, null -> if (isToday) {
                                if (isDark) 0xFFFFFFFF.toInt() else 0xFFD97706.toInt()
                            } else {
                                if (isDark) tintedColor(themeCoordinator.textColor, 80) else 0xFF94A3B8.toInt()
                            }
                        })
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        if (isToday) {
                            background = GradientDrawable().apply {
                                setColor(todayBgColor)
                                setStroke(dp(1), if (isDark) todayAccent else 0xFF0284C7.toInt())
                                cornerRadius = dp(6).toFloat()
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                    })
                }
                rightTable.addView(row)
                rightTable.addView(createDivider())
            }
            rightScroll.addView(rightTable)

            val todayIndex = filteredDates.indexOfFirst { sdf.format(it) == todayStr }
            if (todayIndex >= 0) {
                val targetScrollX = todayIndex * dp(54)
                rightScroll.visibility = View.INVISIBLE
                rightScroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        rightScroll.viewTreeObserver.removeOnPreDrawListener(this)
                        rightScroll.scrollTo(targetScrollX, 0)
                        rightScroll.visibility = View.VISIBLE
                        return true
                    }
                })
            }

            splitLayout.addView(rightScroll)

            matrixContainer.addView(splitLayout)

            if (rangeDays == 0 && !reachedEarliest) {
                val loadMoreBtn = TextView(host).apply {
                    text = "Load More (+30 Days)"
                    setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = if (isDark) {
                        themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 70), 12f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(10), 0, 0)
                    }
                    setOnClickListener {
                        currentAllTimeDays += 30
                        renderMatrix(0)
                    }
                }
                matrixContainer.addView(loadMoreBtn)
            }
        }

        // Filter Bar (14 Days | 30 Days | All Time)
        val filterRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }

        fun makeFilterBtn(label: String, daysVal: Int): TextView {
            val isSel = selectedRangeDays == daysVal
            return TextView(host).apply {
                text = label
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", if (isSel) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(
                    if (isSel) {
                        Color.WHITE
                    } else {
                        if (isDark) 0xAAFFFFFF.toInt() else 0xFF475569.toInt()
                    }
                )
                background = if (isSel) {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(plannerPrimary)
                    }
                } else {
                    if (isDark) {
                        themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(20).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                }
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(6), 0) }
                setOnClickListener {
                    selectedRangeDays = daysVal
                    for (i in 0 until filterRow.childCount) {
                        val child = filterRow.getChildAt(i) as? TextView ?: continue
                        val sel = (child.tag as? Int) == selectedRangeDays
                        child.typeface = Typeface.create("sans-serif-medium", if (sel) Typeface.BOLD else Typeface.NORMAL)
                        child.setTextColor(
                            if (sel) {
                                Color.WHITE
                            } else {
                                if (isDark) 0xAAFFFFFF.toInt() else 0xFF475569.toInt()
                            }
                        )
                        child.background = if (sel) {
                            GradientDrawable().apply {
                                cornerRadius = dp(20).toFloat()
                                setColor(plannerPrimary)
                            }
                        } else {
                            if (isDark) {
                                themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                            } else {
                                GradientDrawable().apply {
                                    cornerRadius = dp(20).toFloat()
                                    setColor(0xFFF1F5F9.toInt())
                                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                                }
                            }
                        }
                    }
                    renderMatrix(selectedRangeDays)
                }
                tag = daysVal
            }
        }
        filterRow.addView(makeFilterBtn("14 Days", 14))
        filterRow.addView(makeFilterBtn("30 Days", 30))
        filterRow.addView(makeFilterBtn("All Time", 0))

        content.addView(filterRow)
        renderMatrix(selectedRangeDays)
        content.addView(matrixContainer)

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(plannerPrimary, 24f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { setMargins(0, dp(14), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)

        if (isFullscreen) {
            dialog.window?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setDecorFitsSystemWindows(false)
                }
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.TRANSPARENT
                navigationBarColor = Color.TRANSPARENT
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeCoordinator.bgColor))
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        } else {
            dialog.window?.apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setLayout((host.resources.displayMetrics.widthPixels * 0.95f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        dialog.show()
    }

    internal fun showFeedbackReportDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // Header Title
        val titleText = TextView(host).apply {
            text = "💬 Report a Problem & Feedback"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        content.addView(titleText)

        val subtitleText = TextView(host).apply {
            text = "Help us improve StudyTimer! Report bugs, suggest features, or share thoughts."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(subtitleText)

        // Category Selector Chips
        val categories = listOf("Bug Report", "Feature Request", "General Feedback")
        var selectedCategory = "Bug Report"

        val chipContainer = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }

        val chipViews = ArrayList<TextView>()
        fun updateChipStyles() {
            chipViews.forEachIndexed { index, chip ->
                val isSelected = categories[index] == selectedCategory
                chip.setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                chip.background = if (isSelected) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 18f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 18f)
                }
            }
        }

        categories.forEach { cat ->
            val chip = TextView(host).apply {
                text = when (cat) {
                    "Bug Report" -> "🐛 Bug"
                    "Feature Request" -> "💡 Feature"
                    else -> "💭 Feedback"
                }
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener {
                    selectedCategory = cat
                    updateChipStyles()
                }
            }
            chipViews.add(chip)
            chipContainer.addView(chip)
        }
        updateChipStyles()
        content.addView(chipContainer)

        // Description Text Field (Max 2,000 characters)
        val inputField = android.widget.EditText(host).apply {
            hint = "Describe what happened or what you'd like to see (max 2,000 chars)..."
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            filters = arrayOf(android.text.InputFilter.LengthFilter(2000))
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 7
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        content.addView(inputField)

        // Contact Email Input Field (Optional, Max 100 characters)
        val contactField = android.widget.EditText(host).apply {
            hint = "Your email for reply (optional)..."
            val userEmail = AuthManager.getUserEmail(host)
            if (!userEmail.isNullOrBlank()) setText(userEmail)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        content.addView(contactField)

        // Anonymous Diagnostic Info Toggle
        var includeDiagnostics = true
        val toggleRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(12))
            isClickable = true
            isFocusable = true
        }

        val checkIcon = TextView(host).apply {
            text = "☑️"
            textSize = 15f
            setPadding(0, 0, dp(8), 0)
        }
        toggleRow.addView(checkIcon)

        val toggleLabel = TextView(host).apply {
            text = "Include anonymous diagnostic info\n(App version, OS, Device model, Sync state)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 11.5f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleRow.addView(toggleLabel)

        toggleRow.setOnClickListener {
            includeDiagnostics = !includeDiagnostics
            checkIcon.text = if (includeDiagnostics) "☑️" else "⬜"
        }
        content.addView(toggleRow)

        val feedbackPrefs = host.getSharedPreferences("studytimer_feedback_prefs", Context.MODE_PRIVATE)
        val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes cooldown
        val lastSubmissionEpoch = feedbackPrefs.getLong("last_feedback_submission_epoch", 0L)
        val isDevBypass = BuildConfig.DEBUG || isDevModeUnlocked

        // Cooldown Warning & Live Countdown View
        val cooldownWarningText = TextView(host).apply {
            setTextColor(0xFFFF7043.toInt()) // Soft coral warning
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(6))
            visibility = View.GONE
        }

        // Urgent Help Fallback Action
        val urgentEmailBtn = TextView(host).apply {
            text = "Need urgent help? [Email studytimer737@gmail.com]"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:studytimer737@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "[StudyTimer Urgent Help]")
                }
                try {
                    host.startActivity(Intent.createChooser(emailIntent, "Email Support..."))
                } catch (_: Exception) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Support Email", "studytimer737@gmail.com"))
                    Toast.makeText(host, "Email copied to clipboard: studytimer737@gmail.com", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Submit Feedback Action Button
        val sendBtn = Button(host).apply {
            text = "🚀 Submit Report"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, dp(4), 0, dp(6))
            }
        }

        // Countdown Timer Handler
        val countdownHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSubmissionEpoch
                val remaining = COOLDOWN_MS - elapsed

                if (!isDevBypass && remaining > 0) {
                    val mins = (remaining / 1000) / 60
                    val secs = (remaining / 1000) % 60
                    sendBtn.isEnabled = false
                    sendBtn.alpha = 0.5f
                    cooldownWarningText.visibility = View.VISIBLE
                    cooldownWarningText.text = "⏳ You recently sent a report. Please wait ${mins}m ${secs}s before submitting another."
                    urgentEmailBtn.visibility = View.VISIBLE
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    sendBtn.isEnabled = true
                    sendBtn.alpha = 1.0f
                    cooldownWarningText.visibility = View.GONE
                    urgentEmailBtn.visibility = View.GONE
                }
            }
        }

        countdownHandler.post(countdownRunnable)
        dialog.setOnDismissListener {
            countdownHandler.removeCallbacks(countdownRunnable)
        }

        sendBtn.setOnClickListener {
            val now = System.currentTimeMillis()
            val elapsed = now - lastSubmissionEpoch
            val remaining = COOLDOWN_MS - elapsed

            if (!isDevBypass && remaining > 0) {
                val mins = (remaining / 1000) / 60
                val secs = (remaining / 1000) % 60
                Toast.makeText(host, "Please wait ${mins}m ${secs}s before submitting another report.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Daily rate limit check (max 10 submissions / 24 hours)
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dailyCount = feedbackPrefs.getInt("submissions_count_$todayDateKey", 0)
            if (!isDevBypass && dailyCount >= 10) {
                Toast.makeText(host, "Daily submission limit reached (10/day). Please try again tomorrow.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val userFeedbackText = inputField.text.toString().trim().take(2000)
            if (userFeedbackText.length < 5) {
                Toast.makeText(host, "Please enter at least 5 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userContactText = contactField.text.toString().trim().take(100)
            sendBtn.isEnabled = false
            sendBtn.text = "⏳ Submitting..."

            val isBatteryOptIgnored = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    pm?.isIgnoringBatteryOptimizations(host.packageName) ?: false
                } else true
            } catch (_: Exception) { false }

            val notifsEnabled = try {
                androidx.core.app.NotificationManagerCompat.from(host).areNotificationsEnabled()
            } catch (_: Exception) { true }

            val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val currentTimerState = prefs.host.getString("timerState", "IDLE") ?: "IDLE"
            val selectedSub = try { SubjectTagManager.getSelectedSubject(host).name } catch (_: Exception) { "General" }
            val dm = host.resources.displayMetrics
            val freeRamMb = Runtime.getRuntime().freeMemory() / (1024 * 1024)
            val maxRamMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayFocusSecs = prefs.getLong("${todayStr}_focus_total", 0L)

            // Build Comprehensive Privacy-Safe Diagnostic Object
            val diagJson = org.json.JSONObject().apply {
                put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                put("android_os", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("brand", Build.BRAND)
                put("timer_mode", timerMode)
                put("timer_state", currentTimerState)
                put("active_subject", selectedSub)
                put("sync_status", if (AuthManager.isLoggedIn(host)) "ONLINE_SYNCED" else "GUEST_OFFLINE")
                put("notifications_enabled", notifsEnabled)
                put("battery_opt_ignored", isBatteryOptIgnored)
                put("leaderboard_participating", LeaderboardManager.isParticipating(host))
                put("live_status_sharing", LeaderboardManager.isLiveStatusSharingEnabled(host))
                put("today_focus_secs", todayFocusSecs)
                put("screen_resolution", "${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi}dpi)")
                put("memory_mb", "$freeRamMb / $maxRamMb MB")
                put("timestamp_epoch", System.currentTimeMillis() / 1000)
            }

            val typeEnum = when (selectedCategory) {
                "Bug Report" -> "BUG_REPORT"
                "Feature Request" -> "FEATURE_REQUEST"
                else -> "GENERAL_FEEDBACK"
            }

            // Execute Real HTTP POST in background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                var isSuccess = false
                var errorMessage = ""
                var statusCode = 0

                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

                val targetEndpoint = if (supabaseUrl.isNotBlank()) {
                    "$supabaseUrl/rest/v1/feedback_reports"
                } else ""

                val payload = org.json.JSONObject().apply {
                    put("type", typeEnum)
                    put("status", "NEW")
                    if (userContactText.isNotBlank()) put("user_contact", userContactText)
                    put("message", userFeedbackText)
                    put("diagnostics", if (includeDiagnostics) diagJson else org.json.JSONObject())
                }

                android.util.Log.d("FeedbackSubmission", "========================================")
                android.util.Log.d("FeedbackSubmission", "Sending to: $targetEndpoint")
                android.util.Log.d("FeedbackSubmission", "Payload: ${payload.toString()}")
                android.util.Log.d("FeedbackSubmission", "========================================")

                if (targetEndpoint.isNotBlank()) {
                    try {
                        val url = java.net.URL(targetEndpoint)
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", supabaseAnonKey)
                        conn.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Prefer", "return=representation")
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doOutput = true

                        conn.outputStream.use { os ->
                            os.write(payload.toString().toByteArray(Charsets.UTF_8))
                        }

                        statusCode = conn.responseCode
                        val responseBody = try {
                            if (statusCode in 200..299) {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                            }
                        } catch (_: Exception) { "Could not read response" }

                        android.util.Log.d("FeedbackSubmission", "Server Response HTTP $statusCode: $responseBody")

                        if (statusCode in 200..299) {
                            isSuccess = true
                        } else {
                            errorMessage = "Server HTTP $statusCode: $responseBody"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FeedbackSubmission", "Network error during feedback post", e)
                        errorMessage = e.localizedMessage ?: "Network connection error"
                    }
                } else {
                    errorMessage = "No backend endpoint configured."
                }

                // Handle on Main Thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (isSuccess) {
                        feedbackPrefs.edit()
                            .putLong("last_feedback_submission_epoch", System.currentTimeMillis())
                            .putInt("submissions_count_$todayDateKey", dailyCount + 1)
                            .apply()
                        Toast.makeText(host, "🎉 Report submitted successfully! Thank you for your feedback.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        sendBtn.isEnabled = true
                        sendBtn.text = "🚀 Submit Report"
                        android.util.Log.w("FeedbackSubmission", "HTTP Failed: $errorMessage. Triggering email fallback.")

                        val devEmail = "studytimer737@gmail.com"
                        val emailSubject = "[StudyTimer Feedback - $selectedCategory] v${BuildConfig.VERSION_NAME}"
                        val diagnosticBlock = if (includeDiagnostics) {
                            """
                            
                            -------------------------------------
                            Diagnostic Info (Helps us fix bugs faster):
                            • App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                            • Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            • Device: ${Build.MANUFACTURER} ${Build.MODEL}
                            • Timer Mode Active: $timerMode
                            • Sync Status: ${if (AuthManager.isLoggedIn(host)) "Online" else "Guest"}
                            -------------------------------------
                            """.trimIndent()
                        } else ""

                        val emailBody = "$userFeedbackText\n$diagnosticBlock"

                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(devEmail))
                            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                            putExtra(Intent.EXTRA_TEXT, emailBody)
                        }

                        try {
                            Toast.makeText(host, "Opening email client to send your feedback...", Toast.LENGTH_LONG).show()
                            host.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email..."))
                            dialog.dismiss()
                        } catch (_: Exception) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("StudyTimer Feedback", "To: $devEmail\nSubject: $emailSubject\n\n$emailBody"))
                            Toast.makeText(host, "Feedback copied to clipboard! Please email to studytimer737@gmail.com", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        content.addView(sendBtn)
        content.addView(cooldownWarningText)
        content.addView(urgentEmailBtn)

        val dismissRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(6), 0, 0)
        }
        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(tintedColor(themeCoordinator.textColor, 170))
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 14f)
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        dismissRow.addView(closeBtn)
        content.addView(dismissRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showAppGuideDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val titleText = TextView(host).apply {
            text = "⚡ StudyTimer — How to Use Summary"
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(titleText)

        val scrollView = ScrollView(host).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val list = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun createGuideCard(sectionNumber: String, sectionTitle: String, icon: String, bullets: List<Pair<String, String>>) {
            val card = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (themeCoordinator.isDarkMode()) 0x18FFFFFF.toInt() else tintedColor(themeCoordinator.primaryColor, 15))
                    setStroke(dp(1), if (themeCoordinator.isDarkMode()) 0x22FFFFFF.toInt() else tintedColor(themeCoordinator.primaryColor, 40))
                }
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(14))
                }
            }

            val headerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }
            val iconView = TextView(host).apply {
                text = icon
                textSize = 18f
                setPadding(0, 0, dp(8), 0)
            }
            val titleView = TextView(host).apply {
                text = "$sectionNumber. $sectionTitle"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            headerRow.addView(iconView)
            headerRow.addView(titleView)
            card.addView(headerRow)

            for ((itemTitle, itemDesc) in bullets) {
                val itemRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(3), 0, dp(3))
                }
                val bulletDot = TextView(host).apply {
                    text = "• "
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, dp(4), 0)
                }
                val itemText = TextView(host).apply {
                    val fullSpannable = android.text.SpannableStringBuilder()
                    val boldSpan = android.text.style.StyleSpan(Typeface.BOLD)
                    val titleFormatted = "$itemTitle: "
                    fullSpannable.append(titleFormatted)
                    fullSpannable.setSpan(boldSpan, 0, titleFormatted.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    fullSpannable.append(itemDesc)
                    text = fullSpannable
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12.5f
                    setLineSpacing(0f, 1.15f)
                    alpha = 0.9f
                }
                itemRow.addView(bulletDot)
                itemRow.addView(itemText)
                card.addView(itemRow)
            }

            list.addView(card)
        }

        // 1. All 4 Timer Modes
        createGuideCard(
            "1",
            "Timer Modes & Focus Flow",
            "⏱️",
            listOf(
                "Subject-Wise Timer" to "Tag each session with subjects, custom color badges & icons to track detailed subject breakdowns.",
                "Pomodoro & White Mode" to "Focus intervals with automatic short/long breaks. Includes an optional Minimal White Background & Black Timer theme.",
                "Custom Countdown" to "Set targeted hours and minutes for timed study sprints, practice tests, and revision blocks.",
                "Stopwatch Mode" to "Open-ended count-up timer with precision lap recordings for flexible study.",
                "Hold to Finish" to "Long-press safety control prevents accidental session terminations."
            )
        )

        // 2. Planner & Habit Tracking
        createGuideCard(
            "2",
            "Planner, Goals & Habit Grid",
            "📋",
            listOf(
                "Daily Habit Checklist" to "Create daily study goals, link subjects, set target minutes, and reorder via hold-and-drag.",
                "Goal Completion Grid & History" to "Tap any goal to inspect its monthly calendar history with checkmark completion or view the multi-day matrix.",
                "Yesterday's Goals Logger" to "Easily retroactively log or adjust completed tasks and habits from the previous day anytime."
            )
        )

        // 3. Insights & Consistency Analytics
        createGuideCard(
            "3",
            "Insights, Heatmap & Calendar",
            "📊",
            listOf(
                "Daily & Weekly Analytics" to "Comprehensive overview of total focus hours, subject distribution pie chart, and daily study rhythms.",
                "Activity Heatmap" to "6-month visual consistency grid highlighting your daily study intensity and habit trends.",
                "Interactive Calendar" to "Explore day-by-day session timelines, individual logs, and goal achievement checkmarks."
            )
        )

        // 4. Themes, Display & Widgets
        createGuideCard(
            "4",
            "Themes, Display & Widgets",
            "🎨",
            listOf(
                "Curated Theme Palettes" to "Sleek Dark, OLED True Black, and High-Contrast Light theme with custom vibrant accents.",
                "Fullscreen Digital Clock" to "Distraction-free full-screen clock with immersive status and navigation bar integration.",
                "Home Screen Widgets" to "Track your daily study progress and launch focus sessions directly from your home screen."
            )
        )

        // 5. Offline Storage & Cloud Sync
        createGuideCard(
            "5",
            "Offline Privacy & Cloud Backup",
            "☁️",
            listOf(
                "100% Offline-First" to "Fast local database ensures all your study logs, streak records, and habits remain strictly private on device.",
                "Google Cloud Sync" to "Optional cloud backup to safely preserve, sync, and restore records across your devices seamlessly.",
                "100% Free & Open" to "Zero advertisements, no subscription paywalls, and fully unlocked features."
            )
        )

        scrollView.addView(list)
        content.addView(scrollView)

        val closeBtn = Button(host).apply {
            text = "GOT IT! LET'S STUDY"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
                dialog.dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, dp(14), 0, 0)
            }
        }
        content.addView(closeBtn)

        dialog.setOnDismissListener {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
        }
        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), (host.resources.displayMetrics.heightPixels * 0.80f).toInt())
        }
        dialog.show()
    }

    private fun showGoalDetailFloatingCard(item: Any) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val itemId: String
        val itemTitle: String
        val itemNote: String
        val itemTargetMins: Int
        val itemIsDeleted: Boolean

        when (item) {
            is MatrixGoalItem -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = item.isDeleted
            }
            is PlannerGoal -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = false
            }
            else -> {
                itemId = runCatching { item.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getId").invoke(item) as String }.getOrNull() ?: ""
                itemTitle = runCatching { item.javaClass.getDeclaredField("title").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTitle").invoke(item) as String }.getOrNull() ?: ""
                itemNote = runCatching { item.javaClass.getDeclaredField("note").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getNote").invoke(item) as String }.getOrNull() ?: ""
                itemTargetMins = runCatching { item.javaClass.getDeclaredField("targetMinutes").apply { isAccessible = true }.get(item) as Int }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTargetMinutes").invoke(item) as Int }.getOrNull() ?: 0
                itemIsDeleted = runCatching { item.javaClass.getDeclaredField("isDeleted").apply { isAccessible = true }.get(item) as Boolean }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("isDeleted").invoke(item) as Boolean }.getOrNull() ?: false
            }
        }

        val goalsJson = host.getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).host.getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val activeGoal = goalsList.find { it.id == itemId }
        val noteText = if (activeGoal != null && activeGoal.note.isNotBlank()) activeGoal.note else itemNote
        val targetMins = if (activeGoal != null && activeGoal.targetMinutes > 0) activeGoal.targetMinutes else itemTargetMins

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        // 1. FIXED Header Tag
        content.addView(TextView(host).apply {
            text = if (itemIsDeleted) "ARCHIVED GOAL" else "GOAL DETAILS"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // 2. Scrollable Body Box (Title + Target + Notes)
        val maxCardBodyHeight = dp(240)
        val bodyScroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
            isVerticalScrollBarEnabled = true
        }

        val bodyBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        bodyBox.addView(TextView(host).apply {
            text = if (itemTitle.isNotBlank()) itemTitle else "Untitled Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        if (targetMins > 0) {
            bodyBox.addView(TextView(host).apply {
                text = "Target: $targetMins min / day"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 13f
                setPadding(0, 0, 0, dp(6))
            })
        }

        if (noteText.isNotBlank()) {
            bodyBox.addView(createDivider())
            bodyBox.addView(TextView(host).apply {
                text = "NOTES"
                setTextColor(tintedColor(themeCoordinator.textColor, 140))
                textSize = 10f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(4))
            })

            val noteBox = TextView(host).apply {
                text = noteText
                setTextColor(if (themeCoordinator.isDarkMode()) themeCoordinator.textColor else 0xFF0F172A.toInt())
                textSize = 14f
                setLineSpacing(dp(3).toFloat(), 1.1f)
                background = if (themeCoordinator.isDarkMode()) {
                    themeCoordinator.createGlassChip(Color.argb(30, 255, 255, 255), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF8FAFC.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }
            bodyBox.addView(noteBox)
        }

        bodyScroll.addView(bodyBox)

        bodyScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bodyScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (bodyScroll.height > maxCardBodyHeight) {
                    bodyScroll.layoutParams = (bodyScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxCardBodyHeight
                    }
                }
            }
        })
        content.addView(bodyScroll)

        // 3. FIXED Action Buttons Row at Bottom
        val buttonRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        val isDark = themeCoordinator.isDarkMode()
        if (activeGoal != null) {
            val historyBtn = TextView(host).apply {
                text = "Goal History"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(plannerPrimary)
                }
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
                setOnClickListener {
                    dialog.dismiss()
                    showGoalHistoryDialog(activeGoal)
                }
            }
            buttonRow.addView(historyBtn)
        }

        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(if (activeGoal != null) Color.argb(40, 255, 255, 255) else plannerPrimary, 20f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(if (activeGoal != null) 0 else LinearLayout.LayoutParams.MATCH_PARENT, dp(44), 1f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonRow.addView(closeBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun showDeletePlannerGoalMatrixDialog(goalId: String, goalTitle: String, onDeleted: () -> Unit) {
        val dialog = Dialog(host)
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(24f)
        }

        root.addView(TextView(host).apply {
            text = "Delete Goal from History?"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        root.addView(TextView(host).apply {
            text = "Goal: $goalTitle\n\nWould you like to keep or delete this goal from your Goal & Habit Grid history?"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13f
            setPadding(0, dp(10), 0, dp(18))
        })

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        btnRow.addView(TextView(host).apply {
            text = "Keep"
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener { dialog.dismiss() }
        })

        btnRow.addView(View(host).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })

        btnRow.addView(TextView(host).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(Color.argb(40, 255, 82, 82), 16f)
            setOnClickListener {
                dialog.dismiss()
                val prefs = host.getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
                val activeJson = prefs.host.getString("session_goals_json", "[]") ?: "[]"
                val activeList = loadSessionGoalsFromJson(activeJson).filterNot { it.id == goalId }
                saveSessionGoalsToJson(activeList)

                PlannerHistoryManager.deleteGoalHistory(host, goalId)

                Toast.makeText(host, "Deleted '$goalTitle' from grid history", Toast.LENGTH_SHORT).show()
                onDeleted()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(host)
                    }
                }.start()
            }
        })

        root.addView(btnRow)
        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    internal fun showAddSessionGoalDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "New Study Goal"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box to ensure buttons never go offscreen
        val scrollContainer = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isVerticalScrollBarEnabled = true
        }

        val inputsBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(host).apply {
            hint = "Goal name (e.g. Math practice, Physics)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(host).apply {
            hint = "Notes (optional)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(host).apply {
            hint = "Target time in minutes (or select below)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) }
        }

        // 1-Tap Duration Presets Row
        val durationPresets = listOf(15, 25, 45, 60, 90, 120)
        val presetScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val presetRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        for (m in durationPresets) {
            val chip = TextView(host).apply {
                text = "${m}m"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    durationInput.setText(m.toString())
                    durationInput.setSelection(durationInput.text.length)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)

        inputsBox.addView(durationInput)
        inputsBox.addView(presetScroll)

        // Subject Tag Selector
        inputsBox.addView(TextView(host).apply {
            text = "Subject (optional)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        var selectedSubjectId: String? = null
        val allSubjects = SubjectTagManager.(host)
        val subjectChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
        val subjectChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun refreshSubjectChips() {
            subjectChipRow.removeAllViews()

            // "All / General" Chip
            val isAllSelected = selectedSubjectId == null || selectedSubjectId == "all"
            val allChip = TextView(host).apply {
                text = "All Subjects"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllSelected) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllSelected) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubjectId = null
                    refreshSubjectChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subjectChipRow.addView(allChip)

            for (sub in allSubjects) {
                val isSelected = selectedSubjectId == sub.id
                val subColor = try { Color.parseColor(sub.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                val chip = TextView(host).apply {
                    text = "${sub.iconEmoji} ${sub.name}"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(subColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubjectId = sub.id
                        refreshSubjectChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subjectChipRow.addView(chip)
            }
        }
        refreshSubjectChips()
        subjectChipScroll.addView(subjectChipRow)
        inputsBox.addView(subjectChipScroll)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save Goal"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val titleText = titleInput.text.toString().trim()
                if (titleText.isNotBlank()) {
                    val noteText = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val targetMins = rawMins.coerceAtMost(1440)
                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).host.getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    currentGoals.add(PlannerGoal(title = titleText, note = noteText, targetMinutes = targetMins, subjectId = selectedSubjectId))
                    saveSessionGoalsToJson(currentGoals)
                    refreshStatsPanel()
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), (host.resources.displayMetrics.heightPixels * 0.80f).toInt().coerceAtMost(android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.show()
    }

    internal fun showEditSessionGoalDialog(goal: PlannerGoal) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, _) = resolvePlannerColors()

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Edit Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box
        val scrollContainer = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isVerticalScrollBarEnabled = true
        }

        val inputsBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(host).apply {
            hint = "Goal name"
            setText(goal.title)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(host).apply {
            hint = "Notes (optional)"
            setText(goal.note)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(host).apply {
            hint = "Target time in minutes (or select below)"
            setText(if (goal.targetMinutes > 0) goal.targetMinutes.toString() else "")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) }
        }
        inputsBox.addView(durationInput)

        // 1-Tap Duration Presets Row
        val durationPresets = listOf(15, 25, 45, 60, 90, 120)
        val presetScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val presetRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        for (m in durationPresets) {
            val chip = TextView(host).apply {
                text = "${m}m"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 40), 10f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    durationInput.setText(m.toString())
                    durationInput.setSelection(durationInput.text.length)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        inputsBox.addView(presetScroll)

        // Subject Tag Selector
        inputsBox.addView(TextView(host).apply {
            text = "Subject (optional)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        var selectedSubjectId: String? = goal.subjectId
        val allSubjects = SubjectTagManager.(host)
        val subjectChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
        val subjectChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun refreshSubjectChips() {
            subjectChipRow.removeAllViews()

            // "All / General" Chip
            val isAllSelected = selectedSubjectId == null || selectedSubjectId == "all"
            val allChip = TextView(host).apply {
                text = "All Subjects"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllSelected) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllSelected) {
                    themeCoordinator.createGlassChip(plannerPrimary, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubjectId = null
                    refreshSubjectChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subjectChipRow.addView(allChip)

            for (sub in allSubjects) {
                val isSelected = selectedSubjectId == sub.id
                val subColor = try { Color.parseColor(sub.colorHex) } catch (_: Exception) { plannerPrimary }
                val chip = TextView(host).apply {
                    text = "${sub.iconEmoji} ${sub.name}"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(subColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubjectId = sub.id
                        refreshSubjectChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subjectChipRow.addView(chip)
            }
        }
        refreshSubjectChips()
        subjectChipScroll.addView(subjectChipRow)
        inputsBox.addView(subjectChipScroll)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save Changes"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(plannerPrimary)
            setOnClickListener {
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    val newNote = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val newTargetMins = rawMins.coerceAtMost(1440)

                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).host.getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val idx = currentGoals.indexOfFirst { it.id == goal.id }
                    if (idx >= 0) {
                        val oldG = currentGoals[idx]
                        currentGoals[idx] = PlannerGoal(id = oldG.id, title = newTitle, note = newNote, targetMinutes = newTargetMins, completed = oldG.completed, checkedAt = oldG.checkedAt, createdAt = oldG.createdAt, subjectId = selectedSubjectId)
                        saveSessionGoalsToJson(currentGoals)
                        refreshStatsPanel()
                    }
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), (host.resources.displayMetrics.heightPixels * 0.80f).toInt().coerceAtMost(android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.show()
    }

    internal fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<LectureScheduleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(LectureScheduleItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Lecture"),
                    startTime = obj.optString("startTime", "09:00"),
                    endTime = obj.optString("endTime", "10:00"),
                    enabled = obj.optBoolean("enabled", true),
                    subjectId = obj.optString("subjectId", "general")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveLectureSchedulesToJson(items: List<LectureScheduleItem>) {
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("enabled", item.enabled)
                put("subjectId", item.subjectId)
            }
            array.put(obj)
        }
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val hasEnabled = items.any { it.enabled }
        prefs.edit()
            .putString("lecture_schedules_json", array.toString())
            .putBoolean("lecture_mode_enabled", hasEnabled)
            .apply()

        if (hasEnabled) {
            val serviceIntent = Intent(this, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    internal fun showLectureScheduleManagerDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val contentWidth = (host.resources.displayMetrics.widthPixels * 0.82).toInt()
        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        val topRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(host).apply {
            text = "\uD83C\uDF93 Class Timetable"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val addBtn = TextView(host).apply {
            text = "+ Add Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 12f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                dialog.dismiss()
                showAddLectureScheduleDialog()
            }
        }
        topRow.addView(addBtn)
        content.addView(topRow)

        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val items = loadLectureSchedulesFromJson(prefs.host.getString("lecture_schedules_json", "[]") ?: "[]")

        fun formatScheduleTime(timeStr: String): String {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return TimeFormat.formatHourMinute(host, h, m)
            }
            return timeStr
        }

        if (items.isEmpty()) {
            content.addView(TextView(host).apply {
                text = "No scheduled classes yet.\nTap '+ Add Class' to set start and end times."
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
            })
        } else {
            val listContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }
            for (item in items) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val textCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val itemSub = SubjectTagManager.resolveSubject(host, item.subjectId)
                textCol.addView(TextView(host).apply {
                    text = "${itemSub.iconEmoji} ${item.title}"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                textCol.addView(TextView(host).apply {
                    text = "${formatScheduleTime(item.startTime)} - ${formatScheduleTime(item.endTime)}"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                })
                row.addView(textCol)

                val toggleSwitch = SwitchMaterial(this).apply {
                    isChecked = item.enabled
                    scaleX = 0.75f
                    scaleY = 0.75f
                    setOnCheckedChangeListener { _, isChecked ->
                        val updated = items.map { if (it.id == item.id) it.copy(enabled = isChecked) else it }
                        saveLectureSchedulesToJson(updated)
                    }
                }

                row.addView(toggleSwitch)

                val editBtn = TextView(host).apply {
                    text = "\u270F\uFE0F"
                    textSize = 14f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        dialog.dismiss()
                        showAddLectureScheduleDialog(editItem = item)
                    }
                }
                row.addView(editBtn)

                val delBtn = TextView(host).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 16f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        showConfirmDialog("Delete Class?", "Are you sure you want to remove '${item.title}' from your timetable?") {
                            val updated = items.filter { it.id != item.id }
                            saveLectureSchedulesToJson(updated)
                            dialog.dismiss()
                            showLectureScheduleManagerDialog()
                        }
                    }
                }
                row.addView(delBtn)
                listContainer.addView(row)
            }
            content.addView(listContainer)
        }

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    internal fun showAddLectureScheduleDialog(editItem: LectureScheduleItem? = null) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val contentWidth = (host.resources.displayMetrics.widthPixels * 0.75).toInt()
            val content = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(18), dp(20), dp(18))
            }

            content.addView(TextView(host).apply {
                text = if (editItem != null) "Edit Class Schedule" else "Add Class Schedule"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            })

            val titleInput = android.widget.EditText(host).apply {
                hint = "Class Title (e.g. Physics 101)"
                if (editItem != null) setText(editItem.title)
                filters = arrayOf(android.text.InputFilter.LengthFilter(100))
                setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(11), dp(14), dp(11))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }
            content.addView(titleInput)

            fun parseIsPm(time24: String?): Boolean {
                if (time24.isNullOrBlank()) return false
                val parts = time24.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 0
                    return h >= 12
                }
                return false
            }

            var startIsPm = parseIsPm(editItem?.startTime)
            var endIsPm = parseIsPm(editItem?.endTime)

            fun createTimeInputGroup(initTime24: String?): Triple<LinearLayout, android.widget.EditText, android.widget.EditText> {
                val group = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                var hInit = ""
                var mInit = ""
                if (!initTime24.isNullOrBlank()) {
                    val parts = initTime24.split(":")
                    if (parts.size == 2) {
                        val h24 = parts[0].toIntOrNull() ?: 0
                        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
                        hInit = String.format(Locale.US, "%02d", h12)
                        mInit = parts[1]
                    }
                }

                val hourEdit = android.widget.EditText(host).apply {
                    hint = "10"
                    if (hInit.isNotBlank()) setText(hInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val colonLabel = TextView(host).apply {
                    text = ":"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(4), 0, dp(4), 0)
                }

                val minEdit = android.widget.EditText(host).apply {
                    hint = "00"
                    if (mInit.isNotBlank()) setText(mInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                hourEdit.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (s?.length == 2) {
                            minEdit.requestFocus()
                        }
                    }
                })

                group.addView(hourEdit)
                group.addView(colonLabel)
                group.addView(minEdit)
                return Triple(group, hourEdit, minEdit)
            }

            content.addView(TextView(host).apply {
                text = "Start Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val startRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (startGroup, startHEdit, startMEdit) = createTimeInputGroup(editItem?.startTime)
            startRow.addView(startGroup)

            val startAmBtn = TextView(host).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val startPmBtn = TextView(host).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            startAmBtn.setOnClickListener { startIsPm = false; startAmBtn.setTextColor(themeCoordinator.bgColor); startAmBtn.background = rippleBackground(themeCoordinator.primaryColor); startPmBtn.setTextColor(themeCoordinator.textColor); startPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            startPmBtn.setOnClickListener { startIsPm = true; startPmBtn.setTextColor(themeCoordinator.bgColor); startPmBtn.background = rippleBackground(themeCoordinator.primaryColor); startAmBtn.setTextColor(themeCoordinator.textColor); startAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            startAmBtn.setTextColor(if (!startIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            startAmBtn.background = if (!startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            startPmBtn.setTextColor(if (startIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            startPmBtn.background = if (startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val startToggleRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            startToggleRow.addView(startAmBtn)
            startToggleRow.addView(startPmBtn)
            startRow.addView(startToggleRow)
            content.addView(startRow)

            content.addView(TextView(host).apply {
                text = "End Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val endRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (endGroup, endHEdit, endMEdit) = createTimeInputGroup(editItem?.endTime)
            endRow.addView(endGroup)

            val endAmBtn = TextView(host).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val endPmBtn = TextView(host).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            endAmBtn.setOnClickListener { endIsPm = false; endAmBtn.setTextColor(themeCoordinator.bgColor); endAmBtn.background = rippleBackground(themeCoordinator.primaryColor); endPmBtn.setTextColor(themeCoordinator.textColor); endPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            endPmBtn.setOnClickListener { endIsPm = true; endPmBtn.setTextColor(themeCoordinator.bgColor); endPmBtn.background = rippleBackground(themeCoordinator.primaryColor); endAmBtn.setTextColor(themeCoordinator.textColor); endAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            endAmBtn.setTextColor(if (!endIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            endAmBtn.background = if (!endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            endPmBtn.setTextColor(if (endIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            endPmBtn.background = if (endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val endToggleRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            endToggleRow.addView(endAmBtn)
            endToggleRow.addView(endPmBtn)
            endRow.addView(endToggleRow)
            content.addView(endRow)

            content.addView(TextView(host).apply {
                text = "Subject Tag"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })

            val availableSubjects = SubjectTagManager.(host).toMutableList()
            val initSubId = editItem?.subjectId ?: SubjectTagManager.(host).id
            var chosenSubId = initSubId

            val subjectRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }

            val subjectSelectBtn = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val curInit = availableSubjects.find { it.id == chosenSubId } ?: availableSubjects.firstOrNull() ?: SubjectTagManager.DEFAULT_SUBJECTS[0]
            val subjectLabelTv = TextView(host).apply {
                text = "${curInit.iconEmoji} ${curInit.name}"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val subjectArrowTv = TextView(host).apply {
                text = " ▾"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            subjectSelectBtn.addView(subjectLabelTv)
            subjectSelectBtn.addView(subjectArrowTv)

            val addQuickSubBtn = TextView(host).apply {
                text = "➕ Add"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
            }

            fun updateSubjectLabel() {
                val cur = availableSubjects.find { it.id == chosenSubId } ?: availableSubjects.firstOrNull() ?: SubjectTagManager.DEFAULT_SUBJECTS[0]
                subjectLabelTv.text = "${cur.iconEmoji} ${cur.name}"
            }

            fun openSubjectPickerMenu() {
                val popup = android.widget.PopupMenu(this, subjectSelectBtn)
                for (i in availableSubjects.indices) {
                    val s = availableSubjects[i]
                    popup.menu.add(0, i, i, "${s.iconEmoji} ${s.name}")
                }
                popup.menu.add(0, 9999, 9999, "➕ Add New Subject...")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.itemId == 9999) {
                        showAddCustomSubjectDialog { newSub ->
                            availableSubjects.clear()
                            availableSubjects.addAll(SubjectTagManager.getAllSubjects(host))
                            chosenSubId = newSub.id
                            updateSubjectLabel()
                        }
                    } else {
                        val sel = availableSubjects.getOrNull(menuItem.itemId)
                        if (sel != null) {
                            chosenSubId = sel.id
                            updateSubjectLabel()
                        }
                    }
                    true
                }
                popup.show()
            }

            subjectSelectBtn.setOnClickListener { openSubjectPickerMenu() }
            addQuickSubBtn.setOnClickListener {
                showAddCustomSubjectDialog { newSub ->
                    availableSubjects.clear()
                    availableSubjects.addAll(SubjectTagManager.getAllSubjects(host))
                    chosenSubId = newSub.id
                    updateSubjectLabel()
                }
            }

            subjectRow.addView(subjectSelectBtn)
            subjectRow.addView(addQuickSubBtn)
            content.addView(subjectRow)

            fun convertTo24h(hEdit: android.widget.EditText, mEdit: android.widget.EditText, isPm: Boolean): String {
                val rawH = hEdit.text.toString().trim().toIntOrNull() ?: return ""
                val rawM = mEdit.text.toString().trim().toIntOrNull() ?: 0
                val h12 = rawH.coerceIn(1, 12)
                val m = rawM.coerceIn(0, 59)
                val h24 = when {
                    isPm && h12 < 12 -> h12 + 12
                    !isPm && h12 == 12 -> 0
                    else -> h12
                }
                return String.format(Locale.US, "%02d:%02d", h24, m)
            }

            val btnRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(10), 0, 0)
            }
            btnRow.addView(Button(host).apply {
                text = "Cancel"
                setTextColor(themeCoordinator.textColor)
                background = null
                setOnClickListener {
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            btnRow.addView(Button(host).apply {
                text = if (editItem != null) "Update Class" else "Save Class"
                setTextColor(themeCoordinator.bgColor)
                background = rippleBackground(themeCoordinator.primaryColor)
                setOnClickListener {
                    val titleText = titleInput.text.toString().trim().take(100)
                    val startText = convertTo24h(startHEdit, startMEdit, startIsPm)
                    val endText = convertTo24h(endHEdit, endMEdit, endIsPm)
                    val selectedSubjectId = chosenSubId

                    if (titleText.isNotBlank() && startText.isNotBlank() && endText.isNotBlank()) {
                        val currentSchedules = loadLectureSchedulesFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).host.getString("lecture_schedules_json", "[]") ?: "[]").toMutableList()
                        if (editItem != null) {
                            val idx = currentSchedules.indexOfFirst { it.id == editItem.id }
                            if (idx != -1) {
                                currentSchedules[idx] = editItem.copy(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId)
                            }
                        } else {
                            currentSchedules.add(LectureScheduleItem(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId))
                        }
                        saveLectureSchedulesToJson(currentSchedules)

                        val serviceIntent = Intent(host, TimerService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            content.addView(btnRow)

            dialog.setContentView(content)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
            dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        } catch (e: Exception) {
            android.util.Log.e("StudyTimer", "Error in showAddLectureScheduleDialog", e)
        }
    }
}
