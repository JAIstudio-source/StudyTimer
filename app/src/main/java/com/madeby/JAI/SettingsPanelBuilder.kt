package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
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
 * 1. Timer & Focus (Interval pickers, auto-start breaks, strict mode, mode picker)
 * 2. Goals & Reminders (Daily goal target picker, custom reminder picker, streak settings)
 * 3. Theme & Appearance (AMOLED/Slate/Light, 3D Bubble/Glass/Classic, Accent colors)
 * 4. Cloud & Backups (Google account sync, backup JSON/CSV export/import)
 * 5. User Profile Management Sub-Screen
 * 6. Developer & Advanced (Strictly gated behind dev unlock / debug)
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
                text = "← Back"
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.bgColor)
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                elevation = dp(6).toFloat()
                setPadding(dp(24), 0, dp(24), 0)
                setOnClickListener {
                    if (currentSettingsTab != AppSettingsTab.HUB) {
                        currentSettingsTab = AppSettingsTab.HUB
                        navigateToPanel(AppPanel.SETTINGS)
                    } else {
                        navigateToPanel(AppPanel.FOCUS)
                    }
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    dp(48),
                    Gravity.BOTTOM or Gravity.END
                ).apply {
                    setMargins(0, 0, dp(20), dp(20))
                }
            }

            // Top Header & Hub Breadcrumb
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(4), dp(6), dp(6))
            }

            val backArrowBtn = TextView(this).apply {
                text = "←"
                textSize = 24f
                setTextColor(themeCoordinator.primaryColor)
                setPadding(dp(4), 0, dp(14), 0)
                setOnClickListener {
                    if (currentSettingsTab != AppSettingsTab.HUB) {
                        currentSettingsTab = AppSettingsTab.HUB
                        navigateToPanel(AppPanel.SETTINGS)
                    } else {
                        navigateToPanel(AppPanel.FOCUS)
                    }
                }
            }
            headerRow.addView(backArrowBtn)

            val headerText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> getString(R.string.settings_title)
                    AppSettingsTab.TIMER -> "Timer & Focus"
                    AppSettingsTab.ANALYTICS -> "Goals & Reminders"
                    AppSettingsTab.THEME -> "Theme & Appearance"
                    AppSettingsTab.CLOUD -> "Cloud & Backups"
                    AppSettingsTab.PROFILE -> "Profile & Account"
                    AppSettingsTab.DEVELOPER -> "Developer Tools"
                    else -> getString(R.string.settings_title)
                }
                setTextColor(themeCoordinator.textColor)
                textSize = 22f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                letterSpacing = -0.01f
            }
            headerRow.addView(headerText)
            settingsRootLayout.addView(headerRow)

            val subtitleText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> "Customize your timer, theme, goals & backups"
                    AppSettingsTab.TIMER -> "Focus durations, break times & timer mode"
                    AppSettingsTab.ANALYTICS -> "Daily targets, goal reminders & chart display"
                    AppSettingsTab.THEME -> "OLED black, light mode, styles & colors"
                    AppSettingsTab.CLOUD -> "Google account sync, auto-backup & restore"
                    AppSettingsTab.PROFILE -> "Profile photo, display name & account status"
                    AppSettingsTab.DEVELOPER -> "Diagnostic tools & debug settings"
                    else -> getString(R.string.settings_subtitle)
                }
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                textSize = 13f
                setPadding(dp(6), 0, dp(6), dp(14))
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
                    isFillViewport = true
                    isNestedScrollingEnabled = true
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                if (captureScrollRef) settingsScrollViewRef = settingsScrollView
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), 0, dp(16), dp(96))
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
                        setColor(if (themeCoordinator.isDarkMode()) Color.parseColor("#22232B") else tintedColor(themeCoordinator.textColor, 25))
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
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
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

            fun createCustomDonutIcon(): View {
                return object : View(this) {
                    private val trackPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dp(4).toFloat()
                    }
                    private val arcPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dp(4).toFloat()
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    private val arcRect = android.graphics.RectF()

                    override fun onDraw(canvas: android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val pad = dp(3).toFloat()
                        arcRect.set(pad, pad, width.toFloat() - pad, height.toFloat() - pad)
                        trackPaint.color = tintedColor(themeCoordinator.primaryColor, 50)
                        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)
                        arcPaint.color = themeCoordinator.primaryColor
                        canvas.drawArc(arcRect, -90f, 220f, false, arcPaint)
                    }
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                }
            }

            fun createSettingsRowWithView(customIconView: View, title: String, subtitle: String, trailingView: View? = null): LinearLayout {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
                }
                row.addView(customIconView)
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
                    val outValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    foreground = getDrawable(outValue.resourceId)
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

                // Avatar bubble with local photo support
                val customAvatarBitmap = LocalAvatarManager.getCircularAvatarBitmap(this, dp(48))
                if (customAvatarBitmap != null) {
                    val avatarImg = ImageView(this).apply {
                        setImageBitmap(customAvatarBitmap)
                        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setStroke(dp(2), themeCoordinator.primaryColor)
                        }
                    }
                    profileRow.addView(avatarImg)
                } else {
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
                }

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

                // --- CATEGORIZED NAVIGATION ROW BUILDER ---
                fun addHubRowToCard(card: LinearLayout, icon: String, title: String, subtitle: String, targetTab: AppSettingsTab) {
                    val chevron = TextView(this).apply {
                        text = "›"
                        textSize = 22f
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.35f
                        setPadding(dp(8), 0, 0, 0)
                    }
                    val row = createSettingsRow(icon, title, subtitle, chevron)
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    row.setBackgroundResource(outVal.resourceId)
                    row.isClickable = true
                    row.isFocusable = true
                    row.setOnClickListener {
                        currentSettingsTab = targetTab
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                    card.addView(row)
                }

                val focusMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                val breakMins = sharedPrefs.safeLong("break_interval_minutes", 5L)
                val timerSub = "Focus: ${focusMins}m  •  Break: ${breakMins}m  •  ${timerMode.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} Mode"

                val dailyGoalSecs = sharedPrefs.getLong("daily_goal_secs", 7200L)
                val analyticsSub = "Target: ${formatGoalLabel(dailyGoalSecs)} • Goal Reminders & Stats"

                val themeSub = "${themeCoordinator.activeBgMode.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} Palette • ${if (themeCoordinator.isGlassStyle()) "Glass" else "Standard"} Style"

                val cloudSub = if (isGoogleAuth) "Connected: $userEmail • Auto Backup Active" else "Sign In, Backup & Cloud Restore"

                // 1. UNIFIED PREFERENCES CARD GROUP
                layout.addView(createSectionLabel("PREFERENCES"))
                val prefsCard = createSettingsCard()
                addHubRowToCard(prefsCard, "⏱️", "Timer & Focus", timerSub, AppSettingsTab.TIMER)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "📊", "Goals & Reminders", analyticsSub, AppSettingsTab.ANALYTICS)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "🎨", "Theme & Appearance", themeSub, AppSettingsTab.THEME)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "☁️", "Cloud, Sync & Backups", cloudSub, AppSettingsTab.CLOUD)

                if (isDevModeUnlocked) {
                    prefsCard.addView(createDivider())
                    addHubRowToCard(prefsCard, "🛠️", "Developer Tools", "Diagnostic tools & debug settings", AppSettingsTab.DEVELOPER)
                }
                layout.addView(prefsCard)

                // 2. UNIFIED COMPACT "ABOUT & LEGAL" ACCORDION CARD
                layout.addView(createSectionLabel("ABOUT & LEGAL"))
                val aboutLegalCard = createSettingsCard()

                var isAboutExpanded = sharedPrefs.getBoolean("about_legal_expanded", false)

                val expandChevron = TextView(this).apply {
                    text = if (isAboutExpanded) "⌄" else "›"
                    textSize = 20f
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    setPadding(dp(8), 0, 0, 0)
                }

                val expandedContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (isAboutExpanded) View.VISIBLE else View.GONE
                }

                val headerRowToggle = createSettingsRow(
                    "ℹ️",
                    "About, Support & Policies",
                    if (isAboutExpanded) "Tap to collapse" else "Version v${currentVersionName()} • Guide, feedback & legal terms",
                    expandChevron
                )
                headerRowToggle.isClickable = true
                headerRowToggle.isFocusable = true
                headerRowToggle.setOnClickListener {
                    isAboutExpanded = !isAboutExpanded
                    sharedPrefs.edit().putBoolean("about_legal_expanded", isAboutExpanded).apply()
                    expandChevron.text = if (isAboutExpanded) "⌄" else "›"
                    expandedContent.visibility = if (isAboutExpanded) View.VISIBLE else View.GONE
                }
                aboutLegalCard.addView(headerRowToggle)

                // Items inside expandedContent
                expandedContent.addView(createDivider())

                val guideRow = createSettingsRow("📖", "How to Use / App Guide", "User manual, timer modes & feature walkthrough")
                guideRow.setOnClickListener {
                    showAppGuideDialog()
                }
                expandedContent.addView(guideRow)
                expandedContent.addView(createDivider())

                val feedbackRow = createSettingsRow("💬", "Report a Problem & Feedback", "Send bug reports, feature suggestions or contact us")
                feedbackRow.setOnClickListener {
                    showFeedbackReportDialog()
                }
                expandedContent.addView(feedbackRow)
                expandedContent.addView(createDivider())

                val updateChip = if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
                    TextView(this).apply {
                        text = "Check"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 45), 12f)
                        setPadding(dp(10), dp(5), dp(10), dp(5))
                    }
                } else null

                val versionRow = createSettingsRow(
                    "🚀",
                    "Version v${currentVersionName()} (Build ${currentVersionCodeLong()})",
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) "Check for the latest release & changelog" else "StudyTimer for Android",
                    updateChip
                )
                versionRow.setOnClickListener {
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
                        checkForUpdates(manual = true)
                    } else {
                        Toast.makeText(this, "Version v${currentVersionName()} is up to date", Toast.LENGTH_SHORT).show()
                    }
                }
                expandedContent.addView(versionRow)
                expandedContent.addView(createDivider())

                val privacyRow = createSettingsRow("🛡️", "Privacy Policy", "Read our data collection, analytics & privacy practices")
                privacyRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/privacy.html")))
                    } catch (_: Exception) {
                        Toast.makeText(this, "Opening privacy policy...", Toast.LENGTH_SHORT).show()
                    }
                }
                expandedContent.addView(privacyRow)
                expandedContent.addView(createDivider())

                val termsRow = createSettingsRow("📜", "Terms of Service", "Review our terms, usage guidelines & licensing")
                termsRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/terms.html")))
                    } catch (_: Exception) {
                        Toast.makeText(this, "Opening terms of service...", Toast.LENGTH_SHORT).show()
                    }
                }
                expandedContent.addView(termsRow)
                expandedContent.addView(createDivider())

                val deleteWebRow = createSettingsRow("🗑️", "Account Deletion Web Portal", "Request permanent deletion of data online (Play Store policy)")
                deleteWebRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/delete-account.html")))
                    } catch (_: Exception) {
                        Toast.makeText(this, "Opening account deletion portal...", Toast.LENGTH_SHORT).show()
                    }
                }
                expandedContent.addView(deleteWebRow)

                aboutLegalCard.addView(expandedContent)
                layout.addView(aboutLegalCard)

                // Developer Credits (Main Settings Hub Only - Natural End of Scroll)
                val creditsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp(24), 0, 0)
                    }
                }
                var devClickCount = 0
                val developedByText = TextView(this).apply {
                    text = getString(R.string.developed_by)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        devClickCount++
                        if (devClickCount >= 5) {
                            devClickCount = 0
                            isDevModeUnlocked = true
                            Toast.makeText(context, "Developer Mode Activated", Toast.LENGTH_SHORT).show()
                            currentSettingsTab = AppSettingsTab.DEVELOPER
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                    }
                }
                creditsContainer.addView(developedByText)
                layout.addView(creditsContainer)
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
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                val profileContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(20), dp(24), dp(20), dp(20))
                }

                // Centered Avatar Frame with Camera Edit Badge
                val avatarWrapper = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, dp(14))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        host.pickProfileAvatar()
                    }
                }

                val customAvatarLarge = LocalAvatarManager.getCircularAvatarBitmap(this, dp(88))
                if (customAvatarLarge != null) {
                    val avatarImg = ImageView(this).apply {
                        setImageBitmap(customAvatarLarge)
                        layoutParams = FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setStroke(dp(2), themeCoordinator.primaryColor)
                        }
                    }
                    avatarWrapper.addView(avatarImg)
                } else {
                    val avatarBigCircle = TextView(this).apply {
                        text = avatarInitials
                        textSize = 34f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#475569"))
                        }
                        layoutParams = FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER)
                    }
                    avatarWrapper.addView(avatarBigCircle)
                }

                // Edit Camera Badge
                val cameraBadge = TextView(this).apply {
                    text = "📷"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (themeCoordinator.isDarkMode()) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
                        setStroke(dp(2), themeCoordinator.bgColor)
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END)
                }
                avatarWrapper.addView(cameraBadge)
                profileContent.addView(avatarWrapper)

                // User Display Name
                val nameText = TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 19f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                profileContent.addView(nameText)

                // User Email
                val emailText = TextView(this).apply {
                    text = userEmail
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(3), 0, dp(8))
                }
                profileContent.addView(emailText)

                // Account Badge Chip
                val accountBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "✓ Google Account" else "👤 Guest Session"
                    setTextColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"), 35), 14f)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                }
                profileContent.addView(accountBadge)

                if (!isGoogleAuth) {
                    val signInCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#4285F4"), 50), 16f)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dp(4), 0, dp(8))
                        }
                    }
                    signInCard.addView(TextView(this).apply {
                        text = "Cloud Backup & Sync"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    signInCard.addView(TextView(this).apply {
                        text = "Sign in with Google to automatically back up your study sessions and keep your progress safe across devices."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        setPadding(0, dp(4), 0, dp(12))
                    })
                    val googleBtn = Button(this).apply {
                        text = "Sign in with Google"
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
                        hint = "Display Name"
                        setText(userName)
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 13.5f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 35), 12f)
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    }
                    profileContent.addView(editNameField)

                    val saveNameBtn = Button(this).apply {
                        text = "Save Name"
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)).apply {
                            setMargins(0, dp(8), 0, 0)
                        }
                        setOnClickListener {
                            val newName = editNameField.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                AuthManager.updateUserName(this@with, newName)
                                Toast.makeText(this@with, "Name saved!", Toast.LENGTH_SHORT).show()
                                navigateToPanel(AppPanel.SETTINGS)
                            }
                        }
                    }
                    profileContent.addView(saveNameBtn)
                }

                profileCard.addView(profileContent)
                layout.addView(profileCard)

                // --- ACCOUNT ACTIONS CARD ---
                layout.addView(createSectionLabel("ACCOUNT"))
                val accountActionsCard = createSettingsCard().apply {
                    val actionsLayout = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }

                    // Sync Status Row
                    val syncRow = LinearLayout(this@with).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(4), 0, dp(12))
                    }
                    val syncIcon = TextView(this@with).apply {
                        text = if (isGoogleAuth) "☁" else "○"
                        textSize = 18f
                        setPadding(0, 0, dp(12), 0)
                    }
                    syncRow.addView(syncIcon)

                    val lastSyncEpoch = sharedPrefs.getLong("last_cloud_sync_timestamp", 0L)
                    val syncCol = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    syncCol.addView(TextView(this@with).apply {
                        text = "Cloud Backup"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    syncCol.addView(TextView(this@with).apply {
                        text = if (isGoogleAuth) {
                            if (lastSyncEpoch > 0L) {
                                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                "Connected • Last synced ${sdf.format(Date(lastSyncEpoch))}"
                            } else {
                                "Connected • Auto-sync active"
                            }
                        } else {
                            "Offline • Your study sessions are saved safely on this device"
                        }
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                    syncRow.addView(syncCol)
                    actionsLayout.addView(syncRow)

                    if (isGoogleAuth) {
                        actionsLayout.addView(createDivider())

                        val signOutBtn = Button(this@with).apply {
                            text = "Sign Out"
                            setTextColor(Color.parseColor("#EF4444"))
                            textSize = 13.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 40), 12f)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                                setMargins(0, dp(10), 0, dp(8))
                            }
                            setOnClickListener {
                                host.showSignOutConfirmDialog()
                            }
                        }
                        actionsLayout.addView(signOutBtn)
                    }

                    actionsLayout.addView(createDivider())

                    // Delete Account Link
                    val deleteAccountLink = TextView(this@with).apply {
                        text = "Delete Account & Clear Cloud Data"
                        setTextColor(Color.parseColor("#EF4444"))
                        alpha = 0.85f
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(12), 0, dp(4))
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            showDeleteAccountDialog()
                        }
                    }
                    actionsLayout.addView(deleteAccountLink)

                    addView(actionsLayout)
                }
                layout.addView(accountActionsCard)
            }

            // ==========================================
            // 3. TIMER & FOCUS CONTROLS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.TIMER) {
                layout.addView(createSectionLabel("TIMER STYLE"))
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

                val stopwatchRow = createSettingsRow("⏱", getString(R.string.mode_stopwatch), getString(R.string.mode_stopwatch_sub), modeRadio(isStopwatch))
                stopwatchRow.setOnClickListener {
                    sharedPrefs.edit().putString("timer_mode", "STOPWATCH").putBoolean("lecture_mode_enabled", false).apply()
                    timerMode = "STOPWATCH"
                    if (currentTimerState == TimerState.IDLE) {
                        focusRemainingSecs = 0L
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(stopwatchRow)
                timerModeCard.addView(createDivider())

                val countdownRow = createSettingsRow("⏳", getString(R.string.mode_pomodoro), getString(R.string.mode_pomodoro_sub), modeRadio(isCountdown))
                countdownRow.setOnClickListener {
                    val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                    val pomoSecs = pomoMins * 60L
                    sharedPrefs.edit()
                        .putString("timer_mode", "COUNTDOWN")
                        .putBoolean("lecture_mode_enabled", false)
                        .putLong("focus_countdown_secs", pomoSecs)
                        .apply()
                    timerMode = "COUNTDOWN"
                    if (currentTimerState == TimerState.IDLE) {
                        focusCountdownSecs = pomoSecs
                        focusRemainingSecs = pomoSecs
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(countdownRow)
                timerModeCard.addView(createDivider())

                val subjectRow = createSettingsRow("📚", "Subject Focus", "Pick a subject and track your study time", modeRadio(isSubject))
                subjectRow.setOnClickListener {
                    val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                    val pomoSecs = pomoMins * 60L
                    sharedPrefs.edit()
                        .putString("timer_mode", "SUBJECT")
                        .putBoolean("lecture_mode_enabled", false)
                        .putBoolean("show_subject_pie_chart", true)
                        .putLong("focus_countdown_secs", pomoSecs)
                        .apply()
                    timerMode = "SUBJECT"
                    if (currentTimerState == TimerState.IDLE) {
                        focusCountdownSecs = pomoSecs
                        focusRemainingSecs = pomoSecs
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(subjectRow)
                timerModeCard.addView(createDivider())

                val lectureRow = createSettingsRow("🎓", "Class Schedule", "Follow your custom class timetable", modeRadio(isLecture))
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
                    layout.addView(createSectionLabel("STUDY & BREAK TIMERS"))
                    val intervalCard = createSettingsCard()

                    val isFreedomMode = sharedPrefs.getBoolean("pomodoro_freedom_mode", false)
                    val freedomSwitch = SwitchMaterial(this).apply {
                        isChecked = isFreedomMode
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_freedom_mode", isChecked).apply()
                            tabPageCache.remove(settingsTabKey(AppSettingsTab.TIMER))
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                    }
                    intervalCard.addView(createSettingsRow("🚀", "Continuous Timer Mode", "Study continuously without automatic breaks or session limits", freedomSwitch))
                    intervalCard.addView(createDivider())

                    fun formatIntervalValue(valMinutes: Long, unit: String): String {
                        if (unit != "min") return "$valMinutes $unit"
                        val h = valMinutes / 60L
                        val m = valMinutes % 60L
                        return when {
                            h > 0L && m > 0L -> "${h}h ${m}m"
                            h > 0L -> "${h}h"
                            else -> "${m}m"
                        }
                    }

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
                            text = formatIntervalValue(curVal, unit)
                            textSize = 14.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setTextColor(themeCoordinator.primaryColor)
                            setPadding(dp(8), 0, dp(8), 0)
                        }

                        fun makeStepBtn(symbol: String, delta: Long): TextView {
                            val btn = TextView(this).apply {
                                text = symbol
                                textSize = 18f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(themeCoordinator.textColor)
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 8f)
                                setPadding(dp(12), dp(4), dp(12), dp(4))
                            }

                            val repeatHandler = Handler(Looper.getMainLooper())
                            var holdTicks = 0

                            fun performStep() {
                                val cur = sharedPrefs.safeLong(key, defaultVal)
                                val multiplier = when {
                                    holdTicks > 30 -> 12L // fast jump (60 min / step)
                                    holdTicks > 15 -> 6L  // 30 min jump
                                    holdTicks > 6 -> 2L   // 10 min jump
                                    else -> 1L
                                }
                                val effectiveDelta = delta * multiplier
                                val next = max(minVal, Math.min(maxVal, cur + effectiveDelta))
                                val editor = sharedPrefs.edit().putLong(key, next)
                                if (key == "study_interval_minutes") {
                                    val nextSecs = next * 60L
                                    editor.putLong("focus_countdown_secs", nextSecs)
                                    if (currentTimerState == TimerState.IDLE) {
                                        focusCountdownSecs = nextSecs
                                        if (timerMode == "COUNTDOWN") {
                                            focusRemainingSecs = nextSecs
                                        }
                                    }
                                } else if (key == "break_interval_minutes") {
                                    val nextBreakSecs = next * 60L
                                    editor.putLong("break_countdown_secs", nextBreakSecs)
                                }
                                editor.apply()
                                valText.text = formatIntervalValue(next, unit)
                                try { btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                            }

                            val repeatRunnable: Runnable = object : Runnable {
                                override fun run() {
                                    holdTicks++
                                    performStep()
                                    val nextDelay = when {
                                        holdTicks > 30 -> 50L
                                        holdTicks > 15 -> 90L
                                        holdTicks > 6 -> 160L
                                        else -> 260L
                                    }
                                    repeatHandler.postDelayed(this, nextDelay)
                                }
                            }

                            btn.setOnTouchListener { v, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        holdTicks = 0
                                        performStep()
                                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).start()
                                        repeatHandler.postDelayed(repeatRunnable, 350)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        repeatHandler.removeCallbacks(repeatRunnable)
                                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                    }
                                }
                                true
                            }

                            return btn
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

                    val maxFocus = if (isFreedomMode) 1440L else 120L
                    val focusSubtitle = if (isFreedomMode) "Extended continuous focus (up to 24h / 1440m)" else "How long each study session lasts"
                    intervalCard.addView(makeIntervalStepper("Study Duration", focusSubtitle, "study_interval_minutes", 25L, 5L, maxFocus, 5L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Short Break", if (isFreedomMode) "Standard break (bypassed in Freedom Mode)" else "Quick break between study sessions", "break_interval_minutes", 5L, 1L, 30L, 1L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Long Break", if (isFreedomMode) "Extended rest (bypassed in Freedom Mode)" else "Longer rest after completing multiple sessions", "long_break_minutes", 15L, 5L, 60L, 5L, "min"))
                    intervalCard.addView(createDivider())
                    intervalCard.addView(makeIntervalStepper("Sessions Until Long Break", if (isFreedomMode) "Cycle limit (uncapped in Freedom Mode)" else "Number of study sessions before a longer rest", "long_break_interval", 4L, 2L, 10L, 1L, "sessions"))
                    intervalCard.addView(createDivider())
                    val isPureWhitePomo = sharedPrefs.getBoolean("pomodoro_pure_white_theme", false)
                    val pureWhiteSwitch = SwitchMaterial(this).apply {
                        isChecked = isPureWhitePomo
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_pure_white_theme", isChecked).apply()
                            tabPageCache.clear()
                            if (currentPanel == AppPanel.FOCUS) {
                                updateVisualStyles()
                            }
                        }
                    }
                    intervalCard.addView(createSettingsRow("⚪", "Pure White Theme for Pomodoro", "Minimalist pure white background with black timer ring & numerals (Timer screen only)", pureWhiteSwitch))
                    layout.addView(intervalCard)
                }

                layout.addView(createSectionLabel("DISPLAY & SCREEN"))
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
                displayCard.addView(createSettingsRow("⏸", getString(R.string.pause_button), getString(R.string.pause_button_sub), pauseButtonSwitch))
                displayCard.addView(createDivider())

                val isLandscapeStopwatchEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
                val landscapeSwitch = SwitchMaterial(this).apply {
                    isChecked = isLandscapeStopwatchEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit()
                            .putBoolean("is_landscape_mode_enabled", isChecked)
                            .putBoolean("true_fullscreen_landscape", isChecked)
                            .apply()
                        if (currentPanel == AppPanel.FOCUS) {
                            applyImmersiveModeForLandscape()
                            buildCurrentPanel()
                        }
                    }
                }
                displayCard.addView(createSettingsRow("📱", "Landscape Fullscreen", "Rotate your phone sideways for a distraction-free fullscreen clock", landscapeSwitch))
                displayCard.addView(createDivider())

                val isPureWhite = sharedPrefs.getBoolean("pureWhiteTimer", false)
                val pureWhiteSwitch = SwitchMaterial(this).apply {
                    isChecked = isPureWhite
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("pureWhiteTimer", isChecked).apply()
                        updateVisualStyles()
                        tabPageCache.clear()
                    }
                }
                displayCard.addView(createSettingsRow("○", "Pure White Clock", "Keep timer numbers clean white instead of using your accent color", pureWhiteSwitch))
                layout.addView(displayCard)

                layout.addView(createSectionLabel("ADJUST STUDY TIME"))
                val adjustCard = createSettingsCard()
                val adjustRow = createSettingsRow("⏱", "Adjust Today's Study Time", "Add missed study minutes or correct your total for today")
                adjustRow.setOnClickListener {
                    DeveloperToolsHelper.showAdjustTodayTimeDialog(host, themeCoordinator, isDeveloperExtended = false)
                }
                adjustCard.addView(adjustRow)
                layout.addView(adjustCard)
            }

            // ==========================================
            // 4. ANALYTICS & GOALS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.ANALYTICS) {
                layout.addView(createSectionLabel("STUDY REMINDERS"))
                val reminderCard = createSettingsCard()
                val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
                val reminderSwitch = SwitchMaterial(this).apply {
                    isChecked = reminderEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()
                        if (isChecked) {
                            GoalReminderScheduler.schedule(context)
                        } else {
                            GoalReminderScheduler.cancel(context)
                        }
                    }
                }
                reminderCard.addView(createSettingsRow("🔔", "Daily Goal Reminder", "Get a friendly evening reminder if you haven't reached your study goal", reminderSwitch))
                reminderCard.addView(createDivider())

                val remHour = sharedPrefs.safeInt("reminder_hour", 20)
                val remMinute = sharedPrefs.safeInt("reminder_minute", 0)
                val timeLabel = TextView(this).apply {
                    text = TimeFormat.formatHourMinute(context, remHour, remMinute)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                }
                val timeRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    setOnClickListener {
                        val curH = sharedPrefs.safeInt("reminder_hour", 20)
                        val curM = sharedPrefs.safeInt("reminder_minute", 0)
                        val is24H = TimeFormat.is24Hour(context)
                        android.app.TimePickerDialog(context, { _, hourOfDay, minute ->
                            sharedPrefs.edit()
                                .putInt("reminder_hour", hourOfDay)
                                .putInt("reminder_minute", minute)
                                .apply()
                            timeLabel.text = TimeFormat.formatHourMinute(context, hourOfDay, minute)
                            GoalReminderScheduler.schedule(context)
                        }, curH, curM, is24H).show()
                    }
                }
                timeRow.addView(TextView(this).apply { text = "⏰"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
                val timeTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                timeTextCol.addView(TextView(this).apply {
                    text = "Reminder Time"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                timeTextCol.addView(TextView(this).apply {
                    text = "Choose when to receive your daily reminder"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                timeRow.addView(timeTextCol)
                timeRow.addView(timeLabel)
                reminderCard.addView(timeRow)
                layout.addView(reminderCard)

                layout.addView(createSectionLabel("DAILY GOALS & STREAKS"))
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
                goalCard.addView(createDivider())
                val adjustStatsRow = createSettingsRow("⏱", "Adjust Today's Study Time", "Add missed study minutes or correct your total for today")
                adjustStatsRow.setOnClickListener {
                    DeveloperToolsHelper.showAdjustTodayTimeDialog(host, themeCoordinator, isDeveloperExtended = false)
                }
                goalCard.addView(adjustStatsRow)
                layout.addView(goalCard)

                layout.addView(createSectionLabel("STATS & CHARTS"))
                val chartsCard = createSettingsCard()
                val isHeatmapEnabled = sharedPrefs.getBoolean("show_focus_heatmap", true)
                val heatmapSwitch = SwitchMaterial(this).apply {
                    isChecked = isHeatmapEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_focus_heatmap", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("🗓", getString(R.string.focus_heatmap_setting), getString(R.string.focus_heatmap_setting_sub), heatmapSwitch))
                chartsCard.addView(createDivider())

                val isPieChartEnabled = sharedPrefs.safeBoolean("show_subject_pie_chart", true)
                val pieChartSwitch = SwitchMaterial(this).apply {
                    isChecked = isPieChartEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_subject_pie_chart", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("📊", "Subject Breakdown Chart", "Show your subject time charts in Stats", pieChartSwitch))
                chartsCard.addView(createDivider())

                val isDonutEnabled = sharedPrefs.safeBoolean("use_donut_chart", true)
                val donutChartSwitch = SwitchMaterial(this).apply {
                    isChecked = isDonutEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("use_donut_chart", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRowWithView(createCustomDonutIcon(), "Donut Chart Style", "Recommended for 8+ subjects. Interactive 3D slices with quick stats in the center.", donutChartSwitch))
                chartsCard.addView(createDivider())

                val isPatternEnabled = sharedPrefs.getBoolean("show_focus_pattern", true)
                val patternSwitch = SwitchMaterial(this).apply {
                    isChecked = isPatternEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_focus_pattern", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("🕒", getString(R.string.focus_pattern_setting), getString(R.string.focus_pattern_setting_sub), patternSwitch))
                layout.addView(chartsCard)
            }

            // ==========================================
            // 6. CLOUD, SYNC & BACKUPS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.CLOUD) {
                layout.addView(createSectionLabel("CLOUD BACKUP"))
                val cloudCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userEmail = AuthManager.getUserEmail(this) ?: "Not Signed In"

                val authRow = createSettingsRow("☁", "Cloud Account", userEmail)
                authRow.setOnClickListener {
                    if (!isGoogleAuth) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }
                cloudCard.addView(authRow)
                cloudCard.addView(createDivider())

                val syncPushBtn = Button(this).apply {
                    text = "Back Up to Cloud Now"
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
                                        Toast.makeText(this@with, "Cloud backup completed successfully!", Toast.LENGTH_SHORT).show()
                                    } else if (result.isUnauthenticated) {
                                        Toast.makeText(this@with, "Please sign in with Google first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this@with, "Backup failed: ${result.errorMessage ?: "Check your internet connection"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }.start()
                    }
                }
                cloudCard.addView(syncPushBtn)

                val syncPullBtn = Button(this).apply {
                    text = "Restore from Cloud Backup"
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
                                        Toast.makeText(this@with, "Cloud backup restored successfully!", Toast.LENGTH_SHORT).show()
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

                layout.addView(createSectionLabel("LOCAL DATA & BACKUPS"))
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

                layout.addView(createSectionLabel("CARD STYLES"))
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

                val bubbleRow = createSettingsRow("🔮", "3D Soft Depth", "Soft elevated cards with smooth depth", modeRadio(isBubble))
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
                // RANDOM THEME / ACCENT GENERATOR
                // ==========================================
                layout.addView(createSectionLabel("RANDOM ACCENT COLORS"))
                val randomThemeCard = createSettingsCard()
                val rollBtn = TextView(this).apply {
                    text = "Randomize"
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(0xFFFFFFFF.toInt())
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                }
                val randomRow = createSettingsRow(
                    "🎲",
                    "Random Accent Colors",
                    "Generate clean, matching colors for Study & Break",
                    rollBtn
                )
                randomRow.setOnClickListener {
                    val focusList = ThemeCoordinator.SOFT_FOCUS_PALETTE
                    val breakList = ThemeCoordinator.SOFT_BREAK_PALETTE
                    val randomFocusHex = focusList.random()
                    var randomBreakHex = breakList.random()
                    while (randomBreakHex.equals(randomFocusHex, ignoreCase = true) && breakList.size > 1) {
                        randomBreakHex = breakList.random()
                    }

                    val newFocus = Color.parseColor(randomFocusHex)
                    val newBreak = Color.parseColor(randomBreakHex)

                    val hsvFocus = FloatArray(3)
                    val hsvBreak = FloatArray(3)
                    Color.colorToHSV(newFocus, hsvFocus)
                    Color.colorToHSV(newBreak, hsvBreak)

                    sharedPrefs.edit()
                        .putInt("customPrimary", newFocus)
                        .putInt("customSecondary", newBreak)
                        .putInt("customHue", hsvFocus[0].toInt())
                        .putInt("customSecondaryHue", hsvBreak[0].toInt())
                        .apply()

                    themeCoordinator.primaryColor = newFocus
                    themeCoordinator.secondaryColor = newBreak
                    (settingsBackFab.background as? GradientDrawable)?.setColor(newFocus)

                    updateVisualStyles()
                    tabPageCache.clear()

                    try {
                        randomRow.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } catch (_: Exception) {}
                    Toast.makeText(this, "New theme colors applied!", Toast.LENGTH_SHORT).show()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                randomThemeCard.addView(randomRow)
                layout.addView(randomThemeCard)

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

                    val colorHeaderRow = LinearLayout(this).apply {
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
                    colorHeaderRow.addView(previewCircle)

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
                    colorHeaderRow.addView(textCol)
                    cardContainer.addView(colorHeaderRow)

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
                        text = "Fine-Tune Hue Slider"
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

                layout.addView(createSectionLabel("STUDY ACCENT COLOR"))
                val focusColorCard = makeAccentColorSection(
                    title = "Study Color",
                    subtitle = "Colors your study timer ring & primary buttons",
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

                layout.addView(createSectionLabel("BREAK ACCENT COLOR"))
                val breakColorCard = makeAccentColorSection(
                    title = "Break Color",
                    subtitle = "Colors your break timer ring & status badges",
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
                layout.addView(createSectionLabel("ADVANCED TOOLS"))
                val devCard = DeveloperToolsHelper.buildDevCard(host, themeCoordinator)
                layout.addView(devCard)
            }

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
