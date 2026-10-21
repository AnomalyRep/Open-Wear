package id.openwear.diskhealth.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.liquidglass.LiquidGlassProviderState
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.BatteryInfo
import id.openwear.diskhealth.data.DeviceInfoReader
import id.openwear.diskhealth.data.PartitionStats
import id.openwear.diskhealth.data.WearItem
import id.openwear.diskhealth.data.WearStats
import id.openwear.diskhealth.ui.Artwork

// -------------------------------------------------------------
// 1. DeviceCard (Matching Image 1)
// -------------------------------------------------------------
@Composable
fun DeviceCard(
    info: DeviceInfoReader.Info,
    isRoot: Boolean,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(20.dp)
    ) {
        Column {
            // Header: Device Model + Root Mode Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = info.model,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2563EB), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRoot) "Root Mode" else "Non-Root Mode",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color(0x33CBD5E1),
                thickness = 1.dp
            )

            // Body: 76x124 phone image + specs column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = Artwork.forModel(info.model)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 76.dp, height = 124.dp)
                        .padding(end = 16.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    SpecRow(R.drawable.ic_cpu, "Processor", info.cpu)
                    Spacer(modifier = Modifier.height(6.dp))
                    SpecRow(R.drawable.ic_gpu, "Graphics", info.gpu)
                    Spacer(modifier = Modifier.height(6.dp))
                    SpecRow(R.drawable.ic_memory, "Memory", info.ram)
                    Spacer(modifier = Modifier.height(6.dp))
                    SpecRow(R.drawable.ic_storage, "Storage", info.storage)
                    Spacer(modifier = Modifier.height(6.dp))
                    SpecRow(R.drawable.ic_slot, "Active System Slot", info.activeSlot)
                    Spacer(modifier = Modifier.height(6.dp))
                    SpecRow(R.drawable.ic_battery, "Battery Capacity", "${info.powerProfile} mAh")
                }
            }
        }
    }
}

