package com.madeby.JAI

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object StreakUpAnimationDialog {

    private class EmberParticle(
        var x: Float,
        var y: Float,
        var speedX: Float,
        var speedY: Float,
        var radius: Float,
        val color: Int,
        var alpha: Float = 1f
    )

    fun show(activity: MainActivity, oldStreak: Int, newStreak: Int) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val primaryColor = activity.themeCoordinator.primaryColor
            val density = activity.resources.displayMetrics.density
            val dp = { v: Float -> (v * density) }
            val dpInt = { v: Int -> (v * density).toInt() }
            val targetWidth = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dpInt(340))

            val amberColor = Color.parseColor("#F59E0B")
            val fireRedColor = Color.parseColor("#EF4444")
            val goldColor = Color.parseColor("#FFD700")

            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#111625"))
                cornerRadius = dp(28f)
                setStroke(dpInt(2), amberColor)
            }

            val container = object : LinearLayout(activity) {
                private val particles = ArrayList<EmberParticle>()
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                private var animator: ValueAnimator? = null
                private val colors = intArrayOf(
                    amberColor,
                    fireRedColor,
                    goldColor,
                    Color.parseColor("#FF7043"),
                    Color.parseColor("#FFA726")
                )

                init {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg
                    clipToOutline = true
                    setPadding(dpInt(22), dpInt(22), dpInt(22), dpInt(20))

                    for (i in 0 until 35) {
                        particles.add(
                            EmberParticle(
                                x = 0f,
                                y = 0f,
                                speedX = (Random.nextFloat() - 0.5f) * dp(3f),
                                speedY = -(Random.nextFloat() * dp(5f) + dp(2f)),
                                radius = (Random.nextFloat() * dp(3.5f) + dp(2f)),
                                color = colors[Random.nextInt(colors.size)]
                            )
                        )
                    }
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    if (w <= 0 || h <= 0) return
                    val cx = w / 2f
                    val cy = h * 0.40f
                    for (p in particles) {
                        p.x = cx + (Random.nextFloat() - 0.5f) * dp(60f)
                        p.y = cy + (Random.nextFloat() - 0.5f) * dp(20f)
                    }
                    animator?.cancel()
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 3200L
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            for (p in particles) {
                                p.x += p.speedX
                                p.y += p.speedY
                                p.speedX += (Random.nextFloat() - 0.5f) * dp(0.3f)
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
                        canvas.drawCircle(p.x, p.y, p.radius, paint)
                    }
                }

                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()
                    animator?.cancel()
                }
            }.apply {
                layoutParams = FrameLayout.LayoutParams(targetWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
            }

            // Top Flame Icon Box with Radial Glow Aura
            val flameBox = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(dpInt(76), dpInt(76)).apply {
                    setMargins(0, 0, 0, dpInt(10))
                }
            }

            val glowView = View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(50, 245, 158, 11))
                }
                layoutParams = FrameLayout.LayoutParams(dpInt(76), dpInt(76), Gravity.CENTER)
            }
            flameBox.addView(glowView)

            val flameIcon = TextView(activity).apply {
                text = "🔥"
                textSize = 44f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
            flameBox.addView(flameIcon)
            container.addView(flameBox)

            // Header Tag: "STREAK LEVEL UP"
            val tagChip = TextView(activity).apply {
                text = "STREAK LEVEL UP!"
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(amberColor)
                background = activity.themeCoordinator.createGlassChip(Color.argb(35, 245, 158, 11), 10f)
                setPadding(dpInt(12), dpInt(4), dpInt(12), dpInt(4))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpInt(12))
                }
            }
            container.addView(tagChip)

            // Number Count-Up Display Container
            val numberContainer = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpInt(64)).apply {
                    setMargins(0, 0, 0, dpInt(12))
                }
            }

            val oldNumberTv = TextView(activity).apply {
                text = "$oldStreak"
                textSize = 42f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            val newNumberTv = TextView(activity).apply {
                text = "$newStreak"
                textSize = 46f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(goldColor)
                gravity = Gravity.CENTER
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            numberContainer.addView(oldNumberTv)
            numberContainer.addView(newNumberTv)
            container.addView(numberContainer)

            // Subtitle / Milestone Progress Description
            val milestoneSubtitle = getMilestoneProgressText(newStreak)
            val subtitleTv = TextView(activity).apply {
                text = milestoneSubtitle
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Color.parseColor("#9CA3AF"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpInt(18))
            }
            container.addView(subtitleTv)

            // Action Button: "Keep Moving Forward 💪"
            val btnBg = GradientDrawable().apply {
                setColor(amberColor)
                cornerRadius = dp(14f)
            }

            val actionBtn = Button(activity).apply {
                text = "Keep Moving Forward 💪"
                setTextColor(Color.WHITE)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = btnBg
                setPadding(dpInt(22), dpInt(10), dpInt(22), dpInt(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpInt(44))
                setOnClickListener {
                    dialog.dismiss()
                }
            }
            container.addView(actionBtn)

            dialog.setContentView(container)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
            }

            // STAGE 1: Smooth Entrance Animation (520ms)
            container.scaleX = 0.5f
            container.scaleY = 0.5f
            container.alpha = 0f
            container.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(520L)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()

            // STAGE 2: Glow Ring Pulse (600ms)
            glowView.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .alpha(0.9f)
                .setDuration(600L)
                .withEndAction {
                    glowView.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.5f).setDuration(400L).start()
                }
                .start()

            // STAGE 3: Number Count-Up Transition (650ms delay, old shrinks out, new pops in)
            Handler(Looper.getMainLooper()).postDelayed({
                oldNumberTv.animate()
                    .scaleX(0.15f)
                    .scaleY(0.15f)
                    .alpha(0f)
                    .setDuration(380L)
                    .withEndAction {
                        oldNumberTv.visibility = View.GONE
                    }
                    .start()

                newNumberTv.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .alpha(1.0f)
                    .setDuration(500L)
                    .setInterpolator(OvershootInterpolator(1.8f))
                    .withEndAction {
                        newNumberTv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300L).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                activity.window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } catch (_: Exception) {}
                        }
                    }
                    .start()

                // Flame Bounce Pulse
                flameIcon.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .rotation(14f)
                    .setDuration(450L)
                    .withEndAction {
                        flameIcon.animate().scaleX(1.0f).scaleY(1.0f).rotation(0f).setDuration(300L).start()
                    }
                    .start()
            }, 650L)

            dialog.show()
        } catch (_: Exception) {}
    }

    private fun getMilestoneProgressText(streak: Int): String {
        return when {
            streak == 7 -> "🥉 7-Day Bronze Streak Milestone Achieved!"
            streak == 14 -> "🥈 14-Day Silver Streak Milestone Achieved!"
            streak == 21 -> "🥇 21-Day Gold Streak Milestone Achieved!"
            streak == 28 -> "💎 28-Day Platinum Streak Milestone Achieved!"
            streak == 50 -> "🔥 50-Day Fire Streak Milestone Achieved!"
            streak >= 100 -> "👑 100-Day Crown Legend Milestone Achieved!"
            streak < 7 -> "Fantastic progress! ${7 - streak} more days to 7-Day Bronze!"
            streak < 14 -> "2-Week push! ${14 - streak} more days to 14-Day Silver!"
            streak < 21 -> "Habit forming! ${21 - streak} more days to 21-Day Gold!"
            streak < 28 -> "Month of focus! ${28 - streak} more days to 28-Day Platinum!"
            streak < 50 -> "Half-century push! ${50 - streak} more days to 50-Day Fire!"
            else -> "Unstoppable momentum! ${100 - streak} more days to 100-Day Crown!"
        }
    }
}
