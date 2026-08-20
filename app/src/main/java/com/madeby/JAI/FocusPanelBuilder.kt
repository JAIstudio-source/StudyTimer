package com.madeby.JAI

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FocusPanelBuilder(private val host: MainActivity) {

    fun build() {
        with(host) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled

        navHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = if (isLandscape) View.GONE else View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), if (isLandscape) dp(8) else 50, dp(16), if (isLandscape) dp(4) else 20)
            }
        }

        val settingsIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_settings) 
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = if (themeCoordinator.isGlassStyle() || themeCoordinator.isBubbleStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = getString(R.string.cd_open_settings)
            setOnClickListener { navigateToPanel(AppPanel.SETTINGS) }
        }
        navHeader.addView(settingsIconView)

        val savedTimerMode = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("timer_mode", "SUBJECT") ?: "SUBJECT"

        val headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        navHeader.addView(headerSpacer)

        val insightsHeaderIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_insights)
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = if (themeCoordinator.isGlassStyle() || themeCoordinator.isBubbleStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = getString(R.string.cd_open_insights)
            visibility = if (isLandscape) View.VISIBLE else View.GONE
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
        }
        navHeader.addView(insightsHeaderIconView)


        val centerClocksWrapper = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled()) {
            val glowAlpha = when {
                themeCoordinator.activeBgMode == "OLED" -> 0.22f
                themeCoordinator.activeBgMode == "LIGHT" -> 0.3f
                else -> 0.38f
            }
            val timerGlow = ImageView(this).apply {
                setImageDrawable(themeCoordinator.createGlowBlob(themeCoordinator.primaryColor))
                alpha = glowAlpha
                layoutParams = FrameLayout.LayoutParams(if (isLandscape) dp(420) else dp(360), if (isLandscape) dp(420) else dp(360), Gravity.CENTER)
            }
            centerClocksWrapper.addView(timerGlow)
        }

        timerRing = TimerRingView(this).apply {
            visibility = if (!isLandscape && (savedTimerMode == "COUNTDOWN" || savedTimerMode == "LECTURE")) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
        }

        statusBadge = TextView(this).apply {
            textSize = if (isLandscape) 12f else 14f
            setPadding(dp(16), dp(4), dp(16), dp(4))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        statusBadgeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, if (isLandscape) dp(8) else 12, 0, 0)
            }
            addView(statusBadge)

            if (savedTimerMode == "LECTURE") {
                val timetableBtn = TextView(host).apply {
                    text = "\uD83D\uDCC5 Class Timetable"
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.accentColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, 120), 20f)
                    setPadding(dp(16), dp(6), dp(16), dp(6))
                    gravity = Gravity.CENTER
                    setOnClickListener { showLectureScheduleManagerDialog() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(14), 0, 0)
                    }

                }
                addView(timetableBtn)
            }
        }



        studyTimerDisplay = TextView(this).apply {
            text = "00:00:00"
            isSingleLine = true
            maxLines = 1
            textSize = if (isLandscape) 104f else 54f 
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 5)
            if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled()) setShadowLayer(14f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

        breakTimerDisplay = TextView(this).apply {
            text = "00:00:00"
            isSingleLine = true
            maxLines = 1
            textSize = if (isLandscape) 20f else 20f
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, if (isLandscape) 8 else 40)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        }

        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                    handlePause()
                    try { centerClocksWrapper.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                    return true
                } else if (currentTimerState == TimerState.PAUSED) {
                    handleStateToggle()
                    try { centerClocksWrapper.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                    return true
                }
                return false
            }
        })
        centerClocksWrapper.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        centerClocksWrapper.addView(timerRing)
        centerClocksWrapper.addView(statusBadgeContainer)
        centerClocksWrapper.addView(studyTimerDisplay)
        centerClocksWrapper.addView(breakTimerDisplay)
        val timerModeSetting = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
        val showSubjectTagging = sharedPrefs.getBoolean("enable_subject_tagging", true) && timerModeSetting != "STOPWATCH"
        val showAmbientSounds = sharedPrefs.safeBoolean("enable_ambient_sounds", false)

        val extraControlsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(6), 0, dp(6))
            }
        }

        if (showSubjectTagging) {
            val isLectureRunning = timerModeSetting == "LECTURE" && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.PAUSED)
            val lectureSubId = sharedPrefs.getString("active_lecture_subject_id", null)
            val activeSubject = if (timerModeSetting == "LECTURE" && lectureSubId != null) {
                SubjectTagManager.resolveSubject(this, lectureSubId)
            } else {
                SubjectTagManager.getSelectedSubject(this)
            }

            val subjectTagBtn = TextView(this).apply {
                text = if (isLectureRunning) "🔒 ${activeSubject.iconEmoji} ${activeSubject.name}" else "${activeSubject.iconEmoji} ${activeSubject.name}  ▾"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 90), 12f)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setOnClickListener {
                    if (isLectureRunning) {
                        Toast.makeText(host, "Subject tag is locked to the scheduled class", Toast.LENGTH_SHORT).show()
                    } else {
                        host.showSubjectPickerDialog()
                    }
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                }
            }
            extraControlsContainer.addView(subjectTagBtn)
        }

        if (showAmbientSounds) {
            val activePreset = AmbientSoundEngine.getActivePreset()
            val ambientSoundBtn = TextView(this).apply {
                text = "🎧 ${activePreset.displayName}"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, 90), 16f)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                gravity = Gravity.CENTER
                setOnClickListener { host.showAmbientSoundDialog(host) }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                }
            }
            extraControlsContainer.addView(ambientSoundBtn)
        }

        controlActionContainer = LinearLayout(this).apply {
            orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (isLandscape) gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, if (isLandscape) 4 else 10, 0, if (isLandscape) dp(12) else 20)
            }
        }

        mainBtn = Button(this).apply {
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(20), dp(11), dp(20), dp(11))
            minimumHeight = dp(46)
            isSoundEffectsEnabled = false
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(140) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { 
                if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4)) 
            }
            setOnClickListener { handleStateToggle() }
            applyBubbleTouchAnimation(this)
        }

        pauseBtn = Button(this).apply {
            text = getString(R.string.btn_pause)
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(20), dp(11), dp(20), dp(11))
            minimumHeight = dp(46)
            isSoundEffectsEnabled = false
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(130) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4))
            }
            setOnClickListener { handlePause() }
            applyBubbleTouchAnimation(this)
        }

        stopBtn = HoldRingButton(this).apply {
            text = getString(R.string.btn_hold_to_end)
            ringColor = themeCoordinator.primaryColor
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isSoundEffectsEnabled = false
            background = outlinedButtonBackground()
            setPadding(dp(20), dp(11), dp(20), dp(11))
            minimumHeight = dp(46)
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(140) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { 
                if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4)) 
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        holdStartTime = SystemClock.uptimeMillis()
                        isHoldingStop = true
                        v.isPressed = true
                        progress = 0f
                        handler.removeCallbacks(holdToEndRunnable)
                        handler.post(holdToEndRunnable)
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resetHoldToEnd()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.x < 0f || event.y < 0f || event.x > v.width || event.y > v.height) {
                            resetHoldToEnd()
                        }
                        true
                    }
                    else -> true
                }
            }
        }

        controlActionContainer.addView(mainBtn)
        controlActionContainer.addView(pauseBtn)
        controlActionContainer.addView(stopBtn)

        val statsFloatingIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_insights)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = if (themeCoordinator.isGlassStyle() || themeCoordinator.isBubbleStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = getString(R.string.cd_open_insights)
            visibility = if (isLandscape) View.GONE else View.VISIBLE
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                gravity = Gravity.END
                setMargins(0, 0, dp(16), if (isLandscape) 5 else 20)
            }
        }

        panelContainer.addView(navHeader)
        panelContainer.addView(centerClocksWrapper)
        if (!isLandscape && (showSubjectTagging || showAmbientSounds)) {
            panelContainer.addView(extraControlsContainer)
        }
        panelContainer.addView(controlActionContainer)
        panelContainer.addView(statsFloatingIcon)

        updateVisualStyles()
        applyTrueFullscreenMode()
        }
    }
}

