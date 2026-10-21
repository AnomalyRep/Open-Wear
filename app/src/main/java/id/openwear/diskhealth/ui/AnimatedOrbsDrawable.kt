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
 * v5 Liquid Glass ambient background.
 *
 * The drawable mirrors the reference aurora mesh: a dark or light base color
 * plus the same five (dark) or four (light) radial orbs. When [animated] is
 * true the orbs drift slowly, matching the Animated Orbs background option.
 */
class AnimatedOrbsDrawable(
    private val dark: Boolean = false,
    private val animated: Boolean = true
) : Drawable(), Animatable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 24_000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    private data class Orb(
        val x: Float,
        val y: Float,
        val radius: Float,
        val color: Int
    )

    private val orbs: List<Orb> = if (animated) {
        // v5 drawAuroraMesh: five dark orbs, four light orbs.
        if (dark) {
            listOf(
                Orb(0.85f, 0.10f, 0.90f, Color.argb(133, 0x1D, 0x4E, 0xD8)),
                Orb(0.08f, 0.40f, 0.85f, Color.argb(115, 0x7C, 0x3A, 0xED)),
                Orb(0.92f, 0.62f, 0.80f, Color.argb(97, 0x43, 0x38, 0xCA)),
                Orb(0.25f, 0.88f, 0.85f, Color.argb(102, 0x02, 0x84, 0xC7)),
                Orb(0.85f, 0.95f, 0.70f, Color.argb(71, 0xBE, 0x18, 0x5D))
            )
        } else {
            listOf(
                Orb(0.85f, 0.10f, 0.90f, Color.argb(115, 0x38, 0xBD, 0xF8)),
                Orb(0.08f, 0.40f, 0.85f, Color.argb(102, 0xA7, 0x8B, 0xFA)),
                Orb(0.92f, 0.62f, 0.80f, Color.argb(77, 0xF4, 0x72, 0xB6)),
                Orb(0.25f, 0.88f, 0.85f, Color.argb(89, 0x2D, 0xD4, 0xBF))
            )
        }
    } else {
        // v5 drawLiquidGlassAmbientBackground: three softer orbs.
        if (dark) {
            listOf(
                Orb(0.85f, 0.15f, 0.85f, Color.argb(71, 0x38, 0xBD, 0xF8)),
                Orb(0.15f, 0.55f, 0.75f, Color.argb(56, 0x81, 0x8C, 0xF8)),
                Orb(0.80f, 0.85f, 0.70f, Color.argb(46, 0xF4, 0x72, 0xB6))
            )
        } else {
            listOf(
                Orb(0.85f, 0.12f, 0.85f, Color.argb(102, 0xBA, 0xE6, 0xFD)),
                Orb(0.15f, 0.50f, 0.75f, Color.argb(82, 0xDD, 0xD6, 0xFE)),
                Orb(0.80f, 0.85f, 0.70f, Color.argb(64, 0xFE, 0xD7, 0xAA))
            )
        }
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return

        paint.shader = null
        paint.color = if (dark) 0xFF070B14.toInt() else 0xFFF1F5F9.toInt()
        canvas.drawRect(bounds, paint)

        val tau = (PI * 2.0).toFloat()
        orbs.forEachIndexed { index, orb ->
            val drift = if (animated) phase * tau + index * 1.1f else 0f
            val cx = w * orb.x + if (animated) sin(drift) * w * 0.035f else 0f
            val cy = h * orb.y + if (animated) cos(drift * 0.9f) * h * 0.025f else 0f
            paint.shader = RadialGradient(
                cx,
                cy,
                w * orb.radius,
                intArrayOf(orb.color, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, w * orb.radius, paint)
        }
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
        if (animated && !animator.isStarted) animator.start()
    }

    override fun stop() {
        if (animator.isStarted) animator.cancel()
    }
}
