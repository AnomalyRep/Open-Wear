package id.openwear.diskhealth.data

import android.content.Context
import android.os.Environment
import java.io.File

data class PartitionItem(val name: String, val path: String)

/**
 * Backup / restore store, ported from v4.0.1 `p053u1.j`, `b` and `f`.
 *
 * Backups live in `/storage/emulated/0/Backup/<partition>.img`. Restores are
 * written back to `/dev/block/bootdevice/by-name/<partition>`. The original
 * blocks restoring boot-critical partitions: boot, dtbo, vbmeta, splash, logo.
 */
object BackupManager {

    val defaultPartitions = listOf("boot", "fsc", "fsg", "dtbo", "vbmeta", "modemst1", "modemst2")
    val restoreBlocked = listOf("boot", "dtbo", "vbmeta", "splash", "logo")

    fun backupDir(): File = File(Environment.getExternalStorageDirectory(), "Backup")

    fun legacyDir(): File = File(Environment.getExternalStorageDirectory(), "PARTITION BACKUP")

    fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun lastBackupTime(context: Context): Long = prefs(context).getLong("last_backup_time", 0L)

    /** All partitions exposed by the bootdevice by-name directory. */
    fun listPartitions(): List<PartitionItem> {
        val output = Shell.run("ls -1 /dev/block/bootdevice/by-name/", 20).stdout
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { PartitionItem(it, "/dev/block/bootdevice/by-name/$it") }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun filterDefaults(all: List<PartitionItem>, includeAdvanced: Boolean): List<PartitionItem> {
        if (includeAdvanced) return all
        return all.filter { item -> defaultPartitions.any { it.equals(item.name, true) } }
    }

    /** Files in the backup folder; dotfiles are skipped. */
    fun listBackups(): List<PartitionItem> {
        val dir = backupDir()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && !it.name.startsWith(".") }
            ?.map { PartitionItem(it.name, it.absolutePath) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    /** True when a backup exists in the current or the legacy folder. */
    fun hasBackup(): Boolean {
        val legacy = legacyDir()
        if (legacy.exists() && legacy.listFiles()?.isNotEmpty() == true) return true
        return listBackups().isNotEmpty()
    }

    suspend fun backup(
        context: Context,
        selected: List<PartitionItem>,
        onProgress: (String) -> Unit
    ): Boolean {
        val dir = backupDir()
        if (!dir.exists()) dir.mkdirs()

        var ok = true
        selected.forEachIndexed { index, item ->
            onProgress("Backing up ${item.name} (${index + 1}/${selected.size})...")
            val target = File(dir, item.name + ".img")
            val result = Shell.runRoot("dd if=${item.path} of=${target.path} bs=4M", 300)
            if (result.exitCode != 0) {
                ok = false
                return@forEachIndexed
            }
        }

        if (ok) {
            prefs(context).edit().putLong("last_backup_time", System.currentTimeMillis()).apply()
        }
        return ok
    }

    suspend fun restore(
        selected: List<PartitionItem>,
        onProgress: (String) -> Unit
    ): Boolean {
        var ok = true
        selected.forEachIndexed { index, item ->
            onProgress("Restoring ${item.name} (${index + 1}/${selected.size})...")
            val partition = item.name.removeSuffix(".img")
            val target = "/dev/block/bootdevice/by-name/$partition"
            val exists = Shell.run("ls $target", 14).exitCode == 0
            if (!exists) return@forEachIndexed
            val result = Shell.runRoot("dd if=${item.path} of=$target bs=4M", 300)
            if (result.exitCode != 0) {
                ok = false
                return@forEachIndexed
            }
        }
        return ok
    }

    fun delete(selected: List<PartitionItem>): Boolean {
        var ok = true
        for (item in selected) {
            val file = File(item.path)
            if (file.exists() && !file.delete()) ok = false
        }
        return ok
    }
}
