package id.openwear.diskhealth.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import id.openwear.diskhealth.R

/** 270-degree gauge from v4 `GaugeView`. 0-100 score. */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val startAngle = 135f
    private val sweepAngle = 270f
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f
        strokeCap = Paint.Cap.ROUND
        color = resolveColor(com.google.android.material.R.attr.colorSurfaceVariant, 0xFFCCCCCC.toInt())
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f
        strokeCap = Paint.Cap.ROUND
        color = resolveColor(android.R.attr.colorPrimary, 0xFF1976D2.toInt())
    }
    private val oval = RectF()
    private var animator: ValueAnimator? = null

    var value: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    fun animateTo(target: Float) {
        animator?.cancel()
        val from = value
        animator = ValueAnimator.ofFloat(from, target.coerceIn(0f, 100f)).apply {
            duration = 750
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                value = it.animatedValue as Float
            }
            start()
        }
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        if (!context.theme.resolveAttribute(attr, value, true)) return fallback
        return if (value.resourceId != 0) {
            androidx.core.content.ContextCompat.getColor(context, value.resourceId)
        } else {
            value.data
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = progressPaint.strokeWidth / 2f
        oval.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(oval, startAngle, sweepAngle, false, trackPaint)

        val fraction = (value / 100f).coerceIn(0f, 1f)
        if (fraction > 0f) {
            canvas.drawArc(oval, startAngle, fraction * sweepAngle, false, progressPaint)
        }
    }
}
