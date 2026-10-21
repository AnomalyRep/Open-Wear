package id.openwear.diskhealth.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Battery graphic matching reference v5.0.0 BatteryCanvasGraphic.
 * 44dp x 78dp aspect, rounded terminal cap, stroke, dynamic level fill,
 * and lightning bolt path when charging.
 */
class BatteryGlyphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var percentage: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var isCharging: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val capRect = RectF()
    private val bodyRect = RectF()
    private val innerFillRect = RectF()
    private val boltPath = Path()

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun resolveThemeColor(attr: Int, defaultColor: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else {
            defaultColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = dp(44f).toInt()
        val h = dp(78f).toInt()
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val outlineColor = resolveThemeColor(
            com.google.android.material.R.attr.colorOutline,
            0xFF94A3B8.toInt()
        )
        val primaryColor = resolveThemeColor(
            com.google.android.material.R.attr.colorPrimary,
            0xFF0284C7.toInt()
        )

        val capWidth = 0.38f * w
        val capHeight = 0.08f * h
        val capLeft = (w - capWidth) / 2f
        val capRadius = dp(4f)

        capPaint.color = outlineColor
        capRect.set(capLeft, 0f, capLeft + capWidth, capHeight)
        canvas.drawRoundRect(capRect, capRadius, capRadius, capPaint)

        val bodyTop = capHeight + dp(2f)
        val bodyHeight = h - bodyTop
        val strokeWidth = dp(2f)
        val bodyRadius = dp(12f)

        strokePaint.color = outlineColor
        strokePaint.strokeWidth = strokeWidth
        val halfStroke = strokeWidth / 2f
        bodyRect.set(halfStroke, bodyTop + halfStroke, w - halfStroke, h - halfStroke)
        canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, strokePaint)

        // Inner fill
        val padding = dp(4f)
        val innerWidth = w - (padding * 2f)
        val innerMaxHeight = bodyHeight - (padding * 2f)
        val fillHeight = (percentage / 100f) * innerMaxHeight
        val fillTop = bodyTop + padding + (innerMaxHeight - fillHeight)
        val fillRadius = dp(8f)

        val fillColor = when {
            isCharging -> primaryColor
            percentage > 40 -> 0xFF22C55E.toInt()
            percentage > 15 -> 0xFFF59E0B.toInt()
            else -> 0xFFEF4444.toInt()
        }
        fillPaint.color = fillColor

        if (fillHeight > 0f) {
            innerFillRect.set(padding, fillTop, padding + innerWidth, bodyTop + padding + innerMaxHeight)
            canvas.drawRoundRect(innerFillRect, fillRadius, fillRadius, fillPaint)
        }

        // Lightning bolt icon when charging
        if (isCharging) {
            val cx = w / 2f
            val cy = bodyTop + (bodyHeight / 2f)
            boltPath.reset()
            boltPath.moveTo(cx + dp(3f), cy - dp(14f))
            boltPath.lineTo(cx - dp(7f), cy + dp(1f))
            boltPath.lineTo(cx - dp(1f), cy + dp(1f))
            boltPath.lineTo(cx - dp(3f), cy + dp(14f))
            boltPath.lineTo(cx + dp(7f), cy - dp(1f))
            boltPath.lineTo(cx + dp(1f), cy - dp(1f))
            boltPath.close()
            canvas.drawPath(boltPath, boltPaint)
        }
    }
}
