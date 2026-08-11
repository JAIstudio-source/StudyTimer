package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import android.view.GestureDetector
import kotlin.math.max

class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDayTap: ((String) -> Unit)? = null
    var onDayLongClick: ((String) -> Unit)? = null
    var forcedCellSize: Float = 0f

    private val cells = HashMap<String, Long>()
    private val dayDates = ArrayList<String>()
    private var todayStr = ""
    private var numCols = 0
    private var cellSize = 12f
    private var gapPx = 0f
    private var labelW = 0f
    private var topPad = 0f
    private var monthRowH = 0f
    private var rowH = 0f
    private val minCellSize = 18f * resources.displayMetrics.density

    private var primary = Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f))
    private var textColor = Color.WHITE
    private val emptyColor = Color.argb(20, 255, 255, 255)
    private var goalFor: ((String) -> Long)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            performClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            handleLongClick(e.x, e.y)
        }
    })

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 9f * resources.displayMetrics.density
    }
    private val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val weekdayLetters = arrayOf("S", "M", "T", "W", "T", "F", "S")

    fun setData(data: Map<String, Long>, primaryColor: Int, text: Int, goalFor: (String) -> Long) {
        primary = primaryColor
        textColor = text
        this.goalFor = goalFor
        cells.clear()
        cells.putAll(data)
        todayStr = dateSdf.format(Date())

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val thisWeekSunday = Calendar.getInstance().apply {
            timeInMillis = todayCal.timeInMillis - (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * 86400000L
        }
        val start = Calendar.getInstance().apply {
            timeInMillis = thisWeekSunday.timeInMillis - 25L * 7L * 86400000L
        }

        numCols = 26
        dayDates.clear()
        for (col in 0 until numCols) {
            for (row in 0..6) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = start.timeInMillis + (col * 7L + row) * 86400000L
                }
                dayDates.add(dateSdf.format(c.time))
            }
        }

        requestLayout()
        invalidate()
    }

    fun getTodayScrollX(viewportWidth: Int): Int {
        val todayIdx = dayDates.indexOf(todayStr)
        val todayCol = if (todayIdx >= 0) todayIdx / 7 else numCols - 1
        val todayX = labelW + todayCol * (cellSize + gapPx) + cellSize / 2f
        val scrollX = (todayX - viewportWidth / 2f).toInt()
        return max(0, scrollX)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        gapPx = 2f * resources.displayMetrics.density
        labelW = 14f * resources.displayMetrics.density
        topPad = 4f * resources.displayMetrics.density
        monthRowH = 14f * resources.displayMetrics.density
        val gridW = width - labelW
        cellSize = if (forcedCellSize > 0f) {
            forcedCellSize
        } else {
            floor((gridW - (numCols - 1) * gapPx) / numCols).toFloat()
        }
        cellSize = max(cellSize, minCellSize)
        rowH = cellSize + gapPx
        val contentW = (labelW + numCols * (cellSize + gapPx) - gapPx).toInt()
        val height = (topPad + monthRowH + 7 * rowH + 4 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(max(contentW, width), height)
    }

    private fun levelFor(focus: Long, dateStr: String): Int {
        if (focus <= 0L) return 0
        val goal = goalFor?.invoke(dateStr) ?: 2700L
        if (goal <= 0L) return 4
        val f = focus.toFloat()
        val g = goal.toFloat()
        return when {
            f <= g * (1f / 6f) -> 1
            f <= g * (2.5f / 6f) -> 2
            f <= g * (4f / 6f) -> 3
            else -> 4
        }
    }

    private fun levelColor(level: Int): Int {
        if (level <= 0) return emptyColor
        val alpha = when (level) {
            1 -> 30
            2 -> 60
            3 -> 110
            else -> 170
        }
        return Color.argb(alpha, Color.red(primary), Color.green(primary), Color.blue(primary))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (col in 0 until numCols) {
            val x = labelW + col * (cellSize + gapPx)
            val firstDate = dayDates.getOrNull(col * 7) ?: continue
            if (col == 0 || monthSdf.format(parseSafe(firstDate)) != monthSdf.format(parseSafe(dayDates.getOrNull((col - 1) * 7) ?: firstDate))) {
                textPaint.color = Color.argb(150, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                canvas.drawText(monthSdf.format(parseSafe(firstDate)), x + cellSize / 2f, topPad + monthRowH - 3f * resources.displayMetrics.density, textPaint)
            }
        }

        for (row in 0..6) {
            textPaint.color = Color.argb(120, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            val ly = topPad + monthRowH + row * rowH + cellSize / 2f + textPaint.textSize / 3f
            canvas.drawText(weekdayLetters[row], labelW / 2f, ly, textPaint)

            for (col in 0 until numCols) {
                val idx = col * 7 + row
                val dateStr = dayDates.getOrNull(idx) ?: continue
                val x = labelW + col * (cellSize + gapPx)
                val y = topPad + monthRowH + row * rowH
                val future = dateStr > todayStr
                val r = RectF(x, y, x + cellSize, y + cellSize)
                paint.color = if (future) Color.argb(8, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                else levelColor(levelFor(cells[dateStr] ?: 0L, dateStr))
                canvas.drawRoundRect(r, 2.5f * resources.displayMetrics.density, 2.5f * resources.displayMetrics.density, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        val col = floor((x - labelW) / (cellSize + gapPx)).toInt()
        val row = floor((y - topPad - monthRowH) / rowH).toInt()
        if (col in 0 until numCols && row in 0..6) {
            val idx = col * 7 + row
            val dateStr = dayDates.getOrNull(idx) ?: return
            if (dateStr > todayStr) return
            onDayTap?.invoke(dateStr)
        }
    }

    private fun handleLongClick(x: Float, y: Float) {
        val col = floor((x - labelW) / (cellSize + gapPx)).toInt()
        val row = floor((y - topPad - monthRowH) / rowH).toInt()
        if (col in 0 until numCols && row in 0..6) {
            val idx = col * 7 + row
            val dateStr = dayDates.getOrNull(idx) ?: return
            if (dateStr > todayStr) return
            onDayLongClick?.invoke(dateStr)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun parseSafe(dateStr: String): Date {
        return try { dateSdf.parse(dateStr) ?: Date() } catch (_: Exception) { Date() }
    }
}
