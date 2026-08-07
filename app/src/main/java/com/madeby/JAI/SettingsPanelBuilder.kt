package com.madeby.JAI

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class SettingsPanelBuilder(private val host: MainActivity) {

    fun build(target: android.view.ViewGroup = host.panelContainer, captureScrollRef: Boolean = true) {
        with(host) {
        if (captureScrollRef) {
            tabPageCache.keys.removeIf { it.startsWith("ST:") }
        }
        val settingsRootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val headerText = TextView(this).apply {
            text = getString(R.string.settings_title)
            setTextColor(themeCoordinator.textColor)
            textSize = 22f
            letterSpacing = 0.02f
            setPadding(dp(6), dp(16), dp(6), dp(4))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setOnLongClickListener {
                isDevModeUnlocked = true
                Toast.makeText(context, getString(R.string.toast_dev_config_enabled), Toast.LENGTH_SHORT).show()
                navigateToPanel(AppPanel.SETTINGS)
                true
            }
        }
        settingsRootLayout.addView(headerText)

        val subtitleText = TextView(this).apply {
            text = getString(R.string.settings_subtitle)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 13f
            setPadding(dp(6), 0, dp(6), dp(16))
        }
        settingsRootLayout.addView(subtitleText)

        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), 0, dp(6), dp(12))
        }

        fun createSettingsTabButton(title: String, targetTab: AppSettingsTab): TextView {
            return TextView(this).apply {
                text = title
                textSize = 14f
                setPadding(dp(20), dp(12), dp(20), dp(12))
                typeface = Typeface.create("sans-serif-medium", if (currentSettingsTab == targetTab) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (currentSettingsTab == targetTab) themeCoordinator.bgColor else themeCoordinator.textColor)
                background = if (currentSettingsTab == targetTab)
                    themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                else
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 80), 50f)
                setOnClickListener {
                    currentSettingsTab = targetTab
                    navigateToPanel(AppPanel.SETTINGS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(8), 0) }
            }
        }
        tabContainer.addView(createSettingsTabButton(getString(R.string.tab_general), AppSettingsTab.SIMPLE))
        tabContainer.addView(createSettingsTabButton(getString(R.string.tab_appearance), AppSettingsTab.THEME))
        settingsRootLayout.addView(tabContainer)

        val existingScrollView = if (captureScrollRef) settingsScrollViewRef else null
        val settingsScrollView: ScrollView
        if (existingScrollView != null) {
            (existingScrollView.parent as? android.view.ViewGroup)?.removeView(existingScrollView)
            settingsScrollView = existingScrollView
        } else {
            settingsScrollView = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                isFillViewport = true
            }
        }
        if (captureScrollRef) settingsScrollViewRef = settingsScrollView
        settingsScrollView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        settingsScrollView.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(120))
        }
        settingsScrollView.addView(layout)

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (currentSettingsTab == AppSettingsTab.SIMPLE) {

            layout.addView(createSectionLabel(getString(R.string.section_goals)))
            val goalCard = createSettingsCard()
            val goalValueText = TextView(this).apply {
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.primaryColor)
                setPadding(dp(6), 0, dp(6), 0)
            }
            fun makeGoalStepBtn(label: String, delta: Long): TextView {
                return TextView(this).apply {
                    text = label
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                    setOnClickListener {
                        val current = sharedPrefs.getLong("daily_goal_secs", 2700L)
                        var next = current + delta
                        next = next.coerceIn(900L, 16 * 3600L)
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        sharedPrefs.edit()
                            .putLong("daily_goal_secs", next)
                            .putLong("${todayStr}_goal_secs", next)
                            .apply()
                        goalValueText.text = formatGoalLabel(next)
                    }
                }
            }
            goalValueText.text = formatGoalLabel(sharedPrefs.getLong("daily_goal_secs", 2700L))
            val goalRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
            }
            goalRow.addView(TextView(this).apply { text = "\uD83C\uDFAF"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
            val goalTextCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal); setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
            goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal_sub); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
            goalRow.addView(goalTextCol)
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            controls.addView(makeGoalStepBtn("\u2212", -900L))
            controls.addView(goalValueText)
            controls.addView(makeGoalStepBtn("+", 900L))
            goalRow.addView(controls)
            goalCard.addView(goalRow)
            goalCard.addView(createDivider())
            val isStreakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
            val streakGoalSwitch = SwitchMaterial(this).apply {
                isChecked = isStreakGoalBased
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("streak_uses_daily_goal", isChecked).apply()
                }
            }
            goalCard.addView(createSettingsRow("\uD83D\uDD25", getString(R.string.streak_uses_goal), getString(R.string.streak_uses_goal_sub), streakGoalSwitch))
            goalCard.addView(createDivider())
            goalCard.addView(TextView(this).apply {
                text = getString(R.string.goal_steps_15)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 11f
                setPadding(dp(18), dp(8), dp(18), dp(14))
            })
            layout.addView(goalCard)

            layout.addView(createSectionLabel(getString(R.string.section_timer_mode)))
            val timerModeCard = createSettingsCard()
            val isCountdown = timerMode == "COUNTDOWN"

            fun modeRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val stopwatchRow = createSettingsRow("\u23F1\uFE0F", getString(R.string.mode_stopwatch), getString(R.string.mode_stopwatch_sub), modeRadio(!isCountdown))
            stopwatchRow.setOnClickListener {
                sharedPrefs.edit().putString("timer_mode", "STOPWATCH").apply()
                timerMode = "STOPWATCH"
                navigateToPanel(AppPanel.SETTINGS)
            }
            timerModeCard.addView(stopwatchRow)
            timerModeCard.addView(createDivider())
            val countdownRow = createSettingsRow("\u23F2\uFE0F", getString(R.string.mode_pomodoro), getString(R.string.mode_pomodoro_sub), modeRadio(isCountdown))
            countdownRow.setOnClickListener {
                sharedPrefs.edit().putString("timer_mode", "COUNTDOWN").apply()
                timerMode = "COUNTDOWN"
                navigateToPanel(AppPanel.SETTINGS)
            }
            timerModeCard.addView(countdownRow)
            layout.addView(timerModeCard)

            if (isCountdown) {
                val durationCard = createSettingsCard()
                val durationValueText = TextView(this).apply {
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(6), 0, dp(6), 0)
                }
                fun makeDurationBtn(label: String, delta: Long): TextView {
                    return TextView(this).apply {
                        text = label
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setTextColor(themeCoordinator.primaryColor)
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
                        setPadding(dp(14), dp(6), dp(14), dp(6))
                        applyHoldToRepeat(this) {
                            val current = sharedPrefs.getLong("focus_countdown_secs", 1500L)
                            var next = current + delta
                            next = next.coerceIn(300L, 4 * 3600L)
                            sharedPrefs.edit().putLong("focus_countdown_secs", next).apply()
                            durationValueText.text = formatCountdown(next)
                        }
                    }
                }
                durationValueText.text = formatCountdown(sharedPrefs.getLong("focus_countdown_secs", 1500L))
                val durationRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                durationRow.addView(TextView(this).apply { text = "\u23F3"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
                val durationTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                durationTextCol.addView(TextView(this).apply { text = getString(R.string.focus_duration); setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
                durationTextCol.addView(TextView(this).apply { text = getString(R.string.focus_duration_sub); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
                durationRow.addView(durationTextCol)
                val durationControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                durationControls.addView(makeDurationBtn("\u2212", -300L))
                durationControls.addView(durationValueText)
                durationControls.addView(makeDurationBtn("+", 300L))
                durationRow.addView(durationControls)
                durationCard.addView(durationRow)
                durationCard.addView(createDivider())
                durationCard.addView(TextView(this).apply {
                    text = getString(R.string.duration_steps_hint)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    setPadding(dp(18), dp(8), dp(18), dp(14))
                })
                layout.addView(durationCard)
            }

            layout.addView(createSectionLabel(getString(R.string.section_daily_reminder)))
            val reminderCard = createSettingsCard()
            val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
            val reminderSwitch = SwitchMaterial(this).apply {
                isChecked = reminderEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()
                    if (isChecked) {
                        ensureExactAlarmPermissionIfNeeded()
                        GoalReminderScheduler.schedule(host)
                    } else {
                        GoalReminderScheduler.cancel(host)
                    }
                }
            }
            reminderCard.addView(createSettingsRow("\uD83D\uDD14", getString(R.string.reminder_evening), getString(R.string.reminder_evening_sub), reminderSwitch))
            reminderCard.addView(createDivider())
            val reminderHour = sharedPrefs.safeInt("reminder_hour", 21)
            val reminderMinute = sharedPrefs.safeInt("reminder_minute", 0)
            val timeLabel = TimeFormat.formatHourMinute(host, reminderHour, reminderMinute)
            val reminderTimeRow = createSettingsRow("\u23F0", getString(R.string.reminder_time), getString(R.string.reminder_time_sub), null)
            reminderTimeRow.addView(TextView(this).apply {
                text = timeLabel
                setTextColor(themeCoordinator.primaryColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            reminderTimeRow.setOnClickListener {
                android.app.TimePickerDialog(host, { _, h, m ->
                    sharedPrefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply()
                    if (sharedPrefs.getBoolean("reminder_enabled", true)) {
                        ensureExactAlarmPermissionIfNeeded()
                        GoalReminderScheduler.schedule(host)
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }, reminderHour, reminderMinute, TimeFormat.is24Hour(host)).show()
            }
            reminderCard.addView(reminderTimeRow)
            layout.addView(reminderCard)

            layout.addView(createSectionLabel(getString(R.string.section_display)))
            val displayCard = createSettingsCard()
            val isZenDefault = sharedPrefs.getBoolean("true_fullscreen_landscape", false)
            val zenSwitch = SwitchMaterial(this).apply {
                isChecked = isZenDefault
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("true_fullscreen_landscape", isChecked).apply()
                    applyImmersiveModeForLandscape()
                }
            }
            displayCard.addView(createSettingsRow("\u2194\uFE0F", getString(R.string.immersive_landscape), getString(R.string.immersive_landscape_sub), zenSwitch))
            displayCard.addView(createDivider())
            val timeFormatLabel = TextView(this).apply {
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(4), dp(4))
            }
            fun refreshTimeFormatLabel() {
                timeFormatLabel.text = when (TimeFormat.currentMode(host)) {
                    TimeFormat.Mode.H24 -> "24H"
                    TimeFormat.Mode.H12 -> "12H"
                    else -> getString(R.string.time_format_system)
                }
                timeFormatLabel.setTextColor(themeCoordinator.primaryColor)
            }
            refreshTimeFormatLabel()
            val timeFormatRow = createSettingsRow("\uD83D\uDD5B", getString(R.string.time_format), getString(R.string.time_format_sub), timeFormatLabel)
            timeFormatRow.setOnClickListener {
                val options = arrayOf(getString(R.string.time_format_follow_system), getString(R.string.time_format_24h), getString(R.string.time_format_12h))
                val modes = arrayOf(TimeFormat.Mode.SYSTEM, TimeFormat.Mode.H24, TimeFormat.Mode.H12)
                android.app.AlertDialog.Builder(host)
                    .setTitle(getString(R.string.time_format))
                    .setSingleChoiceItems(options, modes.indexOf(TimeFormat.currentMode(host))) { dlg, which ->
                        TimeFormat.setMode(host, modes[which])
                        refreshTimeFormatLabel()
                        navigateToPanel(AppPanel.SETTINGS)
                        dlg.dismiss()
                    }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
            }
            displayCard.addView(timeFormatRow)
            displayCard.addView(createDivider())
            val isWhiteTimer = sharedPrefs.getBoolean("pureWhiteTimer", false)
            val whiteTimerSwitch = SwitchMaterial(this).apply {
                isChecked = isWhiteTimer
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("pureWhiteTimer", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("⬜", getString(R.string.pure_white_timer), getString(R.string.pure_white_timer_sub), whiteTimerSwitch))
            displayCard.addView(createDivider())
            val isHeatmapEnabled = sharedPrefs.getBoolean("show_focus_heatmap", true)
            val heatmapSwitch = SwitchMaterial(this).apply {
                isChecked = isHeatmapEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_focus_heatmap", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("\uD83D\uDD25", getString(R.string.focus_heatmap_setting), getString(R.string.focus_heatmap_setting_sub), heatmapSwitch))
            displayCard.addView(createDivider())
            val isPatternEnabled = sharedPrefs.getBoolean("show_focus_pattern", true)
            val patternSwitch = SwitchMaterial(this).apply {
                isChecked = isPatternEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_focus_pattern", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("\u23F1\uFE0F", getString(R.string.focus_pattern_setting), getString(R.string.focus_pattern_setting_sub), patternSwitch))
            displayCard.addView(createDivider())
            val isKeepScreenOn = sharedPrefs.getBoolean("keep_screen_on", true)
            val keepScreenOnSwitch = SwitchMaterial(this).apply {
                isChecked = isKeepScreenOn
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("keep_screen_on", isChecked).apply()
                    updateKeepScreenOn()
                }
            }
            displayCard.addView(createSettingsRow("\uD83D\uDD11", getString(R.string.keep_screen_on), getString(R.string.keep_screen_on_sub), keepScreenOnSwitch))
            displayCard.addView(createDivider())
            val isPauseButtonEnabled = sharedPrefs.getBoolean("show_pause_button", true)
            val pauseButtonSwitch = SwitchMaterial(this).apply {
                isChecked = isPauseButtonEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_pause_button", isChecked).apply()
                    if (currentPanel == AppPanel.FOCUS) updateVisualStyles()
                }
            }
            displayCard.addView(createSettingsRow("\u23F8\uFE0F", getString(R.string.pause_button), getString(R.string.pause_button_sub), pauseButtonSwitch))
            layout.addView(displayCard)

            layout.addView(createSectionLabel(getString(R.string.section_data_management)))
            val dataCard = createSettingsCard()
            val exportRow = createSettingsRow("\uD83D\uDCE4", getString(R.string.export_logs), getString(R.string.export_logs_sub))
            exportRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    val fileDateFormat = SimpleDateFormat("dd_MMM", Locale.getDefault())
                    putExtra(Intent.EXTRA_TITLE, "backup_${fileDateFormat.format(Date())}.json")
                }
                exportLauncher.launch(intent)
            }
            dataCard.addView(exportRow)
            dataCard.addView(createDivider())
            val importRow = createSettingsRow("\uD83D\uDCE5", getString(R.string.import_data), getString(R.string.import_data_sub))
            importRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/json"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                importLauncher.launch(Intent.createChooser(intent, getString(R.string.select_backup_file)))
            }
            dataCard.addView(importRow)
            dataCard.addView(createDivider())
            val csvRow = createSettingsRow("\uD83D\uDCCA", getString(R.string.export_csv), getString(R.string.export_csv_sub))
            csvRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    val fileDateFormat = SimpleDateFormat("dd_MMM", Locale.getDefault())
                    putExtra(Intent.EXTRA_TITLE, "study_log_${fileDateFormat.format(Date())}.csv")
                }
                csvLauncher.launch(intent)
            }
            dataCard.addView(csvRow)
            layout.addView(dataCard)

            layout.addView(createSectionLabel(getString(R.string.section_about)))
            val aboutCard = createSettingsCard()
            val versionRow = createSettingsRow("\uD83D\uDCCB", getString(R.string.version_label), "v${currentVersionName()} \u00B7 build ${currentVersionCodeLong()}")
            aboutCard.addView(versionRow)
            aboutCard.addView(createDivider())
            val updateRow = createSettingsRow("\uD83D\uDD04", getString(R.string.check_updates), getString(R.string.check_updates_sub))
            updateRow.setOnClickListener { checkForUpdates(manual = true) }
            aboutCard.addView(updateRow)
            layout.addView(aboutCard)

            layout.addView(createSectionLabel(getString(R.string.section_experimental)))
            val dangerCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { cornerRadius = 35f; setColor(0x0DFF4444.toInt()); setStroke(1, 0x1AFF4444.toInt()) }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(4)) }
            }
            dangerCard.addView(TextView(this).apply {
                text = getString(R.string.wipe_hint)
                setTextColor(0xFFEF4444.toInt()); alpha = 0.7f; textSize = 12f; setPadding(dp(18), dp(16), dp(18), dp(4))
            })
            val deleteTodayBtn = Button(this).apply {
                text = getString(R.string.wipe_today)
                setTextColor(0xFFEF4444.toInt()); textSize = 14f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply { cornerRadius = 20f; setColor(0x1AFF4444.toInt()) }
                setPadding(dp(24), dp(16), dp(24), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(16)) }
                setOnLongClickListener {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply { remove("${todayStr}_focus_total"); remove("${todayStr}_break_total"); remove("${todayStr}_focus_manual"); remove("${todayStr}_break_manual"); apply() }
                    TimelineLogger.deleteDay(context, todayStr)
                    handleStopSession(silent = true)
                    Toast.makeText(context, getString(R.string.toast_logs_cleared), Toast.LENGTH_SHORT).show()
                    true
                }
                setOnClickListener { Toast.makeText(context, getString(R.string.toast_hold_to_confirm), Toast.LENGTH_SHORT).show() }
            }
            dangerCard.addView(deleteTodayBtn)
            layout.addView(dangerCard)

            if (isDevModeUnlocked) {
                layout.addView(createSectionLabel(getString(R.string.section_developer)))
                val devCard = createSettingsCard().apply { setPadding(20, 20, 20, 20) }
                devCard.addView(TextView(this).apply {
                    text = getString(R.string.dev_tools_hint)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.7f
                    textSize = 12f
                    setPadding(0, 0, 0, dp(10))
                })
                val toolRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, 0) }
                }
                val editorBtn = TextView(this).apply {
                    text = getString(R.string.edit_timeline)
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.bgColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { showDevTimelineEditor() }
                }
                val modeToggleBtn = TextView(this).apply {
                    text = if (isAdjustingFocusMode) "\uD83C\uDFAF Focus Pool" else "\u2615 Break Pool"
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.bgColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { isAdjustingFocusMode = !isAdjustingFocusMode; text = if (isAdjustingFocusMode) "\uD83C\uDFAF Focus Pool" else "\u2615 Break Pool" }
                }
                toolRow.addView(editorBtn)
                toolRow.addView(modeToggleBtn)
                devCard.addView(toolRow)
                devCard.addView(TextView(this).apply {
                    text = getString(R.string.shift_block_hint)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 11f
                    setPadding(0, dp(10), 0, 0)
                })

                fun applyShift(ds: Long) {
                    val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val isFocus = isAdjustingFocusMode
                    val key = if (isFocus) "${todayStr}_focus_total" else "${todayStr}_break_total"
                    val manualKey = if (isFocus) "${todayStr}_focus_manual" else "${todayStr}_break_manual"
                    var cv = prefs.getLong(key, 0L)
                    cv = max(0L, cv + ds)
                    prefs.edit().putLong(key, cv).putLong(manualKey, prefs.getLong(manualKey, 0L) + ds).apply()
                    statsDirty = true
                    recalculateStreak()
                    val what = if (isFocus) "focus" else "break"
                    Toast.makeText(this, getString(R.string.toast_block_shifted, what), Toast.LENGTH_SHORT).show()
                }
                fun makeIncBtn(l: String, ds: Long): Button { return Button(this).apply { text = l; setTextColor(themeCoordinator.textColor); textSize = 12f; typeface = Typeface.MONOSPACE; background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(6, 6, 6, 6) }; setOnClickListener { applyShift(ds) } } }
                val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6, 0, 0) } }
                row1.addView(makeIncBtn("- 1h", -3600L)); row1.addView(makeIncBtn("-15m", -900L))
                val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
                row2.addView(makeIncBtn("+15m", 900L)); row2.addView(makeIncBtn("+ 1h", 3600L))
                devCard.addView(row1); devCard.addView(row2)
                layout.addView(devCard)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                layout.addView(createSectionLabel(getString(R.string.section_dynamic_color)))
                val dynamicCard = createSettingsCard()
                val isDynamic = sharedPrefs.getBoolean("dynamic_color", false)
                val dynamicSwitch = SwitchMaterial(this).apply {
                    isChecked = isDynamic
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("dynamic_color", isChecked).apply()
                        themeCoordinator.applyThemeCoordinates()
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                dynamicCard.addView(createSettingsRow(
                    "\uD83C\uDFA8", getString(R.string.material_you),
                    if (isDynamic) getString(R.string.dynamic_follows_wallpaper) else getString(R.string.dynamic_uses_wallpaper),
                    dynamicSwitch
                ))
                layout.addView(dynamicCard)
            }

            layout.addView(createSectionLabel(getString(R.string.section_dark_mode)))

            val modeCard = createSettingsCard()
            val isEclipse = themeCoordinator.activeBgMode == "ECLIPSE"
            val isLight = themeCoordinator.activeBgMode == "LIGHT"

            fun modeRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val lightRow = createSettingsRow("\u2600\uFE0F", getString(R.string.theme_light), getString(R.string.theme_light_sub), modeRadio(isLight))
            lightRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "LIGHT").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(lightRow)
            modeCard.addView(createDivider())
            val eclipseRow = createSettingsRow("\uD83C\uDF19", getString(R.string.theme_slate), getString(R.string.theme_slate_sub), modeRadio(isEclipse))
            eclipseRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "ECLIPSE").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(eclipseRow)
            modeCard.addView(createDivider())
            val oledRow = createSettingsRow("\u2B24", getString(R.string.theme_amoled), getString(R.string.theme_amoled_sub), modeRadio(!isEclipse && !isLight))
            oledRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "OLED").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(oledRow)
            layout.addView(modeCard)

            layout.addView(createSectionLabel(getString(R.string.section_theme_style)))

            val styleCard = createSettingsCard()
            val isGlass = themeCoordinator.isGlassStyle()

            fun styleRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val glassRow = createSettingsRow("\u2728", getString(R.string.style_glass), getString(R.string.style_glass_sub), styleRadio(isGlass))
            glassRow.setOnClickListener {
                sharedPrefs.edit().putString("ui_style", "GLASS").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            styleCard.addView(glassRow)
            styleCard.addView(createDivider())
            val classicRow = createSettingsRow("\u25A6", getString(R.string.style_classic), getString(R.string.style_classic_sub), styleRadio(!isGlass))
            classicRow.setOnClickListener {
                sharedPrefs.edit().putString("ui_style", "CLASSIC").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            styleCard.addView(classicRow)
            layout.addView(styleCard)

            layout.addView(createSectionLabel("ACCENT COLORS"))

            val randomAccentCard = createSettingsCard()
            val randomAccentRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                isClickable = true
                isFocusable = true
                setOnClickListener { applyRandomBothHues() }
            }
            val randomAccentIcon = TextView(this).apply {
                text = "\uD83C\uDFB2"
                textSize = 24f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(tintedColor(themeCoordinator.primaryColor, 30)) }
            }
            randomAccentRow.addView(randomAccentIcon)
            val randomAccentText = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(16), 0, dp(8), 0)
            }
            randomAccentText.addView(TextView(this).apply {
                text = getString(R.string.randomize_accents)
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            randomAccentText.addView(TextView(this).apply {
                text = getString(R.string.randomize_sub)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setPadding(0, 2, 0, 0)
            })
            randomAccentRow.addView(randomAccentText)
            randomAccentRow.addView(TextView(this).apply {
                text = "\u21BA"
                setTextColor(themeCoordinator.textColor)
                textSize = 20f
            })
            randomAccentCard.addView(randomAccentRow)
            layout.addView(randomAccentCard)

            val spectrumColors = IntArray(9)
            for (i in 0..8) { spectrumColors[i] = Color.HSVToColor(floatArrayOf((i * 45).toFloat(), 0.85f, 0.85f)) }
            val spectrumTrack = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, spectrumColors).apply { cornerRadius = 15f }

            val focusCard = createSettingsCard()

            val focusHeaderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
            val focusHeaderColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            focusHeaderColumn.addView(TextView(this).apply { text = getString(R.string.focus_accent); setTextColor(themeCoordinator.primaryColor); textSize = 12f; letterSpacing = 0.15f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) })
            focusHeaderColumn.addView(TextView(this).apply { text = getString(R.string.adjust_hue_hint); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) })
            focusHeaderRow.addView(focusHeaderColumn)
            val focusToggle = SwitchMaterial(this).apply {
                isChecked = showFocusHueBar
                setOnCheckedChangeListener { _, isChecked ->
                    showFocusHueBar = isChecked
                    navigateToPanel(AppPanel.SETTINGS)
                }
            }
            focusHeaderRow.addView(focusToggle)
            focusCard.addView(focusHeaderRow)

            val currentHue = sharedPrefs.safeInt("customHue", 200)
            if (showFocusHueBar) {
                val primaryHueValue = TextView(this).apply {
                    text = "${currentHue}\u00B0"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(18), 0, dp(18), 0)
                }
                focusCard.addView(primaryHueValue)

                val primaryHueBar = SeekBar(this).apply {
                    max = 360; progress = currentHue; background = spectrumTrack
                    setPadding(20, 14, 20, 14)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(8), 8, dp(8), 12) }
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            primaryHueValue.text = "${progress}\u00B0"
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            themeCoordinator.primaryColor = color
                            primaryHueValue.setTextColor(color)
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val progress = seekBar?.progress ?: currentHue
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customHue", progress).putInt("customPrimary", color).apply()
                            themeCoordinator.applyThemeCoordinates()
                            refreshSettingsPanelPreservingScroll()
                        }
                    })
                }
                focusCard.addView(primaryHueBar)
                focusCard.addView(createDivider())
            }

            val primarySwatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(16), dp(14), dp(16), dp(18))
            }
            fun addPrimarySwatch(colorHex: Int, nameStr: String) {
                val swatchContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                swatchContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(56, 56)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colorHex) }
                    setOnClickListener {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(colorHex, hsv)
                        sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customHue", hsv[0].toInt()).putInt("customPrimary", colorHex).apply()
                        themeCoordinator.applyThemeCoordinates()
                        refreshSettingsPanelPreservingScroll()
                    }
                })
                swatchContainer.addView(TextView(this).apply {
                    text = nameStr
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 0)
                    maxLines = 1
                })
                primarySwatchRow.addView(swatchContainer)
            }
            addPrimarySwatch(0xFFFFFFFF.toInt(), getString(R.string.color_white))
            addPrimarySwatch(0xFFF472B6.toInt(), getString(R.string.color_rose))
            addPrimarySwatch(0xFFC4B5FD.toInt(), getString(R.string.color_lavender))
            addPrimarySwatch(0xFF38BDF8.toInt(), getString(R.string.color_sky))
            addPrimarySwatch(0xFFFB923C.toInt(), getString(R.string.color_orange))
            addPrimarySwatch(0xFFA7F3D0.toInt(), getString(R.string.color_mint))
            focusCard.addView(primarySwatchRow)
            layout.addView(focusCard)

            val breakCard = createSettingsCard()

            val breakHeaderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
            val breakHeaderColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            breakHeaderColumn.addView(TextView(this).apply { text = getString(R.string.break_accent); setTextColor(themeCoordinator.secondaryColor); textSize = 12f; letterSpacing = 0.15f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) })
            breakHeaderColumn.addView(TextView(this).apply { text = getString(R.string.adjust_hue_hint); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) })
            breakHeaderRow.addView(breakHeaderColumn)
            val breakToggle = SwitchMaterial(this).apply {
                isChecked = showBreakHueBar
                setOnCheckedChangeListener { _, isChecked ->
                    showBreakHueBar = isChecked
                    navigateToPanel(AppPanel.SETTINGS)
                }
            }
            breakHeaderRow.addView(breakToggle)
            breakCard.addView(breakHeaderRow)

            val currentSecHue = sharedPrefs.safeInt("customSecondaryHue", 330)
            if (showBreakHueBar) {
                val secondaryHueValue = TextView(this).apply {
                    text = "${currentSecHue}\u00B0"
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(18), 0, dp(18), 0)
                }
                breakCard.addView(secondaryHueValue)

                val secondaryHueBar = SeekBar(this).apply {
                    max = 360; progress = currentSecHue; background = spectrumTrack
                    setPadding(20, 14, 20, 14)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(8), 8, dp(8), 12) }
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            secondaryHueValue.text = "${progress}\u00B0"
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            themeCoordinator.secondaryColor = color
                            secondaryHueValue.setTextColor(color)
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val progress = seekBar?.progress ?: currentSecHue
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customSecondaryHue", progress).putInt("customSecondary", color).apply()
                            themeCoordinator.applyThemeCoordinates()
                            refreshSettingsPanelPreservingScroll()
                        }
                    })
                }
                breakCard.addView(secondaryHueBar)
                breakCard.addView(createDivider())
            }

            val secondarySwatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(16), dp(14), dp(16), dp(18))
            }
            fun addSecondarySwatch(colorHex: Int, nameStr: String) {
                val swatchContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                swatchContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(56, 56)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colorHex) }
                    setOnClickListener {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(colorHex, hsv)
                        sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customSecondaryHue", hsv[0].toInt()).putInt("customSecondary", colorHex).apply()
                        themeCoordinator.applyThemeCoordinates()
                        refreshSettingsPanelPreservingScroll()
                    }
                })
                swatchContainer.addView(TextView(this).apply {
                    text = nameStr
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 0)
                    maxLines = 1
                })
                secondarySwatchRow.addView(swatchContainer)
            }
            addSecondarySwatch(0xFFF472B6.toInt(), "Rose")
            addSecondarySwatch(0xFFFBBF24.toInt(), "Gold")
            addSecondarySwatch(0xFFA78BFA.toInt(), "Violet")
            addSecondarySwatch(0xFF34D399.toInt(), "Emerald")
            addSecondarySwatch(0xFFF87171.toInt(), "Coral")
            breakCard.addView(secondarySwatchRow)
            layout.addView(breakCard)
        }

        val pushToBottomSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        layout.addView(pushToBottomSpacer)

        val creditsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(30), 0, dp(8)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        creditsContainer.addView(TextView(this).apply { text = getString(R.string.developed_by); setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); gravity = Gravity.CENTER })
        creditsContainer.addView(TextView(this).apply { text = getString(R.string.special_thanks); setTextColor(themeCoordinator.textColor); alpha = 0.4f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL); gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0) })
        layout.addView(creditsContainer)

        settingsRootLayout.addView(settingsScrollView)

        val settingsRoot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        settingsRoot.addView(settingsRootLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val settingsBackFab = TextView(this).apply {
            text = getString(R.string.btn_back_label)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(themeCoordinator.primaryColor) }
            elevation = dp(8).toFloat()
            setOnClickListener { navigateToPanel(AppPanel.FOCUS) }
            layoutParams = FrameLayout.LayoutParams(dp(150), dp(54), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(20), dp(20)) }
        }
        settingsRoot.addView(settingsBackFab)

        target.addView(settingsRoot)
        }
    }
}
