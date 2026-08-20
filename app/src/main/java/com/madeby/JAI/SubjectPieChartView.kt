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
        strokeWidth = 5f
        color = Color.parseColor("#111625")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val floatingLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textSize = 44f
    }

    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textSize = 34f
    }

    private val rectF = RectF()

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
        slices.addAll(items)
        totalVal = slices.sumOf { it.value }.coerceAtLeast(0.001)
        invalidate()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width.toFloat(), height.toFloat()) * 0.30f
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
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    private val drawnYList = ArrayList<Float>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        // Pie chart radius tuned to leave ample side margin for 2-line title + time labels
        val radius = min(w, h) * 0.29f
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        if (slices.isEmpty() || totalVal <= 0.0) {
            emptyPaint.strokeWidth = 3f * resources.displayMetrics.density
            emptyPaint.color = Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            glowPaint.color = Color.argb(18, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))

            canvas.drawCircle(cx, cy, radius, glowPaint)
            canvas.drawCircle(cx, cy, radius, emptyPaint)
            canvas.drawText("⏱ No session data", cx, cy - 6f, emptyTextPaint)
            canvas.drawText("Log study time to see breakdown", cx, cy + 34f, subTextPaint)
            return
        }

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

        // 2. Draw AUTO-BALANCED FLOATING labels with exact time & percentage
        startAngle = -90f
        drawnYList.clear()
        val padding = 12f

        for (i in slices.indices) {
            val slice = slices[i]
            val sweepAngle = ((slice.value / totalVal) * 360.0).toFloat()
            if (sweepAngle > 0f) {
                val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())

                val lineStartX = (cx + radius * 0.88f * cos(midAngleRad)).toFloat()
                val lineStartY = (cy + radius * 0.88f * sin(midAngleRad)).toFloat()

                val midX = (cx + radius * 1.34f * cos(midAngleRad)).toFloat()
                var midY = (cy + radius * 1.34f * sin(midAngleRad)).toFloat()

                // Prevent Y collision with previous labels
                for (prevY in drawnYList) {
                    if (abs(midY - prevY) < 58f) {
                        midY += if (midY < cy) -60f else 60f
                    }
                }
                drawnYList.add(midY)

                val isRightSide = cos(midAngleRad) >= 0

                val pct = ((slice.value / totalVal) * 100).toInt()
                val durStr = formatDuration(slice.value.toLong())
                val titleText = "${slice.emoji} ${slice.label}"
                val subText = "$durStr ($pct%)"

                // Dynamic font size fitting to ensure text fits inside card padding
                var currentTextSize = 38f
                floatingLabelPaint.textSize = currentTextSize
                subLabelPaint.textSize = currentTextSize * 0.86f

                var maxLabelWidth = max(floatingLabelPaint.measureText(titleText), subLabelPaint.measureText(subText))

                val maxAllowedWidth = if (isRightSide) (w - padding - midX - 15f) else (midX - padding - 15f)
                if (maxLabelWidth > maxAllowedWidth && maxAllowedWidth > 40f) {
                    val scale = maxAllowedWidth / maxLabelWidth
                    currentTextSize = max(24f, currentTextSize * scale)
                    floatingLabelPaint.textSize = currentTextSize
                    subLabelPaint.textSize = currentTextSize * 0.86f
                    maxLabelWidth = max(floatingLabelPaint.measureText(titleText), subLabelPaint.measureText(subText))
                }

                // Clamp armEndX to prevent clipping beyond container width
                val armEndX = if (isRightSide) {
                    min(midX + 20f, w - padding - maxLabelWidth - 4f)
                } else {
                    max(midX - 20f, padding + maxLabelWidth + 4f)
                }

                val sliceColor = try { Color.parseColor(slice.colorHex) } catch (_: Exception) { primaryColor }
                linePaint.color = sliceColor
                dotPaint.color = sliceColor

                // Draw origin dot on pie edge
                canvas.drawCircle(lineStartX, lineStartY, 7.5f, dotPaint)

                // Draw elbow line (radial line + horizontal elbow arm)
                canvas.drawLine(lineStartX, lineStartY, midX, midY, linePaint)
                canvas.drawLine(midX, midY, armEndX, midY, linePaint)

                floatingLabelPaint.color = textColor
                subLabelPaint.color = sliceColor

                if (isRightSide) {
                    floatingLabelPaint.textAlign = Paint.Align.LEFT
                    subLabelPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(titleText, armEndX + 8f, midY - 4f, floatingLabelPaint)
                    canvas.drawText(subText, armEndX + 8f, midY + currentTextSize + 2f, subLabelPaint)
                } else {
                    floatingLabelPaint.textAlign = Paint.Align.RIGHT
                    subLabelPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(titleText, armEndX - 8f, midY - 4f, floatingLabelPaint)
                    canvas.drawText(subText, armEndX - 8f, midY + currentTextSize + 2f, subLabelPaint)
                }

                startAngle += sweepAngle
            }
        }
    }
}
