package id.openwear.diskhealth.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.liquidglass.LiquidGlassProviderState
import id.openwear.diskhealth.R

data class TabItem(val title: String, val iconRes: Int)

/**
 * Floating bottom tab bar matching reference v5.0.0 `FloatingTabBarKt`.
 * Both tabs and indicator have exact identical height (44dp inside 52dp pill),
 * with spring animation for indicator position and width.
 */
@Composable
fun ComposeFloatingTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tabs = remember {
        listOf(
            TabItem("Wear Level", R.drawable.ic_disk_health),
            TabItem("Storage", R.drawable.ic_storage)
        )
    }

    var tab0WidthPx by remember { mutableFloatStateOf(0f) }
    var tab1WidthPx by remember { mutableFloatStateOf(0f) }

    val targetOffset = if (selectedTab == 0) 0f else tab0WidthPx
    val targetWidth = if (selectedTab == 0) tab0WidthPx else tab1WidthPx

    val indicatorOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420.0f),
        label = "indicatorOffset"
    )

    val indicatorWidth by animateFloatAsState(
        targetValue = if (targetWidth > 0f) targetWidth else 120f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420.0f),
        label = "indicatorWidth"
    )

    // Outer Container Pill: Exactly 52dp height, padding 4dp
    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(52.dp)
            .tabBarLiquidGlass(providerState)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Inner Container: Exactly 44dp height
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(44.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. Sliding Indicator Bubble: Exactly 44dp height
            if (indicatorWidth > 0f) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationX = indicatorOffset }
                        .width(with(density) { indicatorWidth.toDp() })
                        .height(44.dp)
                        .activeTabLiquidGlass(providerState)
                )
            }

            // 2. Tab Items Row: Exactly 44dp height
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = (index == selectedTab)

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .clickable { onTabSelected(index) }
                            .onGloballyPositioned { coords ->
                                val w = coords.size.width.toFloat()
                                if (index == 0) tab0WidthPx = w else tab1WidthPx = w
                            }
                            .padding(horizontal = if (isSelected) 16.dp else 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
