package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.appcompat.widget.AppCompatButton
import kotlin.math.PI
import kotlin.math.min

class HoldRingButton(context: Context) : AppCompatButton(context) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var ringColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        (background as? Soft3DBubbleDrawable)?.isPressed = pressed
        animate().scaleX(if (pressed) 0.96f else 1.0f)
            .scaleY(if (pressed) 0.96f else 1.0f)
            .setDuration(if (pressed) 90L else 220L)
            .setInterpolator(if (pressed) android.view.animation.DecelerateInterpolator() else android.view.animation.OvershootInterpolator(1.3f))
            .start()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val cornerRadius = 80f
    private val ringPath = Path()
    private val cornerRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0.05f) return
        paint.color = ringColor
        val inset = paint.strokeWidth / 2f
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val w = right - left
        val h = bottom - top
        val r = min(cornerRadius, min(w, h) / 2f)
        val arcLen = (PI * r / 2f).toFloat()
        val edgeW = w - 2f * r
        val edgeH = h - 2f * r
        val totalLen = 2f * (edgeW + edgeH) + 4f * arcLen
        var remaining = progress * totalLen
        ringPath.reset()

        fun lineSegment(fullLen: Float, startX: Float, startY: Float, endX: Float, endY: Float) {
            if (remaining <= 0f || fullLen <= 0f) return
            if (remaining >= fullLen) {
                ringPath.moveTo(startX, startY)
                ringPath.lineTo(endX, endY)
                remaining -= fullLen
            } else {
                val t = remaining / fullLen
                ringPath.moveTo(startX, startY)
                ringPath.lineTo(startX + (endX - startX) * t, startY + (endY - startY) * t)
                remaining = 0f
            }
        }

        fun cornerArc(l: Float, t: Float, rVal: Float, b: Float, startAngle: Float) {
            if (remaining <= 0f || arcLen <= 0f) return
            cornerRect.set(l, t, rVal, b)
            if (remaining >= arcLen) {
                ringPath.addArc(cornerRect, startAngle, 90f)
                remaining -= arcLen
            } else {
                ringPath.addArc(cornerRect, startAngle, (remaining / arcLen) * 90f)
                remaining = 0f
            }
        }

        lineSegment(edgeW, left + r, top, right - r, top)
        cornerArc(right - 2f * r, top, right, top + 2f * r, 270f)
        lineSegment(edgeH, right, top + r, right, bottom - r)
        cornerArc(right - 2f * r, bottom - 2f * r, right, bottom, 0f)
        lineSegment(edgeW, right - r, bottom, left + r, bottom)
        cornerArc(left, bottom - 2f * r, left + 2f * r, bottom, 90f)
        lineSegment(edgeH, left, bottom - r, left, top + r)
        canvas.drawPath(ringPath, paint)
    }
}
