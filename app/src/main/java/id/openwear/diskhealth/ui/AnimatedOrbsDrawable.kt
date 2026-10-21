package id.openwear.diskhealth.ui

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight animated orb background used by the v5 Appearance setting.
 *
 * The drawable is intentionally cheap: three moving radial gradients on a
 * transparent canvas, animated with a single ValueAnimator.
 */
class AnimatedOrbsDrawable : Drawable(), Animatable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 18_000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val tau = (PI * 2.0).toFloat()

        drawOrb(
            canvas,
            w * 0.16f + sin(phase * tau) * w * 0.06f,
            h * 0.16f + cos(phase * tau) * h * 0.04f,
            w * 0.58f,
            0x553B82F6
        )
        drawOrb(
            canvas,
            w * 0.84f + cos(phase * tau + 1.2f) * w * 0.05f,
            h * 0.32f + sin(phase * tau + 0.7f) * h * 0.06f,
            w * 0.52f,
            0x44A855F7
        )
        drawOrb(
            canvas,
            w * 0.32f + sin(phase * tau + 2.1f) * w * 0.07f,
            h * 0.86f + cos(phase * tau + 1.8f) * h * 0.04f,
            w * 0.62f,
            0x3314B8A6
        )
    }

    private fun drawOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int
    ) {
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(color, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun isRunning(): Boolean = animator.isRunning

    override fun start() {
        if (!animator.isStarted) animator.start()
    }

    override fun stop() {
        animator.cancel()
    }
}
