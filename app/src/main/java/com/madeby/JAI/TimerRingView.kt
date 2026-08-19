package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.min

class TimerRingView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val strokePx = 14f * density
    private val padPx = 26f * density

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
    }
    private val innerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx * 2.4f
        alpha = 26
    }

    private var color = 0xFF000000.toInt()
    private var trackColor = 0x1A000000.toInt()
    private var sweepDeg = 0f
    private var targetDeg = 0f
    private var animator: ValueAnimator? = null

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val base = if (h > 0) min(w, h) else w
        val size = (base * 0.88f).toInt().coerceAtLeast(1)
        setMeasuredDimension(size, size)
    }

    fun setProgress(fraction: Float, color: Int, track: Int) {
        val f = fraction.coerceIn(0f, 1f)
        val target = f * 360f
        val colorChanged = color != this.color || track != this.trackColor
        if (!colorChanged && abs(target - targetDeg) < 0.5f) return
        this.color = color
        this.trackColor = track
        targetDeg = target
        val from = sweepDeg
        if (abs(target - from) < 0.5f) {
            sweepDeg = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, target).apply {
            duration = 480
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                sweepDeg = a.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun applyArcShader(cx: Float, cy: Float) {
        if (sweepDeg <= 0.01f) {
            ringPaint.shader = null
            return
        }
        val light = blend(color, -1, 0.40f)
        val deep = blend(color, 0xFF000000.toInt(), 0.15f)
        val shader = SweepGradient(
            0f, 0f,
            intArrayOf(light, color, deep),
            floatArrayOf(0f, 0.7f, 1f)
        )
        val matrix = Matrix().apply {
            postTranslate(cx, cy)
            postRotate(-90f, cx, cy)
        }
        shader.setLocalMatrix(matrix)
        ringPaint.shader = shader
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f - strokePx / 2f - padPx
        if (radius <= 0f) return

        trackPaint.color = trackColor
        canvas.drawCircle(cx, cy, radius, trackPaint)

        if (sweepDeg > 0.01f) {
            val inset = strokePx / 2f + padPx
            val bounds = RectF(inset, inset, w - inset, h - inset)

            innerHaloPaint.color = color
            innerHaloPaint.alpha = 24
            canvas.drawArc(bounds, -90f, sweepDeg, false, innerHaloPaint)

            ringPaint.setShadowLayer(12f * density, 0f, 0f, Color.argb(130, Color.red(color), Color.green(color), Color.blue(color)))
            applyArcShader(cx, cy)
            canvas.drawArc(bounds, -90f, sweepDeg, false, ringPaint)
        }
    }
}
