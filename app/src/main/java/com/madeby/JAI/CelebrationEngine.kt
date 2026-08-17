package com.madeby.JAI

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

object CelebrationEngine {

    private const val PREFS_NAME = "studytimer_celebration_prefs"
    private const val KEY_LAST_CELEBRATED_STREAK = "last_celebrated_streak"
    private const val KEY_LAST_CELEBRATED_GOAL_DATE = "last_celebrated_goal_date"

    fun checkAndCelebrate(
        activity: MainActivity,
        sessionSecs: Long,
        todayTotalSecs: Long,
        dailyGoalSecs: Long,
        currentStreak: Int
    ) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val goalReachedToday = todayTotalSecs >= dailyGoalSecs && dailyGoalSecs > 0
        val lastGoalDate = prefs.getString(KEY_LAST_CELEBRATED_GOAL_DATE, null)
        val isNewGoalAchieved = goalReachedToday && lastGoalDate != todayStr

        val lastStreak = prefs.safeInt(KEY_LAST_CELEBRATED_STREAK, 0)
        val isNewStreakMilestone = currentStreak > lastStreak && (currentStreak in listOf(1, 2, 3, 5, 7, 10, 14, 21, 30, 50, 75, 100) || currentStreak % 50 == 0)

        if (isNewGoalAchieved || isNewStreakMilestone) {
            if (isNewGoalAchieved) {
                prefs.edit().putString(KEY_LAST_CELEBRATED_GOAL_DATE, todayStr).apply()
            }
            if (isNewStreakMilestone) {
                prefs.edit().putInt(KEY_LAST_CELEBRATED_STREAK, currentStreak).apply()
            }

            activity.runOnUiThread {
                showCelebrationDialog(activity, isNewGoalAchieved, currentStreak)
            }
        }
    }

    private class ConfettiParticle(
        var x: Float,
        var y: Float,
        var speedX: Float,
        var speedY: Float,
        var rotation: Float,
        var rotSpeed: Float,
        val width: Float,
        val height: Float,
        val color: Int,
        var alpha: Float = 1f
    )

    internal fun showCelebrationDialog(
        activity: MainActivity,
        isGoalAchieved: Boolean,
        streak: Int
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val primaryColor = activity.themeCoordinator.primaryColor
            val density = activity.resources.displayMetrics.density
            val dp = { v: Int -> (v * density).toInt() }
            val targetWidth = (activity.resources.displayMetrics.widthPixels * 0.86f).toInt().coerceAtMost(dp(340))

            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#111625"))
                cornerRadius = 28f * density
                setStroke((2.5f * density).toInt(), primaryColor)
            }

            val container = object : LinearLayout(activity) {
                private val particles = ArrayList<ConfettiParticle>()
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                private var animator: ValueAnimator? = null
                private val colors = intArrayOf(
                    0xFFFFD700.toInt(), // Gold
                    0xFFFF4081.toInt(), // Rose Pink
                    0xFF6B7CFF.toInt(), // Primary Indigo
                    0xFF10B981.toInt(), // Emerald
                    0xFF38BDF8.toInt(), // Sky Blue
                    0xFFF59E0B.toInt(), // Amber
                    0xFFA855F7.toInt()  // Purple
                )

                init {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg
                    clipToOutline = true
                    setPadding(dp(22), dp(22), dp(22), dp(20))
                    for (i in 0 until 40) {
                        particles.add(
                            ConfettiParticle(
                                x = 0f,
                                y = 0f,
                                speedX = (Random.nextFloat() - 0.5f) * 12f * density,
                                speedY = -(Random.nextFloat() * 8f + 3f) * density,
                                rotation = Random.nextFloat() * 360f,
                                rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
                                width = (Random.nextFloat() * 7f + 4f) * density,
                                height = (Random.nextFloat() * 5f + 3f) * density,
                                color = colors[Random.nextInt(colors.size)]
                            )
                        )
                    }
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    if (w <= 0 || h <= 0) return
                    val cx = w / 2f
                    val cy = h * 0.28f
                    for (p in particles) {
                        p.x = cx + (Random.nextFloat() - 0.5f) * 36f
                        p.y = cy + (Random.nextFloat() - 0.5f) * 18f
                    }
                    animator?.cancel()
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 2200L
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            for (p in particles) {
                                p.x += p.speedX
                                p.y += p.speedY
                                p.speedY += 0.40f
                                p.speedX *= 0.985f
                                p.rotation += p.rotSpeed
                                p.alpha = (1f - fraction * 0.95f).coerceIn(0f, 1f)
                            }
                            invalidate()
                        }
                        start()
                    }
                }

                override fun dispatchDraw(canvas: Canvas) {
                    super.dispatchDraw(canvas)
                    for (p in particles) {
                        if (p.alpha <= 0.01f) continue
                        paint.color = p.color
                        paint.alpha = (p.alpha * 255).toInt()
                        canvas.save()
                        canvas.translate(p.x, p.y)
                        canvas.rotate(p.rotation)
                        canvas.drawRect(-p.width / 2, -p.height / 2, p.width / 2, p.height / 2, paint)
                        canvas.restore()
                    }
                }

                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()
                    animator?.cancel()
                }
            }.apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    targetWidth,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val iconTv = TextView(activity).apply {
                text = if (isGoalAchieved) "🎉 🎯 🎉" else "🔥 🏆 🔥"
                textSize = 34f
                gravity = Gravity.CENTER
            }

            val titleTv = TextView(activity).apply {
                text = if (isGoalAchieved) "Daily Focus Goal Reached!" else "$streak Day Study Streak!"
                textSize = 19f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(6))
            }

            val subtitleTv = TextView(activity).apply {
                text = if (isGoalAchieved) {
                    "Outstanding work! You have hit your daily study goal for today."
                } else {
                    "Incredible consistency! You are on a $streak-day study streak. Keep it up!"
                }
                textSize = 13f
                setTextColor(Color.parseColor("#9CA3AF"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(16))
            }

            val btnBg = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = 14f * density
            }

            val actionBtn = Button(activity).apply {
                text = "Keep Moving Forward 💪"
                setTextColor(Color.WHITE)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = btnBg
                setPadding(dp(22), dp(10), dp(22), dp(10))
                setOnClickListener {
                    dialog.dismiss()
                }
            }

            container.addView(iconTv)
            container.addView(titleTv)
            container.addView(subtitleTv)
            container.addView(actionBtn)

            dialog.setContentView(container)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
            }

            // Dynamic Entrance Scaling Animation
            container.scaleX = 0.55f
            container.scaleY = 0.55f
            container.alpha = 0f
            container.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(420L)
                .setInterpolator(OvershootInterpolator(1.7f))
                .start()

            // Icon Bouncing Animation
            iconTv.scaleX = 0.6f
            iconTv.scaleY = 0.6f
            iconTv.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .rotation(10f)
                .setDuration(360L)
                .withEndAction {
                    iconTv.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .rotation(0f)
                        .setDuration(240L)
                        .start()
                }
                .start()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }

            dialog.show()
        } catch (_: Exception) {}
    }
}
