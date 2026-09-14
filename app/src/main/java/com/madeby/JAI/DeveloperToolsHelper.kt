package com.madeby.JAI

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Modern Developer & Debug Suite 2.0 for StudyTimer.
 * Gated strictly behind developer unlock flags, with modular isolated sections for:
 * 1. Manual Session Logger & Mocking:
 *    - Start Time & End Time Pickers (with real-time duration computation & validation)
 *    - Custom subjects, untagged general focus, custom date pickers
 * 2. Database & State Management:
 *    - Realistic, consistent mock data generator (Deterministic template, habitual variance, rest days)
 *    - Live state, preset demo seeds, granular resets
 * 3. Cloud Sync & Conflict Simulation (Force push/pull, simulate conflict)
 * 4. Trigger & Alert Testing (Celebrations, alarms, audio/haptic chimes)
 * 5. UI & Theme Overrides (Edge cases, AMOLED switcher)
 */
object DeveloperToolsHelper {

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun getPickerThemeRes(): Int {
        return R.style.AmoledPickerDialogTheme
    }

    fun buildDevCard(activity: MainActivity, themeCoordinator: ThemeCoordinator): View {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(32f)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(14), 0, dp(14))
            }
        }

        // Header Title
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        header.addView(TextView(activity).apply {
            text = "🛠️ DEV SUITE 2.0"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val openFullBtn = TextView(activity).apply {
            text = "Open Full Console ↗"
            textSize = 11.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { showFullDevConsoleDialog(activity, themeCoordinator) }
        }
        header.addView(openFullBtn)
        container.addView(header)

        // Quick Action Row
        val quickGrid = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }

        fun quickActionBtn(label: String, colorHex: Int, onClick: () -> Unit): View {
            return Button(activity).apply {
                text = label
                textSize = 11f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(colorHex)
                }
                setPadding(dp(4), dp(4), dp(4), dp(4))
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener { onClick() }
            }
        }

        quickGrid.addView(quickActionBtn("+25m Focus", themeCoordinator.primaryColor) {
            fastForwardSession(activity, 25 * 60L)
        })
        quickGrid.addView(quickActionBtn("Adjust Time", Color.parseColor("#38BDF8")) {
            showAdjustTodayTimeDialog(activity, themeCoordinator, isDeveloperExtended = true)
        })
        quickGrid.addView(quickActionBtn("Manual Log", Color.parseColor("#8B5CF6")) {
            showManualSessionLoggerDialog(activity, themeCoordinator)
        })
        quickGrid.addView(quickActionBtn("Seed 7D", Color.parseColor("#10B981")) {
            seedRealisticHistory(activity, days = 7, deterministicSeed = 42L)
        })

        container.addView(quickGrid)
        return container
    }

    fun showFullDevConsoleDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }
        val displayMetrics = activity.resources.displayMetrics

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(30f)
            setPadding(dp(20), dp(20), dp(20), dp(16))
        }

        // Title bar
        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }
        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(activity).apply {
            text = "⚡ Developer Architecture Suite"
            setTextColor(themeCoordinator.textColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        titleCol.addView(TextView(activity).apply {
            text = "Diagnostics, Custom Mocking & Edge-case Testing"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11.5f
            alpha = 0.85f
            setPadding(0, dp(2), 0, 0)
        })
        titleRow.addView(titleCol)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { dialog.dismiss() }
        }
        titleRow.addView(closeBtn)
        root.addView(titleRow)

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (displayMetrics.heightPixels * 0.72f).toInt()
            )
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }

        // Section Builder Helper
        fun addSection(title: String, icon: String, block: LinearLayout.() -> Unit) {
            val sectionHeader = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(14), dp(4), dp(8))
            }
            sectionHeader.addView(TextView(activity).apply {
                text = "$icon $title"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            content.addView(sectionHeader)

            val sectionCard = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(24f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            sectionCard.block()
            content.addView(sectionCard)
        }

        fun devButton(label: String, subtitle: String? = null, colorHex: Int = themeCoordinator.primaryColor, onClick: () -> Unit): View {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(colorHex, 60), 16f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(4), 0, dp(4))
                }
                addView(TextView(activity).apply {
                    text = label
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                if (subtitle != null) {
                    addView(TextView(activity).apply {
                        text = subtitle
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 11f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                setOnClickListener {
                    onClick()
                }
            }
        }

        // 1. Manual Session Logger & Mocking
        addSection("MANUAL SESSION LOGGER & MOCKING", "✍️") {
            addView(devButton("⏱️ Adjust Focus & Break Time (Extended)", "Pick any date, add/deduct/set focus & break records with full accuracy", Color.parseColor("#38BDF8")) {
                dialog.dismiss()
                showAdjustTodayTimeDialog(activity, themeCoordinator, isDeveloperExtended = true)
            })

            addView(devButton("✍️ Custom Manual Session Builder", "Start & End Time pickers, real-time duration, custom or untagged subject") {
                dialog.dismiss()
                showManualSessionLoggerDialog(activity, themeCoordinator)
            })

            val ffRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, dp(6))
            }
            fun ffBtn(txt: String, secs: Long) = Button(activity).apply {
                text = txt
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener {
                    fastForwardSession(activity, secs)
                    Toast.makeText(activity, "Fast-forwarded $txt!", Toast.LENGTH_SHORT).show()
                }
            }
            ffRow.addView(ffBtn("+5m", 300L))
            ffRow.addView(ffBtn("+25m", 1500L))
            ffRow.addView(ffBtn("+1h", 3600L))
            addView(ffRow)

            addView(devButton("Generate Realistic 7-Day History (Deterministic)", "Consistent habits (Math 35%, Physics 30%, Chemistry 20%, Revision 15%)") {
                seedRealisticHistory(activity, days = 7, deterministicSeed = 707L)
                Toast.makeText(activity, "Generated 7-day realistic logs", Toast.LENGTH_SHORT).show()
                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })

            addView(devButton("Generate Heavy 30-Day History (Deterministic)", "Full month heatmap, natural weekend rest patterns, consistent subject roster") {
                seedRealisticHistory(activity, days = 30, deterministicSeed = 3030L)
                Toast.makeText(activity, "Generated 30-day realistic logs", Toast.LENGTH_SHORT).show()
                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })
        }

        // 2. Planner & Habits Lab
        addSection("PLANNER & HABITS LAB", "📋") {
            addView(devButton("✍️ Custom Historical Planner Snapshot Builder", "Date picker, select custom goals/habits & status for ANY past date", Color.parseColor("#8B5CF6")) {
                dialog.dismiss()
                showCustomPlannerSnapshotLoggerDialog(activity, themeCoordinator)
            })

            addView(devButton("🌱 Seed Sample Daily Goals & Habits", "Populates 4 structured planner goals linked with subjects") {
                seedSampleGoals(activity)
                Toast.makeText(activity, "Seeded sample daily planner goals!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🌅 Simulate Next-Day Rollover", "Snapshots today's completion status & resets habits for a new day") {
                simulatePlannerDayRollover(activity)
                Toast.makeText(activity, "Simulated rollover: Snapshotted today & reset habits for a new day!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🔥 Seed 14-Day Realistic Habit History", "Generates 14 days of daily habit snapshots for streaks & matrices") {
                seedRealisticHabitHistory(activity, days = 14)
                Toast.makeText(activity, "Generated 14 days of habit history!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("✅ Mark All Today's Habits Completed", "Sets all current goals as done to test streaks & celebrations") {
                markAllTodayGoals(activity, completed = true)
                Toast.makeText(activity, "Marked all habits completed for today!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🔄 Reset Today's Habits to Incomplete", "Unchecks all current goals for today") {
                markAllTodayGoals(activity, completed = false)
                Toast.makeText(activity, "Reset all habits to incomplete for today!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🗑️ Wipe Planner & Habit History", "Clears all historical snapshots and resets active goals", Color.parseColor("#EF4444")) {
                AlertDialog.Builder(activity)
                    .setTitle("Wipe Planner History?")
                    .setMessage("This will delete all past habit completion snapshots and reset current goals.")
                    .setPositiveButton("WIPE PLANNER") { _, _ ->
                        wipePlannerData(activity)
                        Toast.makeText(activity, "Planner & habit history cleared!", Toast.LENGTH_SHORT).show()
                        activity.refreshStatsPanel()
                        activity.tabPageCache.clear()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })
        }

        // 2. Database & State Management
        addSection("DATABASE & STATE MANAGEMENT", "🗄️") {
            val liveStatsText = TextView(activity).apply {
                val sessionsCount = TimelineLogger.load(activity).size
                val subjectsCount = SubjectTagManager.getAllSubjects(activity).size
                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val streak = prefs.safeInt("current_streak", 0)
                val goalMins = prefs.safeLong("dailyGoalMinutes", 120L)
                text = "📊 Live State:\n• Timeline Entries: $sessionsCount\n• Registered Subjects: $subjectsCount\n• Current Streak: $streak days\n• Daily Goal: ${goalMins}m"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 12f
                typeface = Typeface.MONOSPACE
                background = themeCoordinator.createGlassChip(0x22FFFFFF.toInt(), 12f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            addView(liveStatsText)

            addView(devButton("Seed Preset: Standard Student (4 Subjects)", "Pre-populates Math, Physics, Chemistry, and Revision with balanced ratios") {
                seedPresetStandardStudent(activity)
                Toast.makeText(activity, "Seeded Standard Student preset!", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.recalculateStreak()
                activity.tabPageCache.clear()
            })

            addView(devButton("Reset Streak to 0", "Sets streak counters and calculation dates to zero") {
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .putInt("current_streak", 0)
                    .putLong("streak_last_calculated", 0L)
                    .apply()
                activity.recalculateStreak()
                Toast.makeText(activity, "Streak reset to 0", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Clear Today's Logs Only", "Removes today's focus/break entries without touching past days", Color.parseColor("#EF4444")) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
                    remove("${todayStr}_focus_total")
                    remove("${todayStr}_break_total")
                    remove("${todayStr}_focus_manual")
                    remove("${todayStr}_break_manual")
                }.apply()
                TimelineLogger.deleteDay(activity, todayStr)
                SubjectTagManager.clearTodaySubjectDurations(activity, todayStr)
                activity.statsDirty = true
                activity.recalculateStreak()
                Toast.makeText(activity, "Cleared today's entries", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Wipe Custom Subjects", "Removes user-created subjects and restores defaults") {
                activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE).edit()
                    .putString("custom_subjects_json", "[]")
                    .apply()
                Toast.makeText(activity, "Restored default subjects", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })

            addView(devButton("Wipe All Local Database Logs", "Completely erases timeline and resets all statistics", Color.parseColor("#EF4444")) {
                AlertDialog.Builder(activity)
                    .setTitle("Wipe Entire Database?")
                    .setMessage("This will permanently delete all local timeline sessions, calendar history, and subject counters.")
                    .setPositiveButton("WIPE EVERYTHING") { _, _ ->
                        TimelineLogger.importRaw(activity, "[]")
                        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                        for (k in prefs.all.keys) {
                            if (k.endsWith("_focus_total") || k.endsWith("_break_total") || k.startsWith("subject_")) {
                                editor.remove(k)
                            }
                        }
                        editor.putInt("current_streak", 0).apply()
                        activity.statsDirty = true
                        activity.recalculateStreak()
                        activity.tabPageCache.clear()
                        Toast.makeText(activity, "Database wiped cleanly", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })
        }

        // 3. Sync & Cloud Conflict Simulation
        addSection("SYNC & CLOUD CONFLICT SIMULATION", "☁️") {
            addView(devButton("Simulate Remote Cloud Conflict", "Triggers the 3-option conflict resolution dialog with a mock newer cloud snapshot", Color.parseColor("#F59E0B")) {
                dialog.dismiss()
                simulateCloudConflict(activity)
            })

            addView(devButton("Force Immediate Cloud Push", "Uploads current local state directly to cloud storage") {
                Thread {
                    kotlinx.coroutines.runBlocking {
                        val success = CloudSyncManager.syncDataToCloud(activity, force = true)
                        activity.runOnUiThread {
                            Toast.makeText(activity, if (success) "Cloud push succeeded" else "Cloud push failed (Check network/login)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            })

            addView(devButton("Force Cloud Pull & Restore", "Downloads and overwrites with latest cloud copy") {
                Thread {
                    kotlinx.coroutines.runBlocking {
                        val success = CloudSyncManager.restoreDataFromCloud(activity)
                        activity.runOnUiThread {
                            if (success) {
                                Toast.makeText(activity, "Cloud restored successfully", Toast.LENGTH_SHORT).show()
                                activity.tabPageCache.clear()
                                activity.recreate()
                            } else {
                                Toast.makeText(activity, "No cloud backup found or pull failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }.start()
            })
        }

        // 4. Notifications & Celebration Triggers
        addSection("NOTIFICATIONS & CELEBRATION TRIGGERS", "🎉") {
            addView(devButton("Trigger Goal Celebration Banner", "Launches the interactive particle confetti celebration modal") {
                CelebrationEngine.showCelebrationDialog(activity, isGoalAchieved = true, streak = 5)
            })

            addView(devButton("Trigger Milestone Streak Banner", "Launches 14-day streak celebration particle modal") {
                CelebrationEngine.showCelebrationDialog(activity, isGoalAchieved = false, streak = 14)
            })

            addView(devButton("Trigger Haptic Pulse", "Fires study completion haptic vibration waveform") {
                try {
                    activity.window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (_: Exception) {}
            })
        }

        // 5. UI & Theme Overrides
        addSection("UI & EDGE CASE PREVIEWS", "🎨") {
            addView(devButton("Force Toggle AMOLED / Light Mode", "Instantly swaps color matrix between pitch black and light") {
                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val current = prefs.getString("activeBgMode", "OLED")
                val next = if (current == "LIGHT") "OLED" else "LIGHT"
                prefs.edit().putString("activeBgMode", next).apply()
                themeCoordinator.applyThemeCoordinates()
                activity.tabPageCache.clear()
                dialog.dismiss()
                activity.recreate()
            })

            addView(devButton("Inject Max-Length Subject Names", "Creates extra-long named subject to test legend & chart clipping") {
                val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                val customJson = subPrefs.getString("custom_subjects_json", "[]") ?: "[]"
                val arr = org.json.JSONArray(customJson)
                val longSubject = JSONObject().apply {
                    put("id", "custom_long_${System.currentTimeMillis()}")
                    put("name", "Advanced Quantum Thermodynamics & Electro-Optics Lab II")
                    put("iconEmoji", "🔬")
                    put("colorHex", "#EC4899")
                }
                arr.put(longSubject)
                subPrefs.edit().putString("custom_subjects_json", arr.toString()).apply()
                Toast.makeText(activity, "Inserted long subject name test", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })
        }

        scroll.addView(content)
        root.addView(scroll)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()
    }

    /**
     * Precise Manual Session Logger:
     * - Start Time & End Time Pickers with real-time dynamic duration calculation
     * - Validates that End Time is always later than Start Time
     * - Select registered subject, create a new custom subject, or select "Untagged / General Focus"
     * - Uses custom AMOLED dialog & picker themes to prevent system white/purple clashes
     */
    fun showManualSessionLoggerDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        root.addView(TextView(activity).apply {
            text = "✍️ Manual Session Logger"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        })

        val subjects = SubjectTagManager.getAllSubjects(activity)
        var selectedSubject: SubjectTag? = subjects.firstOrNull() // null = untagged general focus

        val subjectPickerBtn = TextView(activity).apply {
            text = "Tag: ${selectedSubject?.iconEmoji ?: "⏱"} ${selectedSubject?.name ?: "No Subject Tag / General"}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 80), 14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val customInputContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        val customNameEdit = EditText(activity).apply {
            hint = "Enter custom subject name"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        customInputContainer.addView(customNameEdit)

        subjectPickerBtn.setOnClickListener {
            val options = mutableListOf("⏱ No Subject Tag / General Focus", "➕ Create New Subject...")
            options.addAll(subjects.map { "${it.iconEmoji} ${it.name}" })

            AlertDialog.Builder(activity, getPickerThemeRes())
                .setTitle("Select or Create Subject")
                .setItems(options.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> {
                            selectedSubject = null
                            customInputContainer.visibility = View.GONE
                            subjectPickerBtn.text = "Tag: ⏱ No Subject Tag / General Focus"
                        }
                        1 -> {
                            selectedSubject = null
                            customInputContainer.visibility = View.VISIBLE
                            subjectPickerBtn.text = "Tag: ➕ Custom Subject (Type below)"
                        }
                        else -> {
                            selectedSubject = subjects[which - 2]
                            customInputContainer.visibility = View.GONE
                            subjectPickerBtn.text = "Tag: ${selectedSubject?.iconEmoji} ${selectedSubject?.name}"
                        }
                    }
                }
                .show()
        }
        root.addView(subjectPickerBtn)
        root.addView(customInputContainer)

        // Date, Start Time & End Time Setup
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 45) // Default 45m session
        }

        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val datePickerBtn = TextView(activity).apply {
            text = "📅 Date: ${dateFmt.format(startCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, 0)
            }
        }

        val timeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val startTimeBtn = TextView(activity).apply {
            text = "⏰ Start: ${timeFmt.format(startCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(4), 0)
            }
        }

        val endTimeBtn = TextView(activity).apply {
            text = "🏁 End: ${timeFmt.format(endCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, 0, 0)
            }
        }

        timeRow.addView(startTimeBtn)
        timeRow.addView(endTimeBtn)

        val durationSummaryText = TextView(activity).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(4), dp(8), dp(4), dp(12))
        }

        fun updateDurationPreview(): Long {
            val diffMs = endCal.timeInMillis - startCal.timeInMillis
            val diffMins = diffMs / 60000L
            if (diffMins <= 0) {
                durationSummaryText.text = "⚠️ End time must be after start time!"
                durationSummaryText.setTextColor(Color.parseColor("#EF4444"))
                return 0L
            } else {
                val h = diffMins / 60
                val m = diffMins % 60
                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                durationSummaryText.text = "✨ Calculated Focus Duration: $durStr ($diffMins mins)"
                durationSummaryText.setTextColor(themeCoordinator.primaryColor)
                return diffMins * 60L
            }
        }
        updateDurationPreview()

        datePickerBtn.setOnClickListener {
            val dpd = DatePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, y, m, d ->
                    startCal.set(Calendar.YEAR, y)
                    startCal.set(Calendar.MONTH, m)
                    startCal.set(Calendar.DAY_OF_MONTH, d)
                    endCal.set(Calendar.YEAR, y)
                    endCal.set(Calendar.MONTH, m)
                    endCal.set(Calendar.DAY_OF_MONTH, d)
                    datePickerBtn.text = "📅 Date: ${dateFmt.format(startCal.time)}"
                    updateDurationPreview()
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
            )
            dpd.setOnShowListener {
                dpd.getButton(DatePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                dpd.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            dpd.show()
        }

        startTimeBtn.setOnClickListener {
            val tpd = TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    startCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    startCal.set(Calendar.MINUTE, minute)
                    startCal.set(Calendar.SECOND, 0)
                    startTimeBtn.text = "⏰ Start: ${timeFmt.format(startCal.time)}"
                    updateDurationPreview()
                },
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                false
            )
            tpd.setOnShowListener {
                tpd.getButton(TimePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                tpd.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            tpd.show()
        }

        endTimeBtn.setOnClickListener {
            val tpd = TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    endCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    endCal.set(Calendar.MINUTE, minute)
                    endCal.set(Calendar.SECOND, 0)
                    endTimeBtn.text = "🏁 End: ${timeFmt.format(endCal.time)}"
                    updateDurationPreview()
                },
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                false
            )
            tpd.setOnShowListener {
                tpd.getButton(TimePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                tpd.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            tpd.show()
        }

        root.addView(datePickerBtn)
        root.addView(timeRow)
        root.addView(durationSummaryText)

        // Quick Preset Durations
        val quickDurRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }
        fun durPresetBtn(mins: Long) = Button(activity).apply {
            text = "+${mins}m"
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = themeCoordinator.createButtonBackground(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            setOnClickListener {
                endCal.timeInMillis = startCal.timeInMillis + (mins * 60000L)
                endTimeBtn.text = "🏁 End: ${timeFmt.format(endCal.time)}"
                updateDurationPreview()
            }
        }
        quickDurRow.addView(durPresetBtn(25L))
        quickDurRow.addView(durPresetBtn(45L))
        quickDurRow.addView(durPresetBtn(60L))
        quickDurRow.addView(durPresetBtn(90L))
        root.addView(quickDurRow)

        // Insert Button
        val insertBtn = Button(activity).apply {
            text = "LOG SESSION INTO DATABASE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val focusSecs = updateDurationPreview()
                if (focusSecs <= 0) {
                    Toast.makeText(activity, "Invalid duration: End time must be after Start time!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val inputName = customNameEdit.text.toString().trim()
                var finalSubId: String? = null
                var finalSubName: String? = null
                var finalSubColor: String? = null

                if (selectedSubject != null) {
                    finalSubId = selectedSubject!!.id
                    finalSubName = selectedSubject!!.name
                    finalSubColor = selectedSubject!!.colorHex
                } else if (inputName.isNotEmpty()) {
                    // Create and register new custom subject
                    val newId = "custom_${System.currentTimeMillis()}"
                    val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                    val customJson = subPrefs.getString("custom_subjects_json", "[]") ?: "[]"
                    val arr = JSONArray(customJson)
                    arr.put(JSONObject().apply {
                        put("id", newId)
                        put("name", inputName)
                        put("iconEmoji", "📚")
                        put("colorHex", "#38BDF8")
                    })
                    subPrefs.edit().putString("custom_subjects_json", arr.toString()).apply()
                    finalSubId = newId
                    finalSubName = inputName
                    finalSubColor = "#38BDF8"
                }

                val startMs = startCal.timeInMillis
                val endMs = endCal.timeInMillis
                val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startMs))

                val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val curTotal = sharedPrefs.getLong("${dayKey}_focus_total", 0L)
                sharedPrefs.edit()
                    .putLong("${dayKey}_focus_total", curTotal + focusSecs)
                    .putLong("last_data_modified_timestamp", System.currentTimeMillis())
                    .apply()

                if (finalSubId != null) {
                    SubjectTagManager.recordSubjectStudyTime(activity, finalSubId, focusSecs, dayKey)
                }

                TimelineLogger.recordRaw(
                    context = activity,
                    state = "STUDYING",
                    timestamp = startMs,
                    subId = finalSubId,
                    subName = finalSubName,
                    subColor = finalSubColor
                )
                TimelineLogger.recordRaw(
                    context = activity,
                    state = "IDLE",
                    timestamp = endMs
                )

                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
                Toast.makeText(activity, "Successfully logged ${focusSecs / 60}m session for $dayKey!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        root.addView(insertBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun fastForwardSession(activity: MainActivity, extraSecs: Long) {
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val now = System.currentTimeMillis()

        sharedPrefs.edit()
            .putLong("${todayStr}_focus_total", currentFocus + extraSecs)
            .putLong("last_data_modified_timestamp", now)
            .apply()

        val selectedSubject = SubjectTagManager.getSelectedSubject(activity)
        SubjectTagManager.recordSubjectStudyTime(activity, selectedSubject.id, extraSecs, todayStr)

        val startMs = now - (extraSecs * 1000L)
        TimelineLogger.recordRaw(
            context = activity,
            state = "STUDYING",
            timestamp = startMs,
            subId = selectedSubject.id,
            subName = selectedSubject.name,
            subColor = selectedSubject.colorHex
        )
        TimelineLogger.recordRaw(
            context = activity,
            state = "IDLE",
            timestamp = now
        )

        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
    }

    /**
     * Deterministic, Realistic Mock Data Generator:
     * - Fixed Subject Roster: Math (35%), Physics (30%), Chemistry (20%), Revision/General (15%)
     * - Baseline daily routine (~3h to 4.5h) with Gaussian/normal variance (±15-25m)
     * - Realistic habit patterns: Sundays have lighter rest sessions (1h - 1.5h)
     * - Deterministic pseudo-random seed ensures consistent updates rather than wild disjunctions
     */
    private fun seedRealisticHistory(activity: MainActivity, days: Int, deterministicSeed: Long = 42L) {
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Ensure fixed core roster exists
        val coreSubjects = listOf(
            SubjectTag("math", "Mathematics", "📐", "#10B981"),
            SubjectTag("physics", "Physics", "⚛️", "#8B5CF6"),
            SubjectTag("chemistry", "Chemistry", "🧪", "#EC4899"),
            SubjectTag("revision", "Practice & Revision", "📝", "#F59E0B")
        )

        val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
        val customArray = JSONArray().apply {
            for (s in coreSubjects) {
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("iconEmoji", s.iconEmoji)
                    put("colorHex", s.colorHex)
                })
            }
        }
        subPrefs.edit().putString("custom_subjects_json", customArray.toString()).apply()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val timelineList = ArrayList<TimelineEntry>()
        val rng = Random(deterministicSeed)

        for (i in days downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val dateStr = sdf.format(cal.time)

            // Base daily focus: 3.5h (210 mins) on weekdays ± 20 mins variance; 1.2h (72 mins) on Sundays
            val baseMins = if (isSunday) 75L else 210L
            val varianceMins = rng.nextLong(-20, 25)
            val totalDailyFocusSecs = (baseMins + varianceMins).coerceAtLeast(45L) * 60L
            val totalDailyBreakSecs = (totalDailyFocusSecs * 0.20f).toLong()

            editor.putLong("${dateStr}_focus_total", totalDailyFocusSecs)
            editor.putLong("${dateStr}_break_total", totalDailyBreakSecs)

            // Distribution weights: Math 35%, Physics 30%, Chemistry 20%, Revision 15%
            val mathSecs = (totalDailyFocusSecs * 0.35f).toLong()
            val physSecs = (totalDailyFocusSecs * 0.30f).toLong()
            val chemSecs = (totalDailyFocusSecs * 0.20f).toLong()
            val revSecs = (totalDailyFocusSecs - mathSecs - physSecs - chemSecs).coerceAtLeast(0L)

            val sessionSchedule = listOf(
                Pair(coreSubjects[0], mathSecs),
                Pair(coreSubjects[1], physSecs),
                Pair(coreSubjects[2], chemSecs),
                Pair(coreSubjects[3], revSecs)
            ).filter { it.second > 60L }

            var currentSessionStartCal = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            for ((subj, durationSecs) in sessionSchedule) {
                val startMs = currentSessionStartCal.timeInMillis
                val endMs = startMs + (durationSecs * 1000L)

                timelineList.add(
                    TimelineEntry(
                        timestamp = startMs,
                        state = "STUDYING",
                        subId = subj.id,
                        subName = subj.name,
                        subColor = subj.colorHex
                    )
                )
                timelineList.add(
                    TimelineEntry(
                        timestamp = endMs,
                        state = "IDLE"
                    )
                )

                SubjectTagManager.recordSubjectStudyTime(activity, subj.id, durationSecs, dateStr)

                // 15-minute natural break before next session
                currentSessionStartCal.timeInMillis = endMs + (15 * 60 * 1000L)
            }
        }

        editor.putInt("current_streak", days.coerceAtLeast(1))
        editor.putLong("last_data_modified_timestamp", System.currentTimeMillis())
        editor.apply()

        timelineList.sortBy { it.timestamp }
        TimelineLogger.importRaw(activity, timelineToJsonString(timelineList))
    }

    private fun seedPresetStandardStudent(activity: MainActivity) {
        seedRealisticHistory(activity, days = 14, deterministicSeed = 101L)
    }

    private fun simulateCloudConflict(activity: MainActivity) {
        val mockCloudRecord = JSONObject().apply {
            put("user_id", AuthManager.getUserId(activity) ?: "mock_user_123")
            put("user_name", "Cloud Student (Mock)")
            put("updated_at", System.currentTimeMillis() + 3600000L * 24L) // 1 day in the future
            put("last_modified_timestamp", System.currentTimeMillis() + 3600000L * 24L)
            put("prefs_data", JSONObject().apply {
                put("current_streak", 42)
                put("dailyGoalMinutes", 180L)
            }.toString())
            put("timeline_data", "[]")
        }

        val localTs = BackupManager(activity).getLastModifiedTimestamp()
        val cloudTs = System.currentTimeMillis() + 3600000L * 24L

        activity.showSyncConflictDialog(localTs, cloudTs, mockCloudRecord)
    }

    /**
     * Dialog for adjusting study focus and break time.
     * In Normal mode (for standard users), it accurately adjusts TODAY's focus or break time.
     * In Developer mode, its capabilities are extended to allow selecting ANY date (historical/future)
     * and performing Add, Deduct, or Direct Set adjustments.
     */
    fun showAdjustTodayTimeDialog(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        isDeveloperExtended: Boolean = false,
        initialDate: String? = null
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
        val displayMetrics = activity.resources.displayMetrics

        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var targetDateStr = initialDate ?: todayStr

        var isFocusCategory = true // true = Study Focus, false = Break Time
        var actionMode = 0 // 0 = Add, 1 = Deduct, 2 = Set (dev only)

        val subjects = SubjectTagManager.getAllSubjects(activity)
        var selectedSubject: SubjectTag? = subjects.firstOrNull()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        // Title Bar with Close ✕ button
        val titleBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        val titleText = TextView(activity).apply {
            text = if (isDeveloperExtended) "🛠️ Adjust Time & Data (Extended)" else "⏱️ Adjust Today's Time"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        titleBar.addView(titleText)
        titleBar.addView(closeBtn)
        root.addView(titleBar)

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (displayMetrics.heightPixels * 0.72f).toInt()
            )
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        // 1. Category Switcher: Focus vs Break
        val categoryRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 14f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val focusTab = TextView(activity).apply {
            text = "⏱️ Study Focus"
            textSize = 12.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val breakTab = TextView(activity).apply {
            text = "☕ Break Time"
            textSize = 12.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        categoryRow.addView(focusTab)
        categoryRow.addView(breakTab)
        content.addView(categoryRow)

        // 2. Date Information / Selector
        val dateCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val dateLabel = TextView(activity).apply {
            textSize = 12f
            setTextColor(themeCoordinator.textColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        dateCard.addView(dateLabel)

        var updateUI: () -> Unit = {}

        if (isDeveloperExtended) {
            val pickDateBtn = TextView(activity).apply {
                text = "📅 Pick Date ▾"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#38BDF8"))
                }
                setPadding(dp(10), dp(6), dp(10), dp(6))
                setOnClickListener {
                    val cal = Calendar.getInstance()
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(targetDateStr)?.let {
                            cal.time = it
                        }
                    } catch (_: Exception) {}

                    DatePickerDialog(
                        activity,
                        getPickerThemeRes(),
                        { _, year, month, dayOfMonth ->
                            targetDateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            activity.runOnUiThread { updateUI() }
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            }
            dateCard.addView(pickDateBtn)
        }
        content.addView(dateCard)

        // 3. Action Mode Toggle (Add vs Deduct vs Set)
        val modeToggleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 14f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val addBtn = TextView(activity).apply {
            text = "➕ Add"
            textSize = 11.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val deductBtn = TextView(activity).apply {
            text = "➖ Deduct"
            textSize = 11.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val setBtn = TextView(activity).apply {
            text = "🎯 Set Total"
            textSize = 11.5f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            visibility = if (isDeveloperExtended) View.VISIBLE else View.GONE
        }

        modeToggleRow.addView(addBtn)
        modeToggleRow.addView(deductBtn)
        if (isDeveloperExtended) modeToggleRow.addView(setBtn)
        content.addView(modeToggleRow)

        // Preset Chips Container
        val presetContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        content.addView(presetContainer)

        // Minutes Input Field
        val minutesInput = EditText(activity).apply {
            hint = "Enter minutes (e.g. 30)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("30")
            setTextColor(themeCoordinator.textColor)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            textSize = 14.5f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
        }
        content.addView(minutesInput)

        // Subject Selector (Only visible for Study Focus)
        val subjectPickerBtn = TextView(activity).apply {
            text = "Subject Tag: ${selectedSubject?.iconEmoji ?: "⏱"} ${selectedSubject?.name ?: "General Focus"}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        subjectPickerBtn.setOnClickListener {
            val options = mutableListOf("⏱ General Focus (Untagged)")
            options.addAll(subjects.map { "${it.iconEmoji} ${it.name}" })
            AlertDialog.Builder(activity, getPickerThemeRes())
                .setTitle("Attribute to Subject")
                .setItems(options.toTypedArray()) { _, which ->
                    if (which == 0) {
                        selectedSubject = null
                        subjectPickerBtn.text = "Subject Tag: ⏱ General Focus (Untagged)"
                    } else {
                        selectedSubject = subjects[which - 1]
                        subjectPickerBtn.text = "Subject Tag: ${selectedSubject?.iconEmoji} ${selectedSubject?.name}"
                    }
                    updateUI()
                }
                .show()
        }
        content.addView(subjectPickerBtn)

        // Live Calculation Summary Preview Card
        val summaryCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(18f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }

        val summaryCurrentText = TextView(activity).apply {
            textSize = 12f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, dp(4))
        }

        val summaryResultText = TextView(activity).apply {
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        summaryCard.addView(summaryCurrentText)
        summaryCard.addView(summaryResultText)
        content.addView(summaryCard)

        // Apply Button
        val applyBtn = Button(activity).apply {
            text = "APPLY ADJUSTMENT"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
        }
        content.addView(applyBtn)

        scroll.addView(content)
        root.addView(scroll)

        updateUI = {
            val isTargetToday = targetDateStr == todayStr

            // Calculate current effective focus & break times
            val currentStoredFocus = sharedPrefs.getLong("${targetDateStr}_focus_total", 0L)
            val runningStudy = if (isTargetToday) activity.accumulatedStudy else 0L
            val effectiveFocusSecs = currentStoredFocus + runningStudy
            val effectiveFocusMins = effectiveFocusSecs / 60L

            val currentStoredBreak = sharedPrefs.getLong("${targetDateStr}_break_total", 0L)
            val runningBreak = if (isTargetToday) activity.currentBreakSeconds else 0L
            val effectiveBreakSecs = currentStoredBreak + runningBreak
            val effectiveBreakMins = effectiveBreakSecs / 60L

            // Date label
            val dateDisplay = try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(targetDateStr)
                SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(parsed ?: Date())
            } catch (_: Exception) {
                targetDateStr
            }

            dateLabel.text = if (isTargetToday) {
                "📅 Today • $dateDisplay"
            } else {
                "📅 Target Date • $dateDisplay ($targetDateStr)"
            }

            // Category Tab Styles
            if (isFocusCategory) {
                focusTab.setTextColor(Color.WHITE)
                focusTab.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                breakTab.setTextColor(tintedColor(themeCoordinator.textColor, 140))
                breakTab.background = null
                subjectPickerBtn.visibility = View.VISIBLE
            } else {
                breakTab.setTextColor(Color.WHITE)
                breakTab.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.parseColor("#F43F5E"))
                }
                focusTab.setTextColor(tintedColor(themeCoordinator.textColor, 140))
                focusTab.background = null
                subjectPickerBtn.visibility = View.GONE
            }

            // Action Mode Tab Styles
            val activeColor = when (actionMode) {
                0 -> if (isFocusCategory) themeCoordinator.primaryColor else Color.parseColor("#F43F5E")
                1 -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#38BDF8")
            }

            addBtn.setTextColor(if (actionMode == 0) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            addBtn.background = if (actionMode == 0) GradientDrawable().apply {
                cornerRadius = dp(10).toFloat(); setColor(activeColor)
            } else null

            deductBtn.setTextColor(if (actionMode == 1) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            deductBtn.background = if (actionMode == 1) GradientDrawable().apply {
                cornerRadius = dp(10).toFloat(); setColor(Color.parseColor("#EF4444"))
            } else null

            setBtn.setTextColor(if (actionMode == 2) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            setBtn.background = if (actionMode == 2) GradientDrawable().apply {
                cornerRadius = dp(10).toFloat(); setColor(Color.parseColor("#38BDF8"))
            } else null

            // Presets
            presetContainer.removeAllViews()
            val presets = when {
                actionMode == 2 -> if (isFocusCategory) listOf(0L, 30L, 60L, 120L, 240L) else listOf(0L, 5L, 15L, 30L, 60L)
                isFocusCategory -> if (actionMode == 0) listOf(15L, 30L, 45L, 60L, 120L) else listOf(10L, 15L, 30L, 45L, 60L)
                else -> if (actionMode == 0) listOf(5L, 10L, 15L, 20L, 30L) else listOf(5L, 10L, 15L, 20L, 30L)
            }

            for (p in presets) {
                val chip = Button(activity).apply {
                    val label = if (p >= 60 && p % 60 == 0L) "${p / 60}h" else "${p}m"
                    text = when (actionMode) {
                        0 -> "+$label"
                        1 -> "-$label"
                        else -> label
                    }
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (actionMode == 1) Color.parseColor("#3F1B1B") else Color.parseColor("#1E293B"))
                    }
                    layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                        setMargins(dp(2), 0, dp(2), 0)
                    }
                    setOnClickListener {
                        minutesInput.setText(p.toString())
                    }
                }
                presetContainer.addView(chip)
            }

            val enteredMins = minutesInput.text.toString().toLongOrNull() ?: 0L
            val enteredSecs = enteredMins * 60L

            if (isFocusCategory) {
                val curH = effectiveFocusMins / 60L
                val curM = effectiveFocusMins % 60L
                summaryCurrentText.text = "• Current Recorded Focus: ${curH}h ${curM}m ($effectiveFocusMins mins)"

                val newTotalSecs = when (actionMode) {
                    0 -> effectiveFocusSecs + enteredSecs
                    1 -> (effectiveFocusSecs - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }
                val newH = newTotalSecs / 3600L
                val newM = (newTotalSecs % 3600L) / 60L

                when (actionMode) {
                    0 -> {
                        summaryResultText.text = "✨ Will add ${enteredMins}m → New Focus Total: ${newH}h ${newM}m"
                        summaryResultText.setTextColor(themeCoordinator.primaryColor)
                        applyBtn.text = "ADD ${enteredMins}m TO FOCUS"
                    }
                    1 -> {
                        summaryResultText.text = "⚠️ Will deduct ${enteredMins}m → New Focus Total: ${newH}h ${newM}m"
                        summaryResultText.setTextColor(Color.parseColor("#F59E0B"))
                        applyBtn.text = "DEDUCT ${enteredMins}m FROM FOCUS"
                    }
                    else -> {
                        summaryResultText.text = "🎯 Will set total focus to ${enteredMins}m (${newH}h ${newM}m)"
                        summaryResultText.setTextColor(Color.parseColor("#38BDF8"))
                        applyBtn.text = "SET FOCUS TOTAL TO ${enteredMins}m"
                    }
                }
            } else {
                // Break Category
                val curH = effectiveBreakMins / 60L
                val curM = effectiveBreakMins % 60L
                summaryCurrentText.text = "• Current Recorded Break: ${curH}h ${curM}m ($effectiveBreakMins mins)"

                val newTotalSecs = when (actionMode) {
                    0 -> effectiveBreakSecs + enteredSecs
                    1 -> (effectiveBreakSecs - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }
                val newH = newTotalSecs / 3600L
                val newM = (newTotalSecs % 3600L) / 60L

                when (actionMode) {
                    0 -> {
                        summaryResultText.text = "☕ Will add ${enteredMins}m → New Break Total: ${newH}h ${newM}m"
                        summaryResultText.setTextColor(Color.parseColor("#F43F5E"))
                        applyBtn.text = "ADD ${enteredMins}m TO BREAK"
                    }
                    1 -> {
                        summaryResultText.text = "⚠️ Will deduct ${enteredMins}m → New Break Total: ${newH}h ${newM}m"
                        summaryResultText.setTextColor(Color.parseColor("#F59E0B"))
                        applyBtn.text = "DEDUCT ${enteredMins}m FROM BREAK"
                    }
                    else -> {
                        summaryResultText.text = "🎯 Will set total break to ${enteredMins}m (${newH}h ${newM}m)"
                        summaryResultText.setTextColor(Color.parseColor("#38BDF8"))
                        applyBtn.text = "SET BREAK TOTAL TO ${enteredMins}m"
                    }
                }
            }

            applyBtn.background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(activeColor)
            }
        }

        focusTab.setOnClickListener {
            isFocusCategory = true
            updateUI()
        }
        breakTab.setOnClickListener {
            isFocusCategory = false
            updateUI()
        }

        addBtn.setOnClickListener {
            actionMode = 0
            updateUI()
        }
        deductBtn.setOnClickListener {
            actionMode = 1
            updateUI()
        }
        setBtn.setOnClickListener {
            actionMode = 2
            updateUI()
        }

        minutesInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateUI() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        applyBtn.setOnClickListener {
            val enteredMins = minutesInput.text.toString().toLongOrNull()
            if (enteredMins == null || (actionMode != 2 && enteredMins <= 0)) {
                Toast.makeText(activity, "Please enter a valid number of minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val enteredSecs = enteredMins * 60L
            val isTargetToday = targetDateStr == todayStr
            val now = System.currentTimeMillis()

            if (isFocusCategory) {
                val currentStoredFocus = sharedPrefs.getLong("${targetDateStr}_focus_total", 0L)
                val runningStudy = if (isTargetToday) activity.accumulatedStudy else 0L
                val currentEffectiveFocus = currentStoredFocus + runningStudy

                val newTotalFocus = when (actionMode) {
                    0 -> currentEffectiveFocus + enteredSecs
                    1 -> (currentEffectiveFocus - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }
                val deltaSecs = newTotalFocus - currentEffectiveFocus

                when (actionMode) {
                    0 -> TimelineLogger.appendBlockForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        durationSecs = enteredSecs,
                        state = "MANUAL_FOCUS",
                        subId = selectedSubject?.id,
                        subName = selectedSubject?.name,
                        subColor = selectedSubject?.colorHex
                    )
                    1 -> TimelineLogger.deductDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        deductSecs = enteredSecs,
                        isBreak = false
                    )
                    else -> TimelineLogger.setTotalDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        targetSecs = enteredSecs,
                        isBreak = false,
                        subId = selectedSubject?.id,
                        subName = selectedSubject?.name,
                        subColor = selectedSubject?.colorHex
                    )
                }

                sharedPrefs.edit()
                    .putLong("${targetDateStr}_focus_total", newTotalFocus)
                    .putLong("${targetDateStr}_focus_manual", 0L)
                    .putLong("last_data_modified_timestamp", now)
                    .apply()

                if (isTargetToday) {
                    activity.accumulatedStudy = 0L
                    sharedPrefs.edit().putLong("accumulatedStudy", 0L).apply()
                }

                if (selectedSubject != null && deltaSecs != 0L) {
                    SubjectTagManager.adjustSubjectStudyTime(activity, selectedSubject!!.id, deltaSecs, targetDateStr)
                }

                val msg = when (actionMode) {
                    0 -> "Added ${enteredMins}m to ${if (isTargetToday) "today's" else targetDateStr} focus!"
                    1 -> "Deducted ${enteredMins}m from ${if (isTargetToday) "today's" else targetDateStr} focus!"
                    else -> "Set ${if (isTargetToday) "today's" else targetDateStr} focus total to ${enteredMins}m!"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            } else {
                // Break Category
                val currentStoredBreak = sharedPrefs.getLong("${targetDateStr}_break_total", 0L)
                val runningBreak = if (isTargetToday) activity.currentBreakSeconds else 0L
                val currentEffectiveBreak = currentStoredBreak + runningBreak

                val newTotalBreak = when (actionMode) {
                    0 -> currentEffectiveBreak + enteredSecs
                    1 -> (currentEffectiveBreak - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }

                when (actionMode) {
                    0 -> TimelineLogger.appendBlockForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        durationSecs = enteredSecs,
                        state = "MANUAL_BREAK"
                    )
                    1 -> TimelineLogger.deductDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        deductSecs = enteredSecs,
                        isBreak = true
                    )
                    else -> TimelineLogger.setTotalDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        targetSecs = enteredSecs,
                        isBreak = true
                    )
                }

                sharedPrefs.edit()
                    .putLong("${targetDateStr}_break_total", newTotalBreak)
                    .putLong("${targetDateStr}_break_manual", 0L)
                    .putLong("last_data_modified_timestamp", now)
                    .apply()

                if (isTargetToday) {
                    activity.currentBreakSeconds = 0L
                    sharedPrefs.edit().putLong("currentBreakSeconds", 0L).apply()
                }

                val msg = when (actionMode) {
                    0 -> "Added ${enteredMins}m to ${if (isTargetToday) "today's" else targetDateStr} break!"
                    1 -> "Deducted ${enteredMins}m from ${if (isTargetToday) "today's" else targetDateStr} break!"
                    else -> "Set ${if (isTargetToday) "today's" else targetDateStr} break total to ${enteredMins}m!"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }

            TimelineLogger.invalidate()
            activity.statsEngine.forceReconcileDayTotals(targetDateStr)
            activity.invalidateStatsCache()
            activity.recalculateStreak()
            activity.refreshStatsPanel()
            activity.updateVisualStyles()
            StudyWidgetProvider.refresh(activity)

            Thread {
                kotlinx.coroutines.runBlocking {
                    CloudSyncManager.syncDataToCloud(activity)
                }
            }.start()

            dialog.dismiss()
        }

        updateUI()

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val width = (displayMetrics.widthPixels * 0.92f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun seedSampleGoals(activity: MainActivity) {
        val sampleGoals = listOf(
            PlannerGoal(title = "📐 Solve 20 Calculus problems", note = "Derivatives and Integrals", targetMinutes = 60, subjectId = "math"),
            PlannerGoal(title = "⚛️ Physics Numerical Practice", note = "Thermodynamics chapter", targetMinutes = 45, subjectId = "physics"),
            PlannerGoal(title = "🧪 Review Organic Mechanisms", note = "Reaction pathways & notes", targetMinutes = 30, subjectId = "chemistry"),
            PlannerGoal(title = "💧 Daily Hydration & Flashcards", note = "Quick evening revision", targetMinutes = 0, subjectId = null)
        )
        activity.saveSessionGoalsToJson(sampleGoals)
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_planner_reset_date", todayStr).apply()
    }

    private fun simulatePlannerDayRollover(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentGoals = activity.loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
        if (currentGoals.isNotEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            PlannerHistoryManager.snapshotToday(activity, currentGoals)
            val resetGoals = currentGoals.map { it.copy(completed = false, checkedAt = 0L) }
            activity.saveSessionGoalsToJson(resetGoals)
            prefs.edit().putString("last_planner_reset_date", todayStr).apply()
        }
    }

    private fun seedRealisticHabitHistory(activity: MainActivity, days: Int = 14) {
        val sampleGoals = listOf(
            PlannerGoal(id = "sample_goal_1", title = "📐 Solve Calculus Problems", targetMinutes = 45, subjectId = "math"),
            PlannerGoal(id = "sample_goal_2", title = "⚛️ Physics Core Concept Review", targetMinutes = 30, subjectId = "physics"),
            PlannerGoal(id = "sample_goal_3", title = "🧪 Organic Chemistry Reactions", targetMinutes = 30, subjectId = "chemistry"),
            PlannerGoal(id = "sample_goal_4", title = "💧 Daily Review & Flashcards", targetMinutes = 0, subjectId = null)
        )
        activity.saveSessionGoalsToJson(sampleGoals)

        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val rng = Random(888L)

        for (i in days downTo 1) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

            val snapshots = sampleGoals.map { g ->
                val completed = if (isSunday) rng.nextFloat() < 0.60f else rng.nextFloat() < 0.85f
                val isAchieved = completed
                val checkedTime = if (completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L
                PlannerGoalSnapshot(
                    goalId = g.id,
                    title = g.title,
                    targetMinutes = g.targetMinutes,
                    completed = completed,
                    checkedAt = checkedTime,
                    isAchieved = isAchieved,
                    subjectId = g.subjectId
                )
            }

            val array = JSONArray()
            for (s in snapshots) {
                array.put(JSONObject().apply {
                    put("goalId", s.goalId)
                    put("title", s.title)
                    put("targetMinutes", s.targetMinutes)
                    put("completed", s.completed)
                    put("checkedAt", s.checkedAt)
                    put("isAchieved", s.isAchieved)
                    if (s.subjectId != null) put("subjectId", s.subjectId)
                })
            }
            editor.putString("${dateStr}_planner_snapshot", array.toString())
        }
        val todayStr = sdf.format(Date())
        editor.putString("last_planner_reset_date", todayStr)
        editor.apply()
    }

    private fun markAllTodayGoals(activity: MainActivity, completed: Boolean) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentGoals = activity.loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
        val updated = currentGoals.map {
            it.copy(completed = completed, checkedAt = if (completed) System.currentTimeMillis() else 0L)
        }
        activity.saveSessionGoalsToJson(updated)
    }

    private fun wipePlannerData(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (k in prefs.all.keys) {
            if (k.endsWith("_planner_snapshot")) {
                editor.remove(k)
            }
        }
        editor.putString("session_goals_json", "[]")
        editor.apply()
    }

    data class EditableGoalItem(
        var id: String,
        var title: String,
        var targetMinutes: Int,
        var completed: Boolean,
        var isAchieved: Boolean,
        var subjectId: String?
    )

    fun showCustomPlannerSnapshotLoggerDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        // Title
        root.addView(TextView(activity).apply {
            text = "✍️ Custom Historical Planner Logger"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(activity).apply {
            text = "Select any past date and set custom habit completion statuses to test streaks, matrices & insights."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(12))
        })

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } // default yesterday
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateDisplayFmt = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

        val datePickerBtn = TextView(activity).apply {
            text = "📅 Selected Date: ${dateDisplayFmt.format(cal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 60), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        root.addView(datePickerBtn)

        val itemsList = mutableListOf<EditableGoalItem>()

        fun loadGoalsForCurrentDate() {
            val dateStr = sdf.format(cal.time)
            itemsList.clear()
            val existingSnapshots = PlannerHistoryManager.loadDaySnapshot(activity, dateStr)
            if (existingSnapshots.isNotEmpty()) {
                for (s in existingSnapshots) {
                    itemsList.add(EditableGoalItem(s.goalId, s.title, s.targetMinutes, s.completed, s.isAchieved, s.subjectId))
                }
            } else {
                val currentGoals = activity.loadSessionGoalsFromJson(activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]")
                if (currentGoals.isNotEmpty()) {
                    for (g in currentGoals) {
                        itemsList.add(EditableGoalItem(g.id, g.title, g.targetMinutes, true, true, g.subjectId))
                    }
                } else {
                    itemsList.add(EditableGoalItem("g1", "📐 Calculus Practice", 45, true, true, "math"))
                    itemsList.add(EditableGoalItem("g2", "⚛️ Physics Numerical Set", 30, true, true, "physics"))
                    itemsList.add(EditableGoalItem("g3", "🧪 Chemistry Theory Revision", 30, false, false, "chemistry"))
                    itemsList.add(EditableGoalItem("g4", "💧 Daily Routine & Flashcards", 0, true, true, null))
                }
            }
        }
        loadGoalsForCurrentDate()

        val goalsScrollView = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.38f).toInt()
            )
        }

        val goalsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        goalsScrollView.addView(goalsContainer)

        fun renderGoalsList() {
            goalsContainer.removeAllViews()
            for ((idx, item) in itemsList.withIndex()) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                }

                // Completion Toggle
                val toggleBtn = TextView(activity).apply {
                    text = if (item.completed) "✓ Completed" else "✗ Incomplete"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (item.completed) Color.parseColor("#10B981") else Color.parseColor("#475569"))
                    }
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener {
                        item.completed = !item.completed
                        item.isAchieved = item.completed
                        renderGoalsList()
                    }
                }

                val titleCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(10), 0, dp(10), 0)
                    }
                }
                titleCol.addView(TextView(activity).apply {
                    text = item.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                titleCol.addView(TextView(activity).apply {
                    text = if (item.targetMinutes > 0) "Target: ${item.targetMinutes}m" else "Daily Habit (No target)"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })

                val removeBtn = TextView(activity).apply {
                    text = "✕"
                    textSize = 14f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        itemsList.removeAt(idx)
                        renderGoalsList()
                    }
                }

                row.addView(toggleBtn)
                row.addView(titleCol)
                row.addView(removeBtn)
                goalsContainer.addView(row)
            }
        }
        renderGoalsList()

        datePickerBtn.setOnClickListener {
            val dpd = DatePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, y, m, d ->
                    cal.set(Calendar.YEAR, y)
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    datePickerBtn.text = "📅 Selected Date: ${dateDisplayFmt.format(cal.time)}"
                    loadGoalsForCurrentDate()
                    renderGoalsList()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dpd.show()
        }

        root.addView(goalsScrollView)

        // Add custom habit button
        val addCustomHabitBtn = TextView(activity).apply {
            text = "➕ Add Another Habit to this Date"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, dp(12))
            }
            setOnClickListener {
                val inputEdit = EditText(activity).apply {
                    hint = "Habit title (e.g. 📝 Flashcards review)"
                    setTextColor(themeCoordinator.textColor)
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                }
                AlertDialog.Builder(activity, getPickerThemeRes())
                    .setTitle("Add Habit to Snapshot")
                    .setView(inputEdit)
                    .setPositiveButton("Add") { _, _ ->
                        val t = inputEdit.text.toString().trim()
                        if (t.isNotEmpty()) {
                            itemsList.add(EditableGoalItem("custom_${System.currentTimeMillis()}", t, 30, true, true, null))
                            renderGoalsList()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        root.addView(addCustomHabitBtn)

        // Save Button
        val saveBtn = Button(activity).apply {
            text = "SAVE SNAPSHOT FOR THIS DATE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                if (itemsList.isEmpty()) {
                    Toast.makeText(activity, "Please add at least one goal/habit for this snapshot", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val dateStr = sdf.format(cal.time)
                val array = JSONArray()
                for (item in itemsList) {
                    array.put(JSONObject().apply {
                        put("goalId", item.id)
                        put("title", item.title)
                        put("targetMinutes", item.targetMinutes)
                        put("completed", item.completed)
                        put("checkedAt", if (item.completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L)
                        put("isAchieved", item.isAchieved)
                        if (item.subjectId != null) put("subjectId", item.subjectId)
                    })
                }

                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("${dateStr}_planner_snapshot", array.toString()).apply()

                activity.statsDirty = true
                activity.tabPageCache.clear()
                activity.refreshStatsPanel()

                Toast.makeText(activity, "Saved planner snapshot for $dateStr!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        root.addView(saveBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}
