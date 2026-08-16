package com.madeby.JAI

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

object DeveloperToolsHelper {

    fun buildDevCard(activity: MainActivity, themeCoordinator: ThemeCoordinator): View {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }

        val devCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(16), 0, 0)
            }
        }

        val devTitle = TextView(activity).apply {
            text = "🛠️ Developer Tools"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        devCard.addView(devTitle)

        val toolRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(10), 0, 0)
            }
        }

        val editorBtn = Button(activity).apply {
            text = "✎ Edit Timeline"
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            setTextColor(if (themeCoordinator.isDarkMode()) Color.BLACK else Color.WHITE)
            setPadding(dp(10), dp(12), dp(10), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(4), 0)
            }
            setOnClickListener { showDevTimelineEditor(activity, themeCoordinator) }
        }

        val modeToggleBtn = Button(activity).apply {
            fun updateModeStyle() {
                text = if (activity.isAdjustingFocusMode) "🎯 Focus Pool" else "☕ Break Pool"
                background = themeCoordinator.createButtonBackground(
                    if (activity.isAdjustingFocusMode) themeCoordinator.primaryColor else themeCoordinator.accentColor
                )
                setTextColor(if (themeCoordinator.isDarkMode()) Color.BLACK else Color.WHITE)
            }
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(12), dp(10), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, 0, 0)
            }
            updateModeStyle()
            setOnClickListener {
                activity.isAdjustingFocusMode = !activity.isAdjustingFocusMode
                updateModeStyle()
            }
        }

        toolRow.addView(editorBtn)
        toolRow.addView(modeToggleBtn)
        devCard.addView(toolRow)

        devCard.addView(TextView(activity).apply {
            text = activity.getString(R.string.shift_block_hint)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 11f
            setPadding(0, dp(10), 0, 0)
        })

        fun applyShift(ds: Long) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isFocus = activity.isAdjustingFocusMode

            val (existingSessions, existingBreaks) = StatsEngine(activity).dayBlocks(todayStr)
            val currentPoolSecs = if (isFocus) existingSessions.filter { !it.running }.sumOf { it.secs } else existingBreaks.filter { !it.running }.sumOf { it.secs }
            val newPoolSecs = (currentPoolSecs + ds).coerceAtLeast(0L)
            val actualDelta = newPoolSecs - currentPoolSecs

            if (actualDelta == 0L && ds != 0L) {
                Toast.makeText(activity, "Pool is already at 0", Toast.LENGTH_SHORT).show()
                return
            }

            if (isFocus) {
                val selectedSubject = SubjectTagManager.getSelectedSubject(activity)
                if (actualDelta > 0) {
                    TimelineLogger.appendBlockForDay(activity, todayStr, actualDelta, "MANUAL_FOCUS")
                    SubjectTagManager.recordSubjectStudyTime(activity, selectedSubject.id, actualDelta, todayStr)
                } else if (actualDelta < 0) {
                    var needed = -actualDelta
                    for (s in existingSessions.reversed()) {
                        if (needed <= 0) break
                        if (s.secs <= needed) {
                            TimelineLogger.deleteBlock(activity, s.startMs, s.endMs)
                            needed -= s.secs
                        } else {
                            TimelineLogger.replaceBlock(activity, s.startMs, s.endMs, s.startMs, s.endMs - needed * 1000L, if (s.manual) "MANUAL_FOCUS" else "STUDYING")
                            needed = 0
                        }
                    }
                    val curSubjMap = SubjectTagManager.getSubjectDurationsForDate(activity, todayStr)
                    val curSubjSecs = curSubjMap[selectedSubject.id] ?: 0L
                    val deduct = Math.min(curSubjSecs, -actualDelta)
                    if (deduct > 0) {
                        SubjectTagManager.recordSubjectStudyTime(activity, selectedSubject.id, -deduct, todayStr)
                    }
                }
            } else {
                if (actualDelta > 0) {
                    TimelineLogger.appendBlockForDay(activity, todayStr, actualDelta, "MANUAL_BREAK")
                } else if (actualDelta < 0) {
                    var needed = -actualDelta
                    for (b in existingBreaks.reversed()) {
                        if (needed <= 0) break
                        if (b.secs <= needed) {
                            TimelineLogger.deleteBlock(activity, b.startMs, b.endMs)
                            needed -= b.secs
                        } else {
                            TimelineLogger.replaceBlock(activity, b.startMs, b.endMs, b.startMs, b.endMs - needed * 1000L, if (b.manual) "MANUAL_BREAK" else "BREAK")
                            needed = 0
                        }
                    }
                }
            }

            StatsEngine(activity).forceReconcileDayTotals(todayStr)
            activity.statsDirty = true
            activity.statsSnapshotCache = null
            activity.tabPageCache.clear()
            activity.recalculateStreak()
            activity.checkCelebration()
            StudyWidgetProvider.refresh(activity)
            val what = if (isFocus) "Focus Pool" else "Break Pool"
            val sign = if (actualDelta > 0) "+" else ""
            val mins = actualDelta / 60
            val totalStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", newPoolSecs / 3600, (newPoolSecs % 3600) / 60, newPoolSecs % 60)
            Toast.makeText(activity, "$what: $sign${mins}m (Today: $totalStr)", Toast.LENGTH_SHORT).show()
        }

        fun makeIncBtn(l: String, ds: Long): Button {
            return Button(activity).apply {
                text = l
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                typeface = Typeface.MONOSPACE
                background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(6, 6, 6, 6) }
                setOnClickListener { applyShift(ds) }
            }
        }

        val row1 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6, 0, 0) }
        }
        row1.addView(makeIncBtn("- 1h", -3600L))
        row1.addView(makeIncBtn("-15m", -900L))

        val row2 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row2.addView(makeIncBtn("+15m", 900L))
        row2.addView(makeIncBtn("+ 1h", 3600L))

        devCard.addView(row1)
        devCard.addView(row2)

        val subTitle = TextView(activity).apply {
            text = "📚 Add Desired Time to Subject"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(6))
        }
        devCard.addView(subTitle)

        val subjectsList = SubjectTagManager.getAllSubjects(activity)
        var currentSubIndex = 0

        val subSelectBtn = TextView(activity).apply {
            val cur = if (subjectsList.isNotEmpty()) subjectsList[0] else SubjectTagManager.DEFAULT_SUBJECTS[0]
            text = "Subject: ${cur.iconEmoji} ${cur.name}"
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            background = themeCoordinator.createGlassChip(activity.tintedColor(themeCoordinator.primaryColor, 60), 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                if (subjectsList.isNotEmpty()) {
                    currentSubIndex = (currentSubIndex + 1) % subjectsList.size
                    val s = subjectsList[currentSubIndex]
                    text = "Subject: ${s.iconEmoji} ${s.name}"
                }
            }
        }
        devCard.addView(subSelectBtn)

        fun addSubjectTime(secs: Long) {
            val curSub = if (subjectsList.isNotEmpty()) subjectsList[currentSubIndex] else SubjectTagManager.DEFAULT_SUBJECTS[0]
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            TimelineLogger.appendBlockForDay(activity, todayStr, secs, "MANUAL_FOCUS")
            SubjectTagManager.recordSubjectStudyTime(activity, curSub.id, secs, todayStr)

            StatsEngine(activity).forceReconcileDayTotals(todayStr)
            activity.statsDirty = true
            activity.statsSnapshotCache = null
            activity.tabPageCache.clear()
            activity.recalculateStreak()
            activity.checkCelebration()
            StudyWidgetProvider.refresh(activity)

            Toast.makeText(activity, "Added ${secs / 60}m to ${curSub.iconEmoji} ${curSub.name}", Toast.LENGTH_SHORT).show()
        }

        val subRow1 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
        }
        fun makeSubBtn(l: String, ds: Long): Button {
            return Button(activity).apply {
                text = l
                setTextColor(themeCoordinator.textColor)
                textSize = 11f
                typeface = Typeface.MONOSPACE
                background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 4, 4, 4) }
                setOnClickListener { addSubjectTime(ds) }
            }
        }
        subRow1.addView(makeSubBtn("+15m", 900L))
        subRow1.addView(makeSubBtn("+30m", 1800L))
        subRow1.addView(makeSubBtn("+1h", 3600L))
        devCard.addView(subRow1)

        val subRow2 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val customMinsBtn = Button(activity).apply {
            text = "⏱ Custom Mins..."
            setTextColor(themeCoordinator.textColor)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 4, 4, 4) }
            setOnClickListener {
                val input = EditText(activity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    hint = "Enter minutes (e.g. 45)"
                }
                androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Add Minutes to Subject")
                    .setView(input)
                    .setPositiveButton("Add") { _, _ ->
                        val mins = input.text.toString().toLongOrNull() ?: 0L
                        if (mins > 0) addSubjectTime(mins * 60L)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val windowBtn = Button(activity).apply {
            text = "🕒 Time Window..."
            setTextColor(themeCoordinator.textColor)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 4, 4, 4) }
            setOnClickListener {
                val is24 = TimeFormat.is24Hour(activity)
                val nowCal = Calendar.getInstance()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val curSub = if (subjectsList.isNotEmpty()) subjectsList[currentSubIndex] else SubjectTagManager.DEFAULT_SUBJECTS[0]

                TimePickerDialog(activity, { _, h, m ->
                    val startMs = activity.msForDateAndTime(todayStr, h, m)
                    TimePickerDialog(activity, { _, h2, m2 ->
                        val endMs = activity.msForDateAndTime(todayStr, h2, m2)
                        if (endMs <= startMs) {
                            Toast.makeText(activity, activity.getString(R.string.toast_end_after_start), Toast.LENGTH_SHORT).show()
                        } else {
                            TimelineLogger.addBlock(activity, startMs, endMs, "MANUAL_FOCUS")
                            val secs = (endMs - startMs) / 1000L
                            SubjectTagManager.recordSubjectStudyTime(activity, curSub.id, secs, todayStr)
                            StatsEngine(activity).forceReconcileDayTotals(todayStr)
                            activity.statsDirty = true
                            activity.statsSnapshotCache = null
                            activity.tabPageCache.clear()
                            activity.recalculateStreak()
                            activity.checkCelebration()
                            StudyWidgetProvider.refresh(activity)
                            Toast.makeText(activity, "Added ${secs / 60}m block to ${curSub.iconEmoji} ${curSub.name}", Toast.LENGTH_SHORT).show()
                        }
                    }, nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE), is24).apply { setTitle("Window End Time") }.show()
                }, nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE), is24).apply { setTitle("Window Start Time") }.show()
            }
        }

        subRow2.addView(customMinsBtn)
        subRow2.addView(windowBtn)
        devCard.addView(subRow2)

        return devCard
    }

    fun showDevTimelineEditor(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val todayStr = dateSdf.format(Date())
        var selectedStr = todayStr

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(18), dp(18), dp(18), dp(16))
        }
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.timeline_editor)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        var render: () -> Unit = {}

        fun parseSelected(): Calendar {
            val c = Calendar.getInstance()
            c.time = try { dateSdf.parse(selectedStr) ?: Date() } catch (_: Exception) { Date() }
            return c
        }

        val dateLabel = TextView(activity).apply {
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val c = parseSelected()
                android.app.DatePickerDialog(
                    activity,
                    { _, y, m, d ->
                        val nc = Calendar.getInstance()
                        nc.set(y, m, d, 0, 0, 0)
                        nc.set(Calendar.MILLISECOND, 0)
                        selectedStr = dateSdf.format(nc.time)
                        render()
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        fun makeDayNav(label: String, delta: Int): TextView = TextView(activity).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                val c = parseSelected()
                c.add(Calendar.DAY_OF_YEAR, delta)
                selectedStr = dateSdf.format(c.time)
                render()
            }
        }
        val todayBtn = TextView(activity).apply {
            text = activity.getString(R.string.today_label)
            textSize = 11f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                selectedStr = todayStr
                render()
            }
        }
        val dateRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
        }
        dateRow.addView(makeDayNav("\u25C0", -1))
        dateRow.addView(dateLabel)
        dateRow.addView(makeDayNav("\u25B6", 1))
        dateRow.addView(todayBtn)
        content.addView(dateRow)

        val summaryBanner = TextView(activity).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            setPadding(0, dp(4), 0, dp(4))
        }
        content.addView(summaryBanner)

        val blockList = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220)).apply { setMargins(0, dp(4), 0, 0) }
            addView(blockList)
        }
        content.addView(scroll)

        fun quickAdd(secs: Long, isBreak: Boolean) {
            val state = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
            TimelineLogger.appendBlockForDay(activity, selectedStr, secs, state)
            if (!isBreak) {
                val s = SubjectTagManager.getSelectedSubject(activity)
                SubjectTagManager.recordSubjectStudyTime(activity, s.id, secs, selectedStr)
            }
            StatsEngine(activity).forceReconcileDayTotals(selectedStr)
            activity.statsDirty = true
            activity.statsSnapshotCache = null
            activity.tabPageCache.clear()
            activity.recalculateStreak()
            activity.checkCelebration()
            StudyWidgetProvider.refresh(activity)
            render()
            val kind = if (isBreak) "Break" else "Focus"
            Toast.makeText(activity, "Added ${secs / 60}m $kind to $selectedStr", Toast.LENGTH_SHORT).show()
        }

        fun customWindowAdd(isBreak: Boolean) {
            val is24 = TimeFormat.is24Hour(activity)
            val nowCal = Calendar.getInstance()
            val kind = if (isBreak) "break" else "focus"
            TimePickerDialog(
                activity,
                { _, h, m ->
                    val newStartMs = activity.msForDateAndTime(selectedStr, h, m)
                    TimePickerDialog(
                        activity,
                        { _, h2, m2 ->
                            val newEndMs = activity.msForDateAndTime(selectedStr, h2, m2)
                            if (newEndMs <= newStartMs) {
                                Toast.makeText(activity, activity.getString(R.string.toast_end_after_start), Toast.LENGTH_SHORT).show()
                            } else {
                                val state = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
                                TimelineLogger.addBlock(activity, newStartMs, newEndMs, state)
                                if (!isBreak) {
                                    val addedSecs = (newEndMs - newStartMs) / 1000L
                                    val s = SubjectTagManager.getSelectedSubject(activity)
                                    SubjectTagManager.recordSubjectStudyTime(activity, s.id, addedSecs, selectedStr)
                                }
                                StatsEngine(activity).forceReconcileDayTotals(selectedStr)
                                activity.statsDirty = true
                                activity.statsSnapshotCache = null
                                activity.tabPageCache.clear()
                                activity.recalculateStreak()
                                activity.checkCelebration()
                                StudyWidgetProvider.refresh(activity)
                                render()
                                Toast.makeText(activity, activity.getString(R.string.toast_kind_block_added, kind), Toast.LENGTH_SHORT).show()
                            }
                        },
                        nowCal.get(Calendar.HOUR_OF_DAY),
                        nowCal.get(Calendar.MINUTE),
                        is24
                    ).apply { setTitle(activity.getString(R.string.block_end)) }.show()
                },
                nowCal.get(Calendar.HOUR_OF_DAY),
                nowCal.get(Calendar.MINUTE),
                is24
            ).apply { setTitle(activity.getString(R.string.block_start)) }.show()
        }

        fun makeSmallBtn(label: String, onClick: () -> Unit): Button = Button(activity).apply {
            text = label
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(2, 2, 2, 2) }
            setOnClickListener { onClick() }
        }

        val focusPresetsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
        }
        focusPresetsRow.addView(makeSmallBtn("+15m Focus") { quickAdd(900L, false) })
        focusPresetsRow.addView(makeSmallBtn("+30m Focus") { quickAdd(1800L, false) })
        focusPresetsRow.addView(makeSmallBtn("+1h Focus") { quickAdd(3600L, false) })
        content.addView(focusPresetsRow)

        val breakPresetsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        breakPresetsRow.addView(makeSmallBtn("+15m Break") { quickAdd(900L, true) })
        breakPresetsRow.addView(makeSmallBtn("+30m Break") { quickAdd(1800L, true) })
        breakPresetsRow.addView(makeSmallBtn("+1h Break") { quickAdd(3600L, true) })
        content.addView(breakPresetsRow)

        val customRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        customRow.addView(makeSmallBtn("🕒 Custom Focus...") { customWindowAdd(false) })
        customRow.addView(makeSmallBtn("☕ Custom Break...") { customWindowAdd(true) })
        content.addView(customRow)

        render = {
            val isToday = selectedStr == todayStr
            val parsed = try { dateSdf.parse(selectedStr) } catch (_: Exception) { null }
            val display = if (parsed != null) displaySdf.format(parsed) else selectedStr
            dateLabel.text = if (isToday) activity.getString(R.string.today_prefix, display) else display
            dateLabel.setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)

            val (sessions, breaks) = activity.dayBlocks(selectedStr)
            val focusTotal = sessions.filter { !it.running }.sumOf { it.secs }
            val breakTotal = breaks.filter { !it.running }.sumOf { it.secs }
            val fStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", focusTotal / 3600, (focusTotal % 3600) / 60, focusTotal % 60)
            val bStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", breakTotal / 3600, (breakTotal % 3600) / 60, breakTotal % 60)
            summaryBanner.text = "🎯 Focus: $fStr   ☕ Break: $bStr"

            blockList.removeAllViews()
            val rows = ArrayList<Pair<BlockInfo, Boolean>>()
            for (s in sessions) rows.add(Pair(s, false))
            for (b in breaks) rows.add(Pair(b, true))
            rows.sortBy { it.first.startMs }

            if (rows.isEmpty()) {
                blockList.addView(TextView(activity).apply {
                    text = activity.getString(R.string.no_blocks_for_day)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 12f
                    setPadding(0, dp(12), 0, 0)
                })
            }
            for ((b, isBreak) in rows) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(4), 0, dp(4))
                }
                val label = when {
                    b.manual -> activity.getString(R.string.block_manual)
                    isBreak -> activity.getString(R.string.block_break)
                    else -> activity.getString(R.string.block_focus)
                }
                row.addView(TextView(activity).apply {
                    text = label + "  " + formatBlockRow(activity, b.startMs, b.endMs, b.secs)
                    setTextColor(if (isBreak) themeCoordinator.secondaryColor else themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.9f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(activity).apply {
                    text = "\u270E"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14f
                    alpha = if (b.running) 0.2f else 0.8f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        if (b.running) {
                            Toast.makeText(activity, activity.getString(R.string.toast_open_block_hint), Toast.LENGTH_SHORT).show()
                        } else {
                            showBlockEditDialog(activity, themeCoordinator, selectedStr, b, isBreak) { render() }
                        }
                    }
                })
                row.addView(TextView(activity).apply {
                    text = "\u2715"
                    setTextColor(Color.parseColor("#FF4D4D"))
                    textSize = 14f
                    alpha = if (b.running) 0.2f else 0.8f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        if (b.running) {
                            Toast.makeText(activity, activity.getString(R.string.toast_open_block_hint), Toast.LENGTH_SHORT).show()
                        } else {
                            confirmDeleteBlock(activity, themeCoordinator, selectedStr, b, isBreak) { render() }
                        }
                    }
                })
                blockList.addView(row)
            }
        }

        content.addView(Button(activity).apply {
            text = activity.getString(R.string.btn_close)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 12f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(themeCoordinator.primaryColor)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        render()
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showBlockEditDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator, dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) {
        val kind = if (isBreak) "break" else "focus"
        val is24 = TimeFormat.is24Hour(activity)
        val startCal = Calendar.getInstance().apply { timeInMillis = block.startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = block.endMs }
        TimePickerDialog(
            activity,
            { _, h, m ->
                val newStartMs = activity.msForDateAndTime(dateStr, h, m)
                TimePickerDialog(
                    activity,
                    { _, h2, m2 ->
                        val newEndMs = activity.msForDateAndTime(dateStr, h2, m2)
                        if (newEndMs <= newStartMs) {
                            Toast.makeText(activity, activity.getString(R.string.toast_end_after_start), Toast.LENGTH_SHORT).show()
                        } else {
                            activity.applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
                            Toast.makeText(activity, activity.getString(R.string.toast_kind_block_updated, kind), Toast.LENGTH_SHORT).show()
                            onApplied?.invoke() ?: activity.navigateToPanel(AppPanel.STATS)
                        }
                    },
                    endCal.get(Calendar.HOUR_OF_DAY),
                    endCal.get(Calendar.MINUTE),
                    is24
                ).apply { setTitle(activity.getString(R.string.block_end)) }.show()
            },
            startCal.get(Calendar.HOUR_OF_DAY),
            startCal.get(Calendar.MINUTE),
            is24
        ).apply { setTitle(activity.getString(R.string.block_start)) }.show()
    }

    private fun confirmDeleteBlock(activity: MainActivity, themeCoordinator: ThemeCoordinator, dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) {
        val kind = if (isBreak) "break" else "focus"
        activity.showConfirmDialog(
            activity.getString(R.string.confirm_delete_block, kind),
            activity.getString(R.string.confirm_delete_block_msg, formatBlockRow(activity, block.startMs, block.endMs, block.secs))
        ) {
            TimelineLogger.deleteBlock(activity, block.startMs, block.endMs)
            activity.statsEngine.forceReconcileDayTotals(dateStr)
            if (!isBreak) {
                val s = SubjectTagManager.getSelectedSubject(activity)
                val curSubjMap = SubjectTagManager.getSubjectDurationsForDate(activity, dateStr)
                val curSubjSecs = curSubjMap[s.id] ?: 0L
                val deduct = Math.min(curSubjSecs, block.secs)
                if (deduct > 0) SubjectTagManager.recordSubjectStudyTime(activity, s.id, -deduct, dateStr)
            }
            activity.statsDirty = true
            activity.statsSnapshotCache = null
            activity.tabPageCache.clear()
            activity.recalculateStreak()
            activity.checkCelebration()
            StudyWidgetProvider.refresh(activity)
            Toast.makeText(activity, activity.getString(R.string.toast_kind_block_deleted, kind), Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    fun formatBlockRow(context: Context, startMs: Long, endMs: Long, secs: Long): String {
        val dur = when {
            secs >= 3600 -> context.getString(R.string.duration_h_m, secs / 3600, (secs % 3600) / 60)
            secs >= 60 -> context.getString(R.string.duration_m, secs / 60)
            else -> context.getString(R.string.duration_s, secs)
        }
        return "${TimeFormat.formatWallClock(context, startMs)} \u2013 ${TimeFormat.formatWallClock(context, endMs)} \u00B7 $dur"
    }
}
