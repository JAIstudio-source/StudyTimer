package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class DayTimelineDialogHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val statsEngine get() = host.statsEngine

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)

    fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> = statsEngine.dayBlocks(dateStr)

    fun reconcileDayTotals(dateStr: String) {
        val (sessions, breaks) = dayBlocks(dateStr)
        val fSecs = sessions.sumOf { it.secs }
        val bSecs = breaks.sumOf { it.secs }
        host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putLong("${dateStr}_focus_total", fSecs)
            .putLong("${dateStr}_break_total", bSecs)
            .apply()
    }

    fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long = statsEngine.msForDateAndTime(dateStr, h, m)

    fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) {
        TimelineLogger.replaceBlock(
            host,
            block.startMs,
            block.endMs,
            newStartMs,
            newEndMs,
            if (isBreak) "BREAK" else "STUDYING",
            subId = block.subjectId,
            subName = block.subjectName,
            subColor = block.subjectColor
        )
        reconcileDayTotals(dateStr)
        host.invalidateStatsCache()
        host.refreshStatsPanel()
        host.recalculateStreak()
        host.checkCelebration()
        StudyWidgetProvider.refresh(host)
    }

    fun showBlockEditDialog(dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = if (isBreak) "Edit Break Interval" else "Edit Study Session"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })

        content.addView(TextView(host).apply {
            text = "Current: ${TimeFormat.formatWallClock(host, block.startMs)} – ${TimeFormat.formatWallClock(host, block.endMs)} (${formatDuration(block.secs)})"
            textSize = 12.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            setPadding(0, dp(4), 0, dp(14))
        })

        val calStart = Calendar.getInstance().apply { timeInMillis = block.startMs }
        val calEnd = Calendar.getInstance().apply { timeInMillis = block.endMs }

        val timeRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        val startBtn = TextView(host).apply {
            text = "Start: ${TimeFormat.formatWallClock(host, calStart.timeInMillis)}"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                android.app.TimePickerDialog(host, { _, h, m ->
                    calStart.set(Calendar.HOUR_OF_DAY, h)
                    calStart.set(Calendar.MINUTE, m)
                    text = "Start: ${TimeFormat.formatWallClock(host, calStart.timeInMillis)}"
                }, calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE), false).show()
            }
        }
        timeRow.addView(startBtn)

        val endBtn = TextView(host).apply {
            text = "End: ${TimeFormat.formatWallClock(host, calEnd.timeInMillis)}"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(6), 0, 0, 0) }
            setOnClickListener {
                android.app.TimePickerDialog(host, { _, h, m ->
                    calEnd.set(Calendar.HOUR_OF_DAY, h)
                    calEnd.set(Calendar.MINUTE, m)
                    text = "End: ${TimeFormat.formatWallClock(host, calEnd.timeInMillis)}"
                }, calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE), false).show()
            }
        }
        timeRow.addView(endBtn)
        content.addView(timeRow)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        btnRow.addView(cancelBtn)

        val saveBtn = Button(host).apply {
            text = "Save"
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                if (calEnd.timeInMillis > calStart.timeInMillis) {
                    applyBlockEdit(dateStr, block, isBreak, calStart.timeInMillis, calEnd.timeInMillis)
                    dialog.dismiss()
                    onApplied?.invoke()
                } else {
                    Toast.makeText(host, "End time must be after start time", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(saveBtn)
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showDevTimelineEditor() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        showDayDialog(todayStr, "Today's Timeline")
    }

    fun formatBlockRow(startMs: Long, endMs: Long, secs: Long): String {
        return "${TimeFormat.formatWallClock(host, startMs)} – ${TimeFormat.formatWallClock(host, endMs)} (${formatDuration(secs)})"
    }

    fun formatDuration(secs: Long): String {
        if (secs < 60L) return "0m"
        val h = secs / 3600L
        val m = (secs % 3600L) / 60L
        return when {
            h > 0L && m > 0L -> "${h}h ${m}m"
            h > 0L -> "${h}h"
            else -> "${m}m"
        }
    }

    fun focusBlockLabels(): Array<String> = Array(24) { h -> "${h}:00 – ${(h + 1) % 24}:00" }
    fun focusBlockStartLabels(): Array<String> = Array(24) { h -> "${h}:00" }
    fun focusBlockRangeLabel(b: Int): String = "${b}:00 – ${(b + 1) % 24}:00"

    fun fillBlockRows(
        container: LinearLayout,
        sessions: List<BlockInfo>,
        breaks: List<BlockInfo>,
        dateStr: String? = null,
        onDelete: ((BlockInfo, Boolean) -> Unit)? = null
    ) {
        container.removeAllViews()
        val validSessions = sessions.filter { it.secs >= 60L }
        val validBreaks = breaks.filter { it.secs >= 60L }
        val rows = ArrayList<Pair<BlockInfo, Boolean>>()
        for (s in validSessions) rows.add(Pair(s, false))
        for (b in validBreaks) rows.add(Pair(b, true))
        rows.sortBy { it.first.startMs }

        var prevWasBreak = false
        for ((b, isBreak) in rows) {
            val blockLabel: String
            val blockColor: Int
            if (isBreak) {
                blockLabel = if (b.manual) "Break (Manual)" else "Break"
                blockColor = themeCoordinator.secondaryColor
            } else {
                val matchedSub = if (b.subjectId != null || (!b.subjectName.isNullOrBlank() && b.subjectName != "Focus")) {
                    SubjectTagManager.resolveSubject(host, b.subjectId, b.subjectName, b.subjectColor)
                } else null

                blockLabel = if (matchedSub != null) {
                    "${matchedSub.iconEmoji} ${matchedSub.name}"
                } else {
                    if (b.manual) "Focus (Manual)" else "Focus"
                }

                blockColor = try {
                    if (matchedSub != null && matchedSub.colorHex.isNotEmpty()) Color.parseColor(matchedSub.colorHex)
                    else themeCoordinator.primaryColor
                } catch (_: Exception) {
                    themeCoordinator.primaryColor
                }
            }

            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground(10f)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, if (prevWasBreak) dp(8) else dp(4), 0, 0)
                }
            }

            val blockTextView = TextView(host).apply {
                text = "$blockLabel  ${formatBlockRow(b.startMs, b.endMs, b.secs)}"
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setTextColor(blockColor)
                alpha = 0.95f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(blockTextView)

            if (onDelete != null && !b.running) {
                val delBtn = TextView(host).apply {
                    text = "✕"
                    textSize = 12f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(10), dp(4), dp(4), dp(4))
                    setOnClickListener { onDelete(b, isBreak) }
                }
                row.addView(delBtn)
            }

            if (dateStr != null && !b.running) {
                row.setOnClickListener {
                    showBlockEditDialog(dateStr, b, isBreak) {
                        // refresh handled by caller
                    }
                }
            }

            container.addView(row)
            prevWasBreak = isBreak
        }
    }

    fun confirmDeleteBlock(dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) {
        val kind = if (isBreak) "break" else "focus"
        DeveloperToolsHelper.showThemedConfirmDialog(
            activity = host,
            themeCoordinator = themeCoordinator,
            title = "Delete $kind Interval?",
            message = "Remove interval from ${TimeFormat.formatWallClock(host, block.startMs)} to ${TimeFormat.formatWallClock(host, block.endMs)} (${formatDuration(block.secs)})?",
            confirmText = "Delete",
            isDestructive = true
        ) {
            TimelineLogger.deleteBlock(host, block.startMs, block.endMs)
            reconcileDayTotals(dateStr)
            if (!isBreak) {
                val subId = block.subjectId ?: SubjectTagManager.getSelectedSubject(host).id
                val curSubjMap = SubjectTagManager.getSubjectDurationsForDate(host, dateStr)
                val curSubjSecs = curSubjMap[subId] ?: 0L
                val deduct = Math.min(curSubjSecs, block.secs)
                if (deduct > 0) SubjectTagManager.adjustSubjectStudyTime(host, subId, -deduct, dateStr)
            }
            host.invalidateStatsCache()
            host.refreshStatsPanel()
            host.recalculateStreak()
            host.checkCelebration()
            StudyWidgetProvider.refresh(host)
            Toast.makeText(host, "Interval deleted", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    fun confirmDeleteDay(dateStr: String, label: String) {
        DeveloperToolsHelper.showThemedConfirmDialog(
            activity = host,
            themeCoordinator = themeCoordinator,
            title = "Delete $label Records?",
            message = "Are you sure you want to delete all study and break records recorded for $dateStr?",
            confirmText = "Delete All",
            isDestructive = true
        ) {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
                remove("${dateStr}_focus_total")
                remove("${dateStr}_break_total")
                remove("${dateStr}_focus_manual")
                remove("${dateStr}_break_manual")
            }.apply()
            TimelineLogger.deleteDay(host, dateStr)
            SubjectTagManager.clearTodaySubjectDurations(host, dateStr)
            reconcileDayTotals(dateStr)
            host.invalidateStatsCache()
            host.refreshStatsPanel()
            host.recalculateStreak()
            Toast.makeText(host, "Day records deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun showDayDialog(dateStr: String, label: String) {
        val shared = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = host.dateKeyFmt.format(Date())
        val focusSecs = shared.getLong("${dateStr}_focus_total", 0L) + (if (dateStr == todayStr) host.accumulatedStudy else 0L)
        val breakSecs = shared.getLong("${dateStr}_break_total", 0L) + (if (dateStr == todayStr) host.currentBreakSeconds else 0L)
        val (allSessions, allBreaks) = dayBlocks(dateStr)
        val sessions = allSessions.filter { it.secs >= 60L }
        val breaks = allBreaks.filter { it.secs >= 60L }
        val longest = sessions.maxOfOrNull { it.secs } ?: 0L
        val goal = host.resolveGoalFor(dateStr)
        val goalReached = goal > 0L && focusSecs >= goal
        val goalColor = if (goalReached) 0xFF43D36E.toInt() else 0xFFFF4D4D.toInt()

        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        // Header Row with Date Label & Close Button
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        headerRow.addView(TextView(host).apply {
            text = label
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val closeTopBtn = TextView(host).apply {
            text = "✕"
            textSize = 15f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeTopBtn)
        content.addView(headerRow)

        // Daily Goal details & status
        val goalLabel = host.formatGoalLabel(goal)
        val focusLabel = if (focusSecs >= 3600) "${focusSecs / 3600}h ${(focusSecs % 3600) / 60}m" else "${(focusSecs % 3600) / 60}m"
        val pct = if (goal > 0L) (focusSecs.toFloat() / goal.toFloat() * 100f).toInt() else 0

        content.addView(TextView(host).apply {
            text = if (goalReached) {
                "Daily Goal: $goalLabel • Reached ($focusLabel, $pct%)"
            } else if (goal > 0L) {
                val remaining = max(0L, goal - focusSecs)
                val toGo = "${host.formatGoalLabel(remaining)} left"
                "Daily Goal: $goalLabel • $pct% ($toGo)"
            } else {
                "Total Study: $focusLabel (No goal set)"
            }
            setTextColor(goalColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        // Progress bar for daily goal
        if (goal > 0L) {
            val barPct = (focusSecs.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
            val bar = FrameLayout(host).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(4).toFloat()
                    setColor(tintedColor(themeCoordinator.textColor, 26))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply {
                    setMargins(0, dp(7), 0, 0)
                }
            }
            val fill = View(host).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(4).toFloat()
                    setColor(goalColor)
                }
            }
            bar.addView(fill, FrameLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.START))
            bar.post {
                val target = (bar.width * barPct).toInt()
                if (target > 0) {
                    val lp = fill.layoutParams as FrameLayout.LayoutParams
                    lp.width = target.coerceAtLeast(dp(6))
                    fill.layoutParams = lp
                }
            }
            content.addView(bar)
        }

        // Focus & Break Summary
        content.addView(TextView(host).apply {
            text = host.getString(R.string.focus_break_summary, focusSecs / 3600, (focusSecs % 3600) / 60, breakSecs / 3600, (breakSecs % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            textSize = 14.5f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        })

        // Sessions count & longest session
        content.addView(TextView(host).apply {
            text = host.getString(R.string.sessions_summary, sessions.size, longest / 3600, (longest % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(4))
        })

        // Expandable Session Logs dropdown
        if (sessions.isEmpty() && breaks.isEmpty()) {
            content.addView(TextView(host).apply {
                text = host.getString(R.string.no_session_log_day)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            })
        } else {
            val logsRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(4))
            }
            val logsChevron = TextView(host).apply {
                text = "▾"
                textSize = 13f
                setTextColor(themeCoordinator.primaryColor)
            }
            logsRow.addView(TextView(host).apply {
                text = host.getString(R.string.cal_see_logs)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13.5f
                letterSpacing = 0.08f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            logsRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(dp(6), 0) })
            logsRow.addView(logsChevron)

            val logsScroll = ScrollView(host).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180))
                visibility = View.GONE
            }
            val logsBox = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(4), 0, 0)
            }
            logsScroll.addView(logsBox)

            fun refreshLogs() {
                val (curSessions, curBreaks) = dayBlocks(dateStr)
                fillBlockRows(logsBox, curSessions, curBreaks, dateStr = dateStr, onDelete = { b, isBrk ->
                    confirmDeleteBlock(dateStr, b, isBrk) {
                        refreshLogs()
                    }
                })
            }
            refreshLogs()

            logsRow.setOnClickListener {
                val show = logsScroll.visibility != View.VISIBLE
                logsScroll.visibility = if (show) View.VISIBLE else View.GONE
                logsChevron.text = if (show) "▴" else "▾"
            }

            content.addView(logsRow)
            content.addView(logsScroll)
        }

        // View Subject Breakdown Button
        val seePieChartBtn = TextView(host).apply {
            text = "View Subject Breakdown"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 16f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(14), 0, 0)
            }
            setOnClickListener {
                dialog.dismiss()
                host.showPieChartDetailsModal(dateStr)
            }
        }
        content.addView(seePieChartBtn)

        // Bottom Action Buttons (Clear Day & Close)
        val bottomRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }

        val delDayBtn = Button(host).apply {
            text = "Clear Day"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 12.5f
            background = null
            setOnClickListener {
                dialog.dismiss()
                confirmDeleteDay(dateStr, label)
            }
        }
        bottomRow.addView(delDayBtn)

        bottomRow.addView(View(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })

        val closeBtn = TextView(host).apply {
            text = "Close"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12.5f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(tintedColor(themeCoordinator.primaryColor, 26))
            }
            setPadding(dp(20), dp(8), dp(20), dp(8))
            setOnClickListener { dialog.dismiss() }
        }
        bottomRow.addView(closeBtn)
        content.addView(bottomRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(400))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        content.addView(TextView(host).apply {
            text = mName
            setTextColor(themeCoordinator.primaryColor)
            textSize = 15f
            letterSpacing = 0.1f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = host.getString(R.string.focus_break_summary, focusSecs / 3600, (focusSecs % 3600) / 60, breakSecs / 3600, (breakSecs % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            textSize = 14.5f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        })

        val shared = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val daySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val todayStr = daySdf.format(Date())
        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthLongSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val days = shared.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { key ->
                val dStr = key.removeSuffix("_focus_total")
                val parsed = try { daySdf.parse(dStr) } catch (_: Exception) { null }
                if (parsed != null && (monthSdf.format(parsed) == mName || monthLongSdf.format(parsed) == mName)) dStr to parsed else null
            }
            .sortedBy { it.second.time }

        val dayList = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0) }
        if (days.isEmpty()) {
            dayList.addView(TextView(host).apply {
                text = host.getString(R.string.no_days_this_month)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(8), 0, 0)
            })
        } else {
            for ((dStr, parsed) in days) {
                var f = shared.getLong("${dStr}_focus_total", 0L)
                var b = shared.getLong("${dStr}_break_total", 0L)
                if (dStr == todayStr) {
                    f += host.accumulatedStudy
                    b += host.currentBreakSeconds
                }
                if (f <= 0L && b <= 0L) continue
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(5), 0, dp(5))
                }
                row.addView(TextView(host).apply {
                    text = host.getString(R.string.day_row_summary, dayLabelSdf.format(parsed), f / 3600, (f % 3600) / 60, b / 3600, (b % 3600) / 60)
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.setOnClickListener {
                    dialog.dismiss()
                    showDayDialog(dStr, dayLabelSdf.format(parsed))
                }
                dayList.addView(row)
            }
        }
        val dayScroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
            isVerticalScrollBarEnabled = false
        }
        dayScroll.addView(dayList)
        content.addView(dayScroll)

        content.addView(TextView(host).apply {
            text = "Close"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(tintedColor(themeCoordinator.primaryColor, 26))
            }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(14), 0, 0)
            }
            setOnClickListener { dialog.dismiss() }
        })

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
