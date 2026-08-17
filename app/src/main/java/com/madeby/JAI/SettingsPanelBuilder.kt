package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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

/**
 * Clean, Hierarchical Category Navigation (Hub & Spoke) Architecture for Settings.
 * AMOLED #000000 base, #121212 / #141720 card surfaces, 16dp rounded corners.
 *
 * Hub Sections:
 * 0. Top User Profile Card (Avatar/Initials, display name, email, Google Connected status)
 * 1. Timer & Focus Controls (Interval pickers, auto-start breaks, strict mode, mode picker)
 * 2. Sound & Ambience (Ambient loop selectors, volume slider, completion chime)
 * 3. Analytics & Goals (Daily goal target picker, streak settings, heatmap/pie chart options)
 * 4. Cloud, Sync & Backups (Google account sync, backup JSON/CSV export/import)
 * 5. Theme & Styling (AMOLED/Slate/Light, 3D Bubble/Glass/Classic, Accent colors)
 * 6. User Profile Management Sub-Screen
 * 7. Developer & Advanced (Strictly gated behind dev unlock / debug)
 */
class SettingsPanelBuilder(private val host: MainActivity) {

    fun build(target: android.view.ViewGroup = host.panelContainer, captureScrollRef: Boolean = true) {
        with(host) {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

            if (captureScrollRef) {
                tabPageCache.keys.removeIf { it.startsWith("ST:") }
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(this@with)
                    }
                }.start()
            }

