package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build

class ThemeCoordinator(private val context: Context) {

    var activeBgMode = "OLED"
    var uiStyle = "GLASS"
    var bgColor = 0xFF000000.toInt()
    var boxColor = 0xFF121212.toInt()
    var textColor = 0xFFFFFFFF.toInt()
    var primaryColor = Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f))
    var secondaryColor = Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f))
    var accentColor = Color.HSVToColor(floatArrayOf(36f, 0.80f, 0.95f))

    fun applyThemeCoordinates() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putInt("customHue", 190)
                .putInt("customPrimary", Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f)))
                .putInt("customSecondaryHue", 120)
                .putInt("customSecondary", Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f)))
                .apply()
        }

        activeBgMode = sharedPrefs.getString("activeBgMode", "OLED") ?: "OLED"
        uiStyle = sharedPrefs.getString("ui_style", "GLASS") ?: "GLASS"

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
                boxColor = 0xFF121212.toInt()
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

    private fun isDarkMode(): Boolean = activeBgMode != "LIGHT"
    fun isGlassStyle(): Boolean = uiStyle == "GLASS"

    fun createBackgroundDrawable(): GradientDrawable {
        if (!isGlassStyle()) {
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

    fun createCardBackground(cornerRadius: Float = 35f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(boxColor)
                if (activeBgMode == "LIGHT") {
                    setStroke((1 * density).toInt(), Color.argb(30, 15, 23, 42))
                }
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
        val drawable = GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1 * density).toInt(), stroke)
        }
        return drawable
    }

    fun createDialogBackground(cornerRadius: Float = 28f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(boxColor)
            }
        }
        val fill: Int
        val stroke: Int
        when (activeBgMode) {
            "LIGHT" -> {
                fill = 0xFFFDFDFF.toInt()
                stroke = 0x1A0F172A.toInt()
            }
            "ECLIPSE" -> {
                fill = 0xFF151A2E.toInt()
                stroke = 0x2EFFFFFF.toInt()
            }
            else -> {
                fill = 0xFF121214.toInt()
                stroke = 0x26FFFFFF.toInt()
            }
        }
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1 * density).toInt(), stroke)
        }
    }

    fun createGlowGradient(colorHex: Int, cornerRadius: Float = 80f): GradientDrawable {
        if (!isGlassStyle()) {
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

    fun createGlassChip(borderColor: Int, cornerRadius: Float = 30f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(boxColor)
            }
        }
        val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xB3FFFFFF.toInt()
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((2 * density).toInt(), borderColor)
        }
    }

    fun createGlassIconBackground(borderColor: Int): GradientDrawable {
        val density = context.resources.displayMetrics.density
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(boxColor)
            }
        }
        val fill = if (isDarkMode()) 0x1CFFFFFF.toInt() else 0xFFFFFFFF.toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((2 * density).toInt(), borderColor)
        }
    }

    fun createButtonBackground(colorHex: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 80f
            setColor(colorHex)
        }
    }
}
