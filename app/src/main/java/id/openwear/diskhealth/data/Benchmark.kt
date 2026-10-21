package id.openwear.diskhealth.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

/**
 * I/O benchmark ported from v4.0.1 `p045r1.c`.
 *
 * 25,600 * 4 KiB = 100 MB sequential write (with `fd.sync()`), a full read-back
 * and a 4 KiB cached read used as the latency sample in microseconds.
 */
data class BenchmarkResult(
    val writeBytesPerSecond: Long,
    val readBytesPerSecond: Long,
    val latencyMicros: Long
) {
    /** v4 score: ((3000 - latency) / 3000) * 100, capped at 100. */
    val score: Int
        get() = (((3000.0 - latencyMicros) / 3000.0) * 100.0).toInt().coerceIn(0, 100)

    companion object {
        fun formatSpeed(bytesPerSecond: Long): String = formatSize(bytesPerSecond) + "/s"
        fun formatLatency(micros: Long): String = "$micros \u00B5s"

        fun formatSize(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.2f KB", kb)
                else -> "$bytes Bytes"
            }
        }
    }
}

object Benchmark {

    private const val BLOCK = 4096
    private const val BLOCKS = 25_600
    private const val TOTAL_BYTES = BLOCK.toLong() * BLOCKS

    suspend fun run(context: Context, onProgress: (Int) -> Unit = {}): BenchmarkResult =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "io_test_benchmark.dat")
            val buffer = ByteArray(BLOCK)

            var writeMs = 0L
            var readMs = 0L
            var latencyUs = 0L

            try {
                val writeStart = System.nanoTime()
                FileOutputStream(file).use { out ->
                    for (i in 1..BLOCKS) {
                        out.write(buffer)
                        if (i % 1000 == 0) onProgress((i * 50) / BLOCKS)
                    }
                    out.flush()
                    out.fd.sync()
                }
                writeMs = (System.nanoTime() - writeStart) / 1_000_000
                onProgress(50)

                val readStart = System.nanoTime()
                var blocksRead = 0
                FileInputStream(file).use { input ->
                    val readBuffer = ByteArray(BLOCK)
                    while (input.read(readBuffer) != -1) {
                        blocksRead++
                        if (blocksRead % 1000 == 0) {
                            onProgress(50 + (blocksRead * 40) / BLOCKS)
                        }
                    }
                }
                readMs = (System.nanoTime() - readStart) / 1_000_000
                onProgress(90)

                val latencyStart = System.nanoTime()
                FileInputStream(file).use { input ->
                    input.read(ByteArray(BLOCK))
                }
                latencyUs = (System.nanoTime() - latencyStart) / 1000
                onProgress(100)
            } finally {
                runCatching { file.delete() }
            }

            val writeSpeed = if (writeMs <= 0) TOTAL_BYTES else 1000 * TOTAL_BYTES / writeMs
            val readSpeed = if (readMs <= 0) TOTAL_BYTES else 1000 * TOTAL_BYTES / readMs
            BenchmarkResult(writeSpeed, readSpeed, latencyUs)
        }
}
