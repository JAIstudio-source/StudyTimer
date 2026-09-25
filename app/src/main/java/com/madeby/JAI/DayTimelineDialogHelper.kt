package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DayTimelineDialogHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val statsEngine get() = host.statsEngine

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)

    fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> = statsEngine.dayBlocks(dateStr)

    fun reconcileDayTotals(dateStr: String) {
        // Reconcile aggregated day totals from granular timeline
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
            text = if (isBreak) "☕ Edit Break Interval" else "⏱️ Edit Study Session"
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
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showDevTimelineEditor() {
        // Timeline developer editor trigger
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        showDayDialog(todayStr, "Today's Timeline")
    }

    fun formatBlockRow(startMs: Long, endMs: Long, secs: Long): String {
        return "${TimeFormat.formatWallClock(host, startMs)} – ${TimeFormat.formatWallClock(host, endMs)} (${formatDuration(secs)})"
    }

    fun formatDuration(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
    }

    fun focusBlockLabels(): Array<String> = Array(24) { h -> "${h}:00 – ${(h + 1) % 24}:00" }
    fun focusBlockStartLabels(): Array<String> = Array(24) { h -> "${h}:00" }
    fun focusBlockRangeLabel(b: Int): String = "${b}:00 – ${(b + 1) % 24}:00"

    fun fillBlockRows(
        container: LinearLayout,
        sessions: List<BlockInfo>,
        breaks: List<BlockInfo>,
        onDelete: ((BlockInfo, Boolean) -> Unit)? = null
    ) {
        container.removeAllViews()
        val allItems = (sessions.map { it to false } + breaks.map { it to true }).sortedBy { it.first.startMs }

        for ((block, isBreak) in allItems) {
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground(10f)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(4))
                }
            }

            val icon = TextView(host).apply {
                text = if (isBreak) "☕" else "⏱️"
                textSize = 14f
                setPadding(0, 0, dp(8), 0)
            }
            row.addView(icon)

            val blockTextView = TextView(host).apply {
                this.text = formatBlockRow(block.startMs, block.endMs, block.secs)
                textSize = 12.5f
                setTextColor(themeCoordinator.textColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(blockTextView)

            if (onDelete != null) {
                val delBtn = TextView(host).apply {
                    this.text = "✕"
                    textSize = 12f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener { onDelete(block, isBreak) }
                }
                row.addView(delBtn)
            }
            container.addView(row)
        }
    }

    fun confirmDeleteBlock(dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) {
        DeveloperToolsHelper.showThemedConfirmDialog(
            activity = host,
            themeCoordinator = themeCoordinator,
            title = "Delete Interval?",
            message = "Remove interval from ${TimeFormat.formatWallClock(host, block.startMs)} to ${TimeFormat.formatWallClock(host, block.endMs)}?",
            confirmText = "Delete",
            isDestructive = true
        ) {
            TimelineLogger.deleteBlock(host, block.startMs, block.endMs)
            reconcileDayTotals(dateStr)
            host.invalidateStatsCache()
            host.refreshStatsPanel()
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
            TimelineLogger.deleteDay(host, dateStr)
            reconcileDayTotals(dateStr)
            host.invalidateStatsCache()
            host.refreshStatsPanel()
        }
    }

    fun showDayDialog(dateStr: String, label: String) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        headerRow.addView(TextView(host).apply {
            text = "📅 $label"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val closeBtn = TextView(host).apply {
            text = "✕"
            textSize = 15f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        content.addView(headerRow)

        val (sessions, breaks) = dayBlocks(dateStr)
        val fTotal = sessions.sumOf { it.secs }
        val bTotal = breaks.sumOf { it.secs }

        val summaryText = TextView(host).apply {
            text = "Total Focus: ${formatDuration(fTotal)} · Total Break: ${formatDuration(bTotal)}"
            textSize = 12.5f
            setTextColor(themeCoordinator.primaryColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        content.addView(summaryText)

        val scroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220))
        }
        val rowsBox = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

        fun refreshRows() {
            val (curSessions, curBreaks) = dayBlocks(dateStr)
            fillBlockRows(rowsBox, curSessions, curBreaks) { b, isBrk ->
                confirmDeleteBlock(dateStr, b, isBrk) {
                    refreshRows()
                }
            }
        }
        refreshRows()
        scroll.addView(rowsBox)
        content.addView(scroll)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }

        val delDayBtn = Button(host).apply {
            text = "Clear Day"
            setTextColor(Color.parseColor("#EF4444"))
            background = null
            setOnClickListener {
                dialog.dismiss()
                confirmDeleteDay(dateStr, label)
            }
        }
        btnRow.addView(delDayBtn)

        val doneBtn = Button(host).apply {
            text = "Done"
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener { dialog.dismiss() }
        }
        btnRow.addView(doneBtn)
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "🗓️ $mName Overview"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })

        content.addView(TextView(host).apply {
            text = "Monthly focus time and break statistics breakdown:"
            textSize = 12.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(0, dp(3), 0, dp(14))
        })

        val fCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
        }
        fCard.addView(TextView(host).apply {
            text = "TOTAL FOCUS TIME"
            textSize = 11f
            setTextColor(themeCoordinator.primaryColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        fCard.addView(TextView(host).apply {
            text = formatDuration(focusSecs)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(fCard)

        val bCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
        bCard.addView(TextView(host).apply {
            text = "TOTAL BREAK TIME"
            textSize = 11f
            setTextColor(themeCoordinator.secondaryColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        bCard.addView(TextView(host).apply {
            text = formatDuration(breakSecs)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(bCard)

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
