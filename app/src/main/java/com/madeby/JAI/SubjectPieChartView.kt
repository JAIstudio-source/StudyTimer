package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class SubjectPieChartView(context: Context) : View(context) {

    data class PieSlice(
        val label: String,
        val emoji: String,
        val value: Double,
        val colorHex: String,
        val subCount: Int = 1
    )

    private val slices = ArrayList<PieSlice>()
    private val rawItems = ArrayList<PieSlice>()
    private var totalVal = 0.0

    var isDonutMode: Boolean = true
        set(value) {
            field = value
            rebuildSlices()
            invalidate()
        }

    var primaryColor: Int = Color.parseColor("#6366F1")
    var textColor: Int = Color.WHITE

    var onSliceSelectedListener: ((slice: PieSlice?, index: Int) -> Unit)? = null

    var selectedSliceIndex: Int = -1
        private set

    private var popAnimProgress: Float = 0f
    private var popAnimator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    var boxColor: Int = Color.parseColor("#111625")
        set(value) {
            field = value
            strokePaint.color = value
            invalidate()
        }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#111625")
    }

    private val glowRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.MITER
    }

    private val outlineWedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
    }

    private val pieSliceGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Center HUD & Label Paints
    private val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val centerValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val centerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val insideBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
    }

    private val insideSubBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        color = Color.WHITE
    }

    private val centerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(18f, 14f), 0f)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()
    private val outerRectF = RectF()
    private val innerRectF = RectF()
    private val pillRectF = RectF()
    private val wedgePath = Path()

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun formatDuration(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    fun setData(items: List<PieSlice>) {
        rawItems.clear()
        rawItems.addAll(items.filter { it.value > 0 })
        selectedSliceIndex = -1
        popAnimProgress = 0f
        rebuildSlices()
        invalidate()
    }

    private fun rebuildSlices() {
        slices.clear()
        val sum = rawItems.sumOf { it.value }
        if (sum <= 0) {
            totalVal = 0.0
            return
        }

        // Smart Grouping for BOTH Donut and Pie mode:
        // When there are more than 6 subjects, show Top 5 by duration and bundle the rest into "Others"
        if (rawItems.size > 6) {
            val sorted = rawItems.sortedByDescending { it.value }
            val topItems = sorted.take(5)
            val otherItems = sorted.drop(5)
            val otherSum = otherItems.sumOf { it.value }

            slices.addAll(topItems)
            slices.add(PieSlice("Others", "📂", otherSum, "#64748B", otherItems.size))
        } else {
            slices.addAll(rawItems)
        }

        totalVal = slices.sumOf { it.value }.coerceAtLeast(0.001)
    }

    fun selectSlice(index: Int) {
        if (index == selectedSliceIndex) return
        selectedSliceIndex = if (index in slices.indices) index else -1
        animatePop()
        onSliceSelectedListener?.invoke(if (selectedSliceIndex >= 0) slices[selectedSliceIndex] else null, selectedSliceIndex)
    }

    private fun animatePop() {
        popAnimator?.cancel()
        popAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                popAnimProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = if (isDonutMode) height / 2f else height * 0.44f
            val dx = event.x - cx
            val dy = event.y - cy
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (slices.isNotEmpty() && totalVal > 0.0) {
                val outerRadius = min(width.toFloat(), height.toFloat()) * if (isDonutMode) 0.40f else 0.38f
                val innerRadius = if (isDonutMode) outerRadius * 0.56f else 0f

                if (isDonutMode && dist < innerRadius) {
                    // Tapped the center HUD in Donut mode -> toggle/reset selection
                    if (selectedSliceIndex != -1) {
                        selectedSliceIndex = -1
                        animatePop()
                        onSliceSelectedListener?.invoke(null, -1)
                    } else {
                        performClick()
                    }
                    return true
                } else if (dist <= outerRadius + dp(22f)) {
                    // Calculate touch angle: atan2 gives (-PI to PI), where 0 is Right, -PI/2 is Up
                    var touchDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    // Shift so that -90 deg (Top/Up) is 0 deg
                    touchDeg = (touchDeg + 90f + 360f) % 360f

                    var curAngle = 0f
                    var clickedIndex = -1
                    for (i in slices.indices) {
                        val sweep = ((slices[i].value / totalVal) * 360.0).toFloat()
                        if (touchDeg >= curAngle && touchDeg <= curAngle + sweep) {
                            clickedIndex = i
                            break
                        }
                        curAngle += sweep
                    }

                    if (clickedIndex != -1) {
                        if (selectedSliceIndex == clickedIndex) {
                            selectedSliceIndex = -1
                        } else {
                            selectedSliceIndex = clickedIndex
                        }
                        animatePop()
                        onSliceSelectedListener?.invoke(if (selectedSliceIndex >= 0) slices[selectedSliceIndex] else null, selectedSliceIndex)
                        return true
                    }
                }
            }

            // Default click behavior
            performClick()
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun ellipsizeText(text: String, p: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f || p.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val elWidth = p.measureText(ellipsis)
        if (elWidth >= maxWidth) return ""
        var end = text.length
        while (end > 0 && p.measureText(text.substring(0, end)) + elWidth > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end).trimEnd() + ellipsis else ellipsis
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        if (slices.isEmpty() || totalVal <= 0.0) {
            val emptyRadius = min(w, h) * 0.28f
            emptyPaint.strokeWidth = dp(2f)
            emptyPaint.color = Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            glowPaint.color = Color.argb(18, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))

            emptyTextPaint.color = Color.argb(190, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            subTextPaint.color = Color.argb(130, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            emptyTextPaint.textSize = dp(13.5f)
            subTextPaint.textSize = dp(11f)

            canvas.drawCircle(cx, cy, emptyRadius, glowPaint)
            canvas.drawCircle(cx, cy, emptyRadius, emptyPaint)
            canvas.drawText("No study sessions recorded", cx, cy - dp(2f), emptyTextPaint)
            canvas.drawText("Start studying to see your subject breakdown", cx, cy + dp(15f), subTextPaint)
            return
        }

        if (isDonutMode) {
            drawDonutMode(canvas, w, h, cx, cy)
        } else {
            drawModernPieMode(canvas, w, h, cx, cy)
        }
    }

    // =========================================================================
    // 1. INTERACTIVE 3D DONUT CHART RENDERING
    // =========================================================================
    private fun drawDonutMode(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val outerRadius = min(w, h) * 0.40f
        val innerRadius = outerRadius * 0.56f
        val strokeWidth = outerRadius - innerRadius
        val midRadius = (outerRadius + innerRadius) / 2f
        val maxPopOffset = dp(11f)

        strokeArcPaint.strokeWidth = strokeWidth
        var startAngle = -90f

        // Draw unselected slices strictly at anchored angles
        for (i in slices.indices) {
            val slice = slices[i]
            val sweepAngle = ((slice.value / totalVal) * 360.0).toFloat()
            if (sweepAngle > 0f) {
                if (i != selectedSliceIndex) {
                    drawSingleDonutSlice(canvas, cx, cy, innerRadius, outerRadius, midRadius, startAngle, sweepAngle, slice, isSelected = false, popOffset = 0f)
                }
                startAngle += sweepAngle
            }
        }

        // Draw Selected Slice ON TOP, translated outward along its bisector angle
        if (selectedSliceIndex in slices.indices) {
            var curAngle = -90f
            for (i in 0 until selectedSliceIndex) {
                curAngle += ((slices[i].value / totalVal) * 360.0).toFloat()
            }
            val selSlice = slices[selectedSliceIndex]
            val selSweep = ((selSlice.value / totalVal) * 360.0).toFloat()
            val popOffset = maxPopOffset * popAnimProgress

            drawSingleDonutSlice(canvas, cx, cy, innerRadius, outerRadius, midRadius, curAngle, selSweep, selSlice, isSelected = true, popOffset = popOffset)
        }

        // Draw Center Cutout HUD
        drawCenterHUD(canvas, cx, cy, innerRadius)
    }

    private fun drawSingleDonutSlice(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        innerRadius: Float,
        outerRadius: Float,
        midRadius: Float,
        startAngle: Float,
        sweepAngle: Float,
        slice: PieSlice,
        isSelected: Boolean,
        popOffset: Float
    ) {
        val midAngle = startAngle + sweepAngle / 2f
        val midAngleRad = Math.toRadians(midAngle.toDouble())

        val sliceCx = (cx + popOffset * cos(midAngleRad)).toFloat()
        val sliceCy = (cy + popOffset * sin(midAngleRad)).toFloat()

        rectF.set(sliceCx - midRadius, sliceCy - midRadius, sliceCx + midRadius, sliceCy + midRadius)

        val sliceColor = try { Color.parseColor(slice.colorHex) } catch (_: Exception) { primaryColor }

        // Clean modern gap between slices
        val gapAngle = if (slices.size > 1) 1.2f else 0f
        val drawStart = startAngle + gapAngle / 2f
        val drawSweep = max(0.5f, sweepAngle - gapAngle)

        // Edge-to-edge crisp highlight border under/around selected slice (No bulbous half-spheres)
        if (isSelected && popOffset > 0f) {
            glowRingPaint.color = Color.argb(120, Color.red(sliceColor), Color.green(sliceColor), Color.blue(sliceColor))
            glowRingPaint.strokeWidth = strokeArcPaint.strokeWidth + dp(5f)
            canvas.drawArc(rectF, drawStart, drawSweep, false, glowRingPaint)

            // Crisp perimeter outline around the donut wedge
            outlineWedgePaint.color = Color.WHITE
            outlineWedgePaint.strokeWidth = dp(2f)

            outerRectF.set(sliceCx - outerRadius, sliceCy - outerRadius, sliceCx + outerRadius, sliceCy + outerRadius)
            innerRectF.set(sliceCx - innerRadius, sliceCy - innerRadius, sliceCx + innerRadius, sliceCy + innerRadius)

            wedgePath.reset()
            wedgePath.arcTo(outerRectF, drawStart, drawSweep)
            wedgePath.arcTo(innerRectF, drawStart + drawSweep, -drawSweep)
            wedgePath.close()
            canvas.drawPath(wedgePath, outlineWedgePaint)
        }

        // Main Slice Donut Arc
        strokeArcPaint.color = sliceColor
        canvas.drawArc(rectF, drawStart, drawSweep, false, strokeArcPaint)

        // Inside Slice Labels (Subject Name, 2-line splitting for multi-words, and % badge)
        if (drawSweep >= 18f) {
            val badgeRadius = midRadius
            val badgeX = (sliceCx + badgeRadius * cos(midAngleRad)).toFloat()
            val badgeY = (sliceCy + badgeRadius * sin(midAngleRad)).toFloat()

            val pct = Math.round((slice.value / totalVal) * 100).toInt()
            val contrastColor = getContrastingTextColor(sliceColor)

            insideBadgePaint.color = contrastColor
            insideSubBadgePaint.color = Color.argb(210, Color.red(contrastColor), Color.green(contrastColor), Color.blue(contrastColor))

            val maxArcW = (midRadius * Math.toRadians(drawSweep.toDouble())).toFloat() * 0.90f
            val words = slice.label.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            if (drawSweep >= 34f && words.isNotEmpty()) {
                // Wide slice: Show subject name (split across 2 lines if multi-word) + percentage
                insideBadgePaint.textSize = dp(10.5f)
                insideSubBadgePaint.textSize = dp(9f)

                if (words.size >= 2) {
                    val line1 = "${slice.emoji} ${words[0]}"
                    val line2 = "${words.drop(1).joinToString(" ")} $pct%"
                    val el1 = ellipsizeText(line1, insideBadgePaint, maxArcW)
                    val el2 = ellipsizeText(line2, insideSubBadgePaint, maxArcW)

                    canvas.drawText(el1, badgeX, badgeY - dp(3f), insideBadgePaint)
                    canvas.drawText(el2, badgeX, badgeY + dp(9f), insideSubBadgePaint)
                } else {
                    val title = "${slice.emoji} ${words[0]}"
                    val elTitle = ellipsizeText(title, insideBadgePaint, maxArcW)

                    canvas.drawText(elTitle, badgeX, badgeY - dp(3f), insideBadgePaint)
                    canvas.drawText("$pct%", badgeX, badgeY + dp(9f), insideSubBadgePaint)
                }
            } else if (drawSweep >= 26f) {
                // Medium slice: Show Emoji + %
                insideBadgePaint.textSize = dp(11f)
                val badgeText = "${slice.emoji} $pct%"
                val fontMetrics = insideBadgePaint.fontMetrics
                val baseline = badgeY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(badgeText, badgeX, baseline, insideBadgePaint)
            } else {
                // Narrow slice: Show Emoji only
                insideBadgePaint.textSize = dp(11.5f)
                val fontMetrics = insideBadgePaint.fontMetrics
                val baseline = badgeY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(slice.emoji, badgeX, baseline, insideBadgePaint)
            }
        }
    }

    private fun drawCenterHUD(canvas: Canvas, cx: Float, cy: Float, innerRadius: Float) {
        centerBgPaint.color = boxColor
        canvas.drawCircle(cx, cy, innerRadius - dp(1.5f), centerBgPaint)

        val hudRadius = innerRadius - dp(6f)
        val maxTextWidth = hudRadius * 1.75f

        if (selectedSliceIndex in slices.indices) {
            val sel = slices[selectedSliceIndex]
            val pct = Math.round((sel.value / totalVal) * 100).toInt()
            val durStr = formatDuration(sel.value.toLong())
            val sliceColor = try { Color.parseColor(sel.colorHex) } catch (_: Exception) { primaryColor }

            val title = if (sel.subCount > 1) "${sel.emoji} ${sel.label} (${sel.subCount})" else "${sel.emoji} ${sel.label}"
            centerTitlePaint.textSize = dp(13.5f)
            centerTitlePaint.color = textColor
            val elTitle = ellipsizeText(title, centerTitlePaint, maxTextWidth)
            canvas.drawText(elTitle, cx, cy - dp(14f), centerTitlePaint)

            centerValPaint.textSize = dp(19f)
            centerValPaint.color = sliceColor
            canvas.drawText(durStr, cx, cy + dp(7f), centerValPaint)

            centerSubPaint.textSize = dp(11f)
            centerSubPaint.color = Color.argb(170, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            canvas.drawText("$pct% of study time", cx, cy + dp(23f), centerSubPaint)
        } else {
            centerTitlePaint.textSize = dp(12.5f)
            centerTitlePaint.color = Color.argb(160, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            canvas.drawText("Total Study Time", cx, cy - dp(13f), centerTitlePaint)

            centerValPaint.textSize = dp(20f)
            centerValPaint.color = textColor
            canvas.drawText(formatDuration(totalVal.toLong()), cx, cy + dp(8f), centerValPaint)

            centerSubPaint.textSize = dp(10.5f)
            centerSubPaint.color = primaryColor
            canvas.drawText("Tap any slice for details", cx, cy + dp(23f), centerSubPaint)
        }
    }

    // =========================================================================
    // 2. MODERN 3D SOLID PIE CHART RENDERING (Inside Labels + Pop-Out + Zero Clutter)
    // =========================================================================
    private fun drawModernPieMode(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val pieCy = h * 0.44f
        val radius = min(w, h) * 0.38f
        val maxPopOffset = dp(12f)

        strokePaint.strokeWidth = dp(2f)
        var startAngle = -90f

        // 1. Draw unselected slices strictly at anchored angles
        for (i in slices.indices) {
            val slice = slices[i]
            val sweepAngle = ((slice.value / totalVal) * 360.0).toFloat()
            if (sweepAngle > 0f) {
                if (i != selectedSliceIndex) {
                    drawSinglePieSlice(canvas, cx, pieCy, radius, startAngle, sweepAngle, slice, isSelected = false, popOffset = 0f)
                }
                startAngle += sweepAngle
            }
        }

        // 2. Draw selected slice on top with 3D pop elevation & glow
        if (selectedSliceIndex in slices.indices) {
            var curAngle = -90f
            for (i in 0 until selectedSliceIndex) {
                curAngle += ((slices[i].value / totalVal) * 360.0).toFloat()
            }
            val selSlice = slices[selectedSliceIndex]
            val selSweep = ((selSlice.value / totalVal) * 360.0).toFloat()
            val popOffset = maxPopOffset * popAnimProgress

            drawSinglePieSlice(canvas, cx, pieCy, radius, curAngle, selSweep, selSlice, isSelected = true, popOffset = popOffset)
        }

        // 3. Draw Bottom Interactive Focus Strip / Pill for Pie mode
        drawPieBottomPill(canvas, w, h, cx)
    }

    private fun drawSinglePieSlice(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        slice: PieSlice,
        isSelected: Boolean,
        popOffset: Float
    ) {
        val midAngle = startAngle + sweepAngle / 2f
        val midAngleRad = Math.toRadians(midAngle.toDouble())

        val sliceCx = (cx + popOffset * cos(midAngleRad)).toFloat()
        val sliceCy = (cy + popOffset * sin(midAngleRad)).toFloat()

        rectF.set(sliceCx - radius, sliceCy - radius, sliceCx + radius, sliceCy + radius)
        val sliceColor = try { Color.parseColor(slice.colorHex) } catch (_: Exception) { primaryColor }

        // 3D Glow under popped slice
        if (isSelected && popOffset > 0f) {
            pieSliceGlowPaint.color = Color.argb(100, Color.red(sliceColor), Color.green(sliceColor), Color.blue(sliceColor))
            pieSliceGlowPaint.strokeWidth = dp(7f)
            canvas.drawArc(rectF, startAngle, sweepAngle, true, pieSliceGlowPaint)
        }

        // Solid Pie Wedge
        paint.color = sliceColor
        canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
        strokePaint.color = boxColor
        canvas.drawArc(rectF, startAngle, sweepAngle, true, strokePaint)

        // Inside Slice Labeling (Subject Name, 2-line splitting, Emoji, and Percentage)
        if (sweepAngle >= 18f) {
            val textRadius = radius * if (sweepAngle >= 42f) 0.62f else 0.70f
            val labelX = (sliceCx + textRadius * cos(midAngleRad)).toFloat()
            val labelY = (sliceCy + textRadius * sin(midAngleRad)).toFloat()

            val pct = Math.round((slice.value / totalVal) * 100).toInt()
            val contrastColor = getContrastingTextColor(sliceColor)

            insideBadgePaint.color = contrastColor
            insideSubBadgePaint.color = Color.argb(200, Color.red(contrastColor), Color.green(contrastColor), Color.blue(contrastColor))

            val maxInsideW = radius * 0.55f
            val words = slice.label.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            if (sweepAngle >= 42f && words.isNotEmpty()) {
                insideBadgePaint.textSize = dp(11f)
                insideSubBadgePaint.textSize = dp(9.5f)

                if (words.size >= 2) {
                    val line1 = "${slice.emoji} ${words[0]}"
                    val line2 = "${words.drop(1).joinToString(" ")} $pct%"
                    val el1 = ellipsizeText(line1, insideBadgePaint, maxInsideW)
                    val el2 = ellipsizeText(line2, insideSubBadgePaint, maxInsideW)

                    canvas.drawText(el1, labelX, labelY - dp(3f), insideBadgePaint)
                    canvas.drawText(el2, labelX, labelY + dp(10f), insideSubBadgePaint)
                } else {
                    val displayTitle = if (slice.subCount > 1) "${slice.emoji} ${slice.label} (${slice.subCount})" else "${slice.emoji} ${slice.label}"
                    val elTitle = ellipsizeText(displayTitle, insideBadgePaint, maxInsideW)

                    canvas.drawText(elTitle, labelX, labelY - dp(3f), insideBadgePaint)
                    canvas.drawText("$pct%", labelX, labelY + dp(10f), insideSubBadgePaint)
                }
            } else if (sweepAngle >= 26f) {
                insideBadgePaint.textSize = dp(11f)
                val badgeText = "${slice.emoji} $pct%"
                val fontMetrics = insideBadgePaint.fontMetrics
                val baseline = labelY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(badgeText, labelX, baseline, insideBadgePaint)
            } else {
                insideBadgePaint.textSize = dp(12f)
                val fontMetrics = insideBadgePaint.fontMetrics
                val baseline = labelY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(slice.emoji, labelX, baseline, insideBadgePaint)
            }
        }
    }

    private fun drawPieBottomPill(canvas: Canvas, w: Float, h: Float, cx: Float) {
        val pillH = dp(32f)
        val pillY = h - pillH - dp(6f)
        val pillW = min(w - dp(32f), dp(320f))

        pillRectF.set(cx - pillW / 2f, pillY, cx + pillW / 2f, pillY + pillH)

        if (selectedSliceIndex in slices.indices) {
            val sel = slices[selectedSliceIndex]
            val pct = Math.round((sel.value / totalVal) * 100).toInt()
            val durStr = formatDuration(sel.value.toLong())
            val sliceColor = try { Color.parseColor(sel.colorHex) } catch (_: Exception) { primaryColor }

            pillBgPaint.color = Color.argb(45, Color.red(sliceColor), Color.green(sliceColor), Color.blue(sliceColor))
            pillStrokePaint.color = sliceColor
            canvas.drawRoundRect(pillRectF, dp(16f), dp(16f), pillBgPaint)
            canvas.drawRoundRect(pillRectF, dp(16f), dp(16f), pillStrokePaint)

            centerTitlePaint.textSize = dp(12f)
            centerTitlePaint.color = textColor
            val title = if (sel.subCount > 1) "${sel.emoji} ${sel.label} (${sel.subCount}): $durStr ($pct%)" else "${sel.emoji} ${sel.label}: $durStr ($pct%)"
            val elTitle = ellipsizeText(title, centerTitlePaint, pillW - dp(24f))
            canvas.drawText(elTitle, cx, pillY + dp(20f), centerTitlePaint)
        } else {
            pillBgPaint.color = Color.argb(30, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            pillStrokePaint.color = Color.argb(70, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            canvas.drawRoundRect(pillRectF, dp(16f), dp(16f), pillBgPaint)
            canvas.drawRoundRect(pillRectF, dp(16f), dp(16f), pillStrokePaint)

            centerTitlePaint.textSize = dp(11.5f)
            centerTitlePaint.color = Color.argb(200, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            val hintText = "Total: ${formatDuration(totalVal.toLong())} • Tap any slice for details"
            canvas.drawText(hintText, cx, pillY + dp(20f), centerTitlePaint)
        }
    }

    private fun getContrastingTextColor(bgColor: Int): Int {
        val y = (299 * Color.red(bgColor) + 587 * Color.green(bgColor) + 114 * Color.blue(bgColor)) / 1000
        return if (y >= 160) Color.parseColor("#0F172A") else Color.WHITE
    }
}
