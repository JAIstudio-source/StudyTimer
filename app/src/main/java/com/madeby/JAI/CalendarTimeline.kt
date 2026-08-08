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

            val navRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(2), dp(4), dp(2)) }
            val canPrev = !atMinBound()
            val canNext = !atMaxBound()
            val disabledTint = tintedColor(themeCoordinator.textColor, 45)
            navRow.addView(TextView(this).apply {
                text = getString(R.string.cal_prev)
                gravity = Gravity.CENTER
                setTextColor(if (canPrev) themeCoordinator.primaryColor else disabledTint)
                alpha = if (canPrev) 1f else 0.4f
                textSize = 26f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 22f)
                if (canPrev) setOnClickListener { shiftMonth(-1) }
            })
            navRow.addView(TextView(this).apply {
                text = monthLabelSdf.format(anchor.time)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.textColor)
                textSize = 16f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            navRow.addView(TextView(this).apply {
                text = getString(R.string.cal_next)
                gravity = Gravity.CENTER
                setTextColor(if (canNext) themeCoordinator.primaryColor else disabledTint)
                alpha = if (canNext) 1f else 0.4f
                textSize = 26f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 22f)
                if (canNext) setOnClickListener { shiftMonth(1) }
            })
            navRow.addView(TextView(this).apply {
                text = getString(R.string.cal_today)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.primaryColor)
                textSize = 10f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 22f)
                setOnClickListener {
                    val todayCal = Calendar.getInstance()
                    if (calendarYear != todayCal.get(Calendar.YEAR) || calendarMonth != todayCal.get(Calendar.MONTH)) {
                        tabPageCache.remove(statsTabKey(AppStatsTab.TIMELINE))
                        calendarYear = todayCal.get(Calendar.YEAR)
                        calendarMonth = todayCal.get(Calendar.MONTH)
                        navigateToPanel(AppPanel.STATS)
                    }
                }
            })
            content.addView(navRow)

            val wdRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), dp(8), dp(4), dp(2)) }
            val wdBase = WeekHelper.mondayOf(Calendar.getInstance())
            val wdSdf = SimpleDateFormat("EE", Locale.getDefault())
            for (i in 0 until 7) {
                val wdCal = (wdBase.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                wdRow.addView(TextView(this).apply {
                    text = wdSdf.format(wdCal.time).replace(".", "")
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, dp(24), 1f)
                })
            }
            content.addView(wdRow)

            val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            var firstDow = anchor.get(Calendar.DAY_OF_WEEK)
            var offset = firstDow - Calendar.MONDAY
            if (offset < 0) offset += 7
            val daysInMonth = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rows = (offset + daysInMonth + 6) / 7
            var dayNum = 1
            for (r in 0 until rows) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    if (idx < offset || dayNum > daysInMonth) {
                        row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f) })
                    } else {
                        val dateStr = dateKey(year, month, dayNum)
                        val focusSecs = snap.dayFocus[dateStr] ?: 0L
                        val goalSecs = resolveGoalFor(dateStr)
                        row.addView(buildCalendarDayCell(dateStr, dayNum, focusSecs, goalSecs, dateStr == todayStr))
                        dayNum++
                    }
                }
                grid.addView(row)
            }
            content.addView(grid)

            var monthFocus = 0L
            var goalDays = 0
            for (d in 1..daysInMonth) {
                val dateStr = dateKey(year, month, d)
                val f = snap.dayFocus[dateStr] ?: 0L
                if (f > 0L) monthFocus += f
                if (f >= resolveGoalFor(dateStr)) goalDays++
            }
            val summaryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), dp(10), dp(4), 0) }
            if (monthFocus > 0L) {
                summaryRow.addView(TextView(this).apply {
                    text = getString(R.string.cal_goal_days, goalDays)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(12), dp(7), dp(12), dp(7))
                    background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
                })
                summaryRow.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 0) })
                summaryRow.addView(TextView(this).apply {
                    text = getString(R.string.cal_month_focus, monthFocus / 3600, (monthFocus % 3600) / 60)
                    setTextColor(themeCoordinator.textColor)
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(12), dp(7), dp(12), dp(7))
                    background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(tintedColor(themeCoordinator.textColor, 26)) }
                })
            } else {
                summaryRow.addView(TextView(this).apply {
                    text = getString(R.string.cal_month_empty)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.4f
                    textSize = 12f
                    setPadding(dp(6), dp(4), 0, 0)
                })
            }
            content.addView(summaryRow)
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

    private fun buildCalendarDayCell(dateStr: String, day: Int, focusSecs: Long, goalSecs: Long, isToday: Boolean): View {
        return with(host) {
            val green = 0xFF43D36E.toInt()
            val red = 0xFFFF4D4D.toInt()
            val goalReached = goalSecs > 0L && focusSecs >= goalSecs
            val pct = if (goalSecs > 0L) (focusSecs.toFloat() / goalSecs.toFloat()).coerceIn(0f, 1f) else 0f
            val ringSize = dp(32)
            val ringWrapSize = dp(36)
            val stroke = dp(3)
            val parsed = try { dateKeyFmt.parse(dateStr) } catch (_: Exception) { null }
            val lbl = if (parsed != null) dayCellFmt.format(parsed) else dateStr

            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f)
                setOnClickListener { showDayDialog(dateStr, lbl) }
                setOnLongClickListener {
                    if (dateStr != dateKeyFmt.format(Date())) {
                        confirmDeleteDay(dateStr, lbl)
                        true
                    } else false
                }
            }
            cell.addView(TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                if (!goalReached && focusSecs <= 0L && !isToday) alpha = 0.5f
            })
            val ringWrap = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ringWrapSize, ringWrapSize)
                setPadding(0, dp(3), 0, 0)
            }
            if (goalReached) {
                ringWrap.addView(View(this).apply {
                    background = containedGlow(green, 22, 130)
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
                })
                ringWrap.addView(SegmentRing(
                    listOf(1f to green),
                    tintedColor(green, 45),
                    stroke,
                    null
                ), FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
                ringWrap.addView(TextView(this).apply {
                    text = "\u2713"
                    gravity = Gravity.CENTER
                    setTextColor(lightenColor(green, 0.7f))
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                })
            } else {
                if (pct > 0f) {
                    ringWrap.addView(View(this).apply {
                        background = containedGlow(red, 22, 110)
                        layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
                    })
                }
                val segments = if (pct > 0f) listOf(pct to red) else emptyList()
                ringWrap.addView(SegmentRing(
                    segments,
                    tintedColor(themeCoordinator.textColor, 65),
                    stroke,
                    null
                ), FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
            }
            cell.addView(ringWrap)
            cell
        }
    }
}
