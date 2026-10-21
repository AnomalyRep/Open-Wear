package id.openwear.diskhealth.ui

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Orb background matching reference v5.0.0 AnimatedOrbBackgroundKt.
 * Uses the exact 4-orb orbital trajectories, 4-stop radial gradient falloffs,
 * screen blending, and vivid cyber-glass palette (cyan, purple, magenta, electric blue).
 */
class AnimatedOrbsDrawable(
    private val dark: Boolean = true,
    private val animated: Boolean = true
) : Drawable(), Animatable {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (dark) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
    }

    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16_000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    // Exact palette from reference AnimatedOrbBackgroundKt.rememberRandomOrbPalette
    private val orb1Color = 0xFF00EDFF.toInt() // Vibrant Cyan
    private val orb2Color = 0xFF9C27B0.toInt() // Deep Violet / Purple
    private val orb3Color = 0xFFFF007F.toInt() // Magenta / Rose
    private val orb4Color = 0xFF3D5AFE.toInt() // Electric Indigo / Blue

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return

        // Base background matching reference Color(isDark ? 0xFF070B14 : 0xFFF1F5F9)
        basePaint.shader = null
        basePaint.color = if (dark) 0xFF070B14.toInt() else 0xFFF1F5F9.toInt()
        canvas.drawRect(bounds, basePaint)

        val tau = (PI * 2.0).toFloat()
        val p = if (animated) phase * tau else 0f

        // Pulse scale: 0.95f to 1.20f matching reference AnimatedOrbState
        val pulse = 1.0f + 0.12f * sin(p * 2f)

        // Orb 1: Cyan (top-left to center drift)
        val o1x = w * (0.25f + 0.30f * sin(p * 0.8f))
        val o1y = h * (0.25f + 0.20f * cos(p * 0.7f))
        val r1 = 1.10f * w * pulse
        drawGradientOrb(canvas, o1x, o1y, r1, orb1Color, 0.55f, 0.25f, 0.08f)

        // Orb 2: Purple (bottom-right to center drift)
        val o2x = w * (0.75f - 0.35f * sin(p * 0.65f))
        val o2y = h * (0.65f - 0.30f * cos(p * 0.75f))
        val r2 = 1.15f * w * pulse
        drawGradientOrb(canvas, o2x, o2y, r2, orb2Color, 0.50f, 0.20f, 0.06f)

        // Orb 3: Magenta (bottom horizontal wave)
        val o3x = w * (0.45f + 0.40f * cos(p * 0.6f))
        val o3y = h * (0.80f + 0.10f * sin(p * 0.9f))
        val r3 = 1.05f * w * pulse
        drawGradientOrb(canvas, o3x, o3y, r3, orb3Color, 0.45f, 0.18f, 0.05f)

        // Orb 4: Electric Blue (center-left orbit)
        val o4x = w * (0.35f - 0.20f * sin(p * 0.85f))
        val o4y = h * (0.50f + 0.25f * cos(p * 0.8f))
        val r4 = 1.00f * w * pulse
        drawGradientOrb(canvas, o4x, o4y, r4, orb4Color, 0.42f, 0.16f, 0.04f)
    }

    private fun drawGradientOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha0: Float,
        alpha1: Float,
        alpha2: Float
    ) {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        val c0 = Color.argb((alpha0 * 255).toInt(), r, g, b)
        val c1 = Color.argb((alpha1 * 255).toInt(), r, g, b)
        val c2 = Color.argb((alpha2 * 255).toInt(), r, g, b)
        val c3 = Color.TRANSPARENT

        orbPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(c0, c1, c2, c3),
            floatArrayOf(0f, 0.40f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, orbPaint)
        orbPaint.shader = null
    }

    override fun setAlpha(alpha: Int) {
        basePaint.alpha = alpha
        orbPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        basePaint.colorFilter = colorFilter
        orbPaint.colorFilter = colorFilter
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
