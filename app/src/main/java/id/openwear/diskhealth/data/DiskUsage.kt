package id.openwear.diskhealth.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/** Internal and external/secondary storage partition usage. */
data class PartitionStats(
    val internalName: String = "Internal Storage",
    val internalPercent: Int = 0,
    val internalUsedText: String = "-",
    val internalFreeText: String = "-",
    val externalName: String = "android",
    val externalPercent: Int = 0,
    val externalUsedText: String = "-",
    val externalFreeText: String = "-",
    val hasExternal: Boolean = true,
    // Compatibility properties
    val percent: Int = internalPercent,
    val usedText: String = internalUsedText,
    val freeText: String = internalFreeText
)

object DiskUsageReader {

    fun read(context: Context? = null): PartitionStats = runCatching {
        // 1. Internal Storage (/data)
        val dataStat = StatFs(Environment.getDataDirectory().path)
        val dataTotal = dataStat.blockCountLong * dataStat.blockSizeLong
        val dataFree = dataStat.availableBlocksLong * dataStat.blockSizeLong
        val dataUsed = (dataTotal - dataFree).coerceAtLeast(0)
        val dataPct = if (dataTotal <= 0) 0 else ((dataUsed * 100) / dataTotal).toInt()

        // 2. Secondary volume ("android" / shared external or SD card)
        var extName = "android"
        var extUsed = 0L
        var extFree = 0L
        var extPct = 0
        var hasExt = false

        val extDirs = context?.getExternalFilesDirs(null) ?: emptyArray()
        if (extDirs.size > 1 && extDirs[1] != null) {
            val extFile = extDirs[1]
            if (extFile != null) {
                try {
                    val stat = StatFs(extFile.path)
                    val total = stat.blockCountLong * stat.blockSizeLong
                    extFree = stat.availableBlocksLong * stat.blockSizeLong
                    extUsed = (total - extFree).coerceAtLeast(0)
                    extPct = if (total > 0) ((extUsed * 100) / total).toInt() else 0
                    extName = extFile.name.ifEmpty { "android" }
                    hasExt = true
                } catch (_: Throwable) {}
            }
        }

        if (!hasExt) {
            // Check primary external storage (/storage/emulated/0)
            try {
                val stat = StatFs(Environment.getExternalStorageDirectory().path)
                val total = stat.blockCountLong * stat.blockSizeLong
                extFree = stat.availableBlocksLong * stat.blockSizeLong
                extUsed = (total - extFree).coerceAtLeast(0)
                extPct = if (total > 0) ((extUsed * 100) / total).toInt() else 0
                extName = "android"
                hasExt = true
            } catch (_: Throwable) {}
        }

        PartitionStats(
            internalName = "Internal Storage",
            internalPercent = dataPct,
            internalUsedText = size(dataUsed),
            internalFreeText = size(dataFree),
            externalName = extName,
            externalPercent = if (hasExt) extPct else 0,
            externalUsedText = if (hasExt) size(extUsed) else "-",
            externalFreeText = if (hasExt) size(extFree) else "-",
            hasExternal = hasExt
        )
    }.getOrDefault(PartitionStats())

    /** Always two decimals, GB/MB/KB matching original v4/v5 format. */
    fun size(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.2f MB", mb)
            else -> String.format(java.util.Locale.US, "%.2f KB", kb)
        }
    }
}
