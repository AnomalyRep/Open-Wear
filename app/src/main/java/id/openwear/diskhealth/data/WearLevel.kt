package id.openwear.diskhealth.data

import android.content.Context
import id.openwear.diskhealth.R
import java.util.Locale

/**
 * eMMC / UFS lifetime reader, based on the v4.0.1 `p045r1.f` coroutine but
 * extended for modern UFS sysfs.
 *
 * Scan order:
 *   1. `/sys/block/mmcblk0/device/life_time*`  -> eMMC
 *      `/dev/block/mmcblk0/device/life_time*`  -> old-root fallback
 *   2. UFS `health` sysfs / SCSI `health`      -> UFS (DeviceLifeTimeEstA/B)
 *   3. `/sys/devices/` `life_time*` nodes      -> vendor UFS/eMMC
 *   4. `/sys/devices/` `lifetime*` nodes       -> vendor UFS
 *   5. `/proc/ufs_health`                      -> vendor UFS module fallback
 *
 * The first match is `cat`-ed and split on whitespace. Two codes are the two
 * JEDEC estimates; four codes mean two dies/nodes. Everything is persisted in
 * the same `disk_*` preference keys as the original.
 */
data class WearNode(
    val percent: Int = 0,
    val hex: String = "",
    val percentText: String = "0%",
    val visible: Boolean = false
)

data class WearGroup(
    val title: String? = null,
    val nodePath: String? = null,
    val visible: Boolean = false,
    val one: WearNode? = null,
    val two: WearNode? = null
)

data class WearStats(
    val diskType: String = "Unknown",
    val groupA: WearGroup = WearGroup(),
    val groupB: WearGroup = WearGroup()
)

object WearLevelReader {

    private const val PREFS = "wear_prefs"

    private val CODES = linkedMapOf(
        "0x01" to 100, "0x02" to 90, "0x03" to 80, "0x04" to 70,
        "0x05" to 60, "0x06" to 50, "0x07" to 40, "0x08" to 30,
        "0x09" to 20, "0x0a" to 10, "0x0b" to 1
    )

    private const val UFS_ESTIMATE =
        "(?:Device\\s*)?Life\\s*Time\\s*Est(?:imation)?\\s*([AB])" +
            "\\s*[:=]\\s*(0[xX][0-9a-fA-F]+|[0-9a-fA-F]+)"

    private data class ScanResult(
        val path: String,
        val typeRes: Int,
        val values: List<String>,
        val nodes: List<String>
    )

    fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    /** Lower-case, two-digit form so `0X2`, `2` and `0x02` all map the same. */
    fun normalizeHex(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return ""
        val digits = text.removePrefix("0x").removePrefix("0X")
        val value = digits.toIntOrNull(16) ?: return "0x00"
        return String.format(Locale.US, "0x%02x", value)
    }

    fun percentOf(raw: String?): Int = CODES[normalizeHex(raw)] ?: 0

    fun percentText(raw: String?): String {
        val hex = normalizeHex(raw)
        val value = CODES[hex] ?: 0
        return if (hex == "0x0b") "EOL" else "$value%"
    }

    fun node(raw: String?): WearNode {
        if (raw.isNullOrBlank()) return WearNode()
        val hex = normalizeHex(raw)
        return WearNode(percent = percentOf(hex), hex = hex, percentText = percentText(hex), visible = true)
    }

