package id.openwear.diskhealth.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 44dp x 78dp battery canvas graphic reproducing the exact visual from Image 1.
 */
@Composable
fun BatteryCanvasGraphic(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 44.dp, height = 78.dp)) {
        val w = size.width
        val h = size.height

        val strokeColor = Color(0xFF94A3B8)
        val primaryColor = Color(0xFF38BDF8)

        // 1. Terminal cap
        val capW = 0.38f * w
        val capH = 0.08f * h
        val capL = (w - capW) / 2f
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(capL, 0f),
            size = Size(capW, capH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // 2. Battery outer rounded body
        val bodyTop = capH + 2.dp.toPx()
        val bodyH = h - bodyTop
        val strokeW = 2.dp.toPx()
        val halfStroke = strokeW / 2f
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(halfStroke, bodyTop + halfStroke),
            size = Size(w - strokeW, bodyH - strokeW),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = strokeW)
        )

        // 3. Dynamic percentage fill (Red at <=15% as in Image 1 at 13%, Orange >15%, Green >40%)
        val pad = 4.dp.toPx()
        val innerW = w - (pad * 2f)
        val innerMaxH = bodyH - (pad * 2f)
        val pct = percentage.coerceIn(0, 100)
        val fillH = (pct / 100f) * innerMaxH
        val fillTop = bodyTop + pad + (innerMaxH - fillH)

        val fillColor = when {
            isCharging -> primaryColor
            percentage > 40 -> Color(0xFF22C55E)
            percentage > 15 -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444) // Red at 13%
        }

        if (fillH > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(pad, fillTop),
                size = Size(innerW, fillH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }

        // 4. Charging lightning bolt
        if (isCharging) {
            val cx = w / 2f
            val cy = bodyTop + (bodyH / 2f)
            val path = Path().apply {
                moveTo(cx + 3.dp.toPx(), cy - 14.dp.toPx())
                lineTo(cx - 7.dp.toPx(), cy + 1.dp.toPx())
                lineTo(cx - 1.dp.toPx(), cy + 1.dp.toPx())
                lineTo(cx - 3.dp.toPx(), cy + 14.dp.toPx())
                lineTo(cx + 7.dp.toPx(), cy - 1.dp.toPx())
                lineTo(cx + 1.dp.toPx(), cy - 1.dp.toPx())
                close()
            }
            drawPath(path, Color.White)
        }
    }
}
