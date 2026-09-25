package com.madeby.JAI

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubjectDialogHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)

    fun showSubjectPickerDialog() {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(22), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(420)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "🏷️ Select Study Subject"
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(title)

            val subtitle = TextView(host).apply {
                text = "Tap to select · Hold any subject to delete"
                textSize = 12f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(subtitle)

            val scrollContainer = ScrollView(host).apply {
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(280)
                )
            }

            val subjectsListContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val currentSelectedSubj = SubjectTagManager.getSelectedSubject(host)
            val subjectsList = SubjectTagManager.getAllSubjects(host)
            for (subj in subjectsList) {
                val isSelected = subj.id == currentSelectedSubj.id
                val btn = Button(host).apply {
                    text = if (isSelected) "✓  ${subj.iconEmoji} ${subj.name}" else "${subj.iconEmoji} ${subj.name}"
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        val baseColor = try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                        setColor(baseColor)
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setStroke(dp(2), Color.WHITE)
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                    setOnClickListener {
                        SubjectTagManager.setSelectedSubject(host, subj.id)
                        dialog.dismiss()
                        host.buildFocusPanel()
                    }
                    setOnLongClickListener {
                        if (subj.id != "general") {
                            dialog.dismiss()
                            showDeleteSubjectConfirmDialog(subj)
                        } else {
                            Toast.makeText(host, "General subject cannot be deleted", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                }
                subjectsListContainer.addView(btn)
            }

            scrollContainer.addView(subjectsListContainer)
            container.addView(scrollContainer)

            val addBtn = Button(host).apply {
                text = "➕ Add Custom Subject"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(14), 0, 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showAddCustomSubjectDialog()
                }
            }
            container.addView(addBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showDeleteSubjectConfirmDialog(subj: SubjectTag) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(22), dp(22), dp(22), dp(22))
            }

            val title = TextView(host).apply {
                text = "Delete '${subj.name}' Subject?"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(8))
            }
            container.addView(title)

            val msg = TextView(host).apply {
                text = "Are you sure you want to remove ${subj.iconEmoji} ${subj.name}? Existing recorded stats for this subject will remain saved."
                textSize = 13f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(18))
            }
            container.addView(msg)

            val btnRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val cancelBtn = Button(host).apply {
                text = "Cancel"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, dp(6), 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showSubjectPickerDialog()
                }
            }

            val delBtn = Button(host).apply {
                text = "Delete"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EF4444"))
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(6), 0, 0, 0)
                }
                setOnClickListener {
                    SubjectTagManager.removeSubject(host, subj.id)
                    dialog.dismiss()
                    host.buildFocusPanel()
                    showSubjectPickerDialog()
                }
            }

            btnRow.addView(cancelBtn)
            btnRow.addView(delBtn)
            container.addView(btnRow)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showAddCustomSubjectDialog(onSubjectCreated: ((SubjectTag) -> Unit)? = null) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(20), dp(20), dp(20))
            }

            val title = TextView(host).apply {
                text = "➕ Add New Subject"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val input = android.widget.EditText(host).apply {
                hint = "Subject Name (max 25 chars)"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                filters = arrayOf(android.text.InputFilter.LengthFilter(25))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            container.addView(input)

            var selectedColorHex = SubjectTagManager.generateUniqueColor(host)

            val previewRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(14), 0, dp(8))
            }
            val colorPreviewCircle = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(selectedColorHex))
                }
            }
            previewRow.addView(colorPreviewCircle)

            val colorLabel = TextView(host).apply {
                text = "Subject Color: $selectedColorHex"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            previewRow.addView(colorLabel)
            container.addView(previewRow)

            // Preset Swatches Horizontal Row
            val swatchesScroll = android.widget.HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(10))
            }
            val swatchesLayout = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val presetColors = listOf("#6366F1", "#EC4899", "#F97316", "#06B6D4", "#10B981", "#EAB308", "#8B5CF6", "#EF4444", "#3B82F6", "#14B8A6")
            for (hex in presetColors) {
                val swatch = View(host).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        setStroke(dp(2), if (hex.equals(selectedColorHex, true)) Color.WHITE else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        selectedColorHex = hex
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(Color.parseColor(hex))
                        colorLabel.text = "Subject Color: $selectedColorHex"
                    }
                }
                swatchesLayout.addView(swatch)
            }
            swatchesScroll.addView(swatchesLayout)
            container.addView(swatchesScroll)

            // Custom Hue Color Slider Bar
            val hueLabel = TextView(host).apply {
                text = "🎨 Custom Hue Bar"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 11.5f
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(hueLabel)

            val hueSeekBar = android.widget.SeekBar(host).apply {
                max = 360
                progress = 240
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                        val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.85f, 0.90f))
                        selectedColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(colorInt)
                        colorLabel.text = "Subject Color: $selectedColorHex"
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            container.addView(hueSeekBar)

            val saveBtn = Button(host).apply {
                text = "Save Subject"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(themeCoordinator.primaryColor)
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(16), 0, 0)
                }
                setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val created = SubjectTagManager.addCustomSubject(host, name, "📚", selectedColorHex)
                        dialog.dismiss()
                        host.buildFocusPanel()
                        onSubjectCreated?.invoke(created)
                    }
                }
            }
            container.addView(saveBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showPieChartDetailsModal(initialDateKey: String = SubjectTagManager.getTodayKey()) {
        val dialog = Dialog(host, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeCoordinator.bgColor))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            try {
                val isLight = themeCoordinator.activeBgMode == "LIGHT"
                val decor = peekDecorView() ?: decorView
                androidx.core.view.WindowCompat.getInsetsController(this, decor).let { controller ->
                    controller.isAppearanceLightStatusBars = isLight
                    controller.isAppearanceLightNavigationBars = isLight
                }
            } catch (_: Exception) {}
        }
        var currentDateKey = initialDateKey
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val todayKey = SubjectTagManager.getTodayKey()

        val rootLayout = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(themeCoordinator.bgColor)
            setPadding(dp(16), host.getStatusBarHeight() + dp(4), dp(16), dp(16))
        }

        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        headerRow.addView(TextView(host).apply {
            text = "📊 Subject Details"
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val closeBtn = TextView(host).apply {
            text = "✕ Close"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f)
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        rootLayout.addView(headerRow)

        // Date Navigation Row (< 📅 16 Aug 2026 >)
        val dateNavRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        val prevBtn = TextView(host).apply {
            text = "◄"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        val dateTitleBtn = TextView(host).apply {
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, dp(8), 0) }
        }

        val nextBtn = TextView(host).apply {
            text = "►"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        dateNavRow.addView(prevBtn)
        dateNavRow.addView(dateTitleBtn)
        dateNavRow.addView(nextBtn)
        rootLayout.addView(dateNavRow)

        val scrollView = ScrollView(host).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val scrollContent = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        scrollView.addView(scrollContent)
        rootLayout.addView(scrollView)

        fun updateDateView() {
            val parsedDate = try { sdfKey.parse(currentDateKey) } catch (_: Exception) { Date() } ?: Date()
            val formattedTitle = if (currentDateKey == todayKey) "📅 Today (${sdfDisplay.format(parsedDate)})" else "📅 ${sdfDisplay.format(parsedDate)}"
            dateTitleBtn.text = formattedTitle

            val isToday = (currentDateKey == todayKey)
            nextBtn.alpha = if (isToday) 0.3f else 1.0f
            nextBtn.isEnabled = !isToday

            scrollContent.removeAllViews()

            val topChartCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(20)) }
            }

            val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val isDonut = sharedPrefs.safeBoolean("use_donut_chart", true)
            val largePieView = SubjectPieChartView(host).apply {
                primaryColor = themeCoordinator.primaryColor
                textColor = themeCoordinator.textColor
                isDonutMode = isDonut
                boxColor = if (themeCoordinator.isDarkMode()) (if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF111625.toInt()) else 0xFFFFFFFF.toInt()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340))
            }

            val (daySessions, _) = StatsEngine(host).dayBlocks(currentDateKey)
            val subjectMap = LinkedHashMap<String, Pair<SubjectTag, Long>>()

            // Aggregate focus durations directly from unified session logs
            for (s in daySessions) {
                if (s.subjectId != null || (!s.subjectName.isNullOrBlank() && s.subjectName != "Focus")) {
                    val subj = SubjectTagManager.resolveSubject(host, s.subjectId, s.subjectName, s.subjectColor)
                    val currentSecs = subjectMap[subj.id]?.second ?: 0L
                    subjectMap[subj.id] = subj to (currentSecs + s.secs)
                }
            }

            // Fallback to legacy SubjectTagManager if daySessions is empty
            if (subjectMap.isEmpty()) {
                val legacy = SubjectTagManager.getSubjectDurationsForDate(host, currentDateKey)
                for ((subId, secs) in legacy) {
                    val subj = SubjectTagManager.resolveSubject(host, subId)
                    val currentSecs = subjectMap[subj.id]?.second ?: 0L
                    subjectMap[subj.id] = subj to (currentSecs + secs)
                }
            }

            val subjectBreakDurations = SubjectTagManager.getSubjectBreakDurationsForDate(host, currentDateKey)

            val activeSubjectsSorted = subjectMap.values
                .filter { it.second >= 60L }
                .sortedByDescending { it.second }

            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()

            for ((subj, secs) in activeSubjectsSorted) {
                slicesList.add(SubjectPieChartView.PieSlice(subj.name, subj.iconEmoji, secs.toDouble(), subj.colorHex))
            }

            if (slicesList.isNotEmpty()) {
                largePieView.setData(slicesList)
                topChartCard.addView(largePieView)
            } else {
                topChartCard.addView(TextView(host).apply {
                    text = "No subject activity recorded for this day"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(40), 0, dp(40))
                })
            }

            scrollContent.addView(topChartCard)

            scrollContent.addView(TextView(host).apply {
                text = "Today's Sessions ($currentDateKey)"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(4), 0, 0, dp(12))
            })

            val totalSecsAll = subjectMap.values.sumOf { it.second }

            if (slicesList.isEmpty()) {
                val emptyCard = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(24), dp(20), dp(24))
                    gravity = Gravity.CENTER
                }
                emptyCard.addView(TextView(host).apply {
                    text = "No Subject Sessions"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                emptyCard.addView(TextView(host).apply {
                    text = "No study time with subject tags recorded for this day."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                })
                scrollContent.addView(emptyCard)
            } else {
                for (subjPair in activeSubjectsSorted) {
                    val (subj, secs) = subjPair
                    val matchedSessions = daySessions.filter {
                        (it.subjectId != null && it.subjectId == subj.id) ||
                        (it.subjectId == null && subj.id == "general")
                    }

                    val subjCard = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createCardBackground()
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
                    }

                    val headerRowSubj = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val colorCircle = FrameLayout(host).apply {
                        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }) }
                        layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(10), 0) }
                    }

                    headerRowSubj.addView(colorCircle)
                    headerRowSubj.addView(TextView(host).apply {
                        text = "${subj.iconEmoji} ${subj.name}"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 17f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    val pct = if (totalSecsAll > 0) ((secs.toDouble() / totalSecsAll.toDouble()) * 100).toInt() else 0
                    headerRowSubj.addView(TextView(host).apply {
                        text = "$pct% Share"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })

                    subjCard.addView(headerRowSubj)

                    val timingStr = if (matchedSessions.isNotEmpty()) {
                        val firstMs = matchedSessions.minOf { it.startMs }
                        val lastMs = matchedSessions.maxOf { it.endMs }
                        "${TimeFormat.formatWallClock(host, firstMs)} \u2013 ${TimeFormat.formatWallClock(host, lastMs)}"
                    } else if (daySessions.isNotEmpty()) {
                        val firstMs = daySessions.minOf { it.startMs }
                        val lastMs = daySessions.maxOf { it.endMs }
                        "${TimeFormat.formatWallClock(host, firstMs)} \u2013 ${TimeFormat.formatWallClock(host, lastMs)}"
                    } else {
                        "N/A"
                    }

                    val hrs = secs / 3600L
                    val mins = (secs % 3600L) / 60L
                    val remSecs = secs % 60L
                    val focusTimeStr = if (hrs > 0) "${hrs}h ${mins}m ${remSecs}s" else "${mins}m ${remSecs}s"

                    val bSecs = subjectBreakDurations[subj.id] ?: 0L
                    val bHrs = bSecs / 3600L
                    val bMins = (bSecs % 3600L) / 60L
                    val bRemSecs = bSecs % 60L
                    val breakTimeStr = if (bHrs > 0) "${bHrs}h ${bMins}m ${bRemSecs}s" else "${bMins}m ${bRemSecs}s"

                    val detailsText = TextView(host).apply {
                        text = "Study Duration: $focusTimeStr\nBreak Duration: $breakTimeStr\nTime Window: $timingStr"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 13.5f
                        setLineSpacing(dp(4).toFloat(), 1.1f)
                        setPadding(0, dp(10), 0, 0)
                    }
                    subjCard.addView(detailsText)

                    scrollContent.addView(subjCard)
                }
            }
        }

        prevBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDateKey = sdfKey.format(cal.time)
            updateDateView()
        }

        nextBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val newKey = sdfKey.format(cal.time)
            if (newKey <= todayKey) {
                currentDateKey = newKey
                updateDateView()
            }
        }

        dateTitleBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            DatePickerDialog(host, { _, y: Int, m: Int, d: Int ->
                val selCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                val selKey = sdfKey.format(selCal.time)
                if (selKey <= todayKey) {
                    currentDateKey = selKey
                    updateDateView()
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        updateDateView()
        dialog.setContentView(rootLayout)
        dialog.show()
    }
}
