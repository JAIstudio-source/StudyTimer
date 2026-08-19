package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Canvas-drawn weekly summary card with a fixed brand palette (independent of the
 * in-app theme) so shared cards look identical on every device. Renders a 7-day bar
 *  chart, a weekly-goal ring and a focus/break donut, plus stat tiles. Designed
 *  for a 9:16 portrait canvas (1080 x 1920) and scales with the view's size.
 */
class WeeklyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val logoBitmap: android.graphics.Bitmap? = try {
        android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.mylogo)
    } catch (_: Throwable) {
        null
    }

    data class Day(val label: String, val secs: Long, val goal: Long)

    data class CardData(
        val dateRange: String,
        val totalSecs: Long,
        val breakSecs: Long,
        val bestName: String,
        val bestSecs: Long,
        val streak: Int,
        val vsPrev: String,
        val sessionCount: Int,
        val days: List<Day>,
        val hasData: Boolean
    )

    private var data: CardData? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setData(d: CardData) {
        data = d
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = data ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // ---- scales: fonts scale with width and, more strongly, with tallness ----
        val fs = w / 1080f
        val tall = (h / 1080f).coerceIn(1f, 1.78f)
        val sz = fs * (0.4f + 0.6f * tall)
        val p = w * 0.06f
        val gap = 22f * sz

        // ---- brand palette (pure black + gold, fixed not theme-driven) ----
        val bgTop = 0xFF000000.toInt()
        val bgBottom = 0xFF000000.toInt()
        val gold = 0xFFF6CB22.toInt()
        val goldLight = 0xFFFDE68A.toInt()
        val goldDeep = 0xFFEDB01D.toInt()
        val breakCol = 0xFF8B5CF6.toInt()
        val amber = 0xFFFBBF24.toInt()
        val green = 0xFF34D399.toInt()
        val white = Color.WHITE
        val white60 = 0x99FFFFFF.toInt()
        val white40 = 0x66FFFFFF.toInt()
        val white20 = 0x33FFFFFF.toInt()
        val white12 = 0x1FFFFFFF.toInt()
        val tileBg = 0x14FFFFFF.toInt()
        val deltaUp = green
        val deltaDown = 0xFFF87171.toInt()
        val shadow = 0x14000000.toInt()

        // ---- clip to rounded card + gradient background ----
        val clip = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), 44f * fs, 44f * fs, Path.Direction.CW)
        }
        canvas.clipPath(clip)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, bgTop, bgBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        var y = p

        // ================= HEADER =================
        val glyph = 42f * sz
        val gx = p
        val logo = logoBitmap
        if (logo != null) {
            val iconSize = glyph.toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(logo, iconSize, iconSize, true)
            val iconClip = Path().apply {
                addRoundRect(RectF(gx, y, gx + glyph, y + glyph), 12f * fs, 12f * fs, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(iconClip)
            canvas.drawBitmap(scaled, gx, y, null)
            canvas.restore()
        } else {
            val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = amber }
            canvas.drawRoundRect(RectF(gx, y, gx + glyph, y + glyph), 9f * fs, 9f * fs, glyphPaint)
            val pagePaint = Paint().apply {
                color = bgTop; strokeWidth = 2.4f * fs; strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(gx + glyph * 0.28f, y + glyph * 0.45f, gx + glyph * 0.72f, y + glyph * 0.45f, pagePaint)
            canvas.drawLine(gx + glyph * 0.28f, y + glyph * 0.62f, gx + glyph * 0.72f, y + glyph * 0.62f, pagePaint)
        }

        val titlePaint = textPaint(30f * sz, white, Typeface.DEFAULT_BOLD, 0.12f)
        canvas.drawText(
            "WEEKLY SUMMARY",
            gx + glyph + 14f * fs,
            y + glyph / 2f - (titlePaint.fontMetrics.ascent + titlePaint.fontMetrics.descent) / 2f,
            titlePaint
        )
        y += glyph + 10f * sz

        val datePaint = textPaint(22f * sz, white60, Typeface.create("sans-serif-medium", Typeface.NORMAL), 0.16f)
        canvas.drawText(d.dateRange.uppercase(), p, y - datePaint.fontMetrics.ascent, datePaint)
        y += 26f * sz
        canvas.drawRoundRect(RectF(p, y, w - p, y + 1.4f * fs), 1f, 1f, Paint().apply { color = white20 })
        y += gap + 12f * sz

        // ================= HERO TOTAL =================
        val heroLabelPaint = textPaint(20f * sz, white40, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.22f)
        val heroLabel = "TOTAL FOCUS THIS WEEK"
        canvas.drawText(heroLabel, w / 2f - heroLabelPaint.measureText(heroLabel) / 2f, y, heroLabelPaint)
        val heroPaint = textPaint(76f * sz, white, Typeface.DEFAULT_BOLD, 0.02f)
        val heroText = formatTime(d.totalSecs)
        val heroW = heroPaint.measureText(heroText)
        val heroFm = heroPaint.fontMetrics
        val heroTop = y + heroLabelPaint.fontMetrics.descent + 14f * sz
        val heroBaseline = heroTop - heroFm.ascent
        val heroBottom = heroBaseline + heroFm.descent
        // soft golden halo behind the hero total
        val heroGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, heroTop, heroW * 0.85f,
                intArrayOf(0x59F6CB22.toInt(), 0x00F6CB22.toInt()), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w / 2f, heroTop, heroW * 0.85f, heroGlow)
        heroPaint.shader = LinearGradient(
            w / 2f - heroW / 2f, heroTop,
            w / 2f + heroW / 2f, heroBottom,
            intArrayOf(goldLight, gold, goldDeep), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawText(heroText, w / 2f - heroW / 2f, heroBaseline, heroPaint)
        y = heroBaseline + heroFm.descent + 26f * sz

        if (!d.hasData) {
            val cy = h * 0.60f
            val clockR = 46f * sz
            val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f * sz
                color = white20
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawCircle(w / 2f, cy, clockR, clockPaint)
            val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = white40
                strokeWidth = 6f * sz
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(w / 2f, cy, w / 2f, cy - clockR * 0.55f, handPaint)
            canvas.drawLine(w / 2f, cy, w / 2f + clockR * 0.55f, cy, handPaint)

            val phPaint = textPaint(25f * sz, white60, Typeface.create("sans-serif-medium", Typeface.NORMAL))
            val phText = "No sessions recorded this week yet."
            canvas.drawText(phText, w / 2f - phPaint.measureText(phText) / 2f, cy + clockR + 46f * sz, phPaint)
            val subPaint = textPaint(19f * sz, white40, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.18f)
            val subText = "START A SESSION TO BUILD YOUR STREAK"
            canvas.drawText(subText, w / 2f - subPaint.measureText(subText) / 2f, cy + clockR + 46f * sz + 36f * sz, subPaint)
            drawFooter(canvas, w, h, p, sz)
            return
        }

        // ================= DAILY FOCUS BAR CHART =================
        y += 6f * sz
        val sectionPaint = textPaint(20f * sz, white60, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.24f)
        canvas.drawText("DAILY FOCUS", p, y, sectionPaint)
        y += 24f * sz

        val chartTop = y
        val chartH = (h * 0.25f).coerceAtLeast(150f * sz)
        val chartBottom = chartTop + chartH
        val chartLeft = p
        val chartRight = w - p
        val slotW = (chartRight - chartLeft) / 7f
        val maxSecs = d.days.maxOfOrNull { it.secs }?.coerceAtLeast(1L) ?: 1L

        val labelArea = 30f * sz
        val topHeadroom = 70f * sz
        val innerBottom = chartBottom - labelArea
        val barMaxTop = chartTop + topHeadroom
        val chartInnerH = innerBottom - barMaxTop

        // weekly average goal line (computed first so gridlines can dodge it)
        val avgGoal = d.days.map { it.goal }.average().toFloat().coerceAtLeast(0f)
        val goalFrac = if (avgGoal > 0f) (avgGoal / maxSecs).coerceIn(0f, 1f) else 0f
        val goalActive = goalFrac in 0.02f..0.98f
        val goalY = innerBottom - chartInnerH * goalFrac

        // horizontal hour gridlines as a y-axis scale (drawn before bars so they stay behind)
        val maxHours = Math.ceil(maxSecs / 3600.0).toInt().coerceAtLeast(1)
        val hourStep = intArrayOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 48, 96, 168, 336, 720)
            .firstOrNull { maxHours / it <= 5 } ?: (maxHours + 4) / 5
        val gridPaint = Paint().apply {
            color = white20
            strokeWidth = 1.2f * fs
            style = Paint.Style.STROKE
        }
        val gridLabelPaint = textPaint(12f * sz, white40, Typeface.create("sans-serif-medium", Typeface.NORMAL))
        var gh = hourStep
        while (gh <= maxHours) {
            val frac = (gh * 3600.0) / maxSecs
            if (frac <= 1.0) {
                val gy = innerBottom - chartInnerH * frac.toFloat()
                if (goalActive && Math.abs(gy - goalY) <= 2f * sz) {
                    gh += hourStep
                    continue
                }
                canvas.drawLine(chartLeft, gy, chartRight, gy, gridPaint)
                val gl = "${gh}h"
                canvas.drawText(gl, chartLeft + 3f * sz, gy - 4f * sz, gridLabelPaint)
            }
            gh += hourStep
        }

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dayLabelPaint = textPaint(16f * sz, white40, Typeface.create("sans-serif-medium", Typeface.NORMAL))
        for (i in 0..6) {
            val day = d.days[i]
            val cx = chartLeft + slotW * i + slotW / 2f
            val secs = day.secs
            val frac = (secs.toFloat() / maxSecs.toFloat()).coerceIn(0.004f, 1f)
            val bh = (chartInnerH * frac).coerceAtLeast(4f * fs)
            val barW = slotW * 0.5f
            val barLeft = cx - barW / 2f
            val barTop = innerBottom - bh
            val best = secs >= d.bestSecs && d.bestSecs > 0
            barPaint.alpha = 255
            if (best) {
                barPaint.shader = LinearGradient(0f, barTop, 0f, innerBottom, 0xFFFFF7D6.toInt(), gold, Shader.TileMode.CLAMP)
                barPaint.setShadowLayer(14f * fs, 0f, 0f, 0x59F6CB22.toInt())
            } else {
                barPaint.shader = LinearGradient(0f, barTop, 0f, innerBottom, goldLight, gold, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barW, innerBottom), barW / 2f, barW / 2f, barPaint)
            barPaint.setShadowLayer(0f, 0f, 0f, 0)
            barPaint.setShader(null)
            if (best) {
                val crownText = "\uD83D\uDC51"
                val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 34f * sz }
                canvas.drawText(crownText, cx - crownPaint.measureText(crownText) / 2f, barTop - 36f * sz, crownPaint)
            }
            canvas.drawText(day.label, cx - dayLabelPaint.measureText(day.label) / 2f, chartBottom - 2f * sz, dayLabelPaint)
        }

        // weekly average goal as a dashed reference line
        if (goalActive) {
            val dash = Paint().apply {
                color = white40
                strokeWidth = 1.6f * fs
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(8f * fs, 6f * fs), 0f)
            }
            canvas.drawLine(chartLeft, goalY, chartRight, goalY, dash)
            val gLabel = textPaint(12f * sz, white40, Typeface.create("sans-serif-medium", Typeface.NORMAL))
            canvas.drawText("goal", chartRight - gLabel.measureText("goal") - 2f * sz, goalY - 6f * sz, gLabel)
        }

        y = chartBottom + gap

        // ================= GOAL RING + FOCUS/BREAK DONUT =================
        val ringH = (h * 0.16f).coerceAtLeast(150f * sz)
        val halfW = (chartRight - chartLeft) / 2f
        val ringR = (halfW * 0.5f).coerceAtMost(ringH * 0.40f).coerceAtLeast(44f * sz)
        val leftCx = chartLeft + halfW * 0.5f
        val rightCx = chartRight - halfW * 0.5f
        val cy = y + ringH * 0.42f
        val stroke = ringR * 0.17f

        val ringShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = shadow
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 1..3) {
            ringShadow.alpha = (48 - i * 13).coerceAtLeast(0)
            canvas.drawCircle(leftCx, cy + i * (3.6f * sz), ringR, ringShadow)
            canvas.drawCircle(rightCx, cy + i * (3.6f * sz), ringR, ringShadow)
        }

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = white12
            strokeCap = Paint.Cap.ROUND
        }
        val arcRect = RectF(leftCx - ringR, cy - ringR, leftCx + ringR, cy + ringR)
        canvas.drawCircle(leftCx, cy, ringR, trackPaint)
        canvas.drawCircle(rightCx, cy, ringR, trackPaint)

        // weekly goal ring
        val goalSecs = d.days.sumOf { it.goal }
        val goalPct = if (goalSecs > 0) ((d.totalSecs.toFloat() / goalSecs.toFloat()) * 100f).coerceIn(0f, 100f) else 0f
        val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            shader = SweepGradient(leftCx, cy, intArrayOf(goldDeep, goldLight, gold), floatArrayOf(0f, 0.5f, 1f))
        }
        if (goalPct > 0f) {
            val goalHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke * 1.6f
                strokeCap = Paint.Cap.ROUND
                color = 0x19F6CB22.toInt()
            }
            canvas.drawArc(arcRect, -90f, goalPct * 3.6f, false, goalHalo)
            canvas.drawArc(arcRect, -90f, goalPct * 3.6f, false, goalPaint)
        }
        val ringCenterPaint = textPaint(30f * sz, white, Typeface.DEFAULT_BOLD)
        val ringFm = ringCenterPaint.fontMetrics
        val pctText = "${goalPct.toInt()}%"
        canvas.drawText(pctText, leftCx - ringCenterPaint.measureText(pctText) / 2f, cy - (ringFm.ascent + ringFm.descent) / 2f, ringCenterPaint)
        val ringLabel = "Weekly Goal"
        val ringLabelPaint = textPaint(18f * sz, white60, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.1f)
        val flagW = 15f * sz
        val flagH = 16f * sz
        val labelGap = 7f * sz
        val ringLabelW = ringLabelPaint.measureText(ringLabel)
        val ringStartX = leftCx - (flagW + labelGap + ringLabelW) / 2f
        drawFlag(canvas, ringStartX, y + ringH - 4f * sz - flagH, flagW, flagH, goldDeep, fs)
        canvas.drawText(ringLabel, ringStartX + flagW + labelGap, y + ringH - 4f * sz, ringLabelPaint)

        // focus / break donut
        val donutArc = RectF(rightCx - ringR, cy - ringR, rightCx + ringR, cy + ringR)
        val total = d.totalSecs + d.breakSecs
        val focusFrac = if (total > 0) d.totalSecs.toFloat() / total.toFloat() else 0f
        if (focusFrac > 0f) {
            val focusHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke * 1.6f
                strokeCap = Paint.Cap.BUTT
                color = 0x19F6CB22.toInt()
            }
            canvas.drawArc(donutArc, -90f, focusFrac * 360f, false, focusHalo)
            val focusArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.BUTT
                color = gold
            }
            canvas.drawArc(donutArc, -90f, focusFrac * 360f, false, focusArcPaint)
        }
        val breakArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = breakCol
            strokeCap = Paint.Cap.BUTT
        }
        if (focusFrac < 1f) canvas.drawArc(donutArc, -90f + focusFrac * 360f, (1f - focusFrac) * 360f, false, breakArcPaint)
        val donutText = if (focusFrac > 0f) "${(focusFrac * 100f).toInt()}% focus" else "—"
        var donutSize = 30f * sz
        val donutPaint = textPaint(donutSize, white, Typeface.DEFAULT_BOLD)
        val donutMaxW = 2f * (ringR - stroke) * 0.92f
        while (donutPaint.measureText(donutText) > donutMaxW && donutSize > 14f * sz) {
            donutSize -= 2f * sz
            donutPaint.textSize = donutSize
        }
        canvas.drawText(donutText, rightCx - donutPaint.measureText(donutText) / 2f, cy - (ringFm.ascent + ringFm.descent) / 2f, donutPaint)
        // colour-coded legend: gold = Focus, violet = Break
        val legendPaintF = textPaint(18f * sz, gold, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.1f)
        val legendPaintB = textPaint(18f * sz, breakCol, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.1f)
        val focusTxt = "Focus"
        val breakTxt = "Break"
        val gapTxt = 26f * sz
        val focusW = legendPaintF.measureText(focusTxt)
        val breakW = legendPaintB.measureText(breakTxt)
        val legendX = rightCx - (focusW + gapTxt + breakW) / 2f
        val legendY = y + ringH - 4f * sz
        canvas.drawText(focusTxt, legendX, legendY, legendPaintF)
        canvas.drawText(breakTxt, legendX + focusW + gapTxt, legendY, legendPaintB)

        y += ringH + gap

        // ================= STAT TILES =================
        val footerY = h - p * 0.5f
        val tilesBottom = footerY - 26f * sz
        if (tilesBottom - y > 0f) {
            val tileGap = 14f * sz
            val tileW = (chartRight - chartLeft - tileGap) / 2f
            val tileH = ((tilesBottom - y - tileGap) / 2f).coerceAtLeast(36f * sz)
            val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileBg }
            val vsValue = when {
                d.vsPrev.startsWith("-") -> "▼ ${d.vsPrev.substring(1)}"
                d.vsPrev.startsWith("+") -> "▲ ${d.vsPrev.substring(1)}"
                else -> "—"
            }
            val vsColor = when {
                d.vsPrev.startsWith("-") -> deltaDown
                d.vsPrev.startsWith("+") -> deltaUp
                else -> white
            }
            val rows = listOf(
                listOf(Tile("BEST DAY", d.bestName.ifEmpty { "—" }, formatTime(d.bestSecs), white),
                    Tile("STREAK", if (d.streak > 0) "${d.streak} day${if (d.streak == 1) "" else "s"}" else "—", "current streak", amber, flame = d.streak > 0)),
                listOf(Tile("VS LAST WEEK", vsValue, "previous week", vsColor),
                    Tile("SESSIONS", "${d.sessionCount}", "this week", white))
            )
            for (r in 0..1) {
                val rowTop = y + r * (tileH + tileGap)
                for (c in 0..1) {
                    val tx = chartLeft + c * (tileW + tileGap)
                    drawTile(canvas, tilePaint, tx, rowTop, tileW, tileH, rows[r][c], sz, fs)
                }
            }
        }

        drawFooter(canvas, w, h, p, sz)
    }

    private fun drawTile(canvas: Canvas, tilePaint: Paint, x: Float, y: Float, tw: Float, th: Float, tile: Tile, sz: Float, fs: Float) {
        canvas.drawRoundRect(RectF(x, y, x + tw, y + th), 18f * fs, 18f * fs, tilePaint)
        val pad = 16f * sz
        val labelPaint = textPaint(22f * sz, PAL_WHITE60, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.2f)
        val valuePaint = textPaint(35f * sz, tile.valueColor, Typeface.DEFAULT_BOLD)
        val subPaint = textPaint(22f * sz, PAL_WHITE80, Typeface.create("sans-serif-medium", Typeface.NORMAL))

        val labelFm = labelPaint.fontMetrics
        val valueFm = valuePaint.fontMetrics
        val subFm = subPaint.fontMetrics
        val labelBaseline = y + pad - labelFm.ascent
        canvas.drawText(tile.label, x + pad, labelBaseline, labelPaint)

        // value centered between label bottom and sub top, so nothing overlaps at any tile height
        val labelBottom = labelBaseline + labelFm.descent
        val subBaseline = y + th - pad - subFm.descent
        val subTop = subBaseline + subFm.ascent
        val valueCenterY = (labelBottom + subTop) / 2f
        val valueBaseline = valueCenterY - (valueFm.ascent + valueFm.descent) / 2f
        var valueX = x + pad
        if (tile.flame) {
            val flameText = "\uD83D\uDD25"
            val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f * sz }
            canvas.drawText(flameText, valueX, valueBaseline - 2f * sz, flamePaint)
            valueX += flamePaint.measureText(flameText) + 8f * sz
        }
        canvas.drawText(tile.value, valueX, valueBaseline, valuePaint)
        canvas.drawText(tile.sub, x + pad, subBaseline, subPaint)
    }

    private fun drawFlag(canvas: Canvas, x: Float, top: Float, w: Float, h: Float, clr: Int, fs: Float) {
        val pole = Paint().apply { color = clr; strokeWidth = 2.6f * fs; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(x + w * 0.28f, top + h, x + w * 0.28f, top, pole)
        val flag = Path()
        flag.moveTo(x + w * 0.28f, top + h * 0.06f)
        flag.lineTo(x + w, top + h * 0.42f)
        flag.lineTo(x + w * 0.28f, top + h * 0.78f)
        flag.close()
        canvas.drawPath(flag, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = clr })
    }

    private fun drawFooter(canvas: Canvas, w: Float, h: Float, p: Float, sz: Float) {
        val footerPaint = textPaint(19f * sz, PAL_WHITE40, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.26f)
        val ft = "MADE WITH STUDYTIMER"
        canvas.drawText(ft, w / 2f - footerPaint.measureText(ft) / 2f, h - p * 0.5f, footerPaint)
    }

    private data class Tile(val label: String, val value: String, val sub: String, val valueColor: Int, val flame: Boolean = false)

    private fun textPaint(size: Float, color: Int, face: Typeface, ls: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = face
            letterSpacing = ls
        }

    companion object {
        private val PAL_WHITE40: Int = 0x66FFFFFF
        private val PAL_WHITE60: Int = 0x99FFFFFF.toInt()
        private val PAL_WHITE80: Int = 0xCCFFFFFF.toInt()

        fun formatTime(secs: Long): String {
            if (secs <= 0L) return "0m"
            val h = secs / 3600
            val m = (secs % 3600) / 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }
    }
}
