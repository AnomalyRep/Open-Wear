package id.openwear.diskhealth.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.HistoryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight line chart replacing MPAndroidChart from the original build.
 * Draws the same two series (wear level / cycle usage), 0-105 % Y axis and
 * "MMM yy" date labels.
 */
class HistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveTextColor()
        textSize = 9f * density
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveColor(com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY)
        strokeWidth = density
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var seriesA: List<HistoryPoint> = emptyList()
    private var seriesB: List<HistoryPoint> = emptyList()

    private val colorA = context.getColor(R.color.colorPrimary)
    private val colorB = context.getColor(R.color.holo_red_dark)
    private val dateFormat = SimpleDateFormat("MMM yy", Locale.getDefault())

    fun setData(a: List<HistoryPoint>, b: List<HistoryPoint>) {
        seriesA = a.sortedBy { it.t }
        seriesB = b.sortedBy { it.t }
        invalidate()
    }

    private fun resolveTextColor(): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)) {
            context.getColor(value.resourceId)
        } else {
            Color.DKGRAY
        }
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            context.getColor(value.resourceId)
        } else {
            fallback
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = 40f * density
        val right = width - 8f * density
        val top = 20f * density
        val bottom = height - 22f * density
        if (right <= left || bottom <= top) return

        // Legend.
        var legendX = left
        val legendY = 11f * density
        linePaint.color = colorA
        canvas.drawLine(legendX, legendY, legendX + 16f * density, legendY, linePaint)
        canvas.drawText(context.getString(R.string.wear_level), legendX + 20f * density, legendY + 3f * density, textPaint)
        legendX += 20f * density + textPaint.measureText(context.getString(R.string.wear_level)) + 14f * density
        linePaint.color = colorB
        canvas.drawLine(legendX, legendY, legendX + 16f * density, legendY, linePaint)
        canvas.drawText(context.getString(R.string.cycle_usages), legendX + 20f * density, legendY + 3f * density, textPaint)

        // Y grid and labels 0..100.
        for (i in 0..4) {
            val value = i * 25
            val y = bottom - (value / 105f) * (bottom - top)
            canvas.drawLine(left, y, right, y, gridPaint)
            val label = if (i == 0) "0%" else "$value%"
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, left - 5f * density, y + 3f * density, textPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT

        drawSeries(canvas, seriesA, colorA, left, top, right, bottom)
        drawSeries(canvas, seriesB, colorB, left, top, right, bottom)

        // X labels from the combined time range.
        val all = (seriesA + seriesB).sortedBy { it.t }
        if (all.isNotEmpty()) {
            val min = all.first().t
            val max = all.last().t
            val count = 4
            textPaint.textAlign = Paint.Align.CENTER
            for (i in 0 until count) {
                val fraction = if (count == 1) 0f else i / (count - 1f)
                val time = if (max == min) min else min + ((max - min) * fraction).toLong()
                val x = left + fraction * (right - left)
                canvas.drawText(dateFormat.format(Date(time)), x, bottom + 14f * density, textPaint)
            }
            textPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawSeries(
        canvas: Canvas,
        points: List<HistoryPoint>,
        color: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        if (points.isEmpty()) return
        val min = points.first().t
        val max = points.last().t
        val span = if (max == min) 1L else max - min

        val path = Path()
        val fill = Path()
        var lastX = 0f
        points.forEachIndexed { index, point ->
            val x = left + ((point.t - min).toFloat() / span) * (right - left)
            val y = bottom - ((point.v.coerceIn(0, 105)) / 105f) * (bottom - top)
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, bottom)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
            lastX = x
        }
        fill.lineTo(lastX, bottom)
        fill.close()

        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fill, fillPaint)
        fillPaint.shader = null

        linePaint.color = color
        canvas.drawPath(path, linePaint)

        pointPaint.color = color
        if (points.size <= 40) {
            points.forEach { point ->
                val x = left + ((point.t - min).toFloat() / span) * (right - left)
                val y = bottom - ((point.v.coerceIn(0, 105)) / 105f) * (bottom - top)
                canvas.drawCircle(x, y, 2.2f * density, pointPaint)
            }
        }
    }
}
