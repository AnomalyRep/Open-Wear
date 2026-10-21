package id.openwear.diskhealth.data

import android.content.Context
import android.os.Environment
import android.os.StatFs

/** Internal-data partition usage used by the donut chart. */
data class PartitionStats(
    val usedText: String = "-",
    val percent: Int = 0,
    val freeText: String = "-"
)

object DiskUsageReader {

    fun read(): PartitionStats = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val used = (total - free).coerceAtLeast(0)
        val percent = if (total <= 0) 0 else ((used * 100) / total).toInt()
        PartitionStats(
            usedText = size(used),
            percent = percent,
            freeText = size(free)
        )
    }.getOrDefault(PartitionStats())

    /** `p045r1.e.a(long)`: always two decimals, GB/MB/KB. */
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
