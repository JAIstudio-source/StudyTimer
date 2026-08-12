package com.madeby.JAI

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.min

class Soft3DBubbleDrawable(
    private var baseColor: Int,
    private var cornerRadiusPx: Float = 60f,
    private var isDarkBg: Boolean = true,
    private var elevationPx: Float = 8f
) : Drawable() {

    var isPressed: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateSelf()
            }
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }
    private val bottomRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
    }

    fun updateColor(newColor: Int) {
        baseColor = newColor
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val r = min(cornerRadiusPx, min(width, height) / 2f)

        val pressedOffset = if (isPressed) elevationPx * 0.4f else 0f
        val currentElevation = if (isPressed) elevationPx * 0.2f else elevationPx

        // 1. Soft Ambient Drop Shadow (Dark depth underneath)
        val shadowRect = RectF(
            bounds.left + 2f,
            bounds.top + currentElevation,
            bounds.right - 2f,
            bounds.bottom + currentElevation * 1.1f
        )
        shadowPaint.color = Color.argb(if (isDarkBg) 70 else 30, 0, 0, 0)
        canvas.drawRoundRect(shadowRect, r, r, shadowPaint)

        // 2. Main Soft Volumetric 3D Body
        val bodyRect = RectF(
            bounds.left.toFloat(),
            bounds.top.toFloat() + pressedOffset,
            bounds.right.toFloat(),
            bounds.bottom.toFloat() - (elevationPx - pressedOffset)
        )

        val lightTop = blendColor(baseColor, Color.WHITE, if (isPressed) 0.08f else 0.22f)
        val darkBottom = blendColor(baseColor, Color.BLACK, if (isPressed) 0.20f else 0.12f)

        fillPaint.shader = LinearGradient(
            bodyRect.left, bodyRect.top,
            bodyRect.left, bodyRect.bottom,
            intArrayOf(lightTop, baseColor, darkBottom),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(bodyRect, r, r, fillPaint)

        // 3. Subtle Ultra-Refined Top Highlight Stroke (No White Glare/Wash)
        topRimPaint.color = Color.argb(if (isDarkBg) 40 else 25, 255, 255, 255)
        val rimRect = RectF(bodyRect).apply { inset(1f, 1f) }
        canvas.drawRoundRect(rimRect, r, r, topRimPaint)

        // 4. Subtle Bottom Shadow Rim for Tangible 3D Border Definition
        bottomRimPaint.color = Color.argb(if (isDarkBg) 50 else 20, 0, 0, 0)
        val bottomRimRect = RectF(bodyRect).apply { inset(1.5f, 1.5f) }
        canvas.drawRoundRect(bottomRimRect, r, r, bottomRimPaint)
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * ratio).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * ratio).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratio).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        shadowPaint.alpha = alpha
        topRimPaint.alpha = alpha
        bottomRimPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
