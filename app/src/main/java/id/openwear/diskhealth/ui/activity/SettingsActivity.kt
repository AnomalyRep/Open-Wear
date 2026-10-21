package id.openwear.diskhealth.ui.activity

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.kyant.liquidglass.LiquidGlassProviderState
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import id.openwear.diskhealth.BuildConfig
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.AccessMode
import id.openwear.diskhealth.data.BatteryInfoReader
import id.openwear.diskhealth.data.HistoryStore
import id.openwear.diskhealth.ui.compose.AnimatedOrbsBackground
import id.openwear.diskhealth.ui.compose.cardLiquidGlass
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modern v5.0.0 Settings screen matching reference Wear Level Insight.
 * Includes Header Credits card, Appearances with Refraction Sliders,
 * Access Mode, Backup & Restore, Preferences and Dependencies, and More.
 */
class SettingsActivity : ComponentActivity() {

    private fun prefs() = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        setContent {
            val providerState = rememberLiquidGlassProviderState(backgroundColor = Color(0xFF070B14))

            var currentAppearance by remember {
                mutableStateOf(prefs().getString(getString(R.string.key_appearance), "liquid") ?: "liquid")
            }
            var currentBackground by remember {
                mutableStateOf(prefs().getString(getString(R.string.key_background_style), "flat") ?: "flat")
            }
            var currentTheme by remember {
                mutableStateOf(prefs().getString(getString(R.string.key_theme), "Follow system") ?: "Follow system")
            }
            var currentAccessMode by remember {
                mutableStateOf(AccessMode.current(this@SettingsActivity))
            }
            var purchaseDateMillis by remember {
                mutableStateOf(HistoryStore.getPurchaseDate(this@SettingsActivity))
            }
            var batteryCustomCap by remember {
                mutableStateOf(BatteryInfoReader.customCapacity(this@SettingsActivity))
            }
            var refractionAmount by remember {
                mutableFloatStateOf(prefs().getFloat("key_refraction_amount", 128f))
            }
            var refractionHeight by remember {
                mutableFloatStateOf(prefs().getFloat("key_refraction_height", 24f))
            }

            // Dialog states
            var showAppearanceDialog by remember { mutableStateOf(false) }
            var showBackgroundDialog by remember { mutableStateOf(false) }
            var showThemeDialog by remember { mutableStateOf(false) }
            var showAccessModeDialog by remember { mutableStateOf(false) }
            var showBatteryCapDialog by remember { mutableStateOf(false) }
            var showSavedPrefsDialog by remember { mutableStateOf(false) }
            var showDependenciesDialog by remember { mutableStateOf(false) }
            var showContributorsDialog by remember { mutableStateOf(false) }
            var showAppInfoDialog by remember { mutableStateOf(false) }
            var updateStatusText by remember { mutableStateOf<String?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                // Background behind the liquid glass: Sharp Reference Wallpaper
                if (currentBackground == "orbs") {
                    AnimatedOrbsBackground(
                        dark = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .liquidGlassProvider(providerState)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.bg_flat_dark),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .liquidGlassProvider(providerState)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Top Bar: Back arrow + "Settings"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Settings Sections
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 0. Header Credits Card matching Image 1
                        item {
                            HeaderCreditsCard(providerState)
                        }

                        // 1. Appearances matching Image 1
                        item {
                            SectionTitle("Appearances")
                            SettingsCard(providerState) {
                                SettingItemRow(
                                    iconRes = R.drawable.ic_theme,
                                    title = "Appearance",
                                    summary = if (currentAppearance == "liquid") "Liquid Glass" else "MD3 Expressive",
                                    onClick = { showAppearanceDialog = true }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_background,
                                    title = "Background Style",
                                    summary = if (currentBackground == "orbs") "Animated Orbs" else "Flat Color",
                                    onClick = { showBackgroundDialog = true }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_theme,
                                    title = "Theme",
                                    summary = currentTheme,
                                    onClick = { showThemeDialog = true }
                                )
                                ItemDivider()
                                // Slider 1: Refraction Amount Liquid Glass (128 dp)
                                SliderPreferenceItem(
                                    title = "Refraction Amount Liquid Glass",
                                    value = refractionAmount,
                                    range = 0f..256f,
                                    unit = "dp",
                                    onValueChange = {
                                        refractionAmount = it
                                        prefs().edit().putFloat("key_refraction_amount", it).apply()
                                    }
                                )
                                ItemDivider()
                                // Slider 2: Refraction Height Liquid Glass (24 dp)
                                SliderPreferenceItem(
                                    title = "Refraction Height Liquid Glass",
                                    value = refractionHeight,
                                    range = 0f..64f,
                                    unit = "dp",
                                    onValueChange = {
                                        refractionHeight = it
                                        prefs().edit().putFloat("key_refraction_height", it).apply()
                                    }
                                )
                            }
                        }

                        // 2. Access Mode
                        item {
                            SectionTitle("Access Mode")
                            SettingsCard(providerState) {
                                SettingItemRow(
                                    iconRes = R.drawable.ic_shieldbug,
                                    title = "Access Mode",
                                    summary = if (currentAccessMode == AccessMode.ROOT) "Root Mode" else "Non-Root Mode",
                                    onClick = { showAccessModeDialog = true }
                                )
                            }
                        }

                        // 3. Backup & Restore (ROOT only)
                        if (currentAccessMode == AccessMode.ROOT) {
                            item {
                                SectionTitle("Backup & Restore")
                                SettingsCard(providerState) {
                                    SettingItemRow(
                                        iconRes = R.drawable.ic_restore,
                                        title = "Backup & Restore Partitions",
                                        summary = "Backup and restore partition blocks",
                                        onClick = { startActivity(Intent(this@SettingsActivity, BackupRestoreActivity::class.java)) }
                                    )
                                }
                            }
                        }

                        // 4. Preferences and Dependencies
                        item {
                            SectionTitle("Preferences and Dependencies")
                            SettingsCard(providerState) {
                                val pDateStr = if (purchaseDateMillis > 0L) {
                                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(purchaseDateMillis))
                                } else {
                                    "Not set"
                                }
                                SettingItemRow(
                                    iconRes = R.drawable.ic_calendar,
                                    title = "Purchase Date",
                                    summary = pDateStr,
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        if (purchaseDateMillis > 0L) cal.timeInMillis = purchaseDateMillis
                                        DatePickerDialog(
                                            this@SettingsActivity,
                                            { _, y, m, d ->
                                                val chosen = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, y)
                                                    set(Calendar.MONTH, m)
                                                    set(Calendar.DAY_OF_MONTH, d)
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                                HistoryStore.savePurchaseDate(this@SettingsActivity, chosen)
                                                purchaseDateMillis = chosen
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_battery,
                                    title = "Battery Capacity",
                                    summary = if (batteryCustomCap > 0) "$batteryCustomCap mAh" else "System default",
                                    onClick = { showBatteryCapDialog = true }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_db,
                                    title = "Saved Preferences",
                                    summary = "Check saved preferences and configurations",
                                    onClick = { showSavedPrefsDialog = true }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_bracket,
                                    title = "Dependencies",
                                    summary = "Dependencies and libraries used in this app",
                                    onClick = { showDependenciesDialog = true }
                                )
                            }
                        }

                        // 5. More
                        item {
                            SectionTitle("More")
                            SettingsCard(providerState) {
                                SettingItemRow(
                                    iconRes = R.drawable.ic_web,
                                    title = "GitHub Source",
                                    summary = "View source code and repository on GitHub",
                                    onClick = { openUrl("https://github.com/AnomalyRep/Open-Wear") }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_refresh,
                                    title = "Check for Updates",
                                    summary = "Check the latest OpenWear release on GitHub",
                                    onClick = {
                                        Thread {
                                            val latest = fetchLatestTag()
                                            Handler(Looper.getMainLooper()).post {
                                                updateStatusText = when {
                                                    latest == null -> "Could not check for updates"
                                                    latest.trimStart('v', 'V') == BuildConfig.VERSION_NAME ->
                                                        "You are running the latest version\n\nv${BuildConfig.VERSION_NAME}"
                                                    else -> "A newer OpenWear release is available:\n\n$latest"
                                                }
                                            }
                                        }.start()
                                    }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_contributors,
                                    title = "Contributors",
                                    summary = "AnomalyRep",
                                    onClick = { showContributorsDialog = true }
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_telegram,
                                    title = "Telegram Channel",
                                    summary = "Join my Telegram channel for updates and information",
                                    onClick = null
                                )
                                ItemDivider()
                                SettingItemRow(
                                    iconRes = R.drawable.ic_information,
                                    title = "App Info",
                                    summary = "OpenWear v${BuildConfig.VERSION_NAME}",
                                    onClick = { showAppInfoDialog = true }
                                )
                            }
                        }
                    }
                }

                // Modals
                if (showAppearanceDialog) {
                    ChoiceDialog(
                        title = "Appearance",
                        items = listOf("Liquid Glass" to "liquid", "MD3 Expressive" to "md3"),
                        onSelect = { valId ->
                            prefs().edit().putString(getString(R.string.key_appearance), valId).apply()
                            currentAppearance = valId
                            showAppearanceDialog = false
                        },
                        onDismiss = { showAppearanceDialog = false },
                        providerState = providerState
                    )
                }

                if (showBackgroundDialog) {
                    ChoiceDialog(
                        title = "Background Style",
                        items = listOf("Flat Color" to "flat", "Animated Orbs" to "orbs"),
                        onSelect = { valId ->
                            prefs().edit().putString(getString(R.string.key_background_style), valId).apply()
                            currentBackground = valId
                            showBackgroundDialog = false
                        },
                        onDismiss = { showBackgroundDialog = false },
                        providerState = providerState
                    )
                }

                if (showThemeDialog) {
                    val modes = resources.getStringArray(R.array.theme_modes).toList()
                    ChoiceDialog(
                        title = "Choose Theme",
                        items = modes.map { it to it },
                        onSelect = { chosen ->
                            prefs().edit().putString(getString(R.string.key_theme), chosen).apply()
                            currentTheme = chosen
                            showThemeDialog = false
                            val mode = when (modes.indexOf(chosen)) {
                                1 -> AppCompatDelegate.MODE_NIGHT_NO
                                2 -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                            AppCompatDelegate.setDefaultNightMode(mode)
                        },
                        onDismiss = { showThemeDialog = false },
                        providerState = providerState
                    )
                }

                if (showAccessModeDialog) {
                    ChoiceDialog(
                        title = "Access Mode",
                        items = listOf("Root Mode" to "root", "Non-Root Mode" to "non_root"),
                        onSelect = { chosen ->
                            val mode = if (chosen == "root") AccessMode.ROOT else AccessMode.NON_ROOT
                            AccessMode.set(this@SettingsActivity, mode)
                            currentAccessMode = mode
                            showAccessModeDialog = false
                        },
                        onDismiss = { showAccessModeDialog = false },
                        providerState = providerState
                    )
                }

                if (showBatteryCapDialog) {
                    var inputVal by remember { mutableStateOf(if (batteryCustomCap > 0) "$batteryCustomCap" else "") }
                    Dialog(onDismissRequest = { showBatteryCapDialog = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cardLiquidGlass(providerState)
                                .padding(24.dp)
                        ) {
                            Column {
                                Text("Edit Battery Capacity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = inputVal,
                                    onValueChange = { inputVal = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Capacity in mAh") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = {
                                            BatteryInfoReader.setCustomCapacity(this@SettingsActivity, 0)
                                            batteryCustomCap = 0
                                            showBatteryCapDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        Text("Reset")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val v = inputVal.toIntOrNull()
                                            if (v != null && v in 100..30000) {
                                                BatteryInfoReader.setCustomCapacity(this@SettingsActivity, v)
                                                batteryCustomCap = v
                                                showBatteryCapDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSavedPrefsDialog) {
                    val items = prefs().all.entries.map { "${it.key}: ${it.value}" }
                    ListCopyDialog(
                        title = "Saved Preferences",
                        items = items,
                        onDismiss = { showSavedPrefsDialog = false },
                        providerState = providerState
                    )
                }

                if (showDependenciesDialog) {
                    ListCopyDialog(
                        title = "Dependencies",
                        items = DEPENDENCIES.lines().filter { it.isNotBlank() },
                        onDismiss = { showDependenciesDialog = false },
                        providerState = providerState
                    )
                }

                if (showContributorsDialog) {
                    ListCopyDialog(
                        title = "Contributors",
                        items = listOf("AnomalyRep"),
                        onDismiss = { showContributorsDialog = false },
                        providerState = providerState
                    )
                }

                if (showAppInfoDialog) {
                    Dialog(onDismissRequest = { showAppInfoDialog = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cardLiquidGlass(providerState)
                                .padding(24.dp)
                        ) {
                            Column {
                                Text("OpenWear", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Version: ${BuildConfig.VERSION_NAME}\nPackage: ${packageName}\nArchitecture: Kotlin + Jetpack Compose\nLiquid Glass: androidliquidglass\n\nMaintain your storage health",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { showAppInfoDialog = false },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }

                updateStatusText?.let { msg ->
                    Dialog(onDismissRequest = { updateStatusText = null }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cardLiquidGlass(providerState)
                                .padding(24.dp)
                        ) {
                            Column {
                                Text("Update Status", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(msg, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { updateStatusText = null },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun fetchLatestTag(): String? = runCatching {
        val connection = URL("https://api.github.com/repos/AnomalyRep/Open-Wear/releases/latest").openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "OpenWear")
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            val text = connection.inputStream.bufferedReader().readText()
            Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private companion object {
        const val DEPENDENCIES =
            "androidx.appcompat:appcompat:1.7.0\n" +
            "androidx.compose.ui:ui:1.7.0\n" +
            "androidx.compose.material3:material3:1.3.0\n" +
            "com.github.gudrmsglgl:androidliquidglass\n" +
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
    }
}

// -------------------------------------------------------------
// Settings Compose Components
// -------------------------------------------------------------
@Composable
private fun HeaderCreditsCard(
    providerState: LiquidGlassProviderState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0x331E293B), CircleShape)
                    .border(1.dp, Color(0x6694A3B8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_toolbar),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OpenWear", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Projects", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("OpenWear", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Maintain your storage health", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(Color(0x330284C7), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("v${BuildConfig.VERSION_NAME}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF38BDF8),
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    providerState: LiquidGlassProviderState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(vertical = 4.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingItemRow(
    iconRes: Int,
    title: String,
    summary: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (summary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = summary, color = Color(0xFF94A3B8), fontSize = 12.5.sp)
            }
        }
        if (onClick != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SliderPreferenceItem(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "dp",
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .background(Color(0x330284C7), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${value.toInt()} $unit",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF38BDF8),
                inactiveTrackColor = Color(0x3394A3B8)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = Color(0x33CBD5E1),
        thickness = 1.dp
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    providerState: LiquidGlassProviderState
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardLiquidGlass(providerState)
                .padding(24.dp)
        ) {
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                items.forEach { (label, valId) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(valId) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListCopyDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    providerState: LiquidGlassProviderState
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardLiquidGlass(providerState)
                .padding(24.dp)
        ) {
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                items.forEach { item ->
                    Text(
                        text = item,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Close")
                }
            }
        }
    }
}
