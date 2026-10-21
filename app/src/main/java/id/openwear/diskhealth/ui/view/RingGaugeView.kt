package id.openwear.diskhealth.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import id.openwear.diskhealth.R

/**
 * Circular ring progress gauge matching reference v5.0.0
 * CircularWearProgress / StorageDonutChart / BenchmarkGauge.
 */
class RingGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    var strokeWidthDp: Float = 6f
        set(value) {
            field = value
            trackPaint.strokeWidth = dp(value)
            progressPaint.strokeWidth = dp(value)
            invalidate()
        }

    var primaryText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var secondaryText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var customProgressColor: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    var isStorageDonut: Boolean = false
    var secondaryProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val secondaryProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val primaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        try {
            val tf = context.resources.getFont(R.font.sanfranciscoprorounded)
            typeface = Typeface.create(tf, Typeface.BOLD)
        } catch (_: Throwable) {
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        try {
            val tf = context.resources.getFont(R.font.sanfranciscoprorounded)
            typeface = Typeface.create(tf, Typeface.NORMAL)
        } catch (_: Throwable) {
            typeface = Typeface.DEFAULT
        }
    }

    private val oval = RectF()

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun resolveThemeColor(attr: Int, defaultColor: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else {
            defaultColor
        }
    }

    fun animateProgress(toValue: Float, durationMs: Long = 600) {
        val start = progress
        ValueAnimator.ofFloat(start, toValue.coerceIn(0f, 100f)).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val sw = dp(strokeWidthDp)
        val dark = isDark()

        val surfaceVariant = resolveThemeColor(
            com.google.android.material.R.attr.colorSurfaceVariant,
            if (dark) 0xFF334155.toInt() else 0xFFE2E8F0.toInt()
        )
        val onSurface = resolveThemeColor(
            com.google.android.material.R.attr.colorOnSurface,
            if (dark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        )
        val onSurfaceVariant = resolveThemeColor(
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            if (dark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
        )
        val primaryColor = resolveThemeColor(
            com.google.android.material.R.attr.colorPrimary,
            0xFF0284C7.toInt()
        )

        // Health-based color if not custom
        val progressColor = customProgressColor ?: when {
            progress >= 80f -> 0xFF22C55E.toInt()
            progress >= 60f -> 0xFF84CC16.toInt()
            progress >= 40f -> 0xFFF59E0B.toInt()
            progress > 0f -> 0xFFEF4444.toInt()
            else -> onSurfaceVariant
        }

        trackPaint.strokeWidth = sw
        trackPaint.color = if (dark) 0x2EFFFFFF else 0x22000000
        if (isStorageDonut) trackPaint.strokeCap = Paint.Cap.BUTT else trackPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.strokeWidth = sw
        progressPaint.color = progressColor
        if (isStorageDonut) progressPaint.strokeCap = Paint.Cap.BUTT else progressPaint.strokeCap = Paint.Cap.ROUND

        val padding = sw / 2f + dp(2f)
        oval.set(padding, padding, w - padding, h - padding)

        // Track ring (full 360)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        // Progress arc
        val sweepAngle = (progress / 100f) * 360f
        if (sweepAngle > 0f) {
            canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
        }

        // Secondary progress for storage donut
        if (isStorageDonut && secondaryProgress > 0f) {
            secondaryProgressPaint.strokeWidth = sw
            secondaryProgressPaint.strokeCap = Paint.Cap.BUTT
            secondaryProgressPaint.color = 0xFF38BDF8.toInt()
            val secSweep = (secondaryProgress / 100f) * 360f
            canvas.drawArc(oval, -90f + sweepAngle, secSweep, false, secondaryProgressPaint)
        }

        // Center text
        val cx = w / 2f
        val cy = h / 2f

        if (primaryText.isNotEmpty() && secondaryText.isNotEmpty()) {
            primaryTextPaint.textSize = dp(13f)
            primaryTextPaint.color = onSurface
            val pFm = primaryTextPaint.fontMetrics
            val pY = cy - dp(2f)

            secondaryTextPaint.textSize = dp(11f)
            secondaryTextPaint.color = onSurfaceVariant
            val sFm = secondaryTextPaint.fontMetrics
            val sY = cy + dp(12f)

            canvas.drawText(primaryText, cx, pY, primaryTextPaint)
            canvas.drawText(secondaryText, cx, sY, secondaryTextPaint)
        } else if (primaryText.isNotEmpty()) {
            primaryTextPaint.textSize = dp(18f)
            primaryTextPaint.color = onSurface
            val pFm = primaryTextPaint.fontMetrics
            val pY = cy - (pFm.ascent + pFm.descent) / 2f
            canvas.drawText(primaryText, cx, pY, primaryTextPaint)
        }
    }
}
