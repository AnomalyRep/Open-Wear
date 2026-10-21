package id.openwear.diskhealth.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.TextUtils
import id.openwear.diskhealth.R
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.Locale

/**
 * Device information provider, mirroring the v4.0.1 `p051t1.b` helper.
 *
 * Values are deliberately formatted the same way as the original: RAM and
 * external-storage capacities are rounded up to a standard power-of-two size
 * and printed with a trailing " GB".
 */
object DeviceInfoReader {

    data class Info(
        val model: String = "Unknown",
        val cpu: String = "Unknown",
        val gpu: String = "Unknown",
        val ram: String = "Unknown",
        val storage: String = "Unknown",
        val activeSlot: String = "A Only",
        val batteryCapacity: String = "-",
        val powerProfile: String = "0",
        val wearText: String = "Storage Health 0 %"
    )

    fun read(context: Context): Info {
        val prefs = prefs(context)
        val model = runCatching { Build.DEVICE.trim() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase(Locale.ROOT) ?: "Unknown Model"
        prefs.edit().putString("device_model", model).apply()

        val cpu = command("getprop ro.soc.model")
            .ifBlank { command("getprop ro.board.platform") }
            .ifBlank { command("grep 'Hardware' /proc/cpuinfo | cut -d: -f2") }
            .ifBlank { "Unknown" }
        val gpu = command("cat /sys/class/kgsl/kgsl-3d0/gpu_model").ifBlank { "Unknown" }
        val activeSlot = activeSlot(context)
        val battery = command("cat /sys/class/power_supply/battery/charge_full | cut -c1-4")
            .ifBlank { "-" }

        return Info(
            model = model,
            cpu = cpu,
            gpu = gpu,
            ram = standardGb(totalRam(context)),
            storage = standardGb(storageTotalBytes()),
            activeSlot = activeSlot,
            batteryCapacity = battery,
            powerProfile = powerProfile(context),
            wearText = WearLevelReader.healthText(context)
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    private fun command(cmd: String): String =
        Shell.run(cmd, 14).stdout.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    private fun totalRam(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        return info.totalMem
    }

    private fun storageTotalBytes(): Long = runCatching {
        StatFs(Environment.getExternalStorageDirectory().path).totalBytes
    }.getOrDefault(0L)

    /** `p051t1.b.c(long)`: round up to the next standard capacity. */
    fun standardGb(bytes: Long): String {
        val gb = bytes / 1_073_741_824.0
        val steps = listOf(1, 2, 4, 6, 8, 12, 16, 32, 64, 128, 256, 512, 1024)
        val value = steps.firstOrNull { it >= gb } ?: 1024
        return "$value GB"
    }

    private fun activeSlot(context: Context): String {
        val prefs = prefs(context)
        if (!prefs.contains("key_active_slot")) {
            val suffix = command("getprop ro.boot.slot_suffix").trim().lowercase(Locale.ROOT)
            val label = when {
                suffix.startsWith("a") -> "Slot A"
                suffix.startsWith("b") -> "Slot B"
                else -> "A Only"
            }
            val slotSuffix = when (label) {
                "Slot A" -> "_a"
                "Slot B" -> "_b"
                else -> ""
            }
            prefs.edit()
                .putString("key_active_slot", label)
                .putString("slot_suffix", slotSuffix)
                .apply()
        }
        return prefs.getString("key_active_slot", "A Only") ?: "A Only"
    }

    /**
     * v4 casts the reflective result directly to Double. Some OEMs return an
     * Integer, so fall back to Number here instead of crashing.
     */
    private fun powerProfile(context: Context): String = try {
        val clazz = Class.forName("com.android.internal.os.PowerProfile")
        val instance = clazz.getConstructor(Context::class.java).newInstance(context)
        val value = clazz.getMethod("getBatteryCapacity").invoke(instance)
        when (value) {
            is Number -> value.toDouble().toInt().toString()
            else -> "0"
        }
    } catch (t: Throwable) {
        if (t is InvocationTargetException) t.printStackTrace() else t.printStackTrace()
        "0"
    }

    fun powerProfileDouble(context: Context): Double = try {
        val clazz = Class.forName("com.android.internal.os.PowerProfile")
        val instance = clazz.getConstructor(Context::class.java).newInstance(context)
        (clazz.getMethod("getBatteryCapacity").invoke(instance) as Number).toDouble()
    } catch (t: Throwable) {
        0.0
    }

    fun isBlank(value: CharSequence?): Boolean = TextUtils.isEmpty(value)
}
