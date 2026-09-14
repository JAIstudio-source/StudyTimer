package com.madeby.JAI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * High-impact, ultra-modern Canvas-drawn weekly summary card designed for
 * social sharing and personal review (1080x1920 9:16 aspect ratio).
 * Features deep obsidian glassmorphism, bold typography scales, massive progress rings,
 * wide pill bar charts, and perfectly balanced bento metric tiles.
 */
class WeeklyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val logoBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.mylogo)
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

        // Scale factor: based on reference 1080x1920 portrait canvas
        val s = w / 1080f
        val p = 40f * s

        // High-end curated palette
        val bgDark = 0xFF0A0C14.toInt()
        val bgMid = 0xFF101322.toInt()
        val accentPurple = 0xFFA78BFA.toInt()
        val accentIndigo = 0xFF6366F1.toInt()
        val accentCyan = 0xFF38BDF8.toInt()
        val accentEmerald = 0xFF34D399.toInt()
        val accentPink = 0xFFF43F5E.toInt()
        val accentGold = 0xFFFBBF24.toInt()
        val accentTeal = 0xFF2DD4BF.toInt()

        val white = 0xFFFFFFFF.toInt()
        val white90 = 0xE6FFFFFF.toInt()
        val white80 = 0xCCFFFFFF.toInt()
        val white70 = 0xB3FFFFFF.toInt()
        val white50 = 0x80FFFFFF.toInt()
        val white30 = 0x4DFFFFFF.toInt()
        val cardGlassBg = 0x1E1E293B.toInt()
        val cardGlassStroke = 0x38475569.toInt()

        // 1. Clip outer rounded rect
        val cornerRadius = 40f * s
        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 2. Base Dark Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, bgDark, bgMid, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 3. Atmospheric Ambient Glowing Orbs
        val orbTopRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.88f, h * 0.10f, w * 0.72f,
                intArrayOf(0x3E8B5CF6.toInt(), 0x146366F1.toInt(), 0x00000000),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.88f, h * 0.10f, w * 0.72f, orbTopRight)

        val orbMidLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.12f, h * 0.50f, w * 0.68f,
                intArrayOf(0x2C06B6D4.toInt(), 0x103B82F6.toInt(), 0x00000000),
                floatArrayOf(0f, 0.40f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.12f, h * 0.50f, w * 0.68f, orbMidLeft)

        // Subtle Card Inner Border Stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * s
            shader = LinearGradient(0f, 0f, w, h, 0x4DFFFFFF.toInt(), 0x15FFFFFF.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(1.2f * s, 1.2f * s, w - 1.2f * s, h - 1.2f * s), cornerRadius, cornerRadius, borderPaint)

        // ================= HEADER SECTION =================
        var y = 44f * s
        val headerH = 80f * s
        val logoSize = 72f * s

        // Brand Icon / Logo
        val logo = logoBitmap
        if (logo != null) {
            val scaled = Bitmap.createScaledBitmap(logo, logoSize.toInt(), logoSize.toInt(), true)
            val iconClip = Path().apply {
                addRoundRect(RectF(p, y, p + logoSize, y + logoSize), 20f * s, 20f * s, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(iconClip)
            canvas.drawBitmap(scaled, p, y, null)
            canvas.restore()
        } else {
            val iconBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(p, y, p + logoSize, y + logoSize, accentPurple, accentIndigo, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(p, y, p + logoSize, y + logoSize), 20f * s, 20f * s, iconBg)
            val logoTextPaint = textPaint(28f * s, white, Typeface.DEFAULT_BOLD)
            val lt = "ST"
            canvas.drawText(lt, p + (logoSize - logoTextPaint.measureText(lt)) / 2f, y + logoSize * 0.68f, logoTextPaint)
        }

        // Header Title & Tagline
        val titlePaint = textPaint(44f * s, white, Typeface.create("sans-serif", Typeface.BOLD), 0.08f)
        canvas.drawText("STUDYTIMER", p + logoSize + 22f * s, y + 40f * s, titlePaint)

        val badgePaint = textPaint(26f * s, accentPurple, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.16f)
        canvas.drawText("WEEKLY SUMMARY", p + logoSize + 22f * s, y + 74f * s, badgePaint)

        // Date Range Pill Badge (Top Right)
        val dateText = d.dateRange.uppercase()
        val datePaint = textPaint(24f * s, white, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.08f)
        val dateBadgeW = datePaint.measureText(dateText) + 44f * s
        val dateBadgeH = 58f * s
        val dateBadgeX = w - p - dateBadgeW
        val dateBadgeY = y + (headerH - dateBadgeH) / 2f

        val dateBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x2CFFFFFF.toInt()
        }
        val dateBadgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * s
            color = white30
        }
        canvas.drawRoundRect(RectF(dateBadgeX, dateBadgeY, dateBadgeX + dateBadgeW, dateBadgeY + dateBadgeH), dateBadgeH / 2f, dateBadgeH / 2f, dateBadgeBg)
        canvas.drawRoundRect(RectF(dateBadgeX, dateBadgeY, dateBadgeX + dateBadgeW, dateBadgeY + dateBadgeH), dateBadgeH / 2f, dateBadgeH / 2f, dateBadgeStroke)
        canvas.drawText(dateText, dateBadgeX + 22f * s, dateBadgeY + dateBadgeH * 0.65f, datePaint)

        y += headerH + 18f * s

        // ================= HERO TOTAL FOCUS CARD =================
        val heroCardH = 250f * s
        val heroRect = RectF(p, y, w - p, y + heroCardH)

        // Glassmorphic Hero Container
        val heroBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(p, y, w - p, y + heroCardH, 0x381E293B.toInt(), 0x1E0F172A.toInt(), Shader.TileMode.CLAMP)
        }
        val heroStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * s
            shader = LinearGradient(p, y, w - p, y + heroCardH, 0x6EA78BFA.toInt(), 0x2438BDF8.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(heroRect, 30f * s, 30f * s, heroBgPaint)
        canvas.drawRoundRect(heroRect, 30f * s, 30f * s, heroStrokePaint)

        // Hero Label
        val heroLabelPaint = textPaint(34f * s, white90, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.14f)
        canvas.drawText("TOTAL FOCUS TIME", p + 30f * s, y + 50f * s, heroLabelPaint)

        // Hero Big Numbers (High-Impact Display Weight)
        val heroTimeStr = formatTimeDetailed(d.totalSecs)
        val heroNumberPaint = textPaint(98f * s, white, Typeface.create("sans-serif", Typeface.BOLD), 0.02f)
        heroNumberPaint.shader = LinearGradient(
            p + 30f * s, y + 56f * s,
            p + 30f * s, y + 152f * s,
            intArrayOf(0xFFFFFFFF.toInt(), 0xFFE2E8F0.toInt(), 0xFFCBD5E1.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawText(heroTimeStr, p + 30f * s, y + 146f * s, heroNumberPaint)

        // Subtitle / Streak Pill
        val streakText = if (d.streak > 0) "🔥 ${d.streak} Day Streak" else "⚡ Consistency Built"
        val streakPaint = textPaint(24f * s, accentCyan, Typeface.create("sans-serif-medium", Typeface.BOLD))
        canvas.drawText(streakText, p + 30f * s, y + 210f * s, streakPaint)

        // Right side badge in hero card (Session count)
        val sessCountStr = "${d.sessionCount}"
        val sessCountPaint = textPaint(66f * s, accentPurple, Typeface.create("sans-serif", Typeface.BOLD))
        val sessSubPaint = textPaint(24f * s, white80, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.12f)
        val scW = sessCountPaint.measureText(sessCountStr)
        val scSubW = sessSubPaint.measureText("SESSIONS")
        val rightAnchorX = w - p - 36f * s

        canvas.drawText(sessCountStr, rightAnchorX - scW, y + 128f * s, sessCountPaint)
        canvas.drawText("SESSIONS", rightAnchorX - scSubW, y + 164f * s, sessSubPaint)

        y += heroCardH + 18f * s

        // ================= 7-DAY BAR CHART SECTION =================
        val chartSectionH = 470f * s
        val chartRect = RectF(p, y, w - p, y + chartSectionH)

        canvas.drawRoundRect(chartRect, 30f * s, 30f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardGlassBg })
        canvas.drawRoundRect(chartRect, 30f * s, 30f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * s; color = cardGlassStroke
        })

        val chartHeaderPaint = textPaint(34f * s, white90, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.14f)
        canvas.drawText("DAILY FOCUS DISTRIBUTION", p + 30f * s, y + 50f * s, chartHeaderPaint)

        val chartLeft = p + 28f * s
        val chartRight = w - p - 28f * s
        val chartTop = y + 84f * s
        val chartBottom = y + chartSectionH - 64f * s
        val chartInnerH = chartBottom - chartTop
        val slotW = (chartRight - chartLeft) / 7f
        val maxSecs = d.days.maxOfOrNull { it.secs }?.coerceAtLeast(1L) ?: 1L

        val avgGoal = d.days.map { it.goal }.average().toFloat().coerceAtLeast(0f)
        val goalFrac = if (avgGoal > 0f) (avgGoal / maxSecs).coerceIn(0f, 1f) else 0f
        val goalY = chartBottom - chartInnerH * goalFrac

        // Prominent Goal Target Pill in Top-Right of Chart Header (Never gets hidden by bars)
        if (avgGoal > 0f) {
            val goalBadgeText = "🎯 TARGET: ${formatTime(avgGoal.toLong())}/D"
            val goalBadgePaint = textPaint(20f * s, accentGold, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.08f)
            val gbw = goalBadgePaint.measureText(goalBadgeText) + 28f * s
            val gbh = 40f * s
            val gbx = w - p - 28f * s - gbw
            val gby = y + 24f * s
            val gbBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2CFBBF24.toInt() }
            val gbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * s
                color = 0x66FBBF24.toInt()
            }
            canvas.drawRoundRect(RectF(gbx, gby, gbx + gbw, gby + gbh), gbh / 2f, gbh / 2f, gbBg)
            canvas.drawRoundRect(RectF(gbx, gby, gbx + gbw, gby + gbh), gbh / 2f, gbh / 2f, gbStroke)
            canvas.drawText(goalBadgeText, gbx + 14f * s, gby + gbh * 0.68f, goalBadgePaint)
        }

        // Draw dashed goal guideline across the chart track
        if (goalFrac in 0.05f..0.95f) {
            val dashPaint = Paint().apply {
                color = 0x66FBBF24.toInt()
                strokeWidth = 2.0f * s
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(8f * s, 8f * s), 0f)
            }
            canvas.drawLine(chartLeft, goalY, chartRight, goalY, dashPaint)
        }

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val trackBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x24334155.toInt() }
        val dayLabelPaint = textPaint(24f * s, white90, Typeface.create("sans-serif-medium", Typeface.BOLD))

        val barW = 54f * s // bold prominent pill bar

        for (i in 0..6) {
            val day = d.days[i]
            val cx = chartLeft + slotW * i + slotW / 2f
            val barLeft = cx - barW / 2f
            val barRight = cx + barW / 2f

            // Full height background capsule track
            canvas.drawRoundRect(RectF(barLeft, chartTop, barRight, chartBottom), barW / 2f, barW / 2f, trackBarPaint)

            val frac = (day.secs.toFloat() / maxSecs.toFloat()).coerceIn(0f, 1f)
            if (frac > 0.01f) {
                val bh = (chartInnerH * frac).coerceAtLeast(barW)
                val barTop = chartBottom - bh
                val isBest = day.secs >= d.bestSecs && d.bestSecs > 0

                barPaint.shader = if (isBest) {
                    LinearGradient(0f, barTop, 0f, chartBottom, 0xFFFCD34D.toInt(), 0xFFF59E0B.toInt(), Shader.TileMode.CLAMP)
                } else {
                    LinearGradient(0f, barTop, 0f, chartBottom, 0xFFA78BFA.toInt(), 0xFF6366F1.toInt(), Shader.TileMode.CLAMP)
                }
                canvas.drawRoundRect(RectF(barLeft, barTop, barRight, chartBottom), barW / 2f, barW / 2f, barPaint)

                if (isBest) {
                    val crownText = "★"
                    val crownPaint = textPaint(24f * s, accentGold, Typeface.DEFAULT_BOLD)
                    canvas.drawText(crownText, cx - crownPaint.measureText(crownText) / 2f, barTop - 10f * s, crownPaint)
                }
            }

            // Day letter label
            val dayName = day.label
            canvas.drawText(dayName, cx - dayLabelPaint.measureText(dayName) / 2f, chartBottom + 46f * s, dayLabelPaint)
        }

        y += chartSectionH + 18f * s

        // ================= DUAL PROGRESS GAUGES =================
        val gaugeH = 350f * s
        val gaugeGap = 18f * s
        val gaugeW = (w - p * 2f - gaugeGap) / 2f

        // 1. Weekly Goal Gauge Card (Left)
        val leftRect = RectF(p, y, p + gaugeW, y + gaugeH)
        canvas.drawRoundRect(leftRect, 28f * s, 28f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardGlassBg })
        canvas.drawRoundRect(leftRect, 28f * s, 28f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * s; color = cardGlassStroke
        })

        val leftCx = p + gaugeW / 2f
        val gaugeCy = y + gaugeH * 0.44f
        val ringR = 88f * s // scaled up ~40% (diameter 176px)
        val strokeW = 20f * s // bold stroke

        val totalGoalSecs = d.days.sumOf { it.goal }
        val goalPct = if (totalGoalSecs > 0) ((d.totalSecs.toFloat() / totalGoalSecs.toFloat()) * 100f).coerceIn(0f, 100f) else 0f

        // Track Ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = 0x24334155.toInt()
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(leftCx, gaugeCy, ringR, ringPaint)

        // Progress Arc
        if (goalPct > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(leftCx, gaugeCy, intArrayOf(accentEmerald, accentCyan, accentEmerald), floatArrayOf(0f, 0.5f, 1f))
            }
            val arcRect = RectF(leftCx - ringR, gaugeCy - ringR, leftCx + ringR, gaugeCy + ringR)
            canvas.drawArc(arcRect, -90f, (goalPct / 100f) * 360f, false, progressPaint)
        }

        val pctText = "${goalPct.toInt()}%"
        val pctPaint = textPaint(50f * s, white, Typeface.DEFAULT_BOLD)
        canvas.drawText(pctText, leftCx - pctPaint.measureText(pctText) / 2f, gaugeCy + 18f * s, pctPaint)

        val goalCardLabel = "WEEKLY GOAL"
        val gclPaint = textPaint(30f * s, white90, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.14f)
        canvas.drawText(goalCardLabel, leftCx - gclPaint.measureText(goalCardLabel) / 2f, y + gaugeH - 24f * s, gclPaint)

        // 2. Focus vs Break Ratio Card (Right)
        val rightRect = RectF(p + gaugeW + gaugeGap, y, w - p, y + gaugeH)
        canvas.drawRoundRect(rightRect, 28f * s, 28f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardGlassBg })
        canvas.drawRoundRect(rightRect, 28f * s, 28f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * s; color = cardGlassStroke
        })

        val rightCx = p + gaugeW + gaugeGap + gaugeW / 2f
        val totalActivity = d.totalSecs + d.breakSecs
        val focusRatio = if (totalActivity > 0) (d.totalSecs.toFloat() / totalActivity.toFloat()) else 1f

        canvas.drawCircle(rightCx, gaugeCy, ringR, ringPaint)

        val donutArc = RectF(rightCx - ringR, gaugeCy - ringR, rightCx + ringR, gaugeCy + ringR)
        val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = accentIndigo
            strokeCap = Paint.Cap.BUTT
        }
        val breakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = accentPink
            strokeCap = Paint.Cap.BUTT
        }
        if (focusRatio > 0f) {
            canvas.drawArc(donutArc, -90f, focusRatio * 360f, false, focusPaint)
        }
        if (focusRatio < 1f) {
            canvas.drawArc(donutArc, -90f + focusRatio * 360f, (1f - focusRatio) * 360f, false, breakPaint)
        }

        val ratioText = "${(focusRatio * 100f).toInt()}%"
        val ratioPaint = textPaint(50f * s, white, Typeface.DEFAULT_BOLD)
        canvas.drawText(ratioText, rightCx - ratioPaint.measureText(ratioText) / 2f, gaugeCy + 18f * s, ratioPaint)

        val ratioCardLabel = "FOCUS RATIO"
        val rclPaint = textPaint(30f * s, white90, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.14f)
        canvas.drawText(ratioCardLabel, rightCx - rclPaint.measureText(ratioCardLabel) / 2f, y + gaugeH - 24f * s, rclPaint)

        y += gaugeH + 18f * s

        // ================= 2x2 BENTO STAT TILES =================
        val footerY = h - 36f * s
        val bentoAvailableH = footerY - y - 18f * s
        if (bentoAvailableH > 100f * s) {
            val tileGap = 18f * s
            val tileW = (w - p * 2f - tileGap) / 2f
            val tileH = (bentoAvailableH - tileGap) / 2f

            val vsValue = when {
                d.vsPrev.startsWith("-") -> "▼ ${d.vsPrev.substring(1)}"
                d.vsPrev.startsWith("+") -> "▲ ${d.vsPrev.substring(1)}"
                else -> d.vsPrev
            }
            val vsColor = when {
                d.vsPrev.startsWith("-") -> accentPink
                d.vsPrev.startsWith("+") -> accentEmerald
                else -> white90
            }

            // Daily average: total focus time divided by 7 days of the week
            val dailyAvgSecs = d.totalSecs / 7L
            val dailyAvgStr = formatTime(dailyAvgSecs)

            val bentoItems = listOf(
                listOf(
                    BentoTile("BEST DAY", d.bestName.ifEmpty { "—" }, "${formatTime(d.bestSecs)} record", accentGold),
                    BentoTile("STREAK", if (d.streak > 0) "${d.streak} Days" else "0 Days", "Active Streak", accentCyan)
                ),
                listOf(
                    BentoTile("GROWTH", vsValue, "vs Previous Week", vsColor),
                    BentoTile("DAILY AVERAGE", dailyAvgStr, "Week's daily average", accentTeal)
                )
            )

            for (r in 0..1) {
                val rowTop = y + r * (tileH + tileGap)
                for (c in 0..1) {
                    val tx = p + c * (tileW + tileGap)
                    drawBentoCard(canvas, tx, rowTop, tileW, tileH, bentoItems[r][c], s, cardGlassBg, cardGlassStroke, white90, white80)
                }
            }
        }

        // ================= FOOTER WATERMARK =================
        drawFooter(canvas, w, h, s, white80)
    }

    private fun drawBentoCard(
        canvas: Canvas,
        x: Float, y: Float, tw: Float, th: Float,
        tile: BentoTile,
        s: Float,
        bgCol: Int, strokeCol: Int,
        white90Col: Int, white80Col: Int
    ) {
        val rect = RectF(x, y, x + tw, y + th)
        canvas.drawRoundRect(rect, 26f * s, 26f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol })
        canvas.drawRoundRect(rect, 26f * s, 26f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * s; color = strokeCol
        })

        val padX = 26f * s
        val labelPaint = textPaint(30f * s, white90Col, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.12f)
        canvas.drawText(tile.label, x + padX, y + 46f * s, labelPaint)

        val valuePaint = textPaint(52f * s, tile.valueColor, Typeface.DEFAULT_BOLD)
        canvas.drawText(tile.value, x + padX, y + th * 0.58f, valuePaint)

        val subPaint = textPaint(24f * s, white80Col, Typeface.create("sans-serif-medium", Typeface.NORMAL))
        canvas.drawText(tile.sub, x + padX, y + th - 26f * s, subPaint)
    }

    private fun drawFooter(canvas: Canvas, w: Float, h: Float, s: Float, textColor: Int) {
        val footerPaint = textPaint(26f * s, textColor, Typeface.create("sans-serif-medium", Typeface.BOLD), 0.14f)
        val text = "STUDYTIMER • 100% OFFLINE FOCUS & HABIT TRACKER"
        canvas.drawText(text, (w - footerPaint.measureText(text)) / 2f, h - 22f * s, footerPaint)
    }

    private data class BentoTile(val label: String, val value: String, val sub: String, val valueColor: Int)

    private fun textPaint(size: Float, color: Int, face: Typeface, ls: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = face
            letterSpacing = ls
        }

    companion object {
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

        fun formatTimeDetailed(secs: Long): String {
            if (secs <= 0L) return "0h 00m"
            val h = secs / 3600
            val m = (secs % 3600) / 60
            return "${h}h ${String.format(java.util.Locale.US, "%02d", m)}m"
        }
    }
}
