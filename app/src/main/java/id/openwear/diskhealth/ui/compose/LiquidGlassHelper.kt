package id.openwear.diskhealth.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.LiquidGlassProviderState
import com.kyant.liquidglass.highlight.GlassHighlight
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight

/**
 * Exact reference v5.0.0 Liquid Glass parameters matching
 * `id.vern.wearlevelinsight.ui.theme.LiquidGlassKt`.
 */
@Composable
fun Modifier.cardLiquidGlass(
    providerState: LiquidGlassProviderState,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    dark: Boolean = isSystemInDarkTheme()
): Modifier {
    val glassStyle = GlassStyle(
        shape = shape,
        innerRefraction = InnerRefraction(
            height = RefractionHeight(24.dp),
            amount = RefractionAmount((-128).dp),
            eccentricFactor = 1.0f
        ),
        material = GlassMaterial(
            blurRadius = 12.dp,
            brush = SolidColor(Color.White),
            alpha = if (dark) 0.08f else 0.28f
        ),
        highlight = GlassHighlight.Dynamic(
            angle = 45f,
            decay = 1.5f,
            color = Color.White.copy(alpha = if (dark) 0.20f else 0.50f)
        )
    )

    // Exact 4-stop dark and 3-stop light rim border gradient from reference v5.0.0
    val rimBorderBrush = Brush.linearGradient(
        if (dark) {
            listOf(
                Color.White.copy(alpha = 0.60f), // 0x99FFFFFF
                Color(0x6693B9FD),              // 40% Slate/Sky (#93B9FD)
                Color(0x59A77FFA),              // 35% Violet (#A77FFA)
                Color(0x803885F8)               // 50% Primary Blue (#3885F8)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 1.00f),
                Color(0x99BACCFD),
                Color.White.copy(alpha = 0.85f)
            )
        }
    )

    return this
        .liquidGlass(state = providerState, style = glassStyle)
        .border(width = 1.dp, brush = rimBorderBrush, shape = shape)
}

/**
 * Exact outer pill container for the floating tab bar (Height 52dp, no blur, H:24dp A:-24dp).
 */
@Composable
fun Modifier.tabBarLiquidGlass(
    providerState: LiquidGlassProviderState,
    dark: Boolean = isSystemInDarkTheme()
): Modifier {
    val glassStyle = GlassStyle(
        shape = CircleShape,
        innerRefraction = InnerRefraction(
            height = RefractionHeight(24.dp),
            amount = RefractionAmount((-24).dp)
        ),
        material = GlassMaterial(
            blurRadius = 0.dp, // Reference has zero blur on outer container bar
            brush = SolidColor(Color.White),
            alpha = if (dark) 0.06f else 0.16f
        ),
        highlight = GlassHighlight.Dynamic(
            angle = 45f,
            decay = 1.5f,
            color = Color.White.copy(alpha = if (dark) 0.15f else 0.40f)
        )
    )

    val borderBrush = Brush.linearGradient(
        if (dark) {
            listOf(Color(0x6638BDF8), Color(0x66818CF8))
        } else {
            listOf(Color(0x990284C7), Color(0x99BACCFD))
        }
    )

    return this
        .liquidGlass(state = providerState, style = glassStyle)
        .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
}

/**
 * Exact active tab indicator bubble (Height 44dp, blur 12dp, H:24dp, A:-32dp).
 */
@Composable
fun Modifier.activeTabLiquidGlass(
    providerState: LiquidGlassProviderState,
    dark: Boolean = isSystemInDarkTheme()
): Modifier {
    val glassStyle = GlassStyle(
        shape = CircleShape,
        innerRefraction = InnerRefraction(
            height = RefractionHeight(24.dp),
            amount = RefractionAmount((-32).dp)
        ),
        material = GlassMaterial(
            blurRadius = 12.dp,
            brush = SolidColor(Color.White),
            alpha = if (dark) 0.18f else 0.32f
        ),
        highlight = GlassHighlight.Dynamic(
            angle = 45f,
            decay = 1.5f,
            color = Color.White.copy(alpha = if (dark) 0.85f else 0.70f)
        )
    )

    val borderBrush = Brush.linearGradient(
        if (dark) {
            listOf(Color(0xCC38BDF8), Color(0xCC818CF8))
        } else {
            listOf(Color(0xCC0284C7), Color(0xCCBAE6FD))
        }
    )

    return this
        .liquidGlass(state = providerState, style = glassStyle)
        .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
}
