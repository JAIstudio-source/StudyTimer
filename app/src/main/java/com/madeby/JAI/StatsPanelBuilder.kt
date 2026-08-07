package com.madeby.JAI

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class StatsPanelBuilder(private val host: MainActivity) {

    fun build(target: android.view.ViewGroup = host.panelContainer) {
        with(host) {
        val renderTab = currentStatsTab
        val statsRoot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val cached = statsSnapshotCache
        if (cached != null && (statsInternalRefresh || !statsDirty)) {
            renderStatsContent(statsRoot, cached, renderTab)
            target.addView(statsRoot)
            return
        }

        val loading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        loading.addView(ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply { isIndeterminate = true })
        loading.addView(TextView(this).apply {
            text = getString(R.string.crunching_numbers)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(14), 0, 0)
        })
        statsRoot.addView(loading, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        target.addView(statsRoot)

        Thread {
            val snap = computeStatsSnapshot()
            handler.post {
                if (isDestroyed || isFinishing) return@post
                statsSnapshotCache = snap
                statsSnapshotGen++
                statsDirty = false
                statsRoot.removeView(loading)
                renderStatsContent(statsRoot, snap, renderTab)
            }
        }.start()
        }
    }
}
