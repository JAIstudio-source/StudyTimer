package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class InsightsPillNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    private var activeTabIndex = 0
    private var onTabSelectedListener: ((AppStatsTab) -> Unit)? = null
    private var isBarVisible = true

    private val tabs = listOf(
        TabItem(AppStatsTab.OVERVIEW, "Overview", R.drawable.ic_dashboard),
        TabItem(AppStatsTab.TIMELINE, "History", R.drawable.ic_calendar),
        TabItem(AppStatsTab.PLANNER, "Planner", R.drawable.ic_check_circle)
    )

    private data class TabItem(
        val tab: AppStatsTab,
        val label: String,
        val iconRes: Int
    )

    private val tabsContainer: LinearLayout
    private val indicatorView: View
    private val tabViews = mutableListOf<LinearLayout>()
    private val iconViews = mutableListOf<ImageView>()
    private val labelViews = mutableListOf<TextView>()

    private var primaryAccentColor: Int = ThemeCoordinator.INSIGHTS_NAV_ACCENT

    init {
        elevation = 20f * density
        clipToPadding = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            z = 20f * density
        }

        // AMOLED surface #16171D, 1dp border #2A2B36, 32dp corner radius
        val bgDrawable = GradientDrawable().apply {
            cornerRadius = 32f * density
            setColor(Color.parseColor("#16171D"))
            setStroke(dp(1), Color.parseColor("#2A2B36"))
        }
        background = bgDrawable

        // Fluid spring sliding indicator pill
        indicatorView = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = 24f * density
                setColor(tintWithAlpha(primaryAccentColor, 0.18f))
                setStroke(dp(1), tintWithAlpha(primaryAccentColor, 0.45f))
            }
            alpha = 1f
        }
        addView(indicatorView, LayoutParams(0, dp(38)).apply {
            gravity = Gravity.CENTER_VERTICAL
        })

        // Horizontal container for the 3 tabs
        tabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        addView(tabsContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        buildTabs()
    }

    private fun tintWithAlpha(color: Int, alphaRatio: Float): Int {
        val a = (Color.alpha(color) * alphaRatio).toInt().coerceIn(0, 255)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a, r, g, b)
    }

    fun applyTheme(themeCoordinator: ThemeCoordinator) {
        val isDark = themeCoordinator.isDarkMode()
        val bgCol = if (isDark) {
            if (themeCoordinator.activeBgMode == "ECLIPSE") Color.parseColor("#1E293B") else Color.parseColor("#16171D")
        } else {
            Color.parseColor("#FFFFFF")
        }
        val strokeCol = if (isDark) {
            Color.parseColor("#2A2B36")
        } else {
            Color.parseColor("#CBD5E1")
        }
        background = GradientDrawable().apply {
            cornerRadius = 32f * density
            setColor(bgCol)
            setStroke(dp(1), strokeCol)
        }
        setPrimaryColor(themeCoordinator.primaryColor)
    }

    fun setPrimaryColor(color: Int) {
        primaryAccentColor = color
        indicatorView.background = GradientDrawable().apply {
            cornerRadius = 24f * density
            setColor(tintWithAlpha(primaryAccentColor, 0.18f))
            setStroke(dp(1), tintWithAlpha(primaryAccentColor, 0.45f))
        }
        refreshTabVisuals(animated = false)
    }

    fun setOnTabSelectedListener(listener: (AppStatsTab) -> Unit) {
        this.onTabSelectedListener = listener
    }

    private fun buildTabs() {
        tabsContainer.removeAllViews()
        tabViews.clear()
        iconViews.clear()
        labelViews.clear()

        tabs.forEachIndexed { index, tabItem ->
            val tabLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            }

            val icon = ImageView(context).apply {
                setImageResource(tabItem.iconRes)
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
            }

            val label = TextView(context).apply {
                text = tabItem.label
                textSize = 12.5f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                setPadding(dp(6), 0, 0, 0)
                visibility = if (index == activeTabIndex) View.VISIBLE else View.GONE
                alpha = if (index == activeTabIndex) 1f else 0f
            }

            tabLayout.addView(icon)
            tabLayout.addView(label)

            tabLayout.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    }
                }
                false
            }

            tabLayout.setOnClickListener {
                if (activeTabIndex != index) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectTabByIndex(index, animated = true)
                    onTabSelectedListener?.invoke(tabItem.tab)
                }
            }

            tabViews.add(tabLayout)
            iconViews.add(icon)
            labelViews.add(label)
            tabsContainer.addView(tabLayout)
        }

        post {
            refreshTabVisuals(animated = false)
        }
    }

    fun selectTab(tab: AppStatsTab, animated: Boolean = true) {
        val index = when (tab) {
            AppStatsTab.OVERVIEW -> 0
            AppStatsTab.TIMELINE -> 1
            AppStatsTab.PLANNER -> 2
        }
        selectTabByIndex(index, animated)
    }

    fun getActiveTab(): AppStatsTab = tabs[activeTabIndex].tab

    private fun selectTabByIndex(index: Int, animated: Boolean) {
        if (index !in tabs.indices) return
        activeTabIndex = index
        refreshTabVisuals(animated)
    }

    private fun refreshTabVisuals(animated: Boolean) {
        if (tabViews.isEmpty()) return

        tabViews.forEachIndexed { i, _ ->
            val isSelected = (i == activeTabIndex)
            val icon = iconViews[i]
            val label = labelViews[i]

            if (isSelected) {
                icon.setColorFilter(primaryAccentColor)
                label.setTextColor(primaryAccentColor)
                if (animated) {
                    icon.animate().scaleX(1.15f).scaleY(1.15f).setDuration(220).start()
                    label.visibility = View.VISIBLE
                    label.alpha = 0f
                    label.animate().alpha(1f).setDuration(200).start()
                } else {
                    icon.scaleX = 1.15f
                    icon.scaleY = 1.15f
                    label.visibility = View.VISIBLE
                    label.alpha = 1f
                }
            } else {
                val unselectedColor = Color.parseColor("#8E92A8")
                icon.setColorFilter(unselectedColor)
                label.setTextColor(unselectedColor)
                if (animated) {
                    icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start()
                    label.animate().alpha(0f).setDuration(150).withEndAction {
                        label.visibility = View.GONE
                    }.start()
                } else {
                    icon.scaleX = 1.0f
                    icon.scaleY = 1.0f
                    label.visibility = View.GONE
                    label.alpha = 0f
                }
            }
        }

        val activeView = tabViews.getOrNull(activeTabIndex) ?: return
        activeView.post {
            val targetX = activeView.left.toFloat()
            val targetWidth = activeView.width

            if (animated && indicatorView.width > 0) {
                val startX = indicatorView.translationX
                val startWidth = indicatorView.layoutParams.width

                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 280
                    // Damping 0.80f spring overshoot feel
                    interpolator = OvershootInterpolator(1.18f)
                    addUpdateListener { animator ->
                        val fraction = animator.animatedFraction
                        indicatorView.translationX = startX + (targetX - startX) * fraction
                        val w = (startWidth + (targetWidth - startWidth) * fraction).toInt()
                        indicatorView.layoutParams = (indicatorView.layoutParams as LayoutParams).apply {
                            width = w
                        }
                        indicatorView.requestLayout()
                    }
                    start()
                }
            } else {
                indicatorView.translationX = targetX
                indicatorView.layoutParams = (indicatorView.layoutParams as LayoutParams).apply {
                    width = targetWidth
                }
                indicatorView.requestLayout()
            }
        }
    }

    fun setBarVisibility(visible: Boolean) {
        if (isBarVisible == visible) return
        isBarVisible = visible
        val barHeight = if (height > 0) height.toFloat() else dp(48).toFloat()
        val translationTarget = if (visible) 0f else (barHeight + dp(40))
        animate()
            .translationY(translationTarget)
            .alpha(if (visible) 1f else 0f)
            .setDuration(240)
            .start()
    }

    fun forceShowBar() {
        isBarVisible = true
        animate().cancel()
        translationY = 0f
        alpha = 1f
        bringToFront()
    }
}
