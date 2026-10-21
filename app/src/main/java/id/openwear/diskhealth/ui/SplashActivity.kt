package id.openwear.diskhealth.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.kyant.liquidglass.LiquidGlassProviderState
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.AccessMode
import id.openwear.diskhealth.data.HistoryStore
import id.openwear.diskhealth.data.Shell
import id.openwear.diskhealth.ui.activity.MainActivity
import id.openwear.diskhealth.ui.compose.AnimatedOrbsBackground
import id.openwear.diskhealth.ui.compose.WaveCircularProgressBar
import id.openwear.diskhealth.ui.compose.cardLiquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 5-Step Setup Wizard in pure Jetpack Compose using
 * https://github.com/gudrmsglgl/androidliquidglass:
 * Welcome -> Access Mode -> Permissions -> Purchase Date -> Loading.
 */
class SplashActivity : ComponentActivity() {

    private fun prefs() = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        setContent {
            val providerState = rememberLiquidGlassProviderState(backgroundColor = Color(0xFF070B14))
            val coroutineScope = rememberCoroutineScope()

            var currentStep by remember {
                mutableIntStateOf(if (HistoryStore.isPurchaseSet(this@SplashActivity)) 4 else 0)
            }
            var selectedMode by remember { mutableStateOf(AccessMode.ROOT) }
            var isRootGranted by remember { mutableStateOf(false) }
            var rootCheckStatus by remember { mutableStateOf("Checking root access...") }
            var showAgreementDialog by remember {
                mutableStateOf(!prefs().getBoolean("key_agreements", false))
            }

            fun openMain() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }

            fun checkRoot(force: Boolean = false) {
                coroutineScope.launch {
                    rootCheckStatus = "Checking root access..."
                    val rooted = withContext(Dispatchers.IO) { Shell.isRooted(force = force) }
                    isRootGranted = rooted
                    rootCheckStatus = if (rooted) "Root access granted" else "Root access not granted"
                }
            }

            LaunchedEffect(Unit) {
                if (currentStep == 4) {
                    delay(1800)
                    if (!prefs().contains(AccessMode.KEY)) {
                        val mode = withContext(Dispatchers.IO) {
                            if (Shell.isRooted()) AccessMode.ROOT else AccessMode.NON_ROOT
                        }
                        AccessMode.set(this@SplashActivity, mode)
                    }
                    openMain()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Background behind the liquid glass: Sharp Reference Wallpaper
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.bg_flat_dark),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlassProvider(providerState)
                )

                // 5-Step Animated Wizard
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "WizardStep"
                    ) { step ->
                        when (step) {
                            // Step 0: Welcome
                            0 -> {
                                WizardCard(providerState) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(Color(0x330284C7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_memory),
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("OpenWear", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        stringResource(id = R.string.tagline),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Button(
                                        onClick = {
                                            currentStep = 1
                                            checkRoot(force = false)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("START", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }

                            // Step 1: Access Mode
                            1 -> {
                                WizardCard(providerState) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0x330284C7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_shieldbug),
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Access Mode", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(id = R.string.access_mode_wizard_desc),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Root mode card
                                    AccessModeOptionCard(
                                        title = "Root Mode",
                                        desc = "Full kernel sysfs access & raw partition backup via SuperSU/Magisk/KernelSU",
                                        isSelected = (selectedMode == AccessMode.ROOT),
                                        onClick = { selectedMode = AccessMode.ROOT }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Non-root mode card
                                    AccessModeOptionCard(
                                        title = "Non-Root Mode",
                                        desc = "Standalone reading of public 0444 kernel nodes and standard battery API",
                                        isSelected = (selectedMode == AccessMode.NON_ROOT),
                                        onClick = { selectedMode = AccessMode.NON_ROOT }
                                    )

                                    if (selectedMode == AccessMode.ROOT) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0x331E293B), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0x6694A3B8), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rootCheckStatus,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (!isRootGranted) {
                                                Button(
                                                    onClick = { checkRoot(force = true) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                                ) {
                                                    Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            AccessMode.set(this@SplashActivity, selectedMode)
                                            currentStep = 2
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text(
                                            text = "Continue with ${if (selectedMode == AccessMode.ROOT) "Root" else "Non-Root"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            // Step 2: Permissions
                            2 -> {
                                WizardCard(providerState) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0x330284C7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_folder),
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Permission Request", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(id = R.string.we_need_the_following_permissi),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x331E293B), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0x6694A3B8), RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_folder),
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Storage (All files access)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_information),
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                                                runCatching {
                                                    startActivity(
                                                        Intent(
                                                            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                            Uri.parse("package:$packageName")
                                                        )
                                                    )
                                                }
                                            } else {
                                                currentStep = 3
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("GRANT ALL PERMISSIONS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            // Step 3: Purchase Date
                            3 -> {
                                WizardCard(providerState) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0x330284C7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_calendar),
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Purchase Date", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(id = R.string.to_calculate_your_daily_wear_l),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                this@SplashActivity,
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
                                                    HistoryStore.savePurchaseDate(this@SplashActivity, chosen)
                                                    currentStep = 4
                                                    coroutineScope.launch {
                                                        delay(1800)
                                                        openMain()
                                                    }
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("SELECT PURCHASE DATE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            currentStep = 4
                                            coroutineScope.launch {
                                                delay(1800)
                                                openMain()
                                            }
                                        }
                                    ) {
                                        Text("Skip for now", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    }
                                }
                            }

                            // Step 4: Loading Screen (Matching reference!)
                            4 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(Color(0x331E293B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_toolbar),
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("OpenWear", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(id = R.string.tagline),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(36.dp))
                                    WaveCircularProgressBar(
                                        modifier = Modifier.size(36.dp),
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 3.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Reading device storage...", color = Color(0xFF38BDF8), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Disclaimer Modal
                if (showAgreementDialog) {
                    Dialog(onDismissRequest = {}) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cardLiquidGlass(providerState)
                                .padding(24.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.disclaimer_title),
                                    color = Color(0xFFEF4444),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                val scrollState = rememberScrollState()
                                Box(
                                    modifier = Modifier
                                        .height(220.dp)
                                        .verticalScroll(scrollState)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.agreements),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { finishAffinity() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        Text("Disagree")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            prefs().edit().putBoolean("key_agreements", true).apply()
                                            showAgreementDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("Agree")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardCard(
    providerState: LiquidGlassProviderState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardLiquidGlass(providerState)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Composable
private fun AccessModeOptionCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0x330284C7) else Color(0x221E293B))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0x4494A3B8),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(
                text = desc,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
    }
}
