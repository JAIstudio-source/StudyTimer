package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeeklySummaryShareHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val handler get() = host.handler

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)
    private fun rippleBackground(color: Int) = host.rippleBackground(color)
    private fun getString(resId: Int) = host.getString(resId)

    data class WeekStats(
        val totalSecs: Long,
        val bestSecs: Long,
        val bestName: String,
        val bestDate: String,
        val vsPrev: String,
        val dateRange: String,
        val breakSecs: Long,
        val streak: Int,
        val hasData: Boolean,
        val daySecs: LongArray,
        val dayGoals: LongArray,
        val dayLabels: Array<String>,
        val sessionCount: Int
    )

    fun showSummaryCardPreview() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.attributes?.windowAnimations = R.style.DialogScaleAnimation

        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val currentSessionSecs = sharedPrefs.getLong("accumulatedStudy", 0L)
        val currentBreakSecs = sharedPrefs.getLong("currentBreakSeconds", 0L)
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
        val dateRangeFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Fixed output aspect for the shareable card (9:16 portrait)
        val CARD_ASPECT = 0.5625f

        // Compute stats for a given week (0 = this week, -1 = last week, etc.)
        fun computeWeekStats(weekOffset: Int): WeekStats {
            val mo = WeekHelper.mondayOffset(Calendar.getInstance())
            var totalSecs = 0L
            var breakSecs = 0L
            var bestSecs = 0L
            var bestName = "N/A"
            var bestDate = ""
            var sessionCount = 0
            val daySecs = LongArray(7)
            val dayGoals = LongArray(7)
            val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + weekOffset * 7) }
                val dateStr = sdf.format(c.time)
                var dayFocus = sharedPrefs.getLong("${dateStr}_focus_total", 0L)
                var dayBreak = sharedPrefs.getLong("${dateStr}_break_total", 0L)
                if (weekOffset == 0 && dateStr == todayStr) {
                    dayFocus += currentSessionSecs
                    dayBreak += currentBreakSecs
                }
                totalSecs += dayFocus
                breakSecs += dayBreak
                if (dayFocus > 0L) sessionCount++
                daySecs[i] = dayFocus
                dayGoals[i] = host.resolveGoalFor(dateStr)
                if (dayFocus > bestSecs) {
                    bestSecs = dayFocus
                    bestName = dayNameSdf.format(c.time)
                    bestDate = dateRangeFormat.format(c.time)
                }
            }

            var prevSecs = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + (weekOffset - 1) * 7) }
                prevSecs += sharedPrefs.getLong("${sdf.format(c.time)}_focus_total", 0L)
            }

            val vsPrev = if (prevSecs == 0L) {
                getString(R.string.no_previous_data)
            } else {
                val diff = (((totalSecs - prevSecs).toFloat() / prevSecs.toFloat()) * 100).toInt()
                if (diff >= 0) "+$diff%" else "$diff%"
            }

            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + weekOffset * 7) }
            val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + 6 + weekOffset * 7) }
            val dateRange = "${dateRangeFormat.format(startCal.time)} – ${dateRangeFormat.format(endCal.time)}".uppercase()

            // Streak as of the week's end (capped at today), counting back goal-qualifying days
            var streakEnd = endCal.clone() as Calendar
            val todayCal = Calendar.getInstance()
            if (streakEnd.after(todayCal)) streakEnd = todayCal
            var streak = 0
            var streakCal = streakEnd.clone() as Calendar
            while (streak < 365) {
                val dStr = sdf.format(streakCal.time)
                val dSecs = sharedPrefs.getLong("${dStr}_focus_total", 0L)
                val streakThreshold = if (streakGoalBased) host.resolveGoalFor(dStr) else 2700L
                if (dSecs >= streakThreshold) streak++ else break
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            return WeekStats(totalSecs, bestSecs, bestName, bestDate, vsPrev, dateRange, breakSecs, streak, totalSecs > 0L || breakSecs > 0L, daySecs, dayGoals, dayLabels, sessionCount)
        }

        // Render card with given stats at the fixed 9:16 aspect
        var currentBitmap: android.graphics.Bitmap? = null
        fun renderCard(stats: WeekStats) {
            val days = (0..6).map { i -> WeeklyCardView.Day(stats.dayLabels[i], stats.daySecs[i], stats.dayGoals[i]) }
            val card = WeeklyCardView(host).apply {
                setData(
                    WeeklyCardView.CardData(
                        dateRange = stats.dateRange,
                        totalSecs = stats.totalSecs,
                        breakSecs = stats.breakSecs,
                        bestName = stats.bestName,
                        bestSecs = stats.bestSecs,
                        streak = stats.streak,
                        vsPrev = stats.vsPrev,
                        sessionCount = stats.sessionCount,
                        days = days,
                        hasData = stats.hasData
                    )
                )
            }
            currentBitmap = renderWeeklyCardBitmap(card, CARD_ASPECT)
        }

        // Compute min/max week bounds
        val allDates = sharedPrefs.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { try { sdf.parse(it.removeSuffix("_focus_total")) } catch (_: Exception) { null } }
        val minDate = allDates.minOrNull()
        val minWeekOffset = if (minDate != null) {
            val minCal = Calendar.getInstance().apply { time = minDate }
            val daysFromToday = ((System.currentTimeMillis() - minCal.timeInMillis) / 86400000L).toInt()
            -(daysFromToday / 7) - 1
        } else {
            -52
        }
        val maxWeekOffset = 0 // don't show future weeks

        // Default to last week
        var weekOffset = -1
        var currentStats = computeWeekStats(weekOffset)
        renderCard(currentStats)

        // Scaled preview — fits the screen comfortably without scrolling
        val dm = host.resources.displayMetrics
        val reservedH = dp(20 + 20 + 14 + 44 + 16 + 48 + 32)
        val maxH = (dm.heightPixels - reservedH).coerceIn(dp(260), dp(580))
        val maxW = (dm.widthPixels - dp(40)).coerceAtMost(dp(350))
        var previewWidth = maxW
        var previewHeight = (previewWidth / CARD_ASPECT).toInt()
        if (previewHeight > maxH) {
            previewHeight = maxH
            previewWidth = (previewHeight * CARD_ASPECT).toInt()
        }

        val dialogRoot = FrameLayout(host).apply {
            setBackgroundColor(0xF0070A10.toInt())
        }

        val scrollView = ScrollView(host).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val contentColumn = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scrollView.addView(contentColumn)

        val previewImageView = ImageView(host).apply {
            layoutParams = LinearLayout.LayoutParams(previewWidth, previewHeight).apply { setMargins(0, 0, 0, dp(14)) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.cd_weekly_card_preview)
            setImageBitmap(currentBitmap)
        }

        contentColumn.addView(previewImageView)

        // Week navigation row
        val dateRangeText = TextView(host).apply {
            text = currentStats.dateRange
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateNavButtons(prev: Button, next: Button) {
            prev.alpha = if (weekOffset <= minWeekOffset) 0.3f else 1f
            next.alpha = if (weekOffset >= maxWeekOffset) 0.3f else 1f
        }

        val prevWeekBtn = Button(host).apply {
            text = "\u25C0"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val nextWeekBtn = Button(host).apply {
            text = "\u25B6"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        prevWeekBtn.setOnClickListener {
            if (weekOffset > minWeekOffset) {
                weekOffset--
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        nextWeekBtn.setOnClickListener {
            if (weekOffset < maxWeekOffset) {
                weekOffset++
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        // Set initial button states
        updateNavButtons(prevWeekBtn, nextWeekBtn)

        val weekNavRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }
        weekNavRow.addView(prevWeekBtn)
        weekNavRow.addView(dateRangeText)
        weekNavRow.addView(nextWeekBtn)
        contentColumn.addView(weekNavRow)

        // Action buttons
        val actionRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Loading overlay indicator
        val loadingOverlay = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setBackgroundColor(tintedColor(themeCoordinator.bgColor, 220))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        loadingOverlay.addView(android.widget.ProgressBar(host, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
        })
        loadingOverlay.addView(TextView(host).apply {
            text = getString(R.string.saving)
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            setPadding(0, dp(12), 0, 0)
        })

        fun getSafeBitmap() = currentBitmap

        val closeBtn = Button(host).apply {
            text = getString(R.string.btn_close_lower)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val saveBtn = Button(host).apply {
            text = getString(R.string.btn_save)
            setTextColor(themeCoordinator.bgColor)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    if (bmp != null) saveBitmapToMediaStore(bmp)
                    handler.post { loadingOverlay.visibility = View.GONE; dialog.dismiss() }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val shareBtn = Button(host).apply {
            text = getString(R.string.btn_share)
            setTextColor(themeCoordinator.bgColor)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    var shared = false
                    if (bmp != null) {
                        val uri = writeBitmapToCache(bmp)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(shareIntent, getString(R.string.share_chooser_title))
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            handler.post {
                                loadingOverlay.visibility = View.GONE
                                host.startActivity(chooser)
                            }
                            shared = true
                        }
                    }
                    if (!shared) {
                        handler.post {
                            loadingOverlay.visibility = View.GONE
                            Toast.makeText(host, getString(R.string.toast_share_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
        }

        actionRow.addView(closeBtn)
        actionRow.addView(saveBtn)
        actionRow.addView(shareBtn)
        contentColumn.addView(actionRow)

        dialogRoot.addView(scrollView)
        dialogRoot.addView(loadingOverlay)

        dialog.setContentView(dialogRoot)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xF2080C14.toInt()))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    fun showCustomizeHighlightsDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Customize Highlights"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Pin your favorite metrics (minimum 2). Unpinned cards will be hidden to save space."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 12f
            setPadding(0, dp(4), 0, dp(14))
        })

        data class HighlightOption(val key: String, val title: String, val subtitle: String)

        val options = listOf(
            HighlightOption("WEEKLY_TREND", "Weekly Trend", "Compare this week vs last week"),
            HighlightOption("ACTIVE_DAYS", "Active Days", "Track active days and streak count"),
            HighlightOption("BEST_DAY", "Best Day", "Your highest study day of all time"),
            HighlightOption("RECORD_WEEK", "Best Week", "All-time highest study week"),
            HighlightOption("GOAL_SUCCESS", "Daily Goal Hits", "Days where daily target was met"),
            HighlightOption("AVG_SESSION", "Average Session", "Average duration per study session")
        )

        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val pinnedPrefsJson = sharedPrefs.getString("pinned_highlights_order", null)
        val currentPinned = mutableSetOf<String>()
        if (!pinnedPrefsJson.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(pinnedPrefsJson)
                for (i in 0 until arr.length()) currentPinned.add(arr.getString(i))
            } catch (_: Exception) {
                currentPinned.addAll(listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK"))
            }
        } else {
            currentPinned.addAll(listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK"))
        }

        val scroll = ScrollView(host).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (host.resources.displayMetrics.heightPixels * 0.40f).toInt())
        }
        val optionsBox = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(optionsBox)

        fun renderOptions() {
            optionsBox.removeAllViews()
            for (opt in options) {
                val isChecked = currentPinned.contains(opt.key)
                val card = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                    setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        if (isChecked) {
                            if (currentPinned.size <= 2) {
                                Toast.makeText(host, "Keep at least 2 highlights pinned", Toast.LENGTH_SHORT).show()
                            } else {
                                currentPinned.remove(opt.key)
                                renderOptions()
                            }
                        } else {
                            currentPinned.add(opt.key)
                            renderOptions()
                        }
                    }
                }

                val checkBadge = TextView(host).apply {
                    text = if (isChecked) "Pinned" else "Hidden"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(if (isChecked) themeCoordinator.primaryColor else 0xFF475569.toInt())
                    }
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                }
                card.addView(checkBadge)

                val col = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(10), 0, dp(4), 0) }
                }
                col.addView(TextView(host).apply {
                    text = opt.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                col.addView(TextView(host).apply {
                    text = opt.subtitle
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })
                card.addView(col)
                optionsBox.addView(card)
            }
        }
        renderOptions()
        content.addView(scroll)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                if (currentPinned.size < 2) {
                    Toast.makeText(host, "Select at least 2 highlights", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val arr = org.json.JSONArray()
                for (opt in options) {
                    if (currentPinned.contains(opt.key)) arr.put(opt.key)
                }
                sharedPrefs.edit().putString("pinned_highlights_order", arr.toString()).apply()
                host.refreshStatsPanel()
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showWeeklyTrendDetailDialog(thisWeek: Long, prevWeek: Long, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Weekly Trend Comparison"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val trendDiff = if (prevWeek > 0L) ((thisWeek - prevWeek).toFloat() / prevWeek.toFloat() * 100f).toInt() else if (thisWeek > 0L) 100 else 0
        val trendText = when {
            trendDiff > 0 -> "+${trendDiff}% increase compared to last week"
            trendDiff < 0 -> "${trendDiff}% compared to last week"
            else -> "Identical study time to last week"
        }

        content.addView(TextView(host).apply {
            text = "This Week: ${thisWeek / 3600}h ${(thisWeek % 3600) / 60}m · Last Week: ${prevWeek / 3600}h ${(prevWeek % 3600) / 60}m\n$trendText"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val cal = Calendar.getInstance()
        val mondayOffset = WeekHelper.mondayOffset(cal)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDateStr = sdf.format(Date())

        val listContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        for (i in 0..6) {
            val cThis = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset + i) }
            val cPrev = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset - 7 + i) }
            val dateThisStr = sdf.format(cThis.time)
            val isToday = dateThisStr == todayDateStr
            val fThis = snap.dayFocus[dateThisStr] ?: 0L
            val fPrev = snap.dayFocus[sdf.format(cPrev.time)] ?: 0L

            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isToday) {
                    GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        val cardBg = if (themeCoordinator.isDarkMode()) {
                            if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF181A24.toInt()
                        } else {
                            0xFFEDF0F5.toInt()
                        }
                        setColor(cardBg)
                        setStroke(dp(1), themeCoordinator.primaryColor)
                    }
                } else {
                    themeCoordinator.createCardBackground(10f)
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(4))
                }
            }
            val dayNameText = if (isToday) "${weekdays[i]} (Today)" else weekdays[i]
            row.addView(TextView(host).apply {
                text = dayNameText
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(host).apply {
                val thisStr = "${fThis / 3600}h ${(fThis % 3600) / 60}m"
                val prevStr = "${fPrev / 3600}h ${(fPrev % 3600) / 60}m"
                text = "$thisStr (prev: $prevStr)"
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                alpha = if (isToday) 1f else 0.65f
                textSize = 12f
                typeface = Typeface.MONOSPACE
            })
            listContainer.addView(row)
        }
        content.addView(listContainer)

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(14), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showActiveDaysDetailDialog(activeDays: Int, longestStreak: Int, currentStreak: Int, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Study Consistency"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Building a daily study habit is the single most effective way to retain knowledge."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(14))
        })

        fun makeStatRow(label: String, value: String): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground(12f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(6))
                }
                addView(TextView(host).apply {
                    text = label
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(host).apply {
                    text = value
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
            }
        }

        content.addView(makeStatRow("Current Active Streak", "$currentStreak days"))
        content.addView(makeStatRow("Longest Recorded Streak", "$longestStreak days"))
        content.addView(makeStatRow("Total Active Study Days", "$activeDays days"))

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(12), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showBestDayDetailDialog(bestDayLabel: String, bestDaySecs: Long, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "All-Time Best Day"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            val sub = if (bestDaySecs > 0L) "Your single highest study day of all time was on $bestDayLabel." else "Keep studying to set your all-time best study day record."
            text = sub
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(10))
        })

        if (bestDaySecs > 0L) {
            val recordCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(12))
                }
                addView(TextView(host).apply {
                    text = "RECORD FOCUS TIME"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                addView(TextView(host).apply {
                    text = "${bestDaySecs / 3600}h ${(bestDaySecs % 3600) / 60}m studied"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 20f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(6), 0, dp(3))
                })
                addView(TextView(host).apply {
                    text = "Achieved on $bestDayLabel"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
            }
            content.addView(recordCard)
        }

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(6), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showRecordWeekDetailDialog(bestWeekLabel: String, bestWeekSecs: Long, longestStreak: Int, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Best Study Week"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Your all-time highest weekly study record in StudyTimer:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(14))
        })

        fun makeRecordCard(title: String, mainVal: String, subVal: String): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(8))
                }
                addView(TextView(host).apply {
                    text = title
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                addView(TextView(host).apply {
                    text = mainVal
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
                if (subVal.isNotBlank()) {
                    addView(TextView(host).apply {
                        text = subVal
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 11.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
            }
        }

        val weekRecordStr = if (bestWeekSecs > 0L) "${bestWeekSecs / 3600}h ${(bestWeekSecs % 3600) / 60}m studied" else "No best week yet"
        val weekLabelStr = if (bestWeekSecs > 0L) "Week of $bestWeekLabel" else "Study consistently to set your weekly record"
        content.addView(makeRecordCard("BEST STUDY WEEK", weekRecordStr, weekLabelStr))

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showGoalSuccessDetailDialog(snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Daily Goal Success"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val goalHits = snap.goalHits
        content.addView(TextView(host).apply {
            text = "You have met your daily study target on $goalHits days. Keep setting realistic daily goals in the Planner to build long-term focus habits."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showAvgSessionDetailDialog(snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Session Breakdown"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        var totalSessionCount = 0
        var totalDuration = 0L
        var longestSession = 0L
        val entries = TimelineLogger.load(host)
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val allDates = entries.mapNotNull { if (it.timestamp > 0L) dateSdf.format(Date(it.timestamp)) else null }.distinct()
        for (dStr in allDates) {
            val (sessions, _) = host.dayBlocks(dStr)
            for (s in sessions) {
                if (s.secs >= 60L) {
                    totalSessionCount++
                    totalDuration += s.secs
                    if (s.secs > longestSession) longestSession = s.secs
                }
            }
        }
        val avgMins = if (totalSessionCount > 0) ((totalDuration / totalSessionCount) / 60).toInt() else 0

        content.addView(TextView(host).apply {
            text = "Average Session Duration: ${avgMins}m\nTotal Completed Sessions: $totalSessionCount\nLongest Single Session: ${longestSession / 3600}h ${(longestSession % 3600) / 60}m"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(3).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun renderViewToBitmap(view: View): android.graphics.Bitmap {
        val w = dp(360)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val h = view.measuredHeight
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    fun saveBitmapToMediaStore(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val filename = "StudySummary_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/StudyTimer")
                }
            }
            val uri = host.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                host.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    fun renderWeeklyCardBitmap(view: WeeklyCardView, aspect: Float): android.graphics.Bitmap {
        val w = 1080
        val h = (w / aspect).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    fun writeBitmapToCache(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val dir = java.io.File(host.cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "StudySummary_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            }
            androidx.core.content.FileProvider.getUriForFile(host, "${host.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }
}
