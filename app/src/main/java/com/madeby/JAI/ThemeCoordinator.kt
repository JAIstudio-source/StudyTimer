package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build

class ThemeCoordinator(private val context: Context) {

    var activeBgMode = "OLED"
    var uiStyle = "BUBBLE"
    var bgColor = 0xFF000000.toInt()
    var boxColor = 0xFF121212.toInt()
    var textColor = 0xFFFFFFFF.toInt()
    var primaryColor = Color.HSVToColor(floatArrayOf(234f, 0.65f, 0.95f))
    var secondaryColor = Color.HSVToColor(floatArrayOf(1f, 0.65f, 0.95f))
    var accentColor = Color.HSVToColor(floatArrayOf(36f, 0.80f, 0.95f))

    fun applyThemeCoordinates() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putString("ui_style", "BUBBLE")
                .putInt("customHue", 234)
                .putInt("customPrimary", Color.HSVToColor(floatArrayOf(234f, 0.65f, 0.95f)))
                .putInt("customSecondaryHue", 1)
                .putInt("customSecondary", Color.HSVToColor(floatArrayOf(1f, 0.65f, 0.95f)))
                .apply()
        }

        activeBgMode = sharedPrefs.getString("activeBgMode", "OLED") ?: "OLED"
        uiStyle = sharedPrefs.getString("ui_style", "BUBBLE") ?: "BUBBLE"

        when (activeBgMode) {
            "LIGHT" -> {
                bgColor = 0xFFFFFFFF.toInt()
                boxColor = 0xFFEDF0F5.toInt()
                textColor = 0xFF0F172A.toInt()
                accentColor = Color.HSVToColor(floatArrayOf(30f, 0.85f, 0.75f))
            }
            "ECLIPSE" -> {
                bgColor = 0xFF0F172A.toInt()
                boxColor = 0xFF1E293B.toInt()
                textColor = 0xFFF8FAFC.toInt()
                accentColor = Color.HSVToColor(floatArrayOf(36f, 0.85f, 0.95f))
            }
            else -> {
                bgColor = 0xFF000000.toInt()
                boxColor = 0xFF000000.toInt() // True OLED Black
                textColor = 0xFFFFFFFF.toInt()
                accentColor = Color.HSVToColor(floatArrayOf(36f, 0.85f, 0.95f))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sharedPrefs.getBoolean("dynamic_color", false)) {
            primaryColor = context.getColor(android.R.color.system_accent1_500)
            secondaryColor = context.getColor(android.R.color.system_accent2_500)
        } else {
            primaryColor = sharedPrefs.safeInt("customPrimary", Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f)))
            secondaryColor = sharedPrefs.safeInt("customSecondary", Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f)))
        }
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun isDarkMode(): Boolean = activeBgMode != "LIGHT"
    fun isGlassStyle(): Boolean = uiStyle == "GLASS"
    fun isBubbleStyle(): Boolean = uiStyle == "BUBBLE"

    fun createBackgroundDrawable(): GradientDrawable {
        if (!isGlassStyle() && !isBubbleStyle()) {
            return GradientDrawable().apply { setColor(bgColor) }
        }
        val top: Int
        val bottom: Int
        when (activeBgMode) {
            "LIGHT" -> {
                top = 0xFFFDFDFF.toInt()
                bottom = 0xFFEAE6FF.toInt()
            }
            "ECLIPSE" -> {
                top = 0xFF0F172A.toInt()
                bottom = 0xFF1B1B3E.toInt()
            }
            else -> {
                // True black stays true black on OLED — no gradient
                top = 0xFF000000.toInt()
                bottom = 0xFF000000.toInt()
            }
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, bottom))
    }

    fun createCardBackground(cornerRadius: Float = 35f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            val fillTop = when (activeBgMode) {
                "LIGHT" -> 0xFFF8FAFC.toInt()
                "ECLIPSE" -> 0xFF1E293B.toInt()
                else -> 0xFF181B22.toInt()
            }
            val fillBottom = when (activeBgMode) {
                "LIGHT" -> 0xFFEDF2F7.toInt()
                "ECLIPSE" -> 0xFF0F172A.toInt()
                else -> 0xFF0E1015.toInt()
            }
            val strokeColor = if (isDarkMode()) 0x33FFFFFF.toInt() else 0x220F172A.toInt()
            return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(fillTop, fillBottom)).apply {
                this.cornerRadius = cornerRadius * density
                setStroke((1.5f * density).toInt(), strokeColor)
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(boxColor)
                setStroke((1 * density).toInt(), if (activeBgMode == "LIGHT") Color.argb(40, 15, 23, 42) else Color.argb(45, 255, 255, 255))
            }
        }
        val fill: Int
        val stroke: Int
        when (activeBgMode) {
            "LIGHT" -> {
                fill = 0xE6FFFFFF.toInt()
                stroke = 0x1A0F172A.toInt()
            }
            "ECLIPSE" -> {
                fill = 0x16FFFFFF.toInt()
                stroke = 0x2EFFFFFF.toInt()
            }
            else -> {
                fill = 0x0BFFFFFF.toInt()
                stroke = 0x22FFFFFF.toInt()
            }
        }
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1 * density).toInt(), stroke)
        }
    }

    fun createDialogBackground(cornerRadius: Float = 28f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            val dialogTop = when (activeBgMode) {
                "LIGHT" -> 0xFFFFFFFF.toInt()
                "ECLIPSE" -> 0xFF1E293B.toInt()
                else -> 0xFF181B22.toInt()
            }
            val dialogBottom = when (activeBgMode) {
                "LIGHT" -> 0xFFF1F5F9.toInt()
                "ECLIPSE" -> 0xFF0F172A.toInt()
                else -> 0xFF0E1015.toInt()
            }
            return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(dialogTop, dialogBottom)).apply {
                this.cornerRadius = cornerRadius * density
                setStroke((2f * density).toInt(), if (isDarkMode()) 0x44FFFFFF.toInt() else 0x330F172A.toInt())
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(boxColor)
                setStroke((1 * density).toInt(), if (activeBgMode == "LIGHT") Color.argb(45, 15, 23, 42) else Color.argb(55, 255, 255, 255))
            }
        }
        val fill: Int
        val stroke: Int
        when (activeBgMode) {
            "LIGHT" -> {
                fill = 0xF4F8FAFC.toInt()
                stroke = 0x2A0F172A.toInt()
            }
            "ECLIPSE" -> {
                fill = 0xF00F172A.toInt()
                stroke = 0x3EFFFFFF.toInt()
            }
            else -> {
                fill = 0xF2000000.toInt() // True OLED Black Glass Fill
                stroke = 0x33FFFFFF.toInt()
            }
        }
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1 * density).toInt(), stroke)
        }
    }

    fun createGlowGradient(colorHex: Int, cornerRadius: Float = 80f): GradientDrawable {
        if (!isGlassStyle() && !isBubbleStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius
                setColor(colorHex)
            }
        }
        val light = blend(colorHex, -1, 0.30f)
        val dark = blend(colorHex, 0xFF000000.toInt(), 0.20f)
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(light, colorHex, dark)).apply {
            this.cornerRadius = cornerRadius
        }
    }

    fun createGlowBlob(color: Int, radius: Float = 240f, alpha: Int = 55): GradientDrawable {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setGradientType(GradientDrawable.RADIAL_GRADIENT)
            setGradientRadius(radius * context.resources.displayMetrics.density)
            setColors(intArrayOf(Color.argb(alpha, r, g, b), Color.argb(0, r, g, b)))
        }
    }

    fun createGlassChip(borderColor: Int, cornerRadius: Float = 30f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            if (borderColor != 0 && Color.alpha(borderColor) > 150) {
                return Soft3DBubbleDrawable(borderColor, cornerRadius * density, isDarkMode(), elevationPx = 6f)
            }
            val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xE6FFFFFF.toInt()
            val stroke = if (isDarkMode()) 0x28FFFFFF.toInt() else 0x1A0F172A.toInt()
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fill)
                setStroke((1 * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(if (isDarkMode()) Color.argb(35, 255, 255, 255) else Color.argb(20, 15, 23, 42))
                setStroke((1 * density).toInt(), borderColor)
            }
        }
        val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xB3FFFFFF.toInt()
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((2 * density).toInt(), borderColor)
        }
    }

    fun createGlassIconBackground(borderColor: Int): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xFFFFFFFF.toInt()
            val stroke = if (isDarkMode()) 0x28FFFFFF.toInt() else 0x1A0F172A.toInt()
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(boxColor)
                setStroke((1 * density).toInt(), if (activeBgMode == "LIGHT") Color.argb(40, 15, 23, 42) else Color.argb(45, 255, 255, 255))
            }
        }
        val fill = if (isDarkMode()) 0x1CFFFFFF.toInt() else 0xFFFFFFFF.toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((2 * density).toInt(), borderColor)
        }
    }

    fun createButtonBackground(colorHex: Int): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            return Soft3DBubbleDrawable(colorHex, 80f * density, isDarkMode(), elevationPx = 14f)
        }
        return GradientDrawable().apply {
            cornerRadius = 80f * density
            setColor(colorHex)
        }
    }
}
