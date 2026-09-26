package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BreakAndLectureDialogHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)

    fun showBreakDurationDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "☕ Choose Break Duration"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, 0, 0, dp(4))
        })

        content.addView(TextView(host).apply {
            text = "Select your target break length. The timer will notify you when it's time to resume."
            textSize = 12.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            setPadding(0, 0, 0, dp(14))
        })

        val options = listOf(
            Pair("5 Minutes (Quick Rest)", 300L),
            Pair("10 Minutes (Standard)", 600L),
            Pair("15 Minutes (Deep Rest)", 900L),
            Pair("20 Minutes (Long Break)", 1200L),
            Pair("30 Minutes (Meal / Walk)", 1800L)
        )

        for ((label, secs) in options) {
            val btn = Button(host).apply {
                text = label
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(8))
                }
                setOnClickListener {
                    dialog.dismiss()
                    host.handleStartBreakOnly(secs)
                }
            }
            content.addView(btn)
        }

        val cancelBtn = Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(cancelBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showLectureEndDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "🎓 Class Session Finished!"
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(6))
        })

        content.addView(TextView(host).apply {
            text = "Your scheduled lecture duration has completed. Would you like to take a break or extend your session?"
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        })

        val btnBreak = Button(host).apply {
            text = "☕ Take a 5m Break"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.secondaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, 0, 0, dp(10))
            }
            setOnClickListener {
                dialog.dismiss()
                host.handleStartBreak()
            }
        }
        content.addView(btnBreak)

        val btnExtend = Button(host).apply {
            text = "⏱️ Extend +5 Minutes"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, 0, 0, dp(10))
            }
            setOnClickListener {
                dialog.dismiss()
                host.handleExtendLecture(300L)
            }
        }
        content.addView(btnExtend)

        val btnStop = Button(host).apply {
            text = "Finish Session"
            textSize = 13.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            background = null
            setOnClickListener {
                dialog.dismiss()
                host.handleStopSession()
            }
        }
        content.addView(btnStop)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showScheduledLecturePromptDialog(item: LectureScheduleItem, remainingSecs: Long, skipKey: String) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "📅 ${item.title}"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, 0, 0, dp(4))
        })

        val remMins = (remainingSecs / 60).coerceAtLeast(1)
        content.addView(TextView(host).apply {
            text = "Class is currently in session (${item.startTime} – ${item.endTime}). Start countdown timer for the remaining $remMins minutes?"
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            setPadding(0, 0, 0, dp(18))
        })

        val startBtn = Button(host).apply {
            text = "Start Lecture Timer"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, 0, 0, dp(10))
            }
            setOnClickListener {
                dialog.dismiss()
                host.focusCountdownSecs = remainingSecs
                host.focusRemainingSecs = remainingSecs
                host.timerMode = "COUNTDOWN"
                host.handleStateToggle()
            }
        }
        content.addView(startBtn)

        val dismissBtn = Button(host).apply {
            text = "Skip for Today"
            textSize = 13.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            background = null
            setOnClickListener {
                dialog.dismiss()
                host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .putBoolean(skipKey, true)
                    .apply()
            }
        }
        content.addView(dismissBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showInactivityCheckDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "⏸️ Still Studying?"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(6))
        })

        content.addView(TextView(host).apply {
            text = "Your timer has been running for a long session. Are you actively focusing?"
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        })

        val btnKeep = Button(host).apply {
            text = "Yes, Keep Timer Running"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                val confirmIntent = Intent(host, TimerService::class.java).apply {
                    action = TimerService.ACTION_CONFIRM_ACTIVITY
                }
                host.startService(confirmIntent)
            }
        }
        content.addView(btnKeep)

        val btnBreak = Button(host).apply {
            text = "Take a Break"
            textSize = 13.5f
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setOnClickListener {
                dialog.dismiss()
                val confirmIntent = Intent(host, TimerService::class.java).apply {
                    action = TimerService.ACTION_CONFIRM_ACTIVITY
                }
                host.startService(confirmIntent)
                host.handleStartBreak()
            }
        }
        content.addView(btnBreak)

        dialog.setOnDismissListener {
            host.onInactivityDialogDismissed()
        }

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun showNotificationRationaleDialog(onGranted: (() -> Unit)? = null) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "🔔 Enable Notifications"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, 0, 0, dp(6))
        })

        content.addView(TextView(host).apply {
            text = "StudyTimer needs notification access to display the persistent background timer controls and ring alerts when breaks finish."
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            setPadding(0, 0, 0, dp(18))
        })

        val btnAllow = Button(host).apply {
            text = "Grant Permission"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(host, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
                onGranted?.invoke()
            }
        }
        content.addView(btnAllow)

        val btnSkip = Button(host).apply {
            text = "Not Now"
            textSize = 13.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(btnSkip)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
