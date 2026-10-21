package id.openwear.diskhealth.data

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Minimal root shell helper.
 *
 * The original application uses libsu (topjohnwu) to run privileged commands.
 * This rebuild keeps the same command surface but talks to `su` directly so the
 * project stays free of JitPack dependencies and hidden dex payloads.
 */
object Shell {

    data class Result(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val rooted: Boolean
    ) {
        val output: String get() = stdout.trim()
    }

    private val suCandidates = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/sbin/su",
        "/vendor/bin/su"
    )

    @Volatile
    private var cachedRoot: Boolean? = null

    fun suBinary(): String? {
        suCandidates.firstOrNull { File(it).exists() }?.let { return it }
        return runCatching {
            val p = ProcessBuilder("sh", "-c", "which su").start()
            p.waitFor(3, TimeUnit.SECONDS)
            p.inputStream.bufferedReader().readText().trim().ifEmpty { null }
        }.getOrNull()
    }

    /** True when a working su binary can execute `id` as uid 0. */
    fun isRooted(force: Boolean = false): Boolean {
        cachedRoot?.let { if (!force) return it }
        val su = suBinary()
        if (su == null) {
            cachedRoot = false
            return false
        }
        val result = runWith(su, "id", 8)
        val rooted = result.exitCode == 0 && result.stdout.contains("uid=0")
        cachedRoot = rooted
        return rooted
    }

    /** Runs a command through `su -mm -c`, the form used by v4 backup/restore. */
    fun runRoot(command: String, timeoutSeconds: Long = 60): Result {
        val su = suBinary() ?: return run(command, timeoutSeconds)
        return try {
            val process = ProcessBuilder(su, "-mm", "-c", command)
                .redirectErrorStream(false)
                .start()
            val out = StringBuilder()
            val err = StringBuilder()
            val outThread = Thread { runCatching { process.inputStream.bufferedReader().forEachLine { out.appendLine(it) } } }
            val errThread = Thread { runCatching { process.errorStream.bufferedReader().forEachLine { err.appendLine(it) } } }
            outThread.start()
            errThread.start()
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                process.waitFor(2, TimeUnit.SECONDS)
            }
            outThread.join(1000)
            errThread.join(1000)
            Result(
                stdout = out.toString().trim(),
                stderr = err.toString().trim(),
                exitCode = if (finished) process.exitValue() else -1,
                rooted = true
            )
        } catch (t: Throwable) {
            Result("", t.message ?: t.javaClass.simpleName, -1, true)
        }
    }

    fun run(command: String, timeoutSeconds: Long = 20): Result {
        val su = suBinary()
        if (su != null) {
            val rooted = runWith(su, command, timeoutSeconds)
            if (rooted.exitCode == 0 || rooted.stdout.isNotBlank()) return rooted
            // Some su builds require `su 0 sh -c`; try the shell form as a fallback.
            val fallback = runWith(su, "0 sh -c ${quote(command)}", timeoutSeconds)
            if (fallback.exitCode == 0 || fallback.stdout.isNotBlank()) return fallback
            return rooted
        }
        return runWith("sh", command, timeoutSeconds, rooted = false)
    }

    private fun runWith(executable: String, command: String, timeoutSeconds: Long, rooted: Boolean = true): Result {
        return try {
            val process = ProcessBuilder(executable, "-c", command)
                .redirectErrorStream(false)
                .start()

            val out = StringBuilder()
            val err = StringBuilder()
            val outThread = Thread { runCatching { process.inputStream.bufferedReader().forEachLine { out.appendLine(it) } } }
            val errThread = Thread { runCatching { process.errorStream.bufferedReader().forEachLine { err.appendLine(it) } } }
            outThread.start()
            errThread.start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                process.waitFor(2, TimeUnit.SECONDS)
            }
            outThread.join(1000)
            errThread.join(1000)

            Result(
                stdout = out.toString().trim(),
                stderr = err.toString().trim(),
                exitCode = if (finished) process.exitValue() else -1,
                rooted = rooted
            )
        } catch (t: Throwable) {
            Result("", t.message ?: t.javaClass.simpleName, -1, rooted)
        }
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