    /** Performs the root scan and stores results in preferences. */
    suspend fun scan(context: Context) {
        val source = scanEmmc()
            ?: scanUfsHealth()
            ?: scanVendorLifetime()
            ?: scanProcUfs()
            ?: return

        val values = source.values
        val nodes = source.nodes
        if (values.isEmpty()) return

        val editor = prefs(context).edit()
        // Drop stale results so a 4-code eMMC does not leak into a 2-code UFS.
        listOf(
            "disk_hex_1a", "disk_hex_1b", "disk_hex_2a", "disk_hex_2b",
            "disk_single_hex", "disk_node_a", "disk_node_b", "disk_single_node"
        ).forEach { editor.remove(it) }

        editor.putString("disk_health_type", context.getString(source.typeRes))
        if (values.size >= 2) {
            editor.putString("disk_hex_1a", values[0])
            editor.putString("disk_hex_1b", values[1])
            if (values.size >= 4) {
                editor.putString("disk_hex_2a", values[2])
                editor.putString("disk_hex_2b", values[3])
            }
            if (nodes.isNotEmpty()) {
                editor.putString("disk_node_a", nodes[0])
                if (nodes.size > 1) editor.putString("disk_node_b", nodes[1])
            }
        } else {
            editor.putString("disk_single_hex", values[0])
            editor.putString("disk_single_node", source.path)
        }
        editor.apply()

        // History is recorded after the scan, exactly like the original.
        HistoryStore.record(context, "wear_history_a", values.getOrNull(0))
        HistoryStore.record(context, "wear_history_b", values.getOrNull(1))
    }

    private fun firstLine(vararg commands: String): String {
        for (command in commands) {
            val line = Shell.run(command, 20).stdout
                .lineSequence().firstOrNull().orEmpty().trim()
            if (line.isNotEmpty()) return line
        }
        return ""
    }

