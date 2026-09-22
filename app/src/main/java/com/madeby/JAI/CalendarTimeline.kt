package com.madeby.JAI

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
                text = "MONTHLY STUDY HISTORY"
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
                text = "Today"
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
            val wdBg = if (themeCoordinator.isDarkMode()) {
                if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF141620.toInt()
            } else {
                0xFFEDF0F5.toInt()
            }
            val wdRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(wdBg)
                    setStroke(dp(1), if (themeCoordinator.isDarkMode()) 0x22FFFFFF.toInt() else 0xFFCBD5E1.toInt())
                }
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
            val gridAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800
                interpolator = android.view.animation.DecelerateInterpolator()
            }
            val animSupplier: (() -> Float) = { gridAnim.animatedValue as? Float ?: 1f }

            var firstDow = anchor.get(Calendar.DAY_OF_WEEK)
            var offset = firstDow - Calendar.MONDAY
            if (offset < 0) offset += 7
            val daysInMonth = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rows = (offset + daysInMonth + 6) / 7
            var dayNum = 1
            val todayDateKey = todayStr

            val ringViews = mutableListOf<MainActivity.SegmentRing>()
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
                        row.addView(buildCalendarDayCell(dateStr, dayNum, focusSecs, goalSecs, dateStr == todayStr, isFuture, animSupplier, ringViews))
                        dayNum++
                    }
                }
                grid.addView(row)
            }
            gridAnim.addUpdateListener {
                for (i in 0 until ringViews.size) {
                    ringViews[i].invalidate()
                }
            }
            gridAnim.start()
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
                val goalDaysLabel = if (goalDays == 1) "1 Goal Met" else "$goalDays Goals Met"
                summaryRow.addView(TextView(this).apply {
                    text = goalDaysLabel
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
                    text = "${hrs}h ${mins}m Total Study"
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
                    text = "No study sessions recorded for this month"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(dp(4), dp(4), 0, 0)
                })
            }
            calendarCard.addView(summaryRow)

            content.addView(calendarCard)

            // Exam Countdown Section
            buildExamCountdownSection(content)
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

    private fun buildCalendarDayCell(
        dateStr: String,
        day: Int,
        focusSecs: Long,
        goalSecs: Long,
        isToday: Boolean,
        isFuture: Boolean,
        animSupplier: (() -> Float)? = null,
        ringViews: MutableList<MainActivity.SegmentRing>? = null
    ): View {
        return with(host) {
            val isDark = themeCoordinator.isDarkMode()
            val green = if (isDark) 0xFF43D36E.toInt() else 0xFF10B981.toInt()
            val checkmarkColor = if (isDark) lightenColor(green, 0.7f) else 0xFF047857.toInt()
            val red = if (isDark) 0xFFFF4D4D.toInt() else 0xFFEF4444.toInt()
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
                        val todayBg = if (isDark) {
                            tintedColor(themeCoordinator.primaryColor, 35)
                        } else {
                            tintedColor(themeCoordinator.primaryColor, 25)
                        }
                        setColor(todayBg)
                        setStroke(dp(2), themeCoordinator.primaryColor)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        val cellBg = if (isDark) {
                            if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF14151C.toInt()
                        } else {
                            0xFFF8FAFC.toInt()
                        }
                        val strokeCol = if (isDark) 0xFF222430.toInt() else 0xFFCBD5E1.toInt()
                        setColor(cellBg)
                        setStroke(dp(1), strokeCol)
                    }
                }
                setPadding(0, dp(4), 0, dp(4))
                if (isFuture) {
                    alpha = if (isDark) 0.35f else 0.45f
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
                val dayColor = if (isToday) {
                    themeCoordinator.primaryColor
                } else if (goalReached) {
                    if (isDark) 0xFF43D36E.toInt() else 0xFF047857.toInt()
                } else {
                    themeCoordinator.textColor
                }
                setTextColor(dayColor)
                typeface = Typeface.create("sans-serif-medium", if (isToday || goalReached) Typeface.BOLD else Typeface.NORMAL)
                if (!goalReached && focusSecs <= 0L && !isToday && !isFuture) alpha = if (isDark) 0.65f else 0.75f
            })

            val ringWrap = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ringWrapSize, ringWrapSize)
                setPadding(0, dp(2), 0, 0)
            }

            if (goalReached) {
                if (isDark) {
                    ringWrap.addView(View(this).apply {
                        background = containedGlow(green, 20, 130)
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                    })
                } else {
                    ringWrap.addView(View(this).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(0x1F10B981.toInt())
                        }
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                    })
                }
                val ring = SegmentRing(
                    listOf(1f to green),
                    if (isDark) tintedColor(green, 45) else 0x2E10B981.toInt(),
                    stroke,
                    null,
                    animate = false,
                    progressSupplier = animSupplier
                )
                ringViews?.add(ring)
                ringWrap.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
                ringWrap.addView(TextView(this).apply {
                    text = "✓"
                    gravity = Gravity.CENTER
                    setTextColor(checkmarkColor)
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                })
                cell.addView(ringWrap)
            } else if (focusSecs > 0L) {
                val ringColor = if (pct > 0f) red else themeCoordinator.primaryColor
                val segments = if (pct > 0f) listOf(pct to red) else listOf(1f to themeCoordinator.primaryColor)
                if (isDark) {
                    ringWrap.addView(View(this).apply {
                        background = containedGlow(ringColor, 20, 110)
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                    })
                }
                val ring = SegmentRing(
                    segments,
                    if (isDark) tintedColor(themeCoordinator.textColor, 50) else 0x24CBD5E1.toInt(),
                    stroke,
                    null,
                    animate = false,
                    progressSupplier = animSupplier
                )
                ringViews?.add(ring)
                ringWrap.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
                cell.addView(ringWrap)
            } else {
                // Minimal clean dot for inactive days (No hollow rings!)
                val dotView = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isDark) tintedColor(themeCoordinator.textColor, 35) else 0xFFCBD5E1.toInt())
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(4), dp(4), Gravity.CENTER)
                    alpha = if (isFuture) (if (isDark) 0.2f else 0.35f) else (if (isDark) 0.4f else 0.8f)
                }
                ringWrap.addView(dotView)
                cell.addView(ringWrap)
            }

            cell
        }
    }

    private fun buildExamCountdownSection(content: LinearLayout) {
        with(host) {
            val exams = ExamCountdownManager.getExams(this)

            val sectionContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(16), 0, dp(14))
                }
            }

            // Header Row
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), 0, dp(4), dp(10))
            }

            val titleText = TextView(this).apply {
                text = "Upcoming Exams"
                setTextColor(themeCoordinator.textColor)
                textSize = 15.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val addBtn = TextView(this).apply {
                text = "+ Add Exam"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    showAddEditExamModal(null)
                }
            }

            headerRow.addView(titleText)
            headerRow.addView(addBtn)
            sectionContainer.addView(headerRow)

            if (exams.isEmpty()) {
                // Empty state card
                val emptyCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(22), dp(20), dp(22))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        showAddEditExamModal(null)
                    }
                }

                val emptyTitle = TextView(this).apply {
                    text = "No Upcoming Exams"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                val emptySub = TextView(this).apply {
                    text = "Track your exam dates with live countdowns."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(4), 0, dp(12))
                }
                val emptyAction = TextView(this).apply {
                    text = "Set Exam Date"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 12f)
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                }

                emptyCard.addView(emptyTitle)
                emptyCard.addView(emptySub)
                emptyCard.addView(emptyAction)
                sectionContainer.addView(emptyCard)
            } else {
                // List of exam countdown preview cards
                for (exam in exams) {
                    val allSubjects = SubjectTagManager.getAllSubjects(this)
                    val linkedSubject = allSubjects.find { it.id == exam.subjectId }
                    val examColor = if (exam.colorHex != null) {
                        try { Color.parseColor(exam.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                    } else if (linkedSubject != null) {
                        try { Color.parseColor(linkedSubject.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                    } else {
                        themeCoordinator.primaryColor
                    }

                    val breakdown = ExamCountdownManager.getCountdownBreakdown(exam.targetTimestampMs)

                    val examCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createCardBackground()
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(10))
                        }
                        setOnClickListener {
                            showExamDetailsModal(exam)
                        }
                    }

                    // Top Row: Title + Subject Tag Pill
                    val topRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    }

                    val titleView = TextView(this).apply {
                        text = exam.title
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    topRow.addView(titleView)
                    val hasCustomSubject = linkedSubject != null && !linkedSubject.name.equals("General", ignoreCase = true)
                    if (hasCustomSubject) {
                        val subjectTagPill = TextView(this).apply {
                            text = linkedSubject!!.name
                            setTextColor(examColor)
                            textSize = 11.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            background = themeCoordinator.createGlassChip(tintedColor(examColor, 35), 10f)
                            setPadding(dp(10), dp(4), dp(10), dp(4))
                        }
                        topRow.addView(subjectTagPill)
                    }
                    examCard.addView(topRow)

                    // Main Big Countdown Section (High prominence & instant clarity)
                    val countdownContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, dp(10), 0, dp(8))
                    }

                    val mainCountdownText = TextView(this).apply {
                        tag = "exam_main_countdown_${exam.id}"
                        text = breakdown.mainHeadline
                        setTextColor(if (breakdown.isPast) Color.parseColor("#94A3B8") else examColor)
                        textSize = 21f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    }

                    val readableSubText = TextView(this).apply {
                        tag = "exam_sub_countdown_${exam.id}"
                        text = if (breakdown.readableSubtitle.isNotBlank()) breakdown.readableSubtitle else "Scheduled event"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.7f
                        textSize = 12f
                        setPadding(0, dp(2), 0, 0)
                    }

                    countdownContainer.addView(mainCountdownText)
                    countdownContainer.addView(readableSubText)
                    examCard.addView(countdownContainer)

                    // Bottom Row: Target Date & Action hint
                    val bottomRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(4), 0, 0)
                    }

                    val dateFmt = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(exam.targetDateStr)
                        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(parsed ?: Date())
                    } catch (_: Exception) {
                        exam.targetDateStr
                    }

                    val dateText = TextView(this).apply {
                        text = "Exam Date: $dateFmt"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11.5f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tapHint = TextView(this).apply {
                        text = "Details ›"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 11.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    }

                    bottomRow.addView(dateText)
                    bottomRow.addView(tapHint)
                    examCard.addView(bottomRow)

                    sectionContainer.addView(examCard)
                }
            }

            content.addView(sectionContainer)
        }
    }

    private fun showExamDetailsModal(exam: ExamCountdown) {
        val activity = host
        val themeCoordinator = activity.themeCoordinator
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val allSubjects = SubjectTagManager.getAllSubjects(activity)
        val linkedSubject = allSubjects.find { it.id == exam.subjectId }
        val examColor = if (exam.colorHex != null) {
            try { Color.parseColor(exam.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
        } else if (linkedSubject != null) {
            try { Color.parseColor(linkedSubject.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
        } else {
            themeCoordinator.primaryColor
        }

        val breakdown = ExamCountdownManager.getCountdownBreakdown(exam.targetTimestampMs)

        // Title Bar
        val titleBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        val titleView = TextView(activity).apply {
            text = exam.title
            setTextColor(examColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setOnClickListener { dialog.dismiss() }
        }

        titleBar.addView(titleView)
        titleBar.addView(closeBtn)
        root.addView(titleBar)

        val dateDisplay = try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(exam.targetDateStr)
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(parsed ?: Date())
        } catch (_: Exception) {
            exam.targetDateStr
        }

        val dateCard = TextView(activity).apply {
            text = "Exam Date: $dateDisplay"
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.textColor, 25), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
        root.addView(dateCard)

        // Detailed Countdown Cards Row
        if (!breakdown.isPast && !breakdown.isToday) {
            val gridContainer = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(14))
                }
            }

            fun createMetricPill(value: String, label: String): View {
                return LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createGlassChip(host.tintedColor(examColor, 35), 14f)
                    setPadding(dp(6), dp(10), dp(6), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }

                    addView(TextView(activity).apply {
                        text = value
                        setTextColor(Color.WHITE)
                        textSize = 17f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                    })

                    addView(TextView(activity).apply {
                        text = label
                        setTextColor(host.tintedColor(themeCoordinator.textColor, 180))
                        textSize = 10f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(2), 0, 0)
                    })
                }
            }

            if (breakdown.monthsPart > 0) {
                gridContainer.addView(createMetricPill(breakdown.monthsPart.toString(), "MONTHS"))
            }
            gridContainer.addView(createMetricPill(breakdown.daysPart.toString(), "DAYS"))
            gridContainer.addView(createMetricPill(breakdown.hoursPart.toString(), "HOURS"))
            gridContainer.addView(createMetricPill(breakdown.minsPart.toString(), "MINS"))
            root.addView(gridContainer)
        } else if (breakdown.isToday) {
            val todayBanner = TextView(activity).apply {
                text = "Exam is today! Good luck."
                setTextColor(Color.parseColor("#10B981"))
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            }
            root.addView(todayBanner)
        } else {
            val passedBanner = TextView(activity).apply {
                text = "Exam date passed ${breakdown.daysAgo} days ago"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            }
            root.addView(passedBanner)
        }

        if (!exam.notes.isNullOrBlank()) {
            val notesCard = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.textColor, 18), 12f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(14))
                }
            }
            notesCard.addView(TextView(activity).apply {
                text = "Notes / Syllabus:"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            notesCard.addView(TextView(activity).apply {
                text = exam.notes
                setTextColor(themeCoordinator.textColor)
                textSize = 12.5f
                setPadding(0, dp(4), 0, 0)
            })
            root.addView(notesCard)
        }

        // Action Buttons Row (Edit / Delete)
        val actionRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val editBtn = Button(activity).apply {
            text = "Edit / Rename"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.primaryColor, 50), 12f)
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener {
                dialog.dismiss()
                showAddEditExamModal(exam)
            }
        }

        val deleteBtn = Button(activity).apply {
            text = "Delete"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#EF4444"))
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(6), 0, 0, 0)
            }
            setOnClickListener {
                DeveloperToolsHelper.showThemedConfirmDialog(
                    activity = activity,
                    themeCoordinator = themeCoordinator,
                    title = "Delete Exam Countdown?",
                    message = "Are you sure you want to delete '${exam.title}'?",
                    confirmText = "DELETE EXAM",
                    isDestructive = true
                ) {
                    ExamCountdownManager.deleteExam(activity, exam.id)
                    dialog.dismiss()
                    activity.tabPageCache.remove(activity.statsTabKey(AppStatsTab.TIMELINE))
                    activity.refreshStatsPanel()
                    activity.navigateToPanel(AppPanel.STATS)
                    Toast.makeText(activity, "Exam countdown removed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        actionRow.addView(editBtn)
        actionRow.addView(deleteBtn)
        root.addView(actionRow)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val width = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun showAddEditExamModal(existing: ExamCountdown?) {
        val activity = host
        val themeCoordinator = activity.themeCoordinator
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val title = TextView(activity).apply {
            text = if (existing != null) "Edit Exam" else "Add Exam Countdown"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(title)

        // Exam Title Input
        val titleInput = EditText(activity).apply {
            hint = "Exam Name (e.g. Mathematics Final)"
            setText(existing?.title ?: "")
            setTextColor(themeCoordinator.textColor)
            setHintTextColor(host.tintedColor(themeCoordinator.textColor, 100))
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            textSize = 13.5f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        root.addView(titleInput)

        // Target Date Picker
        val cal = Calendar.getInstance()
        if (existing != null) {
            cal.timeInMillis = existing.targetTimestampMs
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 7) // default 1 week ahead
        }

        val dateBtn = TextView(activity).apply {
            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            text = "Exam Date: ${sdf.format(cal.time)}"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.primaryColor, 50), 12f)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        dateBtn.setOnClickListener {
            DatePickerDialog(
                activity,
                R.style.AmoledPickerDialogTheme,
                { _, year, month, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                    dateBtn.text = "Exam Date: ${sdf.format(cal.time)}"
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        root.addView(dateBtn)

        // Subject Link Picker
        val allSubjects = SubjectTagManager.getAllSubjects(activity)
        var selectedSubject: SubjectTag? = allSubjects.find { it.id == existing?.subjectId }

        val subjectBtn = TextView(activity).apply {
            text = "Subject: ${selectedSubject?.name ?: "General / No Subject"}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
            setOnClickListener {
                DeveloperToolsHelper.showThemedSubjectPickerModal(
                    activity = activity,
                    themeCoordinator = themeCoordinator,
                    title = "Link to Subject Tag",
                    includeGeneral = true,
                    includeCreateOption = false,
                    onSelected = { sub ->
                        selectedSubject = sub
                        text = "Subject: ${sub?.name ?: "General / No Subject"}"
                    }
                )
            }
        }
        root.addView(subjectBtn)

        // Notes Input
        val notesInput = EditText(activity).apply {
            hint = "Notes / Syllabus (optional)"
            setText(existing?.notes ?: "")
            setTextColor(themeCoordinator.textColor)
            setHintTextColor(host.tintedColor(themeCoordinator.textColor, 100))
            background = themeCoordinator.createGlassChip(host.tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }
        root.addView(notesInput)

        // Save Button
        val saveBtn = Button(activity).apply {
            text = if (existing != null) "Save Changes" else "Start Countdown"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val enteredTitle = titleInput.text.toString().trim()
                if (enteredTitle.isEmpty()) {
                    Toast.makeText(activity, "Please enter an exam title", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val targetMs = cal.timeInMillis
                val notesText = notesInput.text.toString().trim()
                val colorHex = selectedSubject?.colorHex

                if (existing != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val updated = existing.copy(
                        title = enteredTitle,
                        targetDateStr = sdf.format(Date(targetMs)),
                        targetTimestampMs = targetMs,
                        subjectId = selectedSubject?.id,
                        colorHex = colorHex,
                        notes = notesText.ifEmpty { null }
                    )
                    ExamCountdownManager.updateExam(activity, updated)
                    Toast.makeText(activity, "Exam countdown updated", Toast.LENGTH_SHORT).show()
                } else {
                    ExamCountdownManager.addExam(
                        context = activity,
                        title = enteredTitle,
                        targetTimestampMs = targetMs,
                        subjectId = selectedSubject?.id,
                        colorHex = colorHex,
                        notes = notesText
                    )
                    Toast.makeText(activity, "Exam countdown created", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
                activity.tabPageCache.remove(activity.statsTabKey(AppStatsTab.TIMELINE))
                activity.refreshStatsPanel()
                activity.navigateToPanel(AppPanel.STATS)
            }
        }
        root.addView(saveBtn)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val width = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }
}

