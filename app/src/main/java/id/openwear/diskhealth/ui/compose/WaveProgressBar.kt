package id.openwear.diskhealth.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Indeterminate Wave Circular Progress Bar for loading screens,
 * matching reference v5.0.0 `WaveCircularProgressBar_tAjK0ZQ`.
 */
@Composable
fun WaveCircularProgressBar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 3.5.dp,
    trackColor: Color = Color.Transparent
) {
    val transition = rememberInfiniteTransition("WaveCircularIndeterminate")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val sweepPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepPhase"
    )

    Canvas(modifier = modifier.defaultMinSize(24.dp, 24.dp)) {
        val sizePx = minOf(size.width, size.height)
        val strokePx = strokeWidth.toPx()
        val waveAmp = (0.05f * sizePx).coerceIn(1.5f, 4.0f)
        val baseRadius = ((sizePx - strokePx) - (2 * waveAmp)) / 2.0f
        val center = Offset(size.width / 2.0f, size.height / 2.0f)

        if (trackColor != Color.Transparent) {
            drawCircle(
                color = trackColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
        }

        val sweepAngle = ((sin(sweepPhase) * 0.5f + 0.5f) * 200f) + 60f
        val path = Path()
        var first = true

        var a = 0f
        while (a <= sweepAngle) {
            val currentAngleDeg = rotation + a
            val rad = Math.toRadians(currentAngleDeg.toDouble())
            val r = baseRadius + (sin((a / sweepAngle) * 8.0 * PI + sweepPhase) * waveAmp).toFloat()
            val x = center.x + (cos(rad) * r).toFloat()
            val y = center.y + (sin(rad) * r).toFloat()

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            a += 4f
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

/**
 * Determinate Wave Circular Progress Bar with rippling wave / gerigi animation,
 * matching reference v5.0.0 `WaveCircularProgressBar_jSYEXPI` used in
 * CircularWearProgress and StorageDonutChart.
 */
@Composable
fun DeterminateWaveCircularProgressBar(
    progress: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 5.dp,
    trackColor: Color = Color(0x3394A3B8)
) {
    val transition = rememberInfiniteTransition("WaveCircularDeterminate")

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "AnimatedProg"
    )

    Canvas(modifier = modifier.defaultMinSize(24.dp, 24.dp)) {
        val sizePx = minOf(size.width, size.height)
        val strokePx = strokeWidth.toPx()
        val waveAmp = (0.05f * sizePx).coerceIn(1.5f, 4.0f)
        val baseRadius = ((sizePx - strokePx) - (2 * waveAmp)) / 2.0f
        val center = Offset(size.width / 2.0f, size.height / 2.0f)

        if (trackColor != Color.Transparent) {
            drawCircle(
                color = trackColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
        }

        val sweepAngle = animatedProgress * 360f
        if (sweepAngle > 0f) {
            val path = Path()
            var first = true

            var a = 0f
            while (a <= sweepAngle) {
                val currentAngleDeg = a - 90.0f
                val rad = Math.toRadians(currentAngleDeg.toDouble())
                val r = baseRadius + (sin(((a / 360.0f) * 16.0 * PI) + wavePhase) * waveAmp).toFloat()
                val x = center.x + (cos(rad) * r).toFloat()
                val y = center.y + (sin(rad) * r).toFloat()

                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                a += 4f
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}
