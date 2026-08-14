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

class FocusPanelBuilder(private val host: MainActivity) {

    fun build() {
        with(host) {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        navHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, if (isLandscape) 10 else 50, 0, 20) 
            }
        }

        val settingsIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_settings) 
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(16, 16, 16, 16)
            background = if (themeCoordinator.isGlassStyle() || themeCoordinator.isBubbleStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = getString(R.string.cd_open_settings)
            setOnClickListener { navigateToPanel(AppPanel.SETTINGS) }
        }
        navHeader.addView(settingsIconView)

        val savedTimerMode = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"

        val headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        navHeader.addView(headerSpacer)

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
                layoutParams = FrameLayout.LayoutParams(dp(360), dp(360), Gravity.CENTER)
            }
            centerClocksWrapper.addView(timerGlow)
        }

        timerRing = TimerRingView(this).apply {
            visibility = if (!isLandscape && (savedTimerMode == "COUNTDOWN" || savedTimerMode == "LECTURE")) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
        }

        statusBadge = TextView(this).apply {
            textSize = if (isLandscape) 13f else 14f
            setPadding(dp(18), dp(6), dp(18), dp(6))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        statusBadgeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, if (isLandscape) 4 else 12, 0, 0)
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
            textSize = if (isLandscape) 96f else 54f 
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 5)
            if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled()) setShadowLayer(14f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

        breakTimerDisplay = TextView(this).apply {
            text = "00:00:00"
            isSingleLine = true
            maxLines = 1
            textSize = if (isLandscape) 24f else 20f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, if (isLandscape) 20 else 40)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        }

        centerClocksWrapper.addView(timerRing)
        centerClocksWrapper.addView(statusBadgeContainer)
        centerClocksWrapper.addView(studyTimerDisplay)
        centerClocksWrapper.addView(breakTimerDisplay)

        controlActionContainer = LinearLayout(this).apply {
            orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, if (isLandscape) 10 else 30) }
        }

        mainBtn = Button(this).apply {
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(20), dp(11), dp(20), dp(11))
            minimumHeight = dp(46)
            isSoundEffectsEnabled = false
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply { 
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
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply {
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
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply { 
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
            setPadding(14, 14, 14, 14)
            background = if (themeCoordinator.isGlassStyle() || themeCoordinator.isBubbleStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = getString(R.string.cd_open_insights)
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
            applyBubbleTouchAnimation(this)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.END
                setMargins(0, 0, dp(16), if (isLandscape) 5 else 20)
            }
        }

        panelContainer.addView(navHeader)
        panelContainer.addView(centerClocksWrapper)
        panelContainer.addView(controlActionContainer)
        
        if (!isLandscape) {
            panelContainer.addView(statsFloatingIcon)
        } else {
            navHeader.addView(statsFloatingIcon) 
        }

        updateVisualStyles()
        applyTrueFullscreenMode()
        }
    }
}