            val settingsRootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }

            val settingsBackFab = TextView(this).apply {
                text = getString(R.string.btn_back_label)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.bgColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply { cornerRadius = 50f; setColor(themeCoordinator.primaryColor) }
                elevation = dp(8).toFloat()
                setOnClickListener {
                    if (currentSettingsTab != AppSettingsTab.HUB) {
                        currentSettingsTab = AppSettingsTab.HUB
                        navigateToPanel(AppPanel.SETTINGS)
                    } else {
                        navigateToPanel(AppPanel.FOCUS)
                    }
                }
                layoutParams = FrameLayout.LayoutParams(dp(150), dp(54), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(20), dp(20)) }
            }

            // Top Header & Hub Breadcrumb
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(16), dp(6), dp(8))
            }

            if (currentSettingsTab != AppSettingsTab.HUB) {
                val backArrowBtn = TextView(this).apply {
                    text = "←"
                    textSize = 24f
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(0, 0, dp(14), 0)
                    setOnClickListener {
                        currentSettingsTab = AppSettingsTab.HUB
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                headerRow.addView(backArrowBtn)
            }

            val headerText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> getString(R.string.settings_title)
                    AppSettingsTab.TIMER -> "Timer & Focus"
                    AppSettingsTab.AMBIENCE -> "Sound & Ambience"
                    AppSettingsTab.ANALYTICS -> "Analytics & Goals"
                    AppSettingsTab.CLOUD -> "Cloud, Sync & Backups"
                    AppSettingsTab.THEME -> "Theme & Appearance"
                    AppSettingsTab.PROFILE -> "User Profile & Account"
                    AppSettingsTab.DEVELOPER -> "Developer & Advanced"
                    else -> getString(R.string.settings_title)
                }
                setTextColor(themeCoordinator.textColor)
                textSize = 22f
                letterSpacing = 0.02f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setOnLongClickListener {
                    isDevModeUnlocked = true
                    Toast.makeText(context, getString(R.string.toast_dev_config_enabled), Toast.LENGTH_SHORT).show()
                    navigateToPanel(AppPanel.SETTINGS)
                    true
                }
            }
            headerRow.addView(headerText)
            settingsRootLayout.addView(headerRow)

            val subtitleText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> "Preferences, account management & sync architecture"
                    AppSettingsTab.TIMER -> "Session lengths, break intervals & timer behaviors"
                    AppSettingsTab.AMBIENCE -> "Ambient soundscapes, volume levels & completion alerts"
                    AppSettingsTab.ANALYTICS -> "Daily targets, heatmap parameters & chart filters"
                    AppSettingsTab.CLOUD -> "Cloud backup, local JSON export & sync conflict resolution"
                    AppSettingsTab.THEME -> "AMOLED dark palettes, UI styles & accent colorways"
                    AppSettingsTab.PROFILE -> "User credentials, custom avatar & cloud session"
                    AppSettingsTab.DEVELOPER -> "Mock data generator, schema inspector & conflict simulation"
                    else -> getString(R.string.settings_subtitle)
                }
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 13f
                setPadding(dp(6), 0, dp(6), dp(16))
            }
            settingsRootLayout.addView(subtitleText)

            val existingScrollView = if (captureScrollRef) settingsScrollViewRef else null
            val settingsScrollView: ScrollView
            if (existingScrollView != null) {
                (existingScrollView.parent as? android.view.ViewGroup)?.removeView(existingScrollView)
                settingsScrollView = existingScrollView
            } else {
                settingsScrollView = ScrollView(this).apply {
                    isVerticalScrollBarEnabled = false
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                if (captureScrollRef) settingsScrollViewRef = settingsScrollView
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), 0, dp(16), dp(90))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            settingsScrollView.removeAllViews()
            settingsScrollView.addView(layout)

            fun createSectionLabel(title: String): TextView {
                return TextView(this).apply {
                    text = title
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    letterSpacing = 0.15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(6), dp(18), dp(6), dp(8))
                }
            }

            fun createDivider(): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    background = GradientDrawable().apply {
                        setColor(tintedColor(themeCoordinator.textColor, 25))
                    }
                }
            }

            fun createSettingsCard(): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground(20f)
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(10))
                    }
                }
            }

            fun createSettingsRow(icon: String, title: String, subtitle: String, trailingView: View? = null): LinearLayout {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                val iconView = TextView(this).apply {
                    text = icon
                    textSize = 20f
                    setPadding(0, 0, dp(14), 0)
                }
                row.addView(iconView)
                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                textCol.addView(TextView(this).apply {
                    text = title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                textCol.addView(TextView(this).apply {
                    text = subtitle
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                row.addView(textCol)
                if (trailingView != null) {
                    row.addView(trailingView)
                }
                return row
            }

            // ==========================================
            // 1. SETTINGS HUB DASHBOARD (Hub & Spoke)
            // ==========================================
            if (currentSettingsTab == AppSettingsTab.HUB) {

                // --- TOP USER PROFILE CARD ---
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner"
                val userEmail = AuthManager.getUserEmail(this)
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                val profileTopCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground(24f)
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        currentSettingsTab = AppSettingsTab.PROFILE
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }

                val profileRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Avatar bubble
                val avatarCircle = TextView(this).apply {
                    text = avatarInitials
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#475569"))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                }
                profileRow.addView(avatarCircle)

                val profileTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(14), 0, dp(10), 0)
                }
                profileTextCol.addView(TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val authStatusRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(2), 0, 0)
                }
                val statusDot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                        setMargins(0, 0, dp(6), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    }
                }
                authStatusRow.addView(statusDot)
                authStatusRow.addView(TextView(this).apply {
                    text = if (isGoogleAuth) "${userEmail ?: "Connected"} • Cloud Sync Active" else "Guest / Offline Mode — Tap to Sign In"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                    textSize = 12f
                })
                profileTextCol.addView(authStatusRow)
                profileRow.addView(profileTextCol)

                val editProfileBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "Manage ›" else "Sign In ›"
                    setTextColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#38BDF8"))
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#38BDF8"), 40), 14f)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                }
                profileRow.addView(editProfileBadge)
                profileTopCard.addView(profileRow)
                layout.addView(profileTopCard)

                // --- CATEGORIZED NAVIGATION CARDS ---
                fun createHubCard(icon: String, title: String, subtitle: String, targetTab: AppSettingsTab): View {
                    val card = createSettingsCard()
                    val chevron = TextView(this).apply {
                        text = "›"
                        textSize = 22f
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.35f
                        setPadding(dp(8), 0, 0, 0)
                    }
                    val row = createSettingsRow(icon, title, subtitle, chevron)
                    row.isClickable = true
                    row.isFocusable = true
                    row.setOnClickListener {
                        currentSettingsTab = targetTab
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                    card.addView(row)
                    return card
                }

                val focusMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                val breakMins = sharedPrefs.safeLong("break_interval_minutes", 5L)
                val timerSub = "Focus: ${focusMins}m  •  Break: ${breakMins}m  •  ${timerMode.lowercase().capitalize(Locale.ROOT)} Mode"

                val isAmbienceOn = sharedPrefs.safeBoolean("enable_ambient_sounds", false)
                val ambTrack = sharedPrefs.getString("ambient_sound_type", "RAIN") ?: "Rain"
                val ambVol = sharedPrefs.getInt("ambient_sound_volume", 50)
                val ambienceSub = if (isAmbienceOn) "${ambTrack.capitalize(Locale.ROOT)} Active • ${ambVol}% Volume" else "Ambient Soundscapes & Completion Alerts"

                val dailyGoalSecs = sharedPrefs.getLong("daily_goal_secs", 7200L)
                val analyticsSub = "Target: ${formatGoalLabel(dailyGoalSecs)} • Heatmap & Breakdown Filters"

                val cloudSub = if (isGoogleAuth) "Connected: $userEmail • Supabase Sync" else "Sign In, Backup & Cloud Restore"

                val themeSub = "${themeCoordinator.activeBgMode.capitalize(Locale.ROOT)} Palette • ${if (themeCoordinator.isGlassStyle()) "Glass" else "Standard"} Style"

                layout.addView(createSectionLabel("CONFIGURATION CATEGORIES"))
                layout.addView(createHubCard("⏱️", "Timer & Focus Controls", timerSub, AppSettingsTab.TIMER))
                layout.addView(createHubCard("🎧", "Sound & Ambience", ambienceSub, AppSettingsTab.AMBIENCE))
                layout.addView(createHubCard("📊", "Analytics & Goals", analyticsSub, AppSettingsTab.ANALYTICS))
                layout.addView(createHubCard("☁️", "Cloud, Sync & Backups", cloudSub, AppSettingsTab.CLOUD))
                layout.addView(createHubCard("🎨", "Theme & Appearance", themeSub, AppSettingsTab.THEME))

                if (isDevModeUnlocked) {
                    layout.addView(createSectionLabel("DEVELOPER SUITE"))
                    layout.addView(createHubCard("🛠️", "Developer & Advanced", "Mock Data Generator, State & Schema Inspector", AppSettingsTab.DEVELOPER))
                }

                layout.addView(createSectionLabel("ABOUT & SUPPORT"))
                val aboutCard = createSettingsCard()
                val guideRow = createSettingsRow("📖", "How to Use / App Guide", "View complete feature walkthrough & tips")
                guideRow.setOnClickListener { showAppGuideDialog() }
                aboutCard.addView(guideRow)
                aboutCard.addView(createDivider())

                val feedbackRow = createSettingsRow("💬", "Report a Problem & Feedback", "Send bug reports, logs, or feature requests")
                feedbackRow.setOnClickListener { showFeedbackReportDialog() }
                aboutCard.addView(feedbackRow)
                aboutCard.addView(createDivider())

                var versionTapCount = 0
                val versionRow = createSettingsRow("📋", getString(R.string.version_label), "v${currentVersionName()} • build ${currentVersionCodeLong()}")
                versionRow.setOnClickListener {
                    versionTapCount++
                    if (versionTapCount >= 5) {
                        isDevModeUnlocked = true
                        Toast.makeText(this, getString(R.string.toast_dev_config_enabled), Toast.LENGTH_SHORT).show()
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                aboutCard.addView(versionRow)
                layout.addView(aboutCard)
            }

            // ==========================================
            // 2. USER PROFILE MANAGEMENT SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.PROFILE) {
                layout.addView(createSectionLabel("ACCOUNT & PROFILE"))
                val profileCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner"
                val userEmail = AuthManager.getUserEmail(this) ?: "Offline / Not Signed In"

                val profileContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(18), dp(18), dp(18))
                }

                val avatarHeader = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(16))
                }
                val avatarBigCircle = TextView(this).apply {
                    text = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#475569"))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                }
                avatarHeader.addView(avatarBigCircle)

                val nameCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), 0, 0, 0)
                }
                nameCol.addView(TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 17f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                nameCol.addView(TextView(this).apply {
                    text = if (isGoogleAuth) userEmail else "Guest Mode • Sign in to backup data"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 13f
                    setPadding(0, dp(2), 0, 0)
                })
                avatarHeader.addView(nameCol)
                profileContent.addView(avatarHeader)

                if (!isGoogleAuth) {
                    val signInCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#4285F4"), 50), 16f)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dp(6), 0, dp(12))
                        }
                    }
                    signInCard.addView(TextView(this).apply {
                        text = "🔒 Cloud Sync & History Protection"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    signInCard.addView(TextView(this).apply {
                        text = "Sign in with your Google account to automatically backup study logs, restore progress across devices, and prevent data loss."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        setPadding(0, dp(4), 0, dp(12))
                    })
                    val googleBtn = Button(this).apply {
                        text = "Sign In with Google"
                        setTextColor(Color.WHITE)
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(Color.parseColor("#4285F4"))
                        }
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                        setOnClickListener {
                            startActivity(Intent(this@with, LoginActivity::class.java))
                        }
                    }
                    signInCard.addView(googleBtn)
                    profileContent.addView(signInCard)
                } else {
                    val editNameField = EditText(this).apply {
                        hint = "Edit Display Name"
                        setText(userName)
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 13.5f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 35), 12f)
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                    }
                    profileContent.addView(editNameField)

                    val saveNameBtn = Button(this).apply {
                        text = "Save Profile Name"
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)).apply {
                            setMargins(0, dp(10), 0, 0)
                        }
                        setOnClickListener {
                            val newName = editNameField.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                AuthManager.updateUserName(this@with, newName)
                                Toast.makeText(this@with, "Profile updated!", Toast.LENGTH_SHORT).show()
                                navigateToPanel(AppPanel.SETTINGS)
                            }
                        }
                    }
                    profileContent.addView(saveNameBtn)

                    val logoutBtn = TextView(this).apply {
                        text = "Sign Out from Google Account"
                        setTextColor(Color.parseColor("#EF4444"))
                        textSize = 13f
                        gravity = Gravity.CENTER
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(0, dp(18), 0, dp(4))
                        setOnClickListener {
                            AuthManager.logout(this@with)
                            Toast.makeText(this@with, "Signed out successfully", Toast.LENGTH_SHORT).show()
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                    }
                    profileContent.addView(logoutBtn)
                }

                profileCard.addView(profileContent)
                layout.addView(profileCard)

                layout.addView(createSectionLabel("PRIVACY & DATA OWNERSHIP"))
                val privacyCard = createSettingsCard().apply {
                    val pRow = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(14), dp(18), dp(14))
                    }
                    pRow.addView(TextView(this@with).apply {
                        text = "🔒 Privacy-First App"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    pRow.addView(TextView(this@with).apply {
                        text = "No private notes, task titles, contacts, microphone, or hardware fingerprints are collected. Anonymous ID: ${AppAnalytics.getAnonymousId(this@with).take(8)}..."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11.5f
                        setPadding(0, dp(4), 0, 0)
                    })
                    addView(pRow)
                }
                layout.addView(privacyCard)
            }

            // ==========================================
            // 3. TIMER & FOCUS CONTROLS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.TIMER) {
                layout.addView(createSectionLabel("TIMER OPERATION MODE"))
                val timerModeCard = createSettingsCard()
                val isLecture = timerMode == "LECTURE"
                val isStopwatch = timerMode == "STOPWATCH"
                val isCountdown = timerMode == "COUNTDOWN"
                val isSubject = timerMode == "SUBJECT"

                fun modeRadio(selected: Boolean): View {
                    return View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                            setStroke(dp(2), if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        }
                    }
                }

                val stopwatchRow = createSettingsRow("⏱️", getString(R.string.mode_stopwatch), getString(R.string.mode_stopwatch_sub), modeRadio(isStopwatch))
                stopwatchRow.setOnClickListener {
                    sharedPrefs.edit().putString("timer_mode", "STOPWATCH").putBoolean("lecture_mode_enabled", false).apply()
                    timerMode = "STOPWATCH"
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(stopwatchRow)
                timerModeCard.addView(createDivider())

                val countdownRow = createSettingsRow("⏳", getString(R.string.mode_pomodoro), getString(R.string.mode_pomodoro_sub), modeRadio(isCountdown))
                countdownRow.setOnClickListener {
                    sharedPrefs.edit().putString("timer_mode", "COUNTDOWN").putBoolean("lecture_mode_enabled", false).apply()
                    timerMode = "COUNTDOWN"
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(countdownRow)
                timerModeCard.addView(createDivider())

                val subjectRow = createSettingsRow("📚", "Subject-wise Tagging", "Tag and track focus time by dedicated subject", modeRadio(isSubject))
                subjectRow.setOnClickListener {
                    sharedPrefs.edit().putString("timer_mode", "SUBJECT").putBoolean("lecture_mode_enabled", false).putBoolean("show_subject_pie_chart", true).apply()
                    timerMode = "SUBJECT"
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(subjectRow)
                timerModeCard.addView(createDivider())

                val lectureRow = createSettingsRow("🎓", "Scheduled Lecture Mode", "Auto-tracks focus based on fixed class timetable", modeRadio(isLecture))
                lectureRow.setOnClickListener {
                    val isConfigured = !sharedPrefs.getString("lecture_schedules_json", "").isNullOrEmpty() && sharedPrefs.getString("lecture_schedules_json", "[]") != "[]"
                    sharedPrefs.edit().putString("timer_mode", "LECTURE").putBoolean("lecture_mode_enabled", true).apply()
                    timerMode = "LECTURE"
                    if (isConfigured) {
                        navigateToPanel(AppPanel.FOCUS)
                    } else {
                        showLectureScheduleManagerDialog()
                    }
                }
                timerModeCard.addView(lectureRow)
                layout.addView(timerModeCard)

                // 1. CONDITIONAL POMODORO CUSTOMIZER (Render only when COUNTDOWN / Pomodoro is active)
                if (timerMode == "COUNTDOWN") {
                    layout.addView(createSectionLabel("POMODORO INTERVALS & CYCLES"))
                    val intervalCard = createSettingsCard()

                    fun makeIntervalStepper(title: String, subtitle: String, key: String, defaultVal: Long, minVal: Long, maxVal: Long, stepVal: Long, unit: String): LinearLayout {
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(18), dp(12), dp(18), dp(12))
                        }
                        val textCol = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        textCol.addView(TextView(this).apply {
                            text = title
                            setTextColor(themeCoordinator.textColor)
                            textSize = 14.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        })
                        textCol.addView(TextView(this).apply {
                            text = subtitle
                            setTextColor(themeCoordinator.textColor)
                            alpha = 0.5f
                            textSize = 12f
                            setPadding(0, 2, 0, 0)
                        })
                        row.addView(textCol)

                        val valText = TextView(this).apply {
                            val curVal = sharedPrefs.safeLong(key, defaultVal)
                            text = "$curVal $unit"
                            textSize = 14.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setTextColor(themeCoordinator.primaryColor)
                            setPadding(dp(8), 0, dp(8), 0)
                        }

                        fun makeStepBtn(symbol: String, delta: Long): TextView {
                            return TextView(this).apply {
                                text = symbol
                                textSize = 18f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(themeCoordinator.textColor)
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 8f)
                                setPadding(dp(12), dp(4), dp(12), dp(4))
                                setOnClickListener {
                                    val cur = sharedPrefs.safeLong(key, defaultVal)
                                    val next = max(minVal, Math.min(maxVal, cur + delta))
                                    val editor = sharedPrefs.edit().putLong(key, next)
                                    if (key == "study_interval_minutes") {
                                        editor.putLong("focus_countdown_secs", next * 60L)
                                    }
                                    editor.apply()
                                    valText.text = "$next $unit"
                                }
                            }
                        }

                        val ctrlLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        ctrlLayout.addView(makeStepBtn("−", -stepVal))
                        ctrlLayout.addView(valText)
                        ctrlLayout.addView(makeStepBtn("+", stepVal))
                        row.addView(ctrlLayout)
                        return row
                    }

                    intervalCard.addView(makeIntervalStepper("Focus Duration", "Length of active study intervals", "study_interval_minutes", 25L, 5L, 120L, 5L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Short Break Duration", "Rest period between standard intervals", "break_interval_minutes", 5L, 1L, 30L, 1L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Long Break Duration", "Extended rest after completing a cycle", "long_break_minutes", 15L, 5L, 60L, 5L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Long Break Interval", "Number of focus sessions before a long break", "long_break_interval", 4L, 2L, 10L, 1L, "sessions"))
                    layout.addView(intervalCard)
                }

                layout.addView(createSectionLabel("DISPLAY & SCREEN BEHAVIORS"))
                val displayCard = createSettingsCard()
                val isKeepScreenOn = sharedPrefs.getBoolean("keep_screen_on", true)
                val keepScreenOnSwitch = SwitchMaterial(this).apply {
                    isChecked = isKeepScreenOn
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("keep_screen_on", isChecked).apply()
                        updateKeepScreenOn()
                    }
                }
                displayCard.addView(createSettingsRow("💡", getString(R.string.keep_screen_on), getString(R.string.keep_screen_on_sub), keepScreenOnSwitch))
                displayCard.addView(createDivider())

                val isPauseButtonEnabled = sharedPrefs.getBoolean("show_pause_button", true)
                val pauseButtonSwitch = SwitchMaterial(this).apply {
                    isChecked = isPauseButtonEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_pause_button", isChecked).apply()
                        if (currentPanel == AppPanel.FOCUS) updateVisualStyles()
                    }
                }
                displayCard.addView(createSettingsRow("⏸️", getString(R.string.pause_button), getString(R.string.pause_button_sub), pauseButtonSwitch))
                layout.addView(displayCard)
            }

            // ==========================================
            // 4. SOUND & AMBIENCE SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.AMBIENCE) {
                layout.addView(createSectionLabel("AMBIENT FOCUS SOUNDSCAPES"))
                val ambientCard = createSettingsCard()
                val ambientEnabled = sharedPrefs.safeBoolean("enable_ambient_sounds", false)
                val ambientSwitch = SwitchMaterial(this).apply {
                    isChecked = ambientEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("enable_ambient_sounds", isChecked).apply()
                        if (!isChecked) AmbientSoundEngine.stop()
                    }
                }
                ambientCard.addView(createSettingsRow("🎧", "Ambient Focus Soundscapes", "Enable background audio soundscapes during focus sessions", ambientSwitch))
                ambientCard.addView(createDivider())

                val volRow = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(12), dp(18), dp(14))
                }
                val curVol = sharedPrefs.getInt("ambient_sound_volume", 50)
                val volLabel = TextView(this).apply {
                    text = "🔊 Soundscape Volume: $curVol%"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                volRow.addView(volLabel)
                val volSeekBar = SeekBar(this).apply {
                    max = 100
                    progress = curVol
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, prog: Int, fromUser: Boolean) {
                            volLabel.text = "🔊 Soundscape Volume: $prog%"
                            sharedPrefs.edit().putInt("ambient_sound_volume", prog).apply()
                            AmbientSoundEngine.setVolume(prog / 100f)
                        }
                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                        override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                }
                volRow.addView(volSeekBar)
                ambientCard.addView(volRow)
                layout.addView(ambientCard)
            }

            // ==========================================
            // 5. ANALYTICS & GOALS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.ANALYTICS) {
                layout.addView(createSectionLabel("DAILY TARGETS & STREAKS"))
                val goalCard = createSettingsCard()
                val goalValueText = TextView(this).apply {
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(8), 0, dp(8), 0)
                }
                fun makeGoalStepBtn(symbol: String, stepSecs: Long): TextView {
                    return TextView(this).apply {
                        text = symbol
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(themeCoordinator.textColor)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                        setPadding(dp(14), dp(4), dp(14), dp(4))
                        setOnClickListener {
                            val current = sharedPrefs.getLong("daily_goal_secs", 2700L)
                            val next = max(900L, current + stepSecs)
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                goalRow.addView(TextView(this).apply { text = "🎯"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
                val goalTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal); setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
                goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal_sub); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
                goalRow.addView(goalTextCol)
                val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                controls.addView(makeGoalStepBtn("−", -900L))
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
                goalCard.addView(createSettingsRow("🔥", getString(R.string.streak_uses_goal), getString(R.string.streak_uses_goal_sub), streakGoalSwitch))
                layout.addView(goalCard)

                layout.addView(createSectionLabel("INSIGHTS & VISUALIZATION FILTERS"))
                val chartsCard = createSettingsCard()
                val isHeatmapEnabled = sharedPrefs.getBoolean("show_focus_heatmap", true)
                val heatmapSwitch = SwitchMaterial(this).apply {
                    isChecked = isHeatmapEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_focus_heatmap", isChecked).apply()
                    }
                }
                chartsCard.addView(createSettingsRow("🗓️", getString(R.string.focus_heatmap_setting), getString(R.string.focus_heatmap_setting_sub), heatmapSwitch))
                chartsCard.addView(createDivider())

                val isPieChartEnabled = sharedPrefs.safeBoolean("show_subject_pie_chart", true)
                val pieChartSwitch = SwitchMaterial(this).apply {
                    isChecked = isPieChartEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_subject_pie_chart", isChecked).apply()
                    }
                }
                chartsCard.addView(createSettingsRow("📊", "Subject Pie Chart", "Show subject breakdown and focus depth charts in Insights", pieChartSwitch))
                layout.addView(chartsCard)
            }

            // ==========================================
            // 6. CLOUD, SYNC & BACKUPS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.CLOUD) {
                layout.addView(createSectionLabel("CLOUD SYNCHRONIZATION"))
                val cloudCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userEmail = AuthManager.getUserEmail(this) ?: "Not Signed In"

                val authRow = createSettingsRow("☁️", "Cloud Account", userEmail)
                authRow.setOnClickListener {
                    if (!isGoogleAuth) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }
                cloudCard.addView(authRow)
                cloudCard.addView(createDivider())

                val syncPushBtn = Button(this).apply {
                    text = "⬆️ Force Immediate Cloud Push"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        setMargins(dp(16), dp(12), dp(16), dp(6))
                    }
                    setOnClickListener {
                        Thread {
                            kotlinx.coroutines.runBlocking {
                                val result = CloudSyncManager.syncDataToCloudDetailed(this@with, force = true)
                                runOnUiThread {
                                    if (result.isSuccess) {
                                        Toast.makeText(this@with, "☁️ Cloud sync completed successfully!", Toast.LENGTH_SHORT).show()
                                    } else if (result.isUnauthenticated) {
                                        Toast.makeText(this@with, "⚠️ Please sign in with your Google account first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this@with, "❌ Sync failed: ${result.errorMessage ?: "Check network connection"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }.start()
                    }
                }
                cloudCard.addView(syncPushBtn)

                val syncPullBtn = Button(this).apply {
                    text = "📥 Restore Data from Cloud"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        setMargins(dp(16), 0, dp(16), dp(14))
                    }
                    setOnClickListener {
                        Thread {
                            kotlinx.coroutines.runBlocking {
                                val ok = CloudSyncManager.restoreDataFromCloud(this@with)
                                runOnUiThread {
                                    if (ok) {
                                        Toast.makeText(this@with, "Cloud data restored successfully!", Toast.LENGTH_SHORT).show()
                                        tabPageCache.clear()
                                        statsDirty = true
                                        navigateToPanel(currentPanel)
                                    } else {
                                        Toast.makeText(this@with, "No cloud backup found or restore failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }.start()
                    }
                }
                cloudCard.addView(syncPullBtn)
                layout.addView(cloudCard)

                layout.addView(createSectionLabel("LOCAL BACKUP & EXPORT"))
                val dataCard = createSettingsCard()
                val exportRow = createSettingsRow("📤", getString(R.string.export_logs), getString(R.string.export_logs_sub))
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

                val importRow = createSettingsRow("📥", getString(R.string.import_data), getString(R.string.import_data_sub))
                importRow.setOnClickListener {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/json"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    importLauncher.launch(Intent.createChooser(intent, getString(R.string.select_backup_file)))
                }
                dataCard.addView(importRow)
                dataCard.addView(createDivider())

                val csvRow = createSettingsRow("📊", getString(R.string.export_csv), getString(R.string.export_csv_sub))
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
            }

            // ==========================================
            // 7. THEME & APPEARANCE SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.THEME) {
                layout.addView(createSectionLabel("PALETTE MODE"))
                val modeCard = createSettingsCard()
                val isEclipse = themeCoordinator.activeBgMode == "ECLIPSE"
                val isLight = themeCoordinator.activeBgMode == "LIGHT"

                fun modeRadio(selected: Boolean): View {
                    return View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                            setStroke(dp(2), if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        }
                    }
                }

                val oledRow = createSettingsRow("⬛", getString(R.string.theme_amoled), "Pure pitch AMOLED #000000 background", modeRadio(!isEclipse && !isLight))
                oledRow.setOnClickListener {
                    sharedPrefs.edit().putString("activeBgMode", "OLED").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(oledRow)
                modeCard.addView(createDivider())

                val eclipseRow = createSettingsRow("🌙", getString(R.string.theme_slate), getString(R.string.theme_slate_sub), modeRadio(isEclipse))
                eclipseRow.setOnClickListener {
                    sharedPrefs.edit().putString("activeBgMode", "ECLIPSE").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(eclipseRow)
                modeCard.addView(createDivider())

                val lightRow = createSettingsRow("☀️", getString(R.string.theme_light), getString(R.string.theme_light_sub), modeRadio(isLight))
                lightRow.setOnClickListener {
                    sharedPrefs.edit().putString("activeBgMode", "LIGHT").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(lightRow)
                layout.addView(modeCard)

                layout.addView(createSectionLabel("SURFACE & GLASS STYLING"))
                val styleCard = createSettingsCard()
                val isBubble = themeCoordinator.isBubbleStyle()
                val isGlass = themeCoordinator.isGlassStyle()

                val glassRow = createSettingsRow("✨", getString(R.string.style_glass), getString(R.string.style_glass_sub), modeRadio(isGlass))
                glassRow.setOnClickListener {
                    sharedPrefs.edit().putString("ui_style", "GLASS").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(glassRow)
                styleCard.addView(createDivider())

                val bubbleRow = createSettingsRow("🔮", "3D Look", "Tactile elevated depth and soft shadows", modeRadio(isBubble))
                bubbleRow.setOnClickListener {
                    sharedPrefs.edit().putString("ui_style", "BUBBLE").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(bubbleRow)
                styleCard.addView(createDivider())

                val classicRow = createSettingsRow("◽", getString(R.string.style_classic), getString(R.string.style_classic_sub), modeRadio(!isGlass && !isBubble))
                classicRow.setOnClickListener {
                    sharedPrefs.edit().putString("ui_style", "CLASSIC").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(classicRow)
                layout.addView(styleCard)

                // ==========================================
                // DUAL FOCUS & BREAK ACCENT COLOR CONTROLS
                // ==========================================
                fun makeAccentColorSection(
                    title: String,
                    subtitle: String,
                    prefKey: String,
                    currentColor: Int,
                    palette: List<String>,
                    onColorChanged: (Int) -> Unit
                ): LinearLayout {
                    val card = createSettingsCard()
                    val cardContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(14), dp(18), dp(16))
                    }

                    val headerRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 0, 0, dp(12))
                    }

                    var activeColor = currentColor
                    val hexStr = String.format("#%06X", 0xFFFFFF and activeColor)

                    val previewCircle = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                            setMargins(0, 0, dp(12), 0)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(activeColor)
                            setStroke(dp(2), Color.argb(100, 255, 255, 255))
                        }
                    }
                    headerRow.addView(previewCircle)

                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            minimumHeight = dp(38)
                        }
                    }
                    textCol.addView(TextView(this).apply {
                        text = title
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setSingleLine(true)
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    val hexLabel = TextView(this).apply {
                        text = "$subtitle  •  $hexStr"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 12f
                        setSingleLine(true)
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, 2, 0, 0)
                    }
                    textCol.addView(hexLabel)
                    headerRow.addView(textCol)
                    cardContainer.addView(headerRow)

                    // Curated Aesthetic Soft Swatches Horizontal Scroll
                    val swatchesScroll = android.widget.HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false
                        setPadding(0, 0, 0, dp(12))
                    }
                    val swatchesLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val swatchViews = ArrayList<View>()

                    fun refreshSwatchBorders() {
                        for ((idx, sView) in swatchViews.withIndex()) {
                            val hex = palette[idx]
                            val isMatch = String.format("#%06X", 0xFFFFFF and activeColor).equals(hex, ignoreCase = true)
                            (sView.background as? GradientDrawable)?.apply {
                                setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                            }
                        }
                    }

                    for (hex in palette) {
                        val swatchColor = Color.parseColor(hex)
                        val sView = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                                setMargins(0, 0, dp(8), 0)
                            }
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(swatchColor)
                                val isMatch = String.format("#%06X", 0xFFFFFF and activeColor).equals(hex, ignoreCase = true)
                                setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                            }
                            setOnClickListener {
                                activeColor = swatchColor
                                (previewCircle.background as? GradientDrawable)?.setColor(activeColor)
                                val newHex = String.format("#%06X", 0xFFFFFF and activeColor)
                                hexLabel.text = "$subtitle  •  $newHex"
                                sharedPrefs.edit().putInt(prefKey, activeColor).apply()
                                onColorChanged(activeColor)
                                refreshSwatchBorders()
                            }
                        }
                        swatchViews.add(sView)
                        swatchesLayout.addView(sView)
                    }
                    swatchesScroll.addView(swatchesLayout)
                    cardContainer.addView(swatchesScroll)

                    // Continuous Hue Bar / Slider (0° - 360°)
                    val hsv = FloatArray(3)
                    Color.colorToHSV(activeColor, hsv)

                    val hueLabel = TextView(this).apply {
                        text = "🎨 Fine-Tune Hue Slider"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 11f
                        setPadding(0, 0, 0, dp(4))
                    }
                    cardContainer.addView(hueLabel)

                    val hueSeekBar = android.widget.SeekBar(this).apply {
                        max = 360
                        progress = hsv[0].toInt()
                        setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                                if (!fromUser) return
                                val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.70f, 0.95f))
                                activeColor = colorInt
                                (previewCircle.background as? GradientDrawable)?.setColor(activeColor)
                                val newHex = String.format("#%06X", 0xFFFFFF and activeColor)
                                hexLabel.text = "$subtitle  •  $newHex"
                                sharedPrefs.edit().putInt(prefKey, activeColor).apply()
                                onColorChanged(activeColor)
                                refreshSwatchBorders()
                            }
                            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                        })
                    }
                    cardContainer.addView(hueSeekBar)

                    card.addView(cardContainer)
                    return card
                }

                layout.addView(createSectionLabel("FOCUS SESSION ACCENT COLOR"))
                val focusColorCard = makeAccentColorSection(
                    title = "🎯 Focus Accent Color",
                    subtitle = "Drives focus timer ring & buttons",
                    prefKey = "customPrimary",
                    currentColor = themeCoordinator.primaryColor,
                    palette = ThemeCoordinator.SOFT_FOCUS_PALETTE
                ) { newColor ->
                    themeCoordinator.primaryColor = newColor
                    (settingsBackFab.background as? GradientDrawable)?.setColor(newColor)
                    updateVisualStyles()
                    tabPageCache.clear()
                }
                layout.addView(focusColorCard)

                layout.addView(createSectionLabel("BREAK SESSION ACCENT COLOR"))
                val breakColorCard = makeAccentColorSection(
                    title = "☕ Break Accent Color",
                    subtitle = "Drives break countdown & status badge",
                    prefKey = "customSecondary",
                    currentColor = themeCoordinator.secondaryColor,
                    palette = ThemeCoordinator.SOFT_BREAK_PALETTE
                ) { newColor ->
                    themeCoordinator.secondaryColor = newColor
                    updateVisualStyles()
                    tabPageCache.clear()
                }
                layout.addView(breakColorCard)
            }

            // ==========================================
            // 8. DEVELOPER & ADVANCED SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.DEVELOPER) {
                layout.addView(createSectionLabel("DEVELOPER CONSOLE"))
                val devCard = DeveloperToolsHelper.buildDevCard(host, themeCoordinator)
                layout.addView(devCard)
            }

            // Push to bottom footer
            val pushToBottomSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
            layout.addView(pushToBottomSpacer)

            val creditsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(24), 0, dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            creditsContainer.addView(TextView(this).apply { text = getString(R.string.developed_by); setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); gravity = Gravity.CENTER })
            creditsContainer.addView(TextView(this).apply { text = getString(R.string.special_thanks); setTextColor(themeCoordinator.textColor); alpha = 0.4f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL); gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0) })
            layout.addView(creditsContainer)

            settingsRootLayout.addView(settingsScrollView)

            val settingsRoot = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }
            settingsRoot.addView(settingsRootLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            settingsRoot.addView(settingsBackFab)

            target.addView(settingsRoot)
        }
    }
}
