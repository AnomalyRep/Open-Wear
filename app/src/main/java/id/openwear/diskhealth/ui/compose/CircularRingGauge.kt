package id.openwear.diskhealth.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated Circular Ring Gauge for UFS Wear Level with rippling wave / gerigi edge,
 * matching reference v5.0.0 `CircularWearProgressKt`.
 */
@Composable
fun CircularRingGauge(
    progress: Float, // 0..100
    primaryText: String,
    secondaryText: String = "",
    size: Dp = 96.dp,
    strokeWidth: Dp = 5.dp,
    customColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isPlaceholder = primaryText == "-" || primaryText == "--" || primaryText.isEmpty()

    val healthColor = customColor ?: when {
        isPlaceholder -> Color(0x6694A3B8)
        progress >= 80f -> Color(0xFF2E7D32) // HealthExcellent
        progress >= 60f -> Color(0xFF43A047) // HealthGood
        progress >= 40f -> Color(0xFFF57C00) // HealthFair
        progress > 1f -> Color(0xFFE53935)   // HealthCritical (e.g. 20% in Image 2)
        else -> Color(0xFFB71C1C)            // HealthEol
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Continuous sinusoidal wave progress ring (gerigi gelombang animasi)
        DeterminateWaveCircularProgressBar(
            progress = if (isPlaceholder) 0f else progress / 100f,
            color = healthColor,
            strokeWidth = strokeWidth,
            trackColor = Color(0x2EFFFFFF),
            modifier = Modifier.size(size)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primaryText,
                color = Color(0xFFF8FAFC),
                fontSize = if (secondaryText.isNotEmpty()) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (secondaryText.isNotEmpty()) {
                Text(
                    text = secondaryText,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
