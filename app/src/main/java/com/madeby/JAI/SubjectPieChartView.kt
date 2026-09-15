package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SubjectPieChartView(context: Context) : View(context) {

    data class PieSlice(
        val label: String,
        val emoji: String,
        val value: Double,
        val colorHex: String
    )

    private val slices = ArrayList<PieSlice>()
    private var totalVal = 0.0
    var primaryColor: Int = Color.parseColor("#6366F1")
    var textColor: Int = Color.WHITE

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
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

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val floatingLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
    }

    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
    }

    private val rectF = RectF()

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
        slices.clear()
        val validItems = items.filter { it.value > 0 }
        val sum = validItems.sumOf { it.value }

        // If there are many subjects (> 6) and multiple small slices (< 2.5%), group the tiny slivers into "Other Subjects"
        if (validItems.size > 6 && sum > 0) {
            val mainSlices = ArrayList<PieSlice>()
            var otherSum = 0.0
            var otherCount = 0

            for (item in validItems) {
                val pct = (item.value / sum) * 100.0
                if (pct < 2.5) {
                    otherSum += item.value
                    otherCount++
                } else {
                    mainSlices.add(item)
                }
            }

            if (otherCount > 1) {
                slices.addAll(mainSlices)
                slices.add(PieSlice("Other Subjects", "📂", otherSum, "#64748B"))
            } else {
                slices.addAll(validItems)
            }
        } else {
            slices.addAll(validItems)
        }

        totalVal = slices.sumOf { it.value }.coerceAtLeast(0.001)
        invalidate()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width.toFloat(), height.toFloat()) * 0.28f
            val dx = event.x - cx
            val dy = event.y - cy
            if (dx * dx + dy * dy <= radius * radius) {
                performClick()
                return true
            }
            return false
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(18f, 14f), 0f)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        textAlign = Paint.Align.CENTER
    }

    private data class LabelNode(
        val slice: PieSlice,
        val midAngleRad: Double,
        val lineStartX: Float,
        val lineStartY: Float,
        val idealY: Float,
        var targetY: Float,
        val isRightSide: Boolean,
        val titleText: String,
        val subText: String,
        val sliceColor: Int
    )

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

        // Pie chart radius tuned to leave ample side margin for balanced floating labels
        val radius = min(w, h) * 0.26f
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        if (slices.isEmpty() || totalVal <= 0.0) {
            emptyPaint.strokeWidth = dp(2f)
            emptyPaint.color = Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            glowPaint.color = Color.argb(18, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))

            emptyTextPaint.textSize = dp(13.5f)
            subTextPaint.textSize = dp(11f)

            canvas.drawCircle(cx, cy, radius, glowPaint)
            canvas.drawCircle(cx, cy, radius, emptyPaint)
            canvas.drawText("⏱ No session data", cx, cy - dp(2f), emptyTextPaint)
            canvas.drawText("Log study time to see breakdown", cx, cy + dp(15f), subTextPaint)
            return
        }

        strokePaint.strokeWidth = dp(1.8f)
        linePaint.strokeWidth = dp(1.8f)

        var startAngle = -90f

        // 1. Draw solid pie arcs with slice dividers
        for (slice in slices) {
            val sweepAngle = ((slice.value / totalVal) * 360.0).toFloat()
            if (sweepAngle > 0f) {
                paint.color = try { Color.parseColor(slice.colorHex) } catch (_: Exception) { primaryColor }
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
                canvas.drawArc(rectF, startAngle, sweepAngle, true, strokePaint)
                startAngle += sweepAngle
            }
        }

        // 2. Collect label nodes for both sides
        startAngle = -90f
        val rightNodes = ArrayList<LabelNode>()
        val leftNodes = ArrayList<LabelNode>()

        for (i in slices.indices) {
            val slice = slices[i]
            val sweepAngle = ((slice.value / totalVal) * 360.0).toFloat()
            if (sweepAngle > 0f) {
                val midAngle = startAngle + sweepAngle / 2f
                val midAngleRad = Math.toRadians(midAngle.toDouble())

                val lineStartX = (cx + radius * 0.96f * cos(midAngleRad)).toFloat()
                val lineStartY = (cy + radius * 0.96f * sin(midAngleRad)).toFloat()
                val idealY = (cy + radius * 1.15f * sin(midAngleRad)).toFloat()

                val isRightSide = cos(midAngleRad) >= 0

                val pct = Math.round((slice.value / totalVal) * 100).toInt()
                val durStr = formatDuration(slice.value.toLong())
                val titleText = "${slice.emoji} ${slice.label}".trim()
                val subText = "$durStr ($pct%)"
                val sliceColor = try { Color.parseColor(slice.colorHex) } catch (_: Exception) { primaryColor }

                val node = LabelNode(
                    slice = slice,
                    midAngleRad = midAngleRad,
                    lineStartX = lineStartX,
                    lineStartY = lineStartY,
                    idealY = idealY,
                    targetY = idealY,
                    isRightSide = isRightSide,
                    titleText = titleText,
                    subText = subText,
                    sliceColor = sliceColor
                )

                if (isRightSide) {
                    rightNodes.add(node)
                } else {
                    leftNodes.add(node)
                }

                startAngle += sweepAngle
            }
        }

        // Sort both sides strictly from top to bottom (ascending Y) to guarantee lines never cross
        rightNodes.sortBy { it.idealY }
        leftNodes.sortBy { it.idealY }

        // Layout and render labels for each side with collision resolution
        renderSideLabels(canvas, rightNodes, isRightSide = true, w = w, h = h, cx = cx, radius = radius)
        renderSideLabels(canvas, leftNodes, isRightSide = false, w = w, h = h, cx = cx, radius = radius)
    }

    private fun renderSideLabels(
        canvas: Canvas,
        nodes: ArrayList<LabelNode>,
        isRightSide: Boolean,
        w: Float,
        h: Float,
        cx: Float,
        radius: Float
    ) {
        if (nodes.isEmpty()) return

        val count = nodes.size
        val topBound = dp(18f)
        val bottomBound = h - dp(18f)
        val availableH = bottomBound - topBound

        var titleSize = dp(11.5f)
        var subSize = dp(9.5f)
        var itemHeight = dp(28f)

        // Dynamically scale down text and spacing if many items are clustered on one side
        if (count * itemHeight > availableH) {
            val scale = availableH / (count * itemHeight)
            titleSize = max(dp(8.5f), titleSize * scale)
            subSize = max(dp(7.0f), subSize * scale)
            itemHeight = max(dp(18f), itemHeight * scale)
        }

        val halfItem = itemHeight / 2f
        val minY = topBound + halfItem
        val maxY = bottomBound - halfItem

        // Step 1: Initialize clamped targetY
        for (node in nodes) {
            node.targetY = node.idealY.coerceIn(minY, maxY)
        }

        // Step 2: Forward push downwards (resolve downward overlaps)
        for (i in 1 until count) {
            if (nodes[i].targetY < nodes[i - 1].targetY + itemHeight) {
                nodes[i].targetY = nodes[i - 1].targetY + itemHeight
            }
        }

        // Step 3: Backward push upwards (resolve bottom overflow)
        if (nodes[count - 1].targetY > maxY) {
            nodes[count - 1].targetY = maxY
        }
        for (i in (count - 2) downTo 0) {
            if (nodes[i].targetY > nodes[i + 1].targetY - itemHeight) {
                nodes[i].targetY = nodes[i + 1].targetY - itemHeight
            }
        }

        // Step 4: Final bounds adjustment
        if (nodes[0].targetY < minY) {
            val shift = minY - nodes[0].targetY
            for (node in nodes) {
                node.targetY += shift
            }
            if (nodes[count - 1].targetY > maxY) {
                val uniformGap = if (count > 1) (availableH - itemHeight) / (count - 1) else 0f
                var curY = minY
                for (node in nodes) {
                    node.targetY = curY
                    curY += uniformGap
                }
            }
        }

        // Step 5: Render connector lines, elbow ticks, and 2-line labels
        for (node in nodes) {
            linePaint.color = node.sliceColor
            dotPaint.color = node.sliceColor

            floatingLabelPaint.textSize = titleSize
            subLabelPaint.textSize = subSize

            // Origin dot on pie edge
            canvas.drawCircle(node.lineStartX, node.lineStartY, dp(3f), dotPaint)

            if (isRightSide) {
                val elbowX = min(w - dp(80f), cx + radius + dp(12f))
                val tickEndX = min(w - dp(68f), elbowX + dp(10f))

                // Polyline: Pie -> Elbow -> Horizontal Arm
                canvas.drawLine(node.lineStartX, node.lineStartY, elbowX, node.targetY, linePaint)
                canvas.drawLine(elbowX, node.targetY, tickEndX, node.targetY, linePaint)

                // Text placement
                val textStartX = tickEndX + dp(5f)
                val maxAllowedWidth = max(dp(30f), w - dp(8f) - textStartX)

                floatingLabelPaint.textAlign = Paint.Align.LEFT
                subLabelPaint.textAlign = Paint.Align.LEFT
                floatingLabelPaint.color = textColor
                subLabelPaint.color = node.sliceColor

                val truncatedTitle = ellipsizeText(node.titleText, floatingLabelPaint, maxAllowedWidth)
                val truncatedSub = ellipsizeText(node.subText, subLabelPaint, maxAllowedWidth)

                canvas.drawText(truncatedTitle, textStartX, node.targetY - dp(2f), floatingLabelPaint)
                canvas.drawText(truncatedSub, textStartX, node.targetY + subSize + dp(1f), subLabelPaint)
            } else {
                val elbowX = max(dp(80f), cx - radius - dp(12f))
                val tickEndX = max(dp(68f), elbowX - dp(10f))

                // Polyline: Pie -> Elbow -> Horizontal Arm
                canvas.drawLine(node.lineStartX, node.lineStartY, elbowX, node.targetY, linePaint)
                canvas.drawLine(elbowX, node.targetY, tickEndX, node.targetY, linePaint)

                // Text placement
                val textEndX = tickEndX - dp(5f)
                val maxAllowedWidth = max(dp(30f), textEndX - dp(8f))

                floatingLabelPaint.textAlign = Paint.Align.RIGHT
                subLabelPaint.textAlign = Paint.Align.RIGHT
                floatingLabelPaint.color = textColor
                subLabelPaint.color = node.sliceColor

                val truncatedTitle = ellipsizeText(node.titleText, floatingLabelPaint, maxAllowedWidth)
                val truncatedSub = ellipsizeText(node.subText, subLabelPaint, maxAllowedWidth)

                canvas.drawText(truncatedTitle, textEndX, node.targetY - dp(2f), floatingLabelPaint)
                canvas.drawText(truncatedSub, textEndX, node.targetY + subSize + dp(1f), subLabelPaint)
            }
        }
    }
}
