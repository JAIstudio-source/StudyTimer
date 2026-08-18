package com.madeby.JAI

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarTimeline(private val host: MainActivity) {

    private val dayCellFmt = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    fun build(content: LinearLayout, snap: StatsSnapshot, todayStr: String) {
        with(host) {
            val anchor = calendarAnchor()
            val year = anchor.get(Calendar.YEAR)
            val month = anchor.get(Calendar.MONTH)
            val monthLabelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val dateSdf = dateKeyFmt
            var minY = Int.MAX_VALUE; var minM = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE; var maxM = Int.MIN_VALUE
            for (key in snap.dayFocus.keys) {
                val d = try { dateSdf.parse(key) } catch (_: Exception) { null } ?: continue
                val c = Calendar.getInstance().apply { time = d }
                val y = c.get(Calendar.YEAR); val m = c.get(Calendar.MONTH)
                if (y < minY || (y == minY && m < minM)) { minY = y; minM = m }
                if (y > maxY || (y == maxY && m > maxM)) { maxY = y; maxM = m }
            }
            val now = Calendar.getInstance()
            if (maxY == Int.MIN_VALUE || now.get(Calendar.YEAR) > maxY || (now.get(Calendar.YEAR) == maxY && now.get(Calendar.MONTH) > maxM)) {
                maxY = now.get(Calendar.YEAR); maxM = now.get(Calendar.MONTH)
            }
            if (minY == Int.MAX_VALUE) { minY = maxY; minM = maxM }
            fun atMinBound() = calendarYear == minY && calendarMonth == minM
            fun atMaxBound() = calendarYear == maxY && calendarMonth == maxM

            fun shiftMonth(delta: Int) {
                if (delta < 0 && atMinBound()) return
                if (delta > 0 && atMaxBound()) return
                tabPageCache.remove(statsTabKey(AppStatsTab.TIMELINE))
                calendarMonth += delta
                while (calendarMonth < 0) { calendarMonth += 12; calendarYear-- }
                while (calendarMonth > 11) { calendarMonth -= 12; calendarYear++ }
                navigateToPanel(AppPanel.STATS)
            }

            // Outer Premium Theme Card
            val calendarCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(18), dp(18), dp(18), dp(18))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(16))
                }
            }

            // Card Section Header
            calendarCard.addView(TextView(this).apply {
                text = "📅 HISTORY CALENDAR & FOCUS LOGS"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            })

            // Navigation Row
            val navRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(14))
            }
            val canPrev = !atMinBound()
            val canNext = !atMaxBound()
            val disabledTint = tintedColor(themeCoordinator.textColor, 30)

            val prevBtn = TextView(this).apply {
                text = "‹"
                gravity = Gravity.CENTER
                setTextColor(if (canPrev) themeCoordinator.primaryColor else disabledTint)
                alpha = if (canPrev) 1f else 0.35f
                textSize = 20f
                setPadding(dp(14), dp(4), dp(14), dp(6))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, if (canPrev) 40 else 15), 14f)
                if (canPrev) setOnClickListener { shiftMonth(-1) }
            }

            val monthTitle = TextView(this).apply {
                text = monthLabelSdf.format(anchor.time)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.textColor)
                textSize = 17f
                letterSpacing = -0.01f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nextBtn = TextView(this).apply {
                text = "›"
                gravity = Gravity.CENTER
                setTextColor(if (canNext) themeCoordinator.primaryColor else disabledTint)
                alpha = if (canNext) 1f else 0.35f
                textSize = 20f
                setPadding(dp(14), dp(4), dp(14), dp(6))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, if (canNext) 40 else 15), 14f)
                if (canNext) setOnClickListener { shiftMonth(1) }
            }

            val todayBtn = TextView(this).apply {
                text = "Today 🎯"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
                setOnClickListener {
                    val todayCal = Calendar.getInstance()
                    if (calendarYear != todayCal.get(Calendar.YEAR) || calendarMonth != todayCal.get(Calendar.MONTH)) {
                        tabPageCache.remove(statsTabKey(AppStatsTab.TIMELINE))
                        calendarYear = todayCal.get(Calendar.YEAR)
                        calendarMonth = todayCal.get(Calendar.MONTH)
                        navigateToPanel(AppPanel.STATS)
                    }
                }
            }

            navRow.addView(prevBtn)
            navRow.addView(monthTitle)
            navRow.addView(nextBtn)
            navRow.addView(todayBtn)
            calendarCard.addView(navRow)

            // Styled Weekday Headers Row
            val wdRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = themeCoordinator.createGlassChip(0xFF141620.toInt(), 12f)
                setPadding(dp(2), dp(8), dp(2), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }
            val wdBase = WeekHelper.mondayOf(Calendar.getInstance())
            val wdSdf = SimpleDateFormat("EE", Locale.getDefault())
            for (i in 0 until 7) {
                val wdCal = (wdBase.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                val isWeekend = (i >= 5)
                wdRow.addView(TextView(this).apply {
                    text = wdSdf.format(wdCal.time).replace(".", "").take(3)
                    gravity = Gravity.CENTER
                    setTextColor(if (isWeekend) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    alpha = if (isWeekend) 0.95f else 0.6f
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            calendarCard.addView(wdRow)

            // Days Grid
            val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            var firstDow = anchor.get(Calendar.DAY_OF_WEEK)
            var offset = firstDow - Calendar.MONDAY
            if (offset < 0) offset += 7
            val daysInMonth = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rows = (offset + daysInMonth + 6) / 7
            var dayNum = 1
            val todayDateKey = todayStr

            for (r in 0 until rows) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(4))
                    }
                }
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    if (idx < offset || dayNum > daysInMonth) {
                        row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f) })
                    } else {
                        val dateStr = dateKey(year, month, dayNum)
                        val focusSecs = snap.dayFocus[dateStr] ?: 0L
                        val goalSecs = resolveGoalFor(dateStr)
                        val isFuture = dateStr > todayDateKey
                        row.addView(buildCalendarDayCell(dateStr, dayNum, focusSecs, goalSecs, dateStr == todayStr, isFuture))
                        dayNum++
                    }
                }
                grid.addView(row)
            }
            calendarCard.addView(grid)

            // Monthly Summary Footer Badges (Clean Glassmorphic Chips)
            var monthFocus = 0L
            var goalDays = 0
            for (d in 1..daysInMonth) {
                val dateStr = dateKey(year, month, d)
                val f = snap.dayFocus[dateStr] ?: 0L
                if (f > 0L) monthFocus += f
                if (f >= resolveGoalFor(dateStr)) goalDays++
            }

            val summaryRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(14), 0, 0)
                gravity = Gravity.CENTER_VERTICAL
            }
            if (monthFocus > 0L) {
                summaryRow.addView(TextView(this).apply {
                    text = "🎯 ${goalDays} Goal Days"
                    setTextColor(0xFF43D36E.toInt())
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(0x1F43D36E.toInt())
                        setStroke(dp(1), 0x3343D36E.toInt())
                    }
                })
                summaryRow.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 0) })
                val hrs = monthFocus / 3600
                val mins = (monthFocus % 3600) / 60
                summaryRow.addView(TextView(this).apply {
                    text = "⏱️ ${hrs}h ${mins}m Total Focus"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(tintedColor(themeCoordinator.primaryColor, 35))
                        setStroke(dp(1), tintedColor(themeCoordinator.primaryColor, 70))
                    }
                })
            } else {
                summaryRow.addView(TextView(this).apply {
                    text = "📖 No study activity recorded for this month"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(dp(4), dp(4), 0, 0)
                })
            }
            calendarCard.addView(summaryRow)

            content.addView(calendarCard)
        }
    }

    private fun calendarAnchor(): Calendar {
        val c = Calendar.getInstance()
        if (host.calendarYear == 0) {
            host.calendarYear = c.get(Calendar.YEAR)
            host.calendarMonth = c.get(Calendar.MONTH)
        }
        c.set(Calendar.YEAR, host.calendarYear)
        c.set(Calendar.MONTH, host.calendarMonth)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    private fun dateKey(y: Int, m: Int, d: Int): String {
        val mm = if (m < 9) "0${m + 1}" else "${m + 1}"
        val dd = if (d < 10) "0$d" else "$d"
        return "$y-$mm-$dd"
    }

    private fun containedGlow(color: Int, boxDp: Int, alpha: Int): GradientDrawable {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return with(host) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setGradientType(GradientDrawable.RADIAL_GRADIENT)
                setGradientRadius(dp(boxDp / 2).toFloat())
                setColors(intArrayOf(Color.argb(alpha, r, g, b), Color.argb(0, r, g, b)))
            }
        }
    }

    private fun buildCalendarDayCell(dateStr: String, day: Int, focusSecs: Long, goalSecs: Long, isToday: Boolean, isFuture: Boolean): View {
        return with(host) {
            val green = 0xFF43D36E.toInt()
            val red = 0xFFFF4D4D.toInt()
            val goalReached = goalSecs > 0L && focusSecs >= goalSecs
            val pct = if (goalSecs > 0L) (focusSecs.toFloat() / goalSecs.toFloat()).coerceIn(0f, 1f) else 0f
            val ringSize = dp(26)
            val ringWrapSize = dp(30)
            val stroke = dp(3)
            val parsed = try { dateKeyFmt.parse(dateStr) } catch (_: Exception) { null }
            val lbl = if (parsed != null) dayCellFmt.format(parsed) else dateStr

            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(dp(1), dp(1), dp(1), dp(1)) }
                background = if (isToday) {
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(0xFF1B1E2B.toInt())
                        setStroke(dp(2), themeCoordinator.primaryColor)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(0xFF14151C.toInt())
                        setStroke(dp(1), 0xFF222430.toInt())
                    }
                }
                setPadding(0, dp(4), 0, dp(4))
                if (isFuture) {
                    alpha = 0.35f
                } else {
                    setOnClickListener { showDayDialog(dateStr, lbl) }
                    setOnLongClickListener {
                        if (dateStr != dateKeyFmt.format(Date())) {
                            confirmDeleteDay(dateStr, lbl)
                            true
                        } else false
                    }
                }
            }

            cell.addView(TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                typeface = Typeface.create("sans-serif-medium", if (isToday || goalReached) Typeface.BOLD else Typeface.NORMAL)
                if (!goalReached && focusSecs <= 0L && !isToday && !isFuture) alpha = 0.65f
            })

            val ringWrap = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ringWrapSize, ringWrapSize)
                setPadding(0, dp(2), 0, 0)
            }

            if (goalReached) {
                ringWrap.addView(View(this).apply {
                    background = containedGlow(green, 20, 130)
                    layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                })
                ringWrap.addView(SegmentRing(
                    listOf(1f to green),
                    tintedColor(green, 45),
                    stroke,
                    null
                ), FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
                ringWrap.addView(TextView(this).apply {
                    text = "✓"
                    gravity = Gravity.CENTER
                    setTextColor(lightenColor(green, 0.7f))
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                })
                cell.addView(ringWrap)
            } else if (focusSecs > 0L) {
                val ringColor = if (pct > 0f) red else themeCoordinator.primaryColor
                val segments = if (pct > 0f) listOf(pct to red) else listOf(1f to themeCoordinator.primaryColor)
                ringWrap.addView(View(this).apply {
                    background = containedGlow(ringColor, 20, 110)
                    layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                })
                ringWrap.addView(SegmentRing(
                    segments,
                    tintedColor(themeCoordinator.textColor, 50),
                    stroke,
                    null
                ), FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
                cell.addView(ringWrap)
            } else {
                // Minimal clean dot for inactive days (No hollow rings!)
                val dotView = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(tintedColor(themeCoordinator.textColor, 35))
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(4), dp(4), Gravity.CENTER)
                    alpha = if (isFuture) 0.2f else 0.4f
                }
                ringWrap.addView(dotView)
                cell.addView(ringWrap)
            }

            cell
        }
    }
}