@Composable
private fun SpecRow(iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label : ",
            color = Color(0xFFF8FAFC),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// 2. WearLevelCard (Matching Image 1)
// -------------------------------------------------------------
@Composable
fun WearLevelCard(
    wear: WearStats,
    isRoot: Boolean,
    onHistoryClick: () -> Unit,
    onWearClick: (WearItem) -> Unit,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(20.dp)
    ) {
        Column {
            // Header: Wear Level + History button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_disk_health),
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Wear Level",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0x6694A3B8), RoundedCornerShape(18.dp))
                        .background(Color(0x331E293B), RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onHistoryClick() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "History",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!isRoot) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.need_root_access),
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            } else {
                // Two Columns: Wear Level A (pSLC) | Wear Level B (TLC/MLC)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val itemA = wear.items.getOrNull(0)
                    val itemB = wear.items.getOrNull(1)

                    // Column A
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = itemA != null) { itemA?.let { onWearClick(it) } }
                    ) {
                        Text(
                            text = "Wear Level A (pSLC)",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        CircularRingGauge(
                            progress = itemA?.node?.percent?.toFloat() ?: 0f,
                            primaryText = itemA?.node?.hex?.ifEmpty { "-" } ?: "-",
                            secondaryText = itemA?.node?.percentText ?: "",
                            size = 96.dp
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(120.dp)
                            .background(Color(0x3394A3B8))
                    )

                    // Column B
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = itemB != null) { itemB?.let { onWearClick(it) } }
                    ) {
                        Text(
                            text = "Wear Level B (TLC/MLC)",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        CircularRingGauge(
                            progress = itemB?.node?.percent?.toFloat() ?: 0f,
                            primaryText = itemB?.node?.hex?.ifEmpty { "-" } ?: "-",
                            secondaryText = itemB?.node?.percentText ?: "",
                            size = 96.dp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. BatteryCard (Matching Image 1)
// -------------------------------------------------------------
@Composable
fun BatteryCard(
    battery: BatteryInfo,
    info: DeviceInfoReader.Info,
    onInfoClick: () -> Unit,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(20.dp)
    ) {
        Column {
            // Header: Battery + Status Chip + Info button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery),
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Battery",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x331E293B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x6694A3B8), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = battery.statusText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_information),
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Left battery glyph + Right specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column
                Column(
                    modifier = Modifier.width(90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BatteryCanvasGraphic(
                        percentage = battery.percentage,
                        isCharging = battery.isCharging
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${battery.percentage}%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right column specs
                Column(modifier = Modifier.weight(1f)) {
                    BatterySpecRow("Battery Health", if (battery.healthValid) "${battery.healthPercent}%" else "N/A", isCyan = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    BatterySpecRow("Battery Capacity", if (battery.designCapacityMah > 0) "${battery.designCapacityMah} mAh" else "N/A")
                    Spacer(modifier = Modifier.height(6.dp))
                    BatterySpecRow("Estimated Capacity", if (battery.estimatedCapacityMah > 0) "${battery.estimatedCapacityMah} mAh" else "N/A")
                    Spacer(modifier = Modifier.height(6.dp))
                    BatterySpecRow("Current Voltage", battery.voltage)
                    Spacer(modifier = Modifier.height(6.dp))
                    BatterySpecRow("Cycle Count", battery.cycleCount)
                    Spacer(modifier = Modifier.height(6.dp))
                    BatterySpecRow("Kernel Profile", "${info.powerProfile} mAh")
                }
            }
        }
    }
}

@Composable
private fun BatterySpecRow(label: String, value: String, isCyan: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 12.5.sp
        )
        Text(
            text = value,
            color = if (isCyan) Color(0xFF38BDF8) else Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// 4. StorageCard (Tab 1 - Matching Image 1 exactly!)
// -------------------------------------------------------------
@Composable
fun StorageCard(
    stats: PartitionStats,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chart),
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Storage",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Internal Storage (Cyan)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Internal Storage",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    StorageDonutChart(
                        progress = stats.internalPercent.toFloat(),
                        percentText = "${stats.internalPercent}%",
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StorageLegend(
                        usedSize = stats.internalUsedText,
                        freeSize = stats.internalFreeText,
                        usedColor = Color(0xFF38BDF8)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(130.dp)
                        .background(Color(0x3394A3B8))
                )

                // Column 2: External / android Storage (Purple)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stats.externalName,
                        color = Color(0xFF818CF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    StorageDonutChart(
                        progress = stats.externalPercent.toFloat(),
                        percentText = "${stats.externalPercent}%",
                        color = Color(0xFF818CF8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StorageLegend(
                        usedSize = stats.externalUsedText,
                        freeSize = stats.externalFreeText,
                        usedColor = Color(0xFF818CF8)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageDonutChart(
    progress: Float,
    percentText: String,
    subText: String = "Used",
    color: Color = Color(0xFF38BDF8),
    size: Dp = 76.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Continuous sinusoidal wave progress ring (gerigi gelombang animasi)
        DeterminateWaveCircularProgressBar(
            progress = progress / 100f,
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color(0x2EFFFFFF),
            modifier = Modifier.size(size)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subText,
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StorageLegend(
    usedSize: String,
    freeSize: String,
    usedColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Used row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(usedColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Used", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                Text(usedSize, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Free row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0x6694A3B8), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("free", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                Text(freeSize, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// 5. BenchmarkCard (Tab 1 - Matching Image 1 exactly!)
// -------------------------------------------------------------
@Composable
fun BenchmarkCard(
    score: Int,
    write: String,
    read: String,
    rand4k: String,
    lat: String,
    isRunning: Boolean,
    onRunClick: () -> Unit,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gauge),
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Benchmark",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Chip "Unknown"
                Box(
                    modifier = Modifier
                        .background(Color(0x331E293B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x4D818CF8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Unknown",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bright Blue Pill Button "RUN"
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0284C7), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isRunning) { onRunClick() }
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isRunning) "..." else "RUN",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Body: Horseshoe Gauge on the left + Metrics list on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Horseshoe Gauge
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BenchmarkHorseshoeGauge(
                        score = score,
                        isRunning = isRunning
                    )
                }

                // Right: Metrics
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BenchmarkMetricItem("Write Speed", write)
                    BenchmarkMetricItem("Read Speed", read)
                    BenchmarkMetricItem("Random Access (4K)", rand4k)
                    BenchmarkMetricItem("LATENCY", lat)
                }
            }
        }
    }
}

@Composable
fun BenchmarkHorseshoeGauge(
    score: Int,
    isRunning: Boolean = false,
    size: Dp = 110.dp,
    strokeWidth: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val animScore = remember { Animatable(0f) }

    LaunchedEffect(score) {
        animScore.animateTo(
            targetValue = score.toFloat().coerceIn(0f, 100f),
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sw = strokeWidth.toPx()
            val pad = sw / 2f + 2.dp.toPx()
            val arcSize = Size(size.toPx() - pad * 2, size.toPx() - pad * 2)
            val topLeft = Offset(pad, pad)

            val startAngle = 150f
            val maxSweep = 240f

            // Dark slate horseshoe background track matching Image 1
            drawArc(
                color = Color(0xFF334155),
                startAngle = startAngle,
                sweepAngle = maxSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweep = (animScore.value / 100f) * maxSweep
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF818CF8), Color(0xFF38BDF8), Color(0xFF22C55E)),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = if (isRunning) "..." else "$score",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BenchmarkMetricItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
        Text(text = value, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}
