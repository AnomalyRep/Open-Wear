package id.openwear.diskhealth.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Orb background in Jetpack Compose, reproducing the exact
 * cyber-glass aesthetic seen in reference Image 1.
 */
@Composable
fun AnimatedOrbsBackground(
    dark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition("OrbsTransition")

    val p1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "P1"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val baseBg = if (dark) Color(0xFF070B14) else Color(0xFFF1F5F9)
        drawRect(baseBg)

        val tau = (PI * 2.0).toFloat()
        val phase = p1 * tau
        val pulse = 1.0f + 0.12f * sin(phase * 2f)

        val blendMode = if (dark) BlendMode.Screen else BlendMode.SrcOver

        // Orb 1: Vibrant Cyan (top-left to center drift)
        val o1x = w * (0.25f + 0.30f * sin(phase * 0.8f))
        val o1y = h * (0.25f + 0.20f * cos(phase * 0.7f))
        val r1 = 1.10f * w * pulse
        val c1 = Color(0xFF00EDFF)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c1.copy(alpha = if (dark) 0.55f else 0.85f),
                    c1.copy(alpha = if (dark) 0.25f else 0.45f),
                    c1.copy(alpha = if (dark) 0.08f else 0.15f),
                    Color.Transparent
                ),
                center = Offset(o1x, o1y),
                radius = r1
            ),
            radius = r1,
            center = Offset(o1x, o1y),
            blendMode = blendMode
        )

        // Orb 2: Deep Violet / Purple (bottom-right to center drift)
        val o2x = w * (0.75f - 0.35f * sin(phase * 0.65f))
        val o2y = h * (0.65f - 0.30f * cos(phase * 0.75f))
        val r2 = 1.15f * w * pulse
        val c2 = Color(0xFF9C27B0)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c2.copy(alpha = if (dark) 0.50f else 0.82f),
                    c2.copy(alpha = if (dark) 0.20f else 0.42f),
                    c2.copy(alpha = if (dark) 0.06f else 0.14f),
                    Color.Transparent
                ),
                center = Offset(o2x, o2y),
                radius = r2
            ),
            radius = r2,
            center = Offset(o2x, o2y),
            blendMode = blendMode
        )

        // Orb 3: Magenta / Rose (bottom horizontal wave)
        val o3x = w * (0.45f + 0.40f * cos(phase * 0.6f))
        val o3y = h * (0.80f + 0.10f * sin(phase * 0.9f))
        val r3 = 1.05f * w * pulse
        val c3 = Color(0xFFFF007F)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c3.copy(alpha = if (dark) 0.45f else 0.80f),
                    c3.copy(alpha = if (dark) 0.18f else 0.40f),
                    c3.copy(alpha = if (dark) 0.05f else 0.12f),
                    Color.Transparent
                ),
                center = Offset(o3x, o3y),
                radius = r3
            ),
            radius = r3,
            center = Offset(o3x, o3y),
            blendMode = blendMode
        )

        // Orb 4: Electric Indigo / Blue (center-left orbit)
        val o4x = w * (0.35f - 0.20f * sin(phase * 0.85f))
        val o4y = h * (0.50f + 0.25f * cos(phase * 0.8f))
        val r4 = 1.00f * w * pulse
        val c4 = Color(0xFF3D5AFE)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c4.copy(alpha = if (dark) 0.42f else 0.78f),
                    c4.copy(alpha = if (dark) 0.16f else 0.38f),
                    c4.copy(alpha = if (dark) 0.04f else 0.10f),
                    Color.Transparent
                ),
                center = Offset(o4x, o4y),
                radius = r4
            ),
            radius = r4,
            center = Offset(o4x, o4y),
            blendMode = blendMode
        )
    }
}