    private fun readCodes(path: String): List<String> =
        Shell.run("cat $path", 20).stdout
            .replace('\n', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map(::normalizeHex)

    /** eMMC: sysfs first; `/dev/block/mmcblk0/device` is a broken v4 fallback. */
    private fun scanEmmc(): ScanResult? {
        val path = firstLine(
            // `device` is a symlink; a direct glob still resolves it.
            "ls -1d /sys/block/mmcblk0/device/life_time* 2>/dev/null | head -n 1",
            "find /sys/block/mmcblk0/device/ -type f -name 'life_time*' 2>/dev/null | " +
                "sed -e ':a;N;\$!ba;s/\\n/ /g'",
            "ls -1d /dev/block/mmcblk0/device/life_time* 2>/dev/null | head -n 1",
            "find /dev/block/mmcblk0/device/ -type f -name 'life_time*' 2>/dev/null | " +
                "sed -e ':a;N;\$!ba;s/\\n/ /g'"
        )
        if (path.isEmpty()) return null
        val values = readCodes(path)
        if (values.isEmpty()) return null
        return ScanResult(path, R.string.emmc, values, path.split(Regex("\\s+")))
    }

    /**
     * UFS: modern kernels expose `DeviceLifeTimeEstA/B` in a `health` file,
     * usually below `/sys/devices/platform/soc`, in a `*.ufshc` directory.
     */
    private fun scanUfsHealth(): ScanResult? {
        val path = firstLine(
            "ls -1d /sys/devices/platform/soc/*ufshc*/health " +
                "/sys/devices/platform/soc/*ufs*/health " +
                "/sys/devices/platform/*ufshc*/health " +
                "/sys/class/scsi_host/host*/device/health " +
                "/sys/class/scsi_device/*/device/health " +
                "/sys/block/sd*/device/health 2>/dev/null | head -n 1",
            "find /sys/devices/ -type f -path '*ufs*' -name 'health' 2>/dev/null | head -n 1"
        )
        if (path.isEmpty()) return null
        val text = Shell.run("cat $path", 20).stdout
        val values = parseUfsHealth(text)
        if (values.isEmpty() || values.all { it == "0x00" }) return null
        return ScanResult(path, R.string.ufs, values, listOf(path))
    }

    private fun parseUfsHealth(text: String): List<String> {
        val found = linkedMapOf<String, String>()
        Regex(UFS_ESTIMATE, RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
            found[match.groupValues[1].uppercase(Locale.US)] = match.groupValues[2]
        }
        val a = found["A"]
        val b = found["B"]
        if (a != null) return listOfNotNull(a, b)
        if (b != null) return listOf(b)
        // Some vendor modules print only two raw codes.
        val codes = Regex("0[xX][0-9a-fA-F]+").findAll(text).map { it.value }.toList()
        return if (codes.size == 2) codes else emptyList()
    }

    /** Older/vendor UFS and eMMC nodes: `life_time*` then `lifetime*`. */
    private fun scanVendorLifetime(): ScanResult? {
        for (name in listOf("life_time*", "lifetime*")) {
            val paths = firstLine(
                "find /sys/devices/ -type f -name '$name' 2>/dev/null | " +
                    "sed -e ':a;N;\$!ba;s/\\n/ /g'"
            )
            if (paths.isEmpty()) continue
            val values = readCodes(paths)
            if (values.isEmpty()) continue
            val type = if (paths.contains("mmcblk")) R.string.emmc else R.string.ufs
            return ScanResult(
                paths,
                type,
                values,
                paths.split(Regex("\\s+")).filter { it.isNotEmpty() }
            )
        }
        return null
    }

    /** Vendor kernel modules sometimes expose `/proc/ufs_health`. */
    private fun scanProcUfs(): ScanResult? {
        val text = Shell.run("cat /proc/ufs_health 2>/dev/null", 10).stdout
        if (text.isBlank()) return null
        val values = parseUfsHealth(text)
        if (values.isEmpty() || values.all { it == "0x00" }) return null
        return ScanResult("/proc/ufs_health", R.string.ufs, values, listOf("/proc/ufs_health"))
    }

    /** Rebuilds the UI model from shared preferences (`p045r1.g.b`). */
    fun stats(context: Context): WearStats {
        val p = prefs(context)
        val type = p.getString("disk_health_type", context.getString(R.string.unknown))
            ?: context.getString(R.string.unknown)
        val nodeA = p.getString("disk_node_a", "").orEmpty()
        val nodeB = p.getString("disk_node_b", "").orEmpty()
        val hex1a = p.getString("disk_hex_1a", null)
        val hex2a = p.getString("disk_hex_2a", null)
        val single = p.getString("disk_single_hex", null)
        val hex1b = p.getString("disk_hex_1b", null)
        val hex2b = p.getString("disk_hex_2b", null)

        val sameNode = hex1a != null && hex1b != null && hex1a == hex1b

        val groupA: WearGroup
        val groupB: WearGroup

        if (single == null) {
            groupA = if (hex1a != null) {
                WearGroup(
                    title = context.getString(R.string.wear_level),
                    nodePath = nodeA,
                    visible = !sameNode,
                    one = node(hex1a),
                    two = if (hex2a != null) node(hex2a) else WearNode()
                )
            } else {
                WearGroup()
            }
            groupB = if (hex1b != null) {
                WearGroup(
                    title = context.getString(R.string.cycle_usages),
                    nodePath = nodeB,
                    visible = true,
                    one = node(hex1b),
                    two = if (hex2b != null) node(hex2b) else WearNode()
                )
            } else {
                WearGroup()
            }
        } else {
            val singleNode = p.getString("disk_single_node", "").orEmpty()
            groupA = WearGroup(
                title = context.getString(R.string.wear_level),
                nodePath = singleNode,
                visible = true,
                one = node(single),
                two = WearNode()
            )
            groupB = if (hex1b != null) {
                WearGroup(
                    title = context.getString(R.string.cycle_usages),
                    nodePath = nodeB,
                    visible = true,
                    one = node(hex1b),
                    two = if (hex2b != null) node(hex2b) else WearNode()
                )
            } else {
                WearGroup()
            }
        }

        return WearStats(type, groupA, groupB)
    }

    /** `p045r1.g.c`: "Storage Health N %". */
    fun healthText(context: Context): String {
        val p = prefs(context)
        val hex = p.getString("disk_hex_1a", null) ?: p.getString("disk_single_hex", null)
        val value = if (hex == null) 0 else percentOf(hex)
        return context.getString(R.string.storage_health) + " $value %"
    }
}
