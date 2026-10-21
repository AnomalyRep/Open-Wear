package id.openwear.diskhealth.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import id.openwear.diskhealth.R
import java.io.File
import java.util.Locale

/**
 * v5-style battery health reader.
 *
 * Android exposes very little standardised battery health data, so values are
 * collected in this order: BatteryManager properties, public sysfs nodes, then
 * a calculated estimate from charge_full / charge_full_design. All reads are
 * read-only.
 */
data class BatteryInfo(
    val percentage: Int = 0,
    val statusText: String = "Unknown",
    val isCharging: Boolean = false,
    val healthPercent: Int = 0,
    val healthValid: Boolean = false,
    val designCapacityMah: Int = 0,
    val estimatedCapacityMah: Int = 0,
    val cycleCount: String = "N/A",
    val cycleCountValid: Boolean = false,
    val voltage: String = "N/A",
    val power: String = "N/A",
    val chargerStatus: String = "N/A",
    val customCapacity: Boolean = false
)

object BatteryInfoReader {

    const val KEY_CUSTOM_CAPACITY = "key_battery_capacity"

    private val CYCLE_NODES = listOf(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/power_supply/bms/cycle_count",
        "/sys/class/power_supply/battery/battery_cycle"
    )

    private val SOH_NODES = listOf(
        "/sys/class/power_supply/battery/soh",
        "/sys/class/power_supply/battery/health_percent",
        "/sys/class/power_supply/battery/fg_soh",
        "/sys/class/power_supply/battery/batt_health_pct",
        "/sys/class/power_supply/bms/soh"
    )

    private val DESIGN_NODES = listOf(
        "/sys/class/power_supply/battery/charge_full_design",
        "/sys/class/power_supply/bms/charge_full_design"
    )

    private val FULL_NODES = listOf(
        "/sys/class/power_supply/battery/charge_full",
        "/sys/class/power_supply/bms/charge_full"
    )

    fun customCapacity(context: Context): Int =
        prefs(context).getInt(KEY_CUSTOM_CAPACITY, 0)

    fun setCustomCapacity(context: Context, mah: Int) {
        prefs(context).edit().apply {
            if (mah > 0) putInt(KEY_CUSTOM_CAPACITY, mah)
            else remove(KEY_CUSTOM_CAPACITY)
        }.apply()
    }

    fun read(context: Context, batteryIntent: Intent? = null): BatteryInfo {
        val intent = batteryIntent ?: runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = when {
            level >= 0 && scale > 0 -> (level * 100 / scale).coerceIn(0, 100)
            else -> batteryInt(manager, BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.battery_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.battery_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_full)
            else -> context.getString(R.string.battery_not_charging)
        }

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageVolts = if (voltageMv > 0) voltageMv / 1000.0 else 0.0
        val voltage = if (voltageMv > 0) {
            String.format(Locale.US, "%.2f V", voltageVolts)
        } else {
            "N/A"
        }

        val custom = customCapacity(context)
        val design = when {
            custom > 0 -> custom
            else -> readMah(DESIGN_NODES).takeIf { it > 0 }
                ?: DeviceInfoReader.powerProfileDouble(context).toInt()
        }

        val chargeCounterUah = batteryInt(manager, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val fromCounter = when {
            chargeCounterUah > 0 && percentage > 0 -> {
                val mah = (chargeCounterUah / 1000.0) / (percentage / 100.0)
                if (mah in 500.0..15_000.0) mah.toInt() else 0
            }
            else -> 0
        }
        val estimated = readMah(FULL_NODES).takeIf { it > 0 } ?: fromCounter

        val nativeSoh = readInt(SOH_NODES).takeIf { it in 1..100 }
        val calculatedSoh = when {
            estimated > 0 && design > 0 -> (estimated * 100 / design).coerceIn(1, 100)
            else -> 0
        }
        val health = nativeSoh ?: calculatedSoh

        val cycleValue = readInt(CYCLE_NODES).takeIf { it > 0 }
            ?: if (Build.VERSION.SDK_INT >= 34) {
                intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it > 0 }
            } else null

        var currentMicroAmps: Long =
            batteryInt(manager, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toLong()
        if (currentMicroAmps == 0L) {
            currentMicroAmps = readLong(listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/bms/current_now"
            ))
        }
        val currentAmps = kotlin.math.abs(currentMicroAmps) / 1_000_000.0
        val watts = if (currentAmps > 0.05) voltageVolts * currentAmps else 0.0
        val power = if (watts > 0.1) String.format(Locale.US, "%.1f W", watts) else "N/A"

        val chargeType = intent?.getStringExtra("charge_type").orEmpty()
        val maxVoltage = intent?.getIntExtra("max_charging_voltage", 0) ?: 0
        val usbType = readText("/sys/class/power_supply/usb/type")
        val pd = maxVoltage > 5_000_000 || watts >= 15.0 ||
            chargeType.contains("PD", ignoreCase = true) ||
            usbType.contains("PD", ignoreCase = true)
        val chargerStatus = when {
            !charging -> "N/A"
            pd -> context.getString(R.string.battery_power_delivery)
            watts >= 10.0 -> context.getString(R.string.battery_fast_charge)
            else -> context.getString(R.string.battery_standard_charge)
        }

        return BatteryInfo(
            percentage = percentage,
            statusText = statusText,
            isCharging = charging,
            healthPercent = health,
            healthValid = health > 0,
            designCapacityMah = design.coerceAtLeast(0),
            estimatedCapacityMah = estimated.coerceAtLeast(0),
            cycleCount = cycleValue?.toString() ?: "N/A",
            cycleCountValid = cycleValue != null,
            voltage = voltage,
            power = power,
            chargerStatus = chargerStatus,
            customCapacity = custom > 0
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    private fun batteryInt(manager: BatteryManager?, property: Int): Int =
        runCatching { manager?.getIntProperty(property) ?: 0 }.getOrDefault(0)

    private fun readText(path: String): String = runCatching {
        val file = File(path)
        if (file.exists() && file.canRead()) file.readText().trim() else ""
    }.getOrDefault("")

    private fun readInt(paths: List<String>): Int {
        for (path in paths) {
            readText(path).toIntOrNull()?.let { return it }
        }
        return 0
    }

    private fun readLong(paths: List<String>): Long {
        for (path in paths) {
            readText(path).toLongOrNull()?.let { return it }
        }
        return 0L
    }

    /** Sysfs capacities are normally uAh; accept nAh and mAh values too. */
    private fun readMah(paths: List<String>): Int {
        val raw = readLong(paths)
        if (raw <= 0) return 0
        val mah = when {
            raw > 100_000_000 -> raw / 1_000_000
            raw > 100_000 -> raw / 1_000
            else -> raw
        }
        return mah.toInt().takeIf { it in 100..30_000 } ?: 0
    }
}
