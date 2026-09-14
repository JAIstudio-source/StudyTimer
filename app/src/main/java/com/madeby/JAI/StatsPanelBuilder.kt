package com.madeby.JAI

import android.widget.FrameLayout
import android.widget.LinearLayout

class StatsPanelBuilder(private val host: MainActivity) {

    fun build(target: android.view.ViewGroup = host.panelContainer) {
        with(host) {
            val renderTab = currentStatsTab
            val statsRoot = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }

            val snap = statsSnapshotCache ?: computeStatsSnapshot().also {
                statsSnapshotCache = it
                statsSnapshotGen++
                statsDirty = false
            }

            renderStatsContent(statsRoot, snap, renderTab)
            target.addView(statsRoot)
        }
    }
}
