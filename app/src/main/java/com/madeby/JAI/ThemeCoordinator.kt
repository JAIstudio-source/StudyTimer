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
    var primaryColor = Color.parseColor("#A78BFA")
    var secondaryColor = Color.parseColor("#38BDF8")
    var accentColor = Color.HSVToColor(floatArrayOf(36f, 0.80f, 0.95f))

    companion object {
        val SOFT_FOCUS_PALETTE = listOf(
            "#818CF8", // Soft Lavender / Indigo
            "#60A5FA", // Soft Sky Blue
            "#38BDF8", // Pastel Cyan
            "#A78BFA", // Light Purple
            "#F472B6", // Pastel Rose
            "#FB7185", // Soft Coral
            "#FB923C", // Pastel Peach
            "#FBBF24", // Warm Amber
            "#34D399", // Soft Mint
            "#2DD4BF"  // Aqua Teal
        )

        val SOFT_BREAK_PALETTE = listOf(
            "#34D399", // Mint Green
            "#2DD4BF", // Soothing Teal
            "#38BDF8", // Calm Sky Blue
            "#A7F3D0", // Soft Seafoam
            "#6EE7B7", // Pastel Emerald
            "#818CF8", // Relaxing Lavender
            "#F472B6", // Rose Quartz
            "#FDE047", // Warm Lemon
            "#FDBA74", // Light Apricot
            "#94A3B8"  // Zen Slate
        )

        // Decoupled dedicated analytics accent for Insights floating nav capsule
        val INSIGHTS_NAV_ACCENT = Color.parseColor("#6366F1")
    }

    fun applyThemeCoordinates() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putString("ui_style", "BUBBLE")
                .putInt("customHue", 255)
                .putInt("customPrimary", Color.parseColor("#A78BFA"))
                .putInt("customSecondaryHue", 199)
                .putInt("customSecondary", Color.parseColor("#38BDF8"))
                .apply()
        }

        activeBgMode = sharedPrefs.getString("activeBgMode", "OLED") ?: "OLED"
        uiStyle = sharedPrefs.getString("ui_style", "BUBBLE") ?: "BUBBLE"

        when (activeBgMode) {
            "LIGHT" -> {
                bgColor = 0xFFF8FAFC.toInt()
                boxColor = 0xFFFFFFFF.toInt()
                textColor = 0xFF0F172A.toInt()
                accentColor = Color.parseColor("#4F46E5")
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
            val defaultPrimary = if (activeBgMode == "LIGHT") Color.parseColor("#4F46E5") else Color.parseColor("#A78BFA")
            val defaultSecondary = if (activeBgMode == "LIGHT") Color.parseColor("#0284C7") else Color.parseColor("#38BDF8")
            primaryColor = sharedPrefs.safeInt("customPrimary", defaultPrimary)
            secondaryColor = sharedPrefs.safeInt("customSecondary", defaultSecondary)
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
                top = 0xFFF8FAFC.toInt()
                bottom = 0xFFF1F5F9.toInt()
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

    fun createCardBackground(cornerRadius: Float = 26f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            if (activeBgMode == "LIGHT") {
                return GradientDrawable().apply {
                    this.cornerRadius = cornerRadius * density
                    setColor(0xFFFFFFFF.toInt())
                    setStroke((1f * density).toInt(), 0xFFE2E8F0.toInt())
                }
            }
            val fillTop = when (activeBgMode) {
                "ECLIPSE" -> 0xFF1E293B.toInt()
                else -> 0xFF161820.toInt()
            }
            val fillBottom = when (activeBgMode) {
                "ECLIPSE" -> 0xFF0F172A.toInt()
                else -> 0xFF101217.toInt()
            }
            return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(fillTop, fillBottom)).apply {
                this.cornerRadius = cornerRadius * density
                setStroke((1f * density).toInt(), 0xFF282A36.toInt())
            }
        }
        if (!isGlassStyle()) {
            val strokeCol = if (activeBgMode == "LIGHT") 0xFFE2E8F0.toInt() else 0xFF282A36.toInt()
            val fillCol = if (activeBgMode == "LIGHT") 0xFFFFFFFF.toInt() else (if (activeBgMode == "OLED") 0xFF121318.toInt() else boxColor)
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fillCol)
                setStroke((1 * density).toInt(), strokeCol)
            }
        }
        val fill: Int
        val stroke: Int
        when (activeBgMode) {
            "LIGHT" -> {
                fill = 0xFFFFFFFF.toInt()
                stroke = 0xFFE2E8F0.toInt()
            }
            "ECLIPSE" -> {
                fill = 0x1AFFFFFF.toInt()
                stroke = 0x33FFFFFF.toInt()
            }
            else -> {
                fill = 0xFF121318.toInt()
                stroke = 0xFF282A36.toInt()
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
        if (activeBgMode == "LIGHT") {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(0xFFFFFFFF.toInt())
                setStroke((1f * density).toInt(), 0xFFE2E8F0.toInt())
            }
        }
        if (isBubbleStyle()) {
            val dialogTop = when (activeBgMode) {
                "ECLIPSE" -> 0xFF1E293B.toInt()
                else -> 0xFF161922.toInt()
            }
            val dialogBottom = when (activeBgMode) {
                "ECLIPSE" -> 0xFF0F172A.toInt()
                else -> 0xFF0B0D12.toInt()
            }
            return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(dialogTop, dialogBottom)).apply {
                this.cornerRadius = cornerRadius * density
                setStroke((1.5f * density).toInt(), 0x3EFFFFFF.toInt())
            }
        }
        if (!isGlassStyle()) {
            val fillCol = if (activeBgMode == "OLED") 0xFF0D0F14.toInt() else boxColor
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fillCol)
                setStroke((1 * density).toInt(), Color.argb(55, 255, 255, 255))
            }
        }
        val fill = if (activeBgMode == "ECLIPSE") 0xF00F172A.toInt() else 0xEE0D0F15.toInt()
        val stroke = 0x38FFFFFF.toInt()
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
            val fill = if (isDarkMode()) 0x22FFFFFF.toInt() else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) 0x33FFFFFF.toInt() else (if (borderColor != 0) borderColor else 0x330F172A.toInt())
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fill)
                setStroke((1 * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            val fill = if (isDarkMode()) Color.argb(40, 255, 255, 255) else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) borderColor else (if (borderColor != 0) borderColor else Color.argb(50, 15, 23, 42))
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fill)
                setStroke((1 * density).toInt(), stroke)
            }
        }
        val fill = if (isDarkMode()) 0x22FFFFFF.toInt() else 0xFFF1F5F9.toInt()
        val stroke = if (isDarkMode()) borderColor else (if (borderColor != 0) borderColor else Color.argb(50, 15, 23, 42))
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1.5f * density).toInt(), stroke)
        }
    }

    fun createGlassIconBackground(borderColor: Int): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) 0x28FFFFFF.toInt() else (if (borderColor != 0) borderColor else 0x330F172A.toInt())
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDarkMode()) boxColor else 0xFFF1F5F9.toInt())
                setStroke((1 * density).toInt(), if (activeBgMode == "LIGHT") Color.argb(40, 15, 23, 42) else Color.argb(45, 255, 255, 255))
            }
        }
        val fill = if (isDarkMode()) 0x1CFFFFFF.toInt() else 0xFFF1F5F9.toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((2 * density).toInt(), if (isDarkMode()) borderColor else (if (borderColor != 0) borderColor else Color.argb(40, 15, 23, 42)))
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
