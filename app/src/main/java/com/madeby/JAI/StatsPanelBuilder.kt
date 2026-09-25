package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.Button
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
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class StatsPanelBuilder(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val statsEngine get() = host.statsEngine
    private val dateKeyFmt get() = host.dateKeyFmt
    private val cachedTodayStr get() = host.cachedTodayStr
    private var statsSnapshotCache
        get() = host.statsSnapshotCache
        set(value) { host.statsSnapshotCache = value }
    private var statsSnapshotGen
        get() = host.statsSnapshotGen
        set(value) { host.statsSnapshotGen = value }
    private var statsDirty
        get() = host.statsDirty
        set(value) { host.statsDirty = value }
    private var currentStatsTab
        get() = host.currentStatsTab
        set(value) { host.currentStatsTab = value }
    private val tabPageCache get() = host.tabPageCache
    private var panelContainer get() = host.panelContainer

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
    private fun navigateToPanel(panel: AppPanel) = host.navigateToPanel(panel)
    private fun showSubjectPickerDialog() = host.showSubjectPickerDialog()
    private fun showAddCustomSubjectDialog(onCreated: ((SubjectTag) -> Unit)? = null) = host.showAddCustomSubjectDialog(onCreated)
    private fun showSummaryCardPreview() = host.showSummaryCardPreview()
    private fun showCustomizeHighlightsDialog() = host.showCustomizeHighlightsDialog()
    private fun showWeeklyTrendDetailDialog(tw: Long, pw: Long, s: StatsSnapshot) = host.showWeeklyTrendDetailDialog(tw, pw, s)
    private fun showActiveDaysDetailDialog(ad: Int, ls: Int, cs: Int, s: StatsSnapshot) = host.showActiveDaysDetailDialog(ad, ls, cs, s)
    private fun showBestDayDetailDialog(l: String, s: Long, sn: StatsSnapshot) = host.showBestDayDetailDialog(l, s, sn)
    private fun showRecordWeekDetailDialog(l: String, s: Long, st: Int, sn: StatsSnapshot) = host.showRecordWeekDetailDialog(l, s, st, sn)
    private fun showGoalSuccessDetailDialog(s: StatsSnapshot) = host.showGoalSuccessDetailDialog(s)
    private fun showAvgSessionDetailDialog(s: StatsSnapshot) = host.showAvgSessionDetailDialog(s)

    fun build(target: android.view.ViewGroup = host.panelContainer) {
        val renderTab = currentStatsTab
        val statsRoot = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val snap = statsSnapshotCache ?: computeStatsSnapshot().also {
            statsSnapshotCache = it
            statsSnapshotGen++
            statsDirty = false
        }

        renderStatsContent(statsRoot, snap, renderTab)
        target.addView(statsRoot)
    }

    private fun buildHeatmapData(): Map<String, Long> = statsEngine.buildHeatmapData()

    internal fun resolveSubjectGoalFor(subjectId: String, dateStr: String): Long {
        // 1. Check historical snapshot for the specific date
        val snapshots = PlannerHistoryManager.loadDaySnapshot(this, dateStr)
        val snapshotSubGoals = snapshots.filter { it.subjectId == subjectId && it.targetMinutes > 0 }
        if (snapshotSubGoals.isNotEmpty()) {
            val totalMins = snapshotSubGoals.sumOf { it.targetMinutes }
            if (totalMins > 0) return totalMins * 60L
        }

        // 2. Check active planner goals
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val activeGoals = loadSessionGoalsFromJson(prefs.host.getString("session_goals_json", "[]") ?: "[]")
        val activeSubGoals = activeGoals.filter { it.subjectId == subjectId && it.targetMinutes > 0 }
        if (activeSubGoals.isNotEmpty()) {
            val totalMins = activeSubGoals.sumOf { it.targetMinutes }
            if (totalMins > 0) return totalMins * 60L
        }

        // 3. Baseline fallback if no subject goal is set in planner: 1 hour 30 minutes (90m = 5400s)
        return 90L * 60L
    }

    private fun buildHeatmapFullscreenPanel() {
        val heatmapData = statsSnapshotCache?.heatmapData ?: buildHeatmapData()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, dp(10)) }
        }
        val headerCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerCol.addView(TextView(host).apply {
            text = host.getString(R.string.heatmap_fullscreen_title)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerCol.addView(TextView(host).apply {
            text = host.getString(R.string.heatmap_hint)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(0, dp(3), 0, 0)
        })
        headerRow.addView(headerCol)
        headerRow.addView(TextView(host).apply {
            text = host.getString(R.string.btn_done)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
        })
        root.addView(headerRow)

        val allSubjects = SubjectTagManager.getAllSubjects(host)
        var activeHeatmapSubjectId: String? = null

        val heatmapView = HeatmapView(host).apply {
            forcedCellSize = dp(32).toFloat()
            setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
            onDayTap = { dateStr ->
                val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                host.showDayDialog(dateStr, lbl)
            }
        }

        val subjectScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val subjectRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }

        fun refreshHeatmapFilterChips() {
            subjectRow.removeAllViews()
            val isAll = activeHeatmapSubjectId == null
            val allChip = TextView(host).apply {
                text = "🌐 All Subjects"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAll) Color.WHITE else themeCoordinator.textColor)
                background = if (isAll) themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
                setOnClickListener {
                    activeHeatmapSubjectId = null
                    refreshHeatmapFilterChips()
                    heatmapView.setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
                }
            }
            subjectRow.addView(allChip)

            for (sub in allSubjects) {
                val isSel = activeHeatmapSubjectId == sub.id
                val subCol = try { Color.parseColor(sub.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                val chip = TextView(host).apply {
                    text = "${sub.iconEmoji} ${sub.name}"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSel) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSel) themeCoordinator.createGlassChip(subCol, 12f) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(6), 0)
                    }
                    setOnClickListener {
                        activeHeatmapSubjectId = sub.id
                        refreshHeatmapFilterChips()
                        val filteredData = SubjectTagManager.getSubjectHeatmapData(host, sub.id)
                        heatmapView.setData(filteredData, subCol, themeCoordinator.textColor, { resolveSubjectGoalFor(sub.id, it) })
                    }
                }
                subjectRow.addView(chip)
            }
        }
        refreshHeatmapFilterChips()
        subjectScroll.addView(subjectRow)
        root.addView(subjectScroll)

        val heatmapScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        heatmapScroll.addView(heatmapView)
        heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
        root.addView(heatmapScroll)

        panelContainer.addView(root)
    }

    internal fun showDeleteGoalDialog(dateStr: String, dateLabel: String) {
        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val focusSecs = sharedPrefs.getLong("day_focus_$dateStr", 0L)
        val dialog = Dialog(host)
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(24f)
        }

        root.addView(TextView(host).apply {
            text = "Delete Goal & Habit History?"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val formattedFocus = if (focusSecs >= 3600) "${focusSecs / 3600}h ${(focusSecs % 3600) / 60}m" else "${(focusSecs % 3600) / 60}m"
        root.addView(TextView(host).apply {
            text = "Date: $dateLabel\nRecorded Focus: $formattedFocus\n\nWould you like to keep or delete this goal history entry?"
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
                sharedPrefs.edit()
                    .remove("day_focus_$dateStr")
                    .remove("daily_goal_sec_$dateStr")
                    .apply()
                TimelineLogger.deleteDay(host, dateStr)
                SubjectTagManager.clearTodaySubjectDurations(host, dateStr)
                statsSnapshotCache = null
                tabPageCache.remove(statsTabKey(AppStatsTab.OVERVIEW))
                tabPageCache.remove(statsTabKey(AppStatsTab.TIMELINE))
                if (currentPanel == AppPanel.STATS || currentPanel == AppPanel.HEATMAP) {
                    navigateToPanel(currentPanel)
                }
                Toast.makeText(host, "Goal history deleted for $dateLabel", Toast.LENGTH_SHORT).show()
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

    private var insightsPillBarRef: InsightsPillNavBar? = null

    internal fun buildStatsTabScrollView(snap: StatsSnapshot, tab: AppStatsTab, todayStr: String): ScrollView {
        val scroll = ScrollView(host).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    val dy = scrollY - oldScrollY
                    if (dy > 12 && scrollY > dp(40)) {
                        insightsPillBarRef?.setBarVisibility(false)
                    } else if (dy < -12 || scrollY <= dp(10)) {
                        insightsPillBarRef?.setBarVisibility(true)
                    }
                }
            }
        }
        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(4), dp(8), dp(4), dp(96)) // 96dp bottom padding so cards never clip under pill
        }
        scroll.addView(content)

        when (tab) {
            AppStatsTab.OVERVIEW -> renderOverviewTabContent(content, snap)
            AppStatsTab.TIMELINE -> CalendarTimeline(this).build(content, snap, todayStr)
            AppStatsTab.PLANNER -> renderPlannerTabContent(content, snap)
        }
        return scroll
    }

    internal fun renderStatsContent(statsRoot: FrameLayout, snap: StatsSnapshot, tab: AppStatsTab = currentStatsTab) {
        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        statsRoot.removeAllViews()
        statsRoot.setBackgroundColor(Color.TRANSPARENT)

        val mainColumn = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        // 1. FIXED TOP HEADER (Title & Quote ONLY - Placed OUTSIDE swipable tab area)
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), dp(4))
        }
        headerRow.addView(TextView(host).apply {
            text = host.getString(R.string.insights_title)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 24f
            letterSpacing = -0.02f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        mainColumn.addView(headerRow)

        val isQuoteDismissed = sharedPrefs.getBoolean("quote_dismissed_today_${todayStr}", false)
        if (!isQuoteDismissed) {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val todayQuote = DailyQuotes.getTodayQuote(dayOfYear)
            val quoteRibbon = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    val cardBg = if (themeCoordinator.isDarkMode()) {
                        if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF161822.toInt()
                    } else {
                        0xFFF1F5F9.toInt()
                    }
                    setColor(cardBg)
                    setStroke(dp(1), tintedColor(themeCoordinator.primaryColor, if (themeCoordinator.isDarkMode()) 50 else 90))
                }
                setPadding(dp(14), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(8))
                }
            }
            quoteRibbon.addView(TextView(host).apply {
                text = "✨"
                textSize = 14f
                setPadding(0, 0, dp(8), 0)
            })
            quoteRibbon.addView(TextView(host).apply {
                text = todayQuote
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 12.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val dismissBtn = TextView(host).apply {
                text = "✕"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 13f
                setPadding(dp(8), dp(4), dp(4), dp(4))
                setOnClickListener {
                    sharedPrefs.edit().putBoolean("quote_dismissed_today_${todayStr}", true).apply()
                    (quoteRibbon.parent as? android.view.ViewGroup)?.removeView(quoteRibbon)
                }
            }
            quoteRibbon.addView(dismissBtn)
            mainColumn.addView(quoteRibbon)
        }

        // 2. ISOLATED TAB CONTENT CONTAINER (Takes only remaining space below header)
        val isSwipeNavEnabled = sharedPrefs.getBoolean("swipe_insights_nav", true)
        val tabHost = object : FrameLayout(host) {
            var onSwipeLeft: (() -> Unit)? = null
            var onSwipeRight: (() -> Unit)? = null
            private var downX = 0f
            private var downY = 0f
            private var isHorizontalSwipe = false

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                if (!isSwipeNavEnabled) return super.onInterceptTouchEvent(ev)
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.x
                        downY = ev.y
                        isHorizontalSwipe = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(ev.x - downX)
                        val dy = Math.abs(ev.y - downY)
                        val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop * 2
                        if (dx > slop && dx > dy * 2f) {
                            isHorizontalSwipe = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                if (!isSwipeNavEnabled) return super.onTouchEvent(ev)
                when (ev.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        if (isHorizontalSwipe) {
                            val diff = ev.x - downX
                            if (diff < -dp(45)) {
                                onSwipeLeft?.invoke()
                                return true
                            } else if (diff > dp(45)) {
                                onSwipeRight?.invoke()
                                return true
                            }
                        }
                    }
                }
                return true
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(Color.TRANSPARENT)
        }
        val tabViews = java.util.EnumMap<AppStatsTab, ScrollView>(AppStatsTab::class.java)
        fun getOrCreateTabView(targetTab: AppStatsTab): ScrollView {
            return tabViews.getOrPut(targetTab) {
                buildStatsTabScrollView(snap, targetTab, todayStr)
            }
        }
        val initialView = getOrCreateTabView(tab)
        hasPlayedStatsEntranceAnimation = true
        tabHost.addView(initialView)
        mainColumn.addView(tabHost)

        statsRoot.addView(mainColumn)

        // 3. FLOATING BOTTOM PILL NAV BAR (Fixed at bottom overlay, Z-Index layer above content)
        val pillBar = InsightsPillNavBar(host).apply {
            applyTheme(themeCoordinator)
            selectTab(currentStatsTab, animated = false)
            setOnTabSelectedListener { targetTab ->
                if (currentStatsTab != targetTab) {
                    currentStatsTab = targetTab
                    val nextTabView = getOrCreateTabView(targetTab)
                    (nextTabView.parent as? android.view.ViewGroup)?.removeView(nextTabView)
                    tabHost.removeAllViews()
                    tabHost.addView(nextTabView)
                }
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                setMargins(dp(24), 0, dp(24), dp(16))
            }
        }

        tabHost.onSwipeLeft = {
            val nextTab = when (currentStatsTab) {
                AppStatsTab.OVERVIEW -> AppStatsTab.TIMELINE
                AppStatsTab.TIMELINE -> AppStatsTab.PLANNER
                AppStatsTab.PLANNER -> null
            }
            if (nextTab != null) {
                currentStatsTab = nextTab
                val nextTabView = getOrCreateTabView(nextTab)
                (nextTabView.parent as? android.view.ViewGroup)?.removeView(nextTabView)
                tabHost.removeAllViews()
                tabHost.addView(nextTabView)
                pillBar.selectTab(nextTab, animated = true)
            }
        }
        tabHost.onSwipeRight = {
            val prevTab = when (currentStatsTab) {
                AppStatsTab.PLANNER -> AppStatsTab.TIMELINE
                AppStatsTab.TIMELINE -> AppStatsTab.OVERVIEW
                AppStatsTab.OVERVIEW -> null
            }
            if (prevTab != null) {
                currentStatsTab = prevTab
                val prevTabView = getOrCreateTabView(prevTab)
                (prevTabView.parent as? android.view.ViewGroup)?.removeView(prevTabView)
                tabHost.removeAllViews()
                tabHost.addView(prevTabView)
                pillBar.selectTab(prevTab, animated = true)
            }
        }

        insightsPillBarRef = pillBar
        statsRoot.addView(pillBar)
    }

    internal fun renderOverviewTabContent(content: LinearLayout, snap: StatsSnapshot) {
        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val todayFocus = snap.todayFocus
        val todayBreak = snap.todayBreak
        val todayH = todayFocus / 3600
        val todayM = (todayFocus % 3600) / 60
        val todayBH = todayBreak / 3600
        val todayBM = (todayBreak % 3600) / 60
        val streak = snap.streak

        val avg7 = snap.avg7
        val avgH = avg7 / 3600
        val avgM = (avg7 % 3600) / 60

        val chartCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val heroCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, dp(8)) }
        }

        val heroGoalSecs = snap.heroGoalSecs
        val heroGoalPctRaw = if (heroGoalSecs > 0) todayFocus.toFloat() / heroGoalSecs.toFloat() * 100f else 0f
        val heroGoalPct = heroGoalPctRaw.coerceIn(0f, 100f)
        val goalReached = todayFocus >= heroGoalSecs && heroGoalSecs > 0
        val goalRingColor = if (goalReached) 0xFF43D36E.toInt() else themeCoordinator.primaryColor

        // Top Header with Label & Streak Chip
        val heroTopRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heroTopRow.addView(TextView(host).apply {
            text = "TODAY'S FOCUS"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        heroTopRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })

        val streakChip = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                val streakBg = if (themeCoordinator.isDarkMode()) {
                    if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF1E212D.toInt()
                } else {
                    0xFFEDF0F5.toInt()
                }
                setColor(streakBg)
                setStroke(dp(1), tintedColor(themeCoordinator.primaryColor, if (themeCoordinator.isDarkMode()) 60 else 90))
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        streakChip.addView(ImageView(host).apply {
            setImageResource(R.drawable.ic_flame)
            setColorFilter(themeCoordinator.primaryColor)
            contentDescription = host.getString(R.string.content_desc_streak)
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
        })
        streakChip.addView(TextView(host).apply {
            text = "${streak}d streak"
            setTextColor(themeCoordinator.textColor)
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(4), 0, 0, 0)
        })
        heroTopRow.addView(streakChip)
        heroCard.addView(heroTopRow)

        // Main Counter paired with Circular Progress Ring
        val heroCenterRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }

        val counterCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val timeRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        timeRow.addView(TextView(host).apply {
            tag = "overview_today_focus_time"
            text = host.getString(R.string.duration_h_m, todayH, todayM)
            setTextColor(themeCoordinator.textColor)
            textSize = 32f
            letterSpacing = -0.03f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val yesterdaySecs = snap.yesterdaySecs
        val trendPct = if (yesterdaySecs > 0L) {
            (((todayFocus - yesterdaySecs).toFloat() / yesterdaySecs.toFloat()) * 100f).toInt()
        } else if (todayFocus > 0L) {
            100
        } else {
            0
        }

        val trendChip = TextView(host).apply {
            tag = "overview_trend_chip"
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(7), dp(2), dp(7), dp(2))
            when {
                yesterdaySecs == 0L && todayFocus == 0L -> {
                    text = "0%"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                }
                trendPct > 0 -> {
                    text = "+${trendPct}%"
                    setTextColor(0xFF43D36E.toInt())
                    background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(0x2243D36E.toInt()) }
                }
                trendPct < 0 -> {
                    text = "${trendPct}%"
                    setTextColor(0xFFFF5252.toInt())
                    background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(0x22FF5252.toInt()) }
                }
                else -> {
                    text = "0%"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                }
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(8), 0, 0, dp(4))
            }
        }
        timeRow.addView(trendChip)
        counterCol.addView(timeRow)

        val targetSubtext = TextView(host).apply {
            tag = "overview_target_subtext"
            text = "Target: ${formatGoalLabel(heroGoalSecs)} · ${(heroGoalPctRaw).toInt()}% completed"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(3), 0, 0)
        }
        counterCol.addView(targetSubtext)
        heroCenterRow.addView(counterCol)

        // Circular Progress Ring
        val ringSize = dp(64)
        val goalRingWrap = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize)
        }
        goalRingWrap.addView(SegmentRing(
            listOf(heroGoalPct / 100f to goalRingColor),
            if (themeCoordinator.isDarkMode()) 0xFF1E212D.toInt() else 0xFFE2E8F0.toInt(),
            dp(7),
            Pair(goalRingColor, lightenColor(goalRingColor, 0.25f)),
            animate = !hasPlayedStatsEntranceAnimation
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        goalRingWrap.addView(TextView(host).apply {
            tag = "overview_ring_center_text"
            text = if (goalReached) "✓" else "${heroGoalPctRaw.toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(goalRingColor)
            textSize = if (goalReached) 18f else 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        heroCenterRow.addView(goalRingWrap)
        heroCard.addView(heroCenterRow)

        // Inline Badges Row: [🎯 Goal Reached!] | [☕ 0m break] | [📊 7D Avg: ...]
        val inlineBadgesRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }

        fun createPillBadge(emoji: String, textStr: String, isGreenAccent: Boolean = false, viewTag: String? = null): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    if (isGreenAccent) {
                        val badgeBg = if (themeCoordinator.isDarkMode()) 0x2443D36E.toInt() else 0x1A43D36E.toInt()
                        setColor(badgeBg)
                        setStroke(dp(1), 0x8843D36E.toInt())
                    } else {
                        val badgeBg = if (themeCoordinator.isDarkMode()) {
                            if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF181A24.toInt()
                        } else {
                            0xFFEDF0F5.toInt()
                        }
                        val strokeCol = if (themeCoordinator.isDarkMode()) 0xFF282A36.toInt() else 0xFFCBD5E1.toInt()
                        setColor(badgeBg)
                        setStroke(dp(1), strokeCol)
                    }
                }
                setPadding(dp(10), dp(5), dp(10), dp(5))
                if (emoji.isNotBlank()) {
                    addView(TextView(host).apply {
                        text = emoji
                        textSize = 11.5f
                        setPadding(0, 0, dp(4), 0)
                    })
                }
                addView(TextView(host).apply {
                    if (viewTag != null) tag = viewTag
                    text = textStr
                    setTextColor(if (isGreenAccent) 0xFF43D36E.toInt() else themeCoordinator.textColor)
                    alpha = if (isGreenAccent) 1f else 0.9f
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
        }

        val remainingSecs = (heroGoalSecs - todayFocus).coerceAtLeast(0L)
        val remainingLabel = if (goalReached) "Goal Reached!" else "${formatGoalLabel(remainingSecs)} left"
        val breakLabel = if (todayBH > 0) "${todayBH}h ${todayBM}m break" else "${todayBM}m break"
        inlineBadgesRow.addView(createPillBadge("", remainingLabel, isGreenAccent = goalReached, viewTag = "overview_remaining_badge"))
        inlineBadgesRow.addView(createPillBadge("", breakLabel, viewTag = "overview_break_badge"))
        inlineBadgesRow.addView(createPillBadge("", "${avgH}h ${avgM}m 7d avg"))
        heroCard.addView(inlineBadgesRow)

        val chipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val filterLabels = listOf(host.getString(R.string.filter_7d), host.getString(R.string.filter_30d), host.getString(R.string.filter_all))
        val filterValues = listOf(7, 30, -1)
        for (idx in filterLabels.indices) {
            val isActive = selectedDaysFilter == filterValues[idx]
            chipRow.addView(TextView(host).apply {
                text = filterLabels[idx]
                textSize = 12.5f
                setPadding(dp(14), dp(6), dp(14), dp(6))
                setTextColor(if (isActive) themeCoordinator.primaryColor else themeCoordinator.textColor)
                alpha = if (isActive) 1f else 0.65f
                typeface = Typeface.create("sans-serif-medium", if (isActive) Typeface.BOLD else Typeface.NORMAL)
                background = if (isActive) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f) else null
                setOnClickListener {
                    selectedDaysFilter = filterValues[idx]
                    sharedPrefs.edit().putInt("selected_days_filter", selectedDaysFilter).apply()
                    navigateToPanel(AppPanel.STATS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(6), 0) }
            })
        }
        chartCard.addView(chipRow)

        chartCard.addView(TextView(host).apply {
            text = host.getString(R.string.goal_mark, formatGoalLabel(dailyGoalSecs()))
            setTextColor(themeCoordinator.textColor)
            alpha = 0.5f
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(4), 0, dp(4), dp(8))
        })

        val chartContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        chartCard.addView(chartContainer)

        fun buildChart(daysLimit: Int) {
            chartContainer.removeAllViews()
            var maxDayFocusFound = 1L

            if (daysLimit == 7) {
                val displaySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val cal = Calendar.getInstance()
                val mondayOffset = WeekHelper.mondayOffset(cal)
                val weekData = ArrayList<Pair<String, String>>()

                for (i in 0..6) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -mondayOffset + i)
                    val dStr = sdf.format(c.time)
                    weekData.add(Pair(dStr, "${weekdayNames[i]} ${displaySdf.format(c.time)}"))
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L

                for ((dStr, label) in weekData) {
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val dayGoalSecs = resolveGoalFor(dStr)
                    val dayGoalRatio = (dayGoalSecs.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
                    val row = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(host).apply {
                        text = label
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = dayGoalRatio,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = isToday,
                        barHeight = dp(16)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(16), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(host).apply {
                        text = host.getString(R.string.duration_h_m, f / 3600, (f % 3600) / 60)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, label) }
                    chartContainer.addView(row)
                }
            } else if (daysLimit == 30) {
                val labelSdf = SimpleDateFormat("d MMM", Locale.getDefault())
                for (i in 0 until 30) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L

                for (i in 29 downTo 0) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val dayGoalSecs = resolveGoalFor(dStr)
                    val dayGoalRatio = (dayGoalSecs.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
                    val row = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(2)) }
                    row.addView(TextView(host).apply {
                        text = labelSdf.format(c.time)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(55), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = dayGoalRatio,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = isToday,
                        barHeight = dp(13)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(13), 1f).apply { setMargins(dp(4), 0, dp(6), 0) } })
                    row.addView(TextView(host).apply {
                        text = host.getString(R.string.duration_h_m, f / 3600, (f % 3600) / 60)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, labelSdf.format(c.time)) }
                    chartContainer.addView(row)
                }
            } else {
                var maxM = 1L
                for (mb in snap.monthBuckets) if (mb.focus > maxM) maxM = mb.focus
                val scale = if (maxM <= 0L) 3600L else Math.ceil(maxM.toDouble() / 3600.0).toLong() * 3600L

                for (mb in snap.monthBuckets) {
                    val mSecs = mb.focus
                    val row = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(host).apply { text = mb.label; setTextColor(themeCoordinator.textColor); textSize = 13f; layoutParams = LinearLayout.LayoutParams(dp(75), LinearLayout.LayoutParams.WRAP_CONTENT) })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) mSecs.toFloat() / scale.toFloat() else 0f,
                        goalRatio = -1f,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = false,
                        barHeight = dp(14)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(14), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(host).apply { text = host.getString(R.string.duration_h_m, mSecs / 3600, (mSecs % 3600) / 60); setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.MONOSPACE; alpha = 0.85f })
                    row.setOnClickListener { showMonthDialog(mb.label, mSecs, mb.breakSecs) }
                    chartContainer.addView(row)
                }
            }
        }

        buildChart(selectedDaysFilter)

        // DUAL-MODE PIE CHART CARD (Placed strictly BELOW Focus Pattern Chart)
        val pieCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
        }

        val pieHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        pieHeaderRow.addView(TextView(host).apply {
            text = "SUBJECT BREAKDOWN"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val tapDetailsBtn = TextView(host).apply {
            text = "View Details ›"
            setTextColor(if (themeCoordinator.isDarkMode()) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = if (themeCoordinator.isDarkMode()) {
                themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 14f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setOnClickListener { showPieChartDetailsModal() }
        }

        pieHeaderRow.addView(tapDetailsBtn)
        pieCard.addView(pieHeaderRow)

        val pieModeRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }

        var currentPieMode = sharedPrefs.safeInt("pie_chart_mode", 0) // 0: Subject Sessions, 1: Focus Depth & Quality Ratio

        fun updatePieChartContent(mode: Int, container: LinearLayout) {
            container.removeAllViews()
            val isDonut = sharedPrefs.safeBoolean("use_donut_chart", true)
            val pieView = SubjectPieChartView(host).apply {
                primaryColor = themeCoordinator.primaryColor
                textColor = themeCoordinator.textColor
                isDonutMode = isDonut
                boxColor = if (themeCoordinator.isDarkMode()) (if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF111625.toInt()) else 0xFFFFFFFF.toInt()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340))
                setOnClickListener { showPieChartDetailsModal() }
            }

            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()

            if (mode == 0) {
                // Mode 1: Subject Sessions Breakdown derived from unified daily session logs
                val todayStr = dateKeyFmt.format(Date())
                val (allDaySessions, _) = dayBlocks(todayStr)

                // Group unified sessions by subject (aggregating duration + resolved SubjectTag)
                val subjectMap = LinkedHashMap<String, Pair<SubjectTag, Long>>()
                for (s in allDaySessions) {
                    val subId = s.subjectId ?: "general"
                    val subj = SubjectTagManager.resolveSubject(host, subId, s.subjectName, s.subjectColor)
                    val currentSecs = subjectMap[subj.id]?.second ?: 0L
                    subjectMap[subj.id] = subj to (currentSecs + s.secs)
                }

                // If no unified timeline entries exist yet for today, fallback to SubjectTagManager durations for today specifically
                if (subjectMap.isEmpty()) {
                    val legacyDurations = SubjectTagManager.getSubjectDurationsForDate(host, todayStr)
                    for ((subId, secs) in legacyDurations) {
                        val subj = SubjectTagManager.resolveSubject(host, subId)
                        val currentSecs = subjectMap[subj.id]?.second ?: 0L
                        subjectMap[subj.id] = subj to (currentSecs + secs)
                    }
                }

                val totalSecsAll = subjectMap.values.sumOf { it.second }

                if (subjectMap.isEmpty() || totalSecsAll < 60L) {
                    val emptyBox = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    }
                    emptyBox.addView(TextView(host).apply {
                        text = "No Study Time Today"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                    })
                    emptyBox.addView(TextView(host).apply {
                        text = "Study for at least 1 minute to see your subject breakdown."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 12.5f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                    container.addView(emptyBox)
                } else {
                    val sortedSubjects = subjectMap.values
                        .filter { it.second >= 60L }
                        .sortedByDescending { it.second }

                    for ((subj, secs) in sortedSubjects) {
                        slicesList.add(SubjectPieChartView.PieSlice(subj.name, subj.iconEmoji, secs.toDouble(), subj.colorHex))
                    }

                    if (slicesList.isEmpty()) {
                        val emptyBox = LinearLayout(host).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            setPadding(0, dp(24), 0, dp(24))
                        }
                        emptyBox.addView(TextView(host).apply {
                            text = "More Data Needed"
                            setTextColor(themeCoordinator.textColor)
                            textSize = 15f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            gravity = Gravity.CENTER
                        })
                        container.addView(emptyBox)
                    } else {
                        pieView.setData(slicesList)
                        container.addView(pieView)
                    }
                }
            } else {
                // Mode 2: Session Quality & Focus Depth Ratio (Deep Focus vs Standard Focus vs Light Focus)
                val totalFocusSecs = snap.todayFocus
                if (totalFocusSecs < 60L) {
                    val emptyBox = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    }
                    emptyBox.addView(TextView(host).apply {
                        text = "Not Enough Data"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                    })
                    emptyBox.addView(TextView(host).apply {
                        text = "Study for at least 1 minute to analyze your focus depth."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 12.5f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                    container.addView(emptyBox)
                } else {
                    // Compute Focus Depth Distribution strictly from real session block durations
                    val todayStr = dateKeyFmt.format(Date())
                    val (daySessions, _) = statsEngine.dayBlocks(todayStr)
                    var deepSecs = 0L
                    var stdSecs = 0L
                    var lightSecs = 0L

                    for (b in daySessions) {
                        val durationSecs = b.secs
                        when {
                            durationSecs >= 2400L -> deepSecs += durationSecs // >= 40 mins (Deep Focus)
                            durationSecs >= 900L -> stdSecs += durationSecs   // 15 to 40 mins (Standard Focus)
                            else -> lightSecs += durationSecs                 // < 15 mins (Light Focus)
                        }
                    }

                    val isCurrentlyStudying = (currentTimerState == TimerState.STUDYING)
                    if (isCurrentlyStudying && accumulatedStudy > 0L) {
                        when {
                            accumulatedStudy >= 2400L -> deepSecs += accumulatedStudy
                            accumulatedStudy >= 900L -> stdSecs += accumulatedStudy
                            else -> lightSecs += accumulatedStudy
                        }
                    }

                    val sumCalc = deepSecs + stdSecs + lightSecs
                    if (sumCalc < totalFocusSecs && totalFocusSecs > 0L) {
                        val diff = totalFocusSecs - sumCalc
                        if (sumCalc > 0L) {
                            deepSecs += (diff * (deepSecs.toDouble() / sumCalc)).toLong()
                            stdSecs += (diff * (stdSecs.toDouble() / sumCalc)).toLong()
                            lightSecs = max(0L, totalFocusSecs - deepSecs - stdSecs)
                        } else {
                            if (totalFocusSecs >= 2400L) deepSecs = totalFocusSecs
                            else if (totalFocusSecs >= 900L) stdSecs = totalFocusSecs
                            else lightSecs = totalFocusSecs
                        }
                    }

                    if (deepSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Deep Focus (≥40m)", "🔥", deepSecs.toDouble(), "#10B981"))
                    }
                    if (stdSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Standard Focus (15-40m)", "⚡", stdSecs.toDouble(), "#3B82F6"))
                    }
                    if (lightSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Light Focus (<15m)", "☕", lightSecs.toDouble(), "#F59E0B"))
                    }

                    pieView.setData(slicesList)
                    container.addView(pieView)
                }
            }
        }

        val pieContentContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

        val m1Btn = TextView(host).apply {
            text = "Subject Breakdown"
            textSize = 12f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val m2Btn = TextView(host).apply {
            text = "Focus Depth"
            textSize = 12f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        fun refreshModeButtons() {
            val isDark = themeCoordinator.isDarkMode()
            val activeBg = if (isDark) {
                themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 100), 14f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            m1Btn.setTextColor(if (currentPieMode == 0) (if (isDark) themeCoordinator.primaryColor else 0xFF0F172A.toInt()) else (if (isDark) themeCoordinator.textColor else 0xFF64748B.toInt()))
            m1Btn.alpha = if (currentPieMode == 0) 1f else 0.6f
            m1Btn.background = if (currentPieMode == 0) activeBg else null

            m2Btn.setTextColor(if (currentPieMode == 1) (if (isDark) themeCoordinator.primaryColor else 0xFF0F172A.toInt()) else (if (isDark) themeCoordinator.textColor else 0xFF64748B.toInt()))
            m2Btn.alpha = if (currentPieMode == 1) 1f else 0.6f
            m2Btn.background = if (currentPieMode == 1) activeBg else null
        }

        m1Btn.setOnClickListener {
            currentPieMode = 0
            sharedPrefs.edit().putInt("pie_chart_mode", 0).apply()
            refreshModeButtons()
            updatePieChartContent(0, pieContentContainer)
        }

        m2Btn.setOnClickListener {
            currentPieMode = 1
            sharedPrefs.edit().putInt("pie_chart_mode", 1).apply()
            refreshModeButtons()
            updatePieChartContent(1, pieContentContainer)
        }

        refreshModeButtons()
        pieModeRow.addView(m1Btn)
        pieModeRow.addView(m2Btn)
        pieCard.addView(pieModeRow)
        pieCard.addView(pieContentContainer)
        updatePieChartContent(currentPieMode, pieContentContainer)
        val totalLifeFocus = snap.totalLifeFocus
        val totalLifeBreak = snap.totalLifeBreak
        val totalLife = snap.totalLife

        val lifetimeCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val lifetimeTopRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lifetimeLeftCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val lifetimeFocusLabel = TextView(host).apply { text = host.getString(R.string.lifetime_focus); setTextColor(themeCoordinator.primaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeFocusValue = TextView(host).apply { setTextColor(themeCoordinator.primaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, dp(10)) }
        val lifetimeBreakLabel = TextView(host).apply { text = host.getString(R.string.lifetime_breaks); setTextColor(themeCoordinator.secondaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeBreakValue = TextView(host).apply { setTextColor(themeCoordinator.secondaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, 0) }
        lifetimeFocusValue.text = host.getString(R.string.duration_h_m, totalLifeFocus / 3600, (totalLifeFocus % 3600) / 60)
        lifetimeBreakValue.text = host.getString(R.string.duration_h_m, totalLifeBreak / 3600, (totalLifeBreak % 3600) / 60)
        lifetimeLeftCol.addView(lifetimeFocusLabel)
        lifetimeLeftCol.addView(lifetimeFocusValue)
        lifetimeLeftCol.addView(lifetimeBreakLabel)
        lifetimeLeftCol.addView(lifetimeBreakValue)
        lifetimeTopRow.addView(lifetimeLeftCol)

        val focusFrac = if (totalLife > 0L) totalLifeFocus.toFloat() / totalLife.toFloat() else 0f
        val breakFrac = if (totalLife > 0L) totalLifeBreak.toFloat() / totalLife.toFloat() else 0f
        val lifetimeDonutWrap = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }
        lifetimeDonutWrap.addView(SegmentRing(
            listOf(focusFrac to themeCoordinator.primaryColor, breakFrac to themeCoordinator.secondaryColor),
            themeCoordinator.bgColor,
            dp(10),
            null
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        lifetimeDonutWrap.addView(TextView(host).apply {
            text = "${(focusFrac * 100f).toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        lifetimeTopRow.addView(lifetimeDonutWrap)
        lifetimeCard.addView(lifetimeTopRow)

        val longestStreak = snap.longestStreak
        val activeDays = snap.activeDays
        val bestWeekdaySecs = snap.bestWeekdaySecs
        val bestWeekdayName = snap.bestWeekdayName
        val bestWeekSecs = snap.bestWeekSecs
        val bestWeekLabel = snap.bestWeekLabel

        val thisWeek = snap.thisWeek
        val prevWeek = snap.prevWeek

        val goalHits = snap.goalHits

        // Pinned Highlights Metric Grid
        val highlightItems = ArrayList<View>()

        fun createGridMetricCard(
            iconRes: Int,
            color: Int,
            tag: String,
            value: String,
            subtext: String,
            badgeText: String? = null,
            badgeColor: Int = 0xFFF59E0B.toInt(),
            onTap: (() -> Unit)? = null
        ): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(18f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (onTap != null) {
                    setOnClickListener { onTap() }
                }

                val topRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val iconBox = FrameLayout(host).apply {
                    background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(tintedColor(color, 35)) }
                    layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
                }
                iconBox.addView(ImageView(host).apply {
                    setImageResource(iconRes)
                    setColorFilter(color)
                    layoutParams = FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER)
                })
                topRow.addView(iconBox)

                topRow.addView(TextView(host).apply {
                    text = tag
                    setTextColor(color)
                    textSize = 10f
                    letterSpacing = 0.12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(8), 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                if (!badgeText.isNullOrBlank()) {
                    topRow.addView(TextView(host).apply {
                        text = badgeText
                        setTextColor(badgeColor)
                        textSize = 9.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(badgeColor, 40), 6f)
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                    })
                }

                addView(topRow)

                addView(TextView(host).apply {
                    text = value
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(8), 0, 0)
                })

                if (subtext.isNotBlank()) {
                    addView(TextView(host).apply {
                        text = subtext
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 11f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setPadding(0, dp(2), 0, 0)
                    })
                }
            }
        }

        val allAvailableKeys = listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK", "GOAL_SUCCESS", "AVG_SESSION")
        val pinnedPrefsJson = sharedPrefs.host.getString("pinned_highlights_order", null)
        val pinnedKeys = if (!pinnedPrefsJson.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(pinnedPrefsJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val k = arr.host.getString(i)
                    if (allAvailableKeys.contains(k) && !list.contains(k)) list.add(k)
                }
                if (list.size >= 2) list else listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK")
            } catch (_: Exception) {
                listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK")
            }
        } else {
            listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK")
        }

        for (k in pinnedKeys) {
            when (k) {
                "WEEKLY_TREND" -> {
                    val trendDiff = if (prevWeek > 0L) ((thisWeek - prevWeek).toFloat() / prevWeek.toFloat() * 100f).toInt() else if (thisWeek > 0L) 100 else 0
                    val trendVal = when {
                        trendDiff > 0 -> "+${trendDiff}% vs last week"
                        trendDiff < 0 -> "${trendDiff}% vs last week"
                        else -> "Same as last week"
                    }
                    val isRecord = thisWeek > 0L && thisWeek >= bestWeekSecs && bestWeekSecs > 0L
                    val badge = if (isRecord) "Personal Best" else null
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_trending, themeCoordinator.primaryColor,
                        "WEEKLY TREND", trendVal, "${thisWeek / 3600}h ${(thisWeek % 3600) / 60}m studied",
                        badgeText = badge,
                        badgeColor = 0xFFF59E0B.toInt(),
                        onTap = { showWeeklyTrendDetailDialog(thisWeek, prevWeek, snap) }
                    ))
                }
                "ACTIVE_DAYS" -> {
                    val streakVal = snap.streak
                    val streakMilestone = when {
                        streakVal >= 100 -> "100d Club"
                        streakVal >= 30 -> "30d Streak"
                        streakVal >= 14 -> "14d Streak"
                        streakVal >= 7 -> "7d Streak"
                        streakVal >= 3 -> "3d Streak"
                        else -> null
                    }
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_book, themeCoordinator.secondaryColor,
                        "ACTIVE DAYS", "$activeDays day${if (activeDays == 1) "" else "s"}", "Days with study sessions",
                        badgeText = streakMilestone,
                        badgeColor = 0xFF10B981.toInt(),
                        onTap = { showActiveDaysDetailDialog(activeDays, longestStreak, streakVal, snap) }
                    ))
                }
                "BEST_DAY" -> {
                    val bestDayVal = if (snap.bestDaySecs > 0L) snap.bestDayLabel else "No best day yet"
                    val bestDaySub = if (snap.bestDaySecs > 0L) "${snap.bestDaySecs / 3600}h ${(snap.bestDaySecs % 3600) / 60}m studied" else "Keep studying"
                    val powerDay = if (snap.bestDaySecs >= 3600L * 3) "Power Day" else null
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_trending, themeCoordinator.primaryColor,
                        "BEST DAY", bestDayVal, bestDaySub,
                        badgeText = powerDay,
                        badgeColor = 0xFF3B82F6.toInt(),
                        onTap = { showBestDayDetailDialog(snap.bestDayLabel, snap.bestDaySecs, snap) }
                    ))
                }
                "RECORD_WEEK" -> {
                    val recordVal = if (bestWeekSecs > 0L) "Week of $bestWeekLabel" else "No best week yet"
                    val recordSub = if (bestWeekSecs > 0L) "${bestWeekSecs / 3600}h ${(bestWeekSecs % 3600) / 60}m" else "Keep studying"
                    val isNewBest = thisWeek > 0L && thisWeek == bestWeekSecs
                    val badge = if (isNewBest) "New Record!" else null
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_medal, themeCoordinator.secondaryColor,
                        "BEST WEEK", recordVal, recordSub,
                        badgeText = badge,
                        badgeColor = 0xFFF59E0B.toInt(),
                        onTap = { showRecordWeekDetailDialog(bestWeekLabel, bestWeekSecs, longestStreak, snap) }
                    ))
                }
                "GOAL_SUCCESS" -> {
                    val rateText = if (goalHits > 0) "$goalHits days hit" else "No goals hit"
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_check_circle, themeCoordinator.primaryColor,
                        "GOAL SUCCESS", rateText, "Daily target reached",
                        badgeText = if (goalHits >= 10) "High Focus" else null,
                        badgeColor = 0xFF10B981.toInt(),
                        onTap = { showGoalSuccessDetailDialog(snap) }
                    ))
                }
                "AVG_SESSION" -> {
                    var totalSessionCount = 0
                    var totalDuration = 0L
                    val entries = TimelineLogger.load(host)
                    val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val allDates = entries.mapNotNull { if (it.timestamp > 0L) dateSdf.format(Date(it.timestamp)) else null }.distinct()
                    for (dStr in allDates) {
                        val (sessions, _) = dayBlocks(dStr)
                        for (s in sessions) {
                            if (s.secs >= 60L) {
                                totalSessionCount++
                                totalDuration += s.secs
                            }
                        }
                    }
                    val avgMins = if (totalSessionCount > 0) ((totalDuration / totalSessionCount) / 60).toInt() else 0
                    val avgVal = if (avgMins > 0) "${avgMins}m average" else "No sessions"
                    highlightItems.add(createGridMetricCard(
                        R.drawable.ic_clock, themeCoordinator.secondaryColor,
                        "AVG SESSION", avgVal, if (totalSessionCount > 0) "$totalSessionCount total sessions" else "Start a session",
                        badgeText = if (avgMins >= 45) "Deep Work" else null,
                        badgeColor = 0xFFA78BFA.toInt(),
                        onTap = { showAvgSessionDetailDialog(snap) }
                    ))
                }
            }
        }

        val highlightsGrid = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Add items as 2-column rows
        for (i in 0 until highlightItems.size step 2) {
            val gridRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(8))
                }
            }
            gridRow.addView(highlightItems[i])
            if (i + 1 < highlightItems.size) {
                val spacer = View(host).apply { layoutParams = LinearLayout.LayoutParams(dp(8), dp(1)) }
                gridRow.addView(spacer)
                gridRow.addView(highlightItems[i + 1])
            } else {
                // If odd number, pad with invisible spacer matching 1f weight
                val emptySpacer = View(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply { setMargins(dp(8), 0, 0, 0) } }
                gridRow.addView(emptySpacer)
            }
            highlightsGrid.addView(gridRow)
        }

        val hasAnySessions = snap.hasAnySessions
        if (!hasAnySessions) {
                content.addView(LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(28), dp(20), dp(28))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
                    addView(TextView(host).apply {
                        text = "\uD83C\uDFA7\uFE0F"
                        textSize = 36f
                        gravity = Gravity.CENTER
                    })
                    addView(TextView(host).apply {
                        text = host.getString(R.string.no_sessions_yet)
                        setTextColor(themeCoordinator.textColor)
                        textSize = 16f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(10), 0, 0)
                    })
                    addView(TextView(host).apply {
                        text = host.getString(R.string.insights_empty_hint)
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                })
            } else {
            (heroCard.parent as? android.view.ViewGroup)?.removeView(heroCard)
            (chartCard.parent as? android.view.ViewGroup)?.removeView(chartCard)
            content.addView(heroCard)
            content.addView(chartCard)
            buildChart(selectedDaysFilter)

            val showHeatmap = snap.showHeatmap

            if (showHeatmap) {
                val heatmapData = snap.heatmapData

                val heatmapCard = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val heatmapHeaderRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val heatmapHeaderCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                heatmapHeaderCol.addView(TextView(host).apply {
                    text = host.getString(R.string.focus_heatmap)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                heatmapHeaderCol.addView(TextView(host).apply {
                    text = host.getString(R.string.heatmap_hint)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    setPadding(0, dp(2), 0, 0)
                })
                heatmapHeaderRow.addView(heatmapHeaderCol)
                heatmapHeaderRow.addView(TextView(host).apply {
                    text = host.getString(R.string.btn_fullscreen)
                    setTextColor(if (themeCoordinator.isDarkMode()) themeCoordinator.primaryColor else 0xFF0F172A.toInt())
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18).toFloat()
                        if (themeCoordinator.isDarkMode()) {
                            setStroke(dp(1).toInt(), tintedColor(themeCoordinator.primaryColor, 180))
                            setColor(tintedColor(themeCoordinator.primaryColor, 25))
                        } else {
                            setStroke(dp(1).toInt(), 0xFFCBD5E1.toInt())
                            setColor(0xFFF1F5F9.toInt())
                        }
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener { navigateToPanel(AppPanel.HEATMAP) }
                })
                heatmapCard.addView(heatmapHeaderRow)
                val heatmapScroll = HorizontalScrollView(host).apply {
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isFillViewport = true
                }
                heatmapScroll.addView(HeatmapView(host).apply {
                    setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
                    onDayTap = { dateStr ->
                        val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                        val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                        host.showDayDialog(dateStr, lbl)
                    }
                })
                heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
                heatmapCard.addView(heatmapScroll)

                val legendRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, 0)
                }
                legendRow.addView(TextView(host).apply { text = host.getString(R.string.legend_less); setTextColor(themeCoordinator.textColor); alpha = 0.6f; textSize = 10f })
                
                legendRow.addView(View(host).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { setMargins(dp(4), 0, dp(4), 0) }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(3).toFloat()
                        if (themeCoordinator.isDarkMode()) {
                            setColor(Color.argb(20, 255, 255, 255))
                        } else {
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                })

                fun addLegendLevel(alphaDark: Int, alphaLight: Int) {
                    legendRow.addView(View(host).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { setMargins(dp(4), 0, dp(4), 0) }
                        val a = if (themeCoordinator.isDarkMode()) alphaDark else alphaLight
                        background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.argb(a, Color.red(themeCoordinator.primaryColor), Color.green(themeCoordinator.primaryColor), Color.blue(themeCoordinator.primaryColor))) }
                    })
                }
                addLegendLevel(35, 75); addLegendLevel(75, 135); addLegendLevel(135, 195); addLegendLevel(200, 255)
                legendRow.addView(TextView(host).apply { text = host.getString(R.string.legend_more); setTextColor(themeCoordinator.textColor); alpha = 0.6f; textSize = 10f })
                heatmapCard.addView(legendRow)

                content.addView(heatmapCard)
            }

            var using7 = true

            val patternCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
            }
            val patternHeader = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            patternHeader.addView(TextView(host).apply {
                text = host.getString(R.string.focus_pattern)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val segWrap = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(tintedColor(themeCoordinator.textColor, 16)) }
                setPadding(dp(3), dp(2), dp(3), dp(2))
            }
            val seg7 = TextView(host).apply {
                text = host.getString(R.string.week_7d)
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            val seg30 = TextView(host).apply {
                text = host.getString(R.string.week_30d)
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            fun styleSeg(btn: TextView, selected: Boolean) {
                val isDark = themeCoordinator.isDarkMode()
                btn.setTextColor(if (selected) (if (isDark) themeCoordinator.bgColor else 0xFFFFFFFF.toInt()) else (if (isDark) themeCoordinator.textColor else 0xFF64748B.toInt()))
                btn.alpha = if (selected) 1f else 0.7f
                btn.background = if (selected) GradientDrawable().apply {
                    cornerRadius = dp(11).toFloat()
                    setColor(if (isDark) themeCoordinator.primaryColor else 0xFF0F172A.toInt())
                } else null
            }
            segWrap.addView(seg7); segWrap.addView(seg30)
            patternHeader.addView(segWrap)

            val hidePatternBtn = TextView(host).apply {
                text = "✕"
                textSize = 13f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                setPadding(dp(10), dp(4), dp(4), dp(4))
                setOnClickListener {
                    sharedPrefs.edit().putBoolean("show_focus_pattern", false).apply()
                    (patternCard.parent as? android.view.ViewGroup)?.removeView(patternCard)
                    Toast.makeText(host, "Daily Study Rhythm hidden (re-enable in Settings)", Toast.LENGTH_SHORT).show()
                }
            }
            patternHeader.addView(hidePatternBtn)
            patternCard.addView(patternHeader)

            val blockLabels = focusBlockLabels()
            val startLabels = focusBlockStartLabels()
            val cellsRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
            val cellsScroll = HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            cellsScroll.addView(cellsRow)
            patternCard.addView(cellsScroll)

            val footerText = TextView(host).apply {
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            }
            patternCard.addView(footerText)

            fun showBlockDialog(b: Int, secs: Long, total: Long, winLabel: String) {
                val pct = if (total > 0L) (secs * 100.0 / total) else 0.0
                val dialog = android.app.Dialog(host)
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val dialogContent = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(28f)
                    setPadding(dp(20), dp(20), dp(20), dp(18))
                }
                dialogContent.addView(TextView(host).apply {
                    text = focusBlockRangeLabel(b)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                dialogContent.addView(TextView(host).apply {
                    text = host.getString(R.string.block_focus_total, formatDuration(secs))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(8), 0, 0)
                })
                dialogContent.addView(TextView(host).apply {
                    text = host.getString(R.string.block_fraction, String.format(java.util.Locale.getDefault(), "%.0f%%", pct), winLabel)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 12f
                    setPadding(0, dp(4), 0, 0)
                })
                dialogContent.addView(TextView(host).apply {
                    text = host.getString(R.string.btn_close)
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
                    setOnClickListener { dialog.dismiss() }
                })
                dialog.setContentView(dialogContent)
                dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.show()
            }

            fun rebuildCells() {
                val blockSecs = if (using7) snap.blockSecs7 else snap.blockSecs30
                val maxBlock = if (using7) snap.maxBlock7 else snap.maxBlock30
                val total = if (using7) snap.patternTotal7 else snap.patternTotal30
                val winLabel = if (using7) "7-day" else "30-day"
                cellsRow.removeAllViews()
                for (b in 0 until 12) {
                    val v = blockSecs[b]
                    val intensity = if (maxBlock > 0L) v.toFloat() / maxBlock.toFloat() else 0f
                    val col = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(8), 0, 0)
                    }
                    val tile = FrameLayout(host).apply {
                        background = if (v > 0L) GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            val isDark = themeCoordinator.isDarkMode()
                            val alpha = if (isDark) (40 + (185 * intensity).toInt()) else (75 + (180 * intensity).toInt())
                            setColor(tintedColor(themeCoordinator.primaryColor, alpha))
                        } else GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            if (themeCoordinator.isDarkMode()) {
                                setColor(tintedColor(themeCoordinator.textColor, 16))
                            } else {
                                setColor(0xFFF1F5F9.toInt())
                                setStroke(dp(1), 0xFFE2E8F0.toInt())
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                        setOnClickListener {
                            showBlockDialog(b, v, total, winLabel)
                        }
                    }
                    col.addView(tile)
                    col.addView(TextView(host).apply {
                        text = startLabels[b]
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.45f
                        textSize = 8f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                        setPadding(0, dp(4), 0, 0)
                    })
                    col.addView(TextView(host).apply {
                        text = if (v > 0L && total > 0L) "${(v * 100.0 / total).toInt()}%" else ""
                        setTextColor(themeCoordinator.primaryColor)
                        alpha = if (v > 0L) 0.85f else 0.3f
                        textSize = 9f
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    col.setOnClickListener {
                        showBlockDialog(b, v, total, winLabel)
                    }
                    cellsRow.addView(col)
                }
                if (total > 0L) {
                    var maxBlockVal = 0L
                    for (b in 0 until 12) if (blockSecs[b] > maxBlockVal) maxBlockVal = blockSecs[b]

                    val peakBlocks = mutableListOf<Int>()
                    if (maxBlockVal > 0L) {
                        for (b in 0 until 12) {
                            if (blockSecs[b] >= (maxBlockVal * 0.70f).toLong()) {
                                peakBlocks.add(b)
                            }
                        }
                    }

                    val ranges = mutableListOf<String>()
                    var idx = 0
                    while (idx < peakBlocks.size) {
                        val start = peakBlocks[idx]
                        var end = start
                        while (idx + 1 < peakBlocks.size && peakBlocks[idx + 1] == end + 1) {
                            end = peakBlocks[idx + 1]
                            idx++
                        }
                        val startLabel = blockLabels[start].split("-", "\u2013").first().trim()
                        val endLabel = blockLabels[end].split("-", "\u2013").last().trim()
                        ranges.add("$startLabel \u2013 $endLabel")
                        idx++
                    }

                    val rangeStr = if (ranges.isNotEmpty()) ranges.joinToString(" & ") else blockLabels[0].replace("-", " \u2013 ")
                    footerText.text = host.getString(R.string.most_focused, rangeStr)
                    footerText.setAlpha(0.7f)
                    cellsScroll.post {
                        val colW = dp(42)
                        val firstPeak = peakBlocks.firstOrNull() ?: 0
                        val target = (firstPeak * colW - (cellsScroll.width - colW) / 2).coerceAtLeast(0)
                        cellsScroll.smoothScrollTo(target, 0)
                    }
                } else {
                    footerText.text = host.getString(R.string.no_focus_recent, if (using7) "7" else "30")
                    footerText.setAlpha(0.5f)
                }
            }

            styleSeg(seg7, true); styleSeg(seg30, false)
            seg7.setOnClickListener {
                using7 = true
                styleSeg(seg7, true); styleSeg(seg30, false)
                rebuildCells()
            }
            seg30.setOnClickListener {
                using7 = false
                styleSeg(seg7, false); styleSeg(seg30, true)
                rebuildCells()
            }
            rebuildCells()
            if (snap.showPattern) {
                (patternCard.parent as? android.view.ViewGroup)?.removeView(patternCard)
                content.addView(patternCard)
            }

            val shouldShowPieCard = snap.showPieChart

            if (shouldShowPieCard) {
                (pieCard.parent as? android.view.ViewGroup)?.removeView(pieCard)
                content.addView(pieCard)
            }
            }

            content.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)) })
            val highlightsHeaderRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(6))
            }
            highlightsHeaderRow.addView(TextView(host).apply {
                text = "HIGHLIGHTS"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val customizeBtn = TextView(host).apply {
                text = "Customize ›"
                setTextColor(themeCoordinator.primaryColor)
                alpha = 0.85f
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(4), dp(4))
                setOnClickListener {
                    showCustomizeHighlightsDialog()
                }
            }
            highlightsHeaderRow.addView(customizeBtn)
            content.addView(highlightsHeaderRow)
            content.addView(highlightsGrid)
            content.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)) })
            content.addView(lifetimeCard)

            val exportCardBtn = TextView(host).apply {
                text = host.getString(R.string.export_summary_card)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(12)) }
                setOnClickListener {
                    showSummaryCardPreview()
                }
            }
            content.addView(exportCardBtn)
    }

    internal fun invalidateStatsCache() {
        statsDirty = true
        statsSnapshotGen++
        statsSnapshotCache = null
        tabPageCache.clear()
    }

    internal fun refreshStatsPanel() {
        invalidateStatsCache()
        if (host.currentPanel == AppPanel.STATS) {
            host.navigateToPanel(AppPanel.STATS)
        }
    }

    internal fun checkAndResetGoalsForNewDay() {
        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = dateKeyFmt.format(Date())
        val lastResetDate = sharedPrefs.getString("last_planner_reset_date", "") ?: ""

        if (lastResetDate != todayStr) {
            val goals = loadSessionGoalsFromJson(sharedPrefs.getString("session_goals_json", "[]") ?: "[]")
            if (goals.isNotEmpty()) {
                if (lastResetDate.isNotEmpty()) {
                    PlannerHistoryManager.snapshotForDate(host, lastResetDate, goals)
                } else {
                    PlannerHistoryManager.snapshotToday(host, goals)
                }
                val resetGoals = goals.map { it.copy(completed = false, checkedAt = 0L) }
                saveSessionGoalsToJson(resetGoals)
            }
            sharedPrefs.edit().putString("last_planner_reset_date", todayStr).apply()
            refreshStatsPanel()
        }
    }
}
