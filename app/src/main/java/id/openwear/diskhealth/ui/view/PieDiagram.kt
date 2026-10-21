package id.openwear.diskhealth.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

/** Donut storage chart from v4 `PieDiagram`; stroke width is 120 px there. */
class PieDiagram @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 120f
        strokeCap = Paint.Cap.BUTT
    }
    private val primary = resolveColor(android.R.attr.colorPrimary, 0xFF1976D2.toInt())
    private val surface = resolveColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE7E0EC.toInt())
    private val oval = RectF()

    var percent: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) context.getColor(value.resourceId) else fallback
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = paint.strokeWidth / 2f
        oval.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = surface
        canvas.drawOval(oval, paint)
        if (percent > 0f) {
            paint.color = primary
            canvas.drawArc(oval, -90f, (percent / 100f) * 360f, false, paint)
        }
    }
}
