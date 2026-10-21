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

data class WearItem(
    val title: String = "",
    val nodePath: String? = null,
    val node: WearNode = WearNode(),
    val visible: Boolean = true,
    val preEol: Boolean = false
)

data class WearStats(
    val diskType: String = "Unknown",
    val groupA: WearGroup = WearGroup(),
    val groupB: WearGroup = WearGroup(),
    val preEol: WearNode? = null,
    val items: List<WearItem> = emptyList()
)

object WearLevelReader {

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
        val nodes: List<String>,
        val preEol: String? = null
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

    /**
     * JEDEC PRE_EOL_INFO values use their own labels instead of the life-time
     * percentage table. 0x01 is healthy, 0x02 warning, 0x03 urgent.
     */
    fun preEolNode(raw: String?): WearNode {
        if (raw.isNullOrBlank()) return WearNode()
        val hex = normalizeHex(raw)
        val percent = when (hex) {
            "0x01" -> 100
            "0x02" -> 80
            "0x03" -> 50
            "0x0b" -> 1
            else -> 0
        }
        val text = when (hex) {
            "0x01" -> "Normal"
            "0x02" -> "Warning"
            "0x03" -> "Urgent"
            "0x0b" -> "EOL"
            else -> hex
        }
        return WearNode(percent = percent, hex = hex, percentText = text, visible = true)
    }

    /** Performs the scan and stores results in preferences. */
    suspend fun scan(context: Context) {
        val mode = AccessMode.current(context)
        val source = scanEmmc(mode)
            ?: scanUfsHealth(mode)
            ?: scanVendorLifetime(mode)
            ?: scanProcUfs(mode)
            ?: return

        val values = source.values
        val nodes = source.nodes
        if (values.isEmpty()) return

        val editor = prefs(context).edit()
        // Drop stale results so a 4-code eMMC does not leak into a 2-code UFS.
        listOf(
            "disk_hex_1a", "disk_hex_1b", "disk_hex_2a", "disk_hex_2b",
            "disk_single_hex", "disk_node_a", "disk_node_b", "disk_single_node",
            "disk_pre_eol"
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
        source.preEol?.let { editor.putString("disk_pre_eol", normalizeHex(it)) }
        editor.apply()

        // History is recorded after the scan, exactly like the original.
        HistoryStore.record(context, "wear_history_a", values.getOrNull(0))
        HistoryStore.record(context, "wear_history_b", values.getOrNull(1))
    }

    private fun exec(mode: AccessMode, command: String, timeoutSeconds: Long = 20): String =
        if (mode == AccessMode.ROOT) {
            Shell.run(command, timeoutSeconds).stdout
        } else {
            Shell.runUser(command, timeoutSeconds).stdout
        }

    private fun firstLine(mode: AccessMode, vararg commands: String): String {
        for (command in commands) {
            val line = exec(mode, command, 20)
                .lineSequence().firstOrNull().orEmpty().trim()
            if (line.isNotEmpty()) return line
        }
        return ""
    }

    private fun readCodes(mode: AccessMode, path: String): List<String> =
        exec(mode, "cat $path", 20)
            .replace('\n', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map(::normalizeHex)

    /** eMMC: sysfs first; `/dev/block/mmcblk0/device` is a broken v4 fallback. */
    private fun scanEmmc(mode: AccessMode): ScanResult? {
        val path = firstLine(
            mode,
            // `device` is a symlink; a direct glob still resolves it.
            "ls -1d /sys/block/mmcblk0/device/life_time* 2>/dev/null | head -n 1",
            "find /sys/block/mmcblk0/device/ -type f -name 'life_time*' 2>/dev/null | " +
                "sed -e ':a;N;\$!ba;s/\\n/ /g'",
            "ls -1d /dev/block/mmcblk0/device/life_time* 2>/dev/null | head -n 1",
            "find /dev/block/mmcblk0/device/ -type f -name 'life_time*' 2>/dev/null | " +
                "sed -e ':a;N;\$!ba;s/\\n/ /g'"
        )
        if (path.isEmpty()) return null
        val values = readCodes(mode, path)
        if (values.isEmpty()) return null
        val preEol = firstLine(
            mode,
            "cat /sys/block/mmcblk0/device/pre_eol_info 2>/dev/null",
            "cat /sys/block/mmcblk0/device/pre_eol 2>/dev/null",
            "cat /dev/block/mmcblk0/device/pre_eol_info 2>/dev/null"
        ).takeIf { it.isNotEmpty() }
        return ScanResult(
            path,
            R.string.emmc,
            values,
            path.split(Regex("\\s+")),
            preEol
        )
    }

    /**
     * UFS: modern kernels expose `DeviceLifeTimeEstA/B` in a `health` file,
     * usually below `/sys/devices/platform/soc`, in a `*.ufshc` directory.
     */
    private fun scanUfsHealth(mode: AccessMode): ScanResult? {
        val path = firstLine(
            mode,
            "ls -1d /sys/devices/platform/soc/*ufshc*/health " +
                "/sys/devices/platform/soc/*ufs*/health " +
                "/sys/devices/platform/*ufshc*/health " +
                "/sys/class/scsi_host/host*/device/health " +
                "/sys/class/scsi_device/*/device/health " +
                "/sys/block/sd*/device/health 2>/dev/null | head -n 1",
            "find /sys/devices/ -type f -path '*ufs*' -name 'health' 2>/dev/null | head -n 1"
        )
        if (path.isEmpty()) return null
        val text = exec(mode, "cat $path", 20)
        val parsed = parseUfsHealth(text)
        if (parsed.values.isEmpty() || parsed.values.all { it == "0x00" }) return null
        return ScanResult(path, R.string.ufs, parsed.values, listOf(path), parsed.preEol)
    }

    private data class ParsedHealth(val values: List<String>, val preEol: String?)

    private fun parseUfsHealth(text: String): ParsedHealth {
        val found = linkedMapOf<String, String>()
        Regex(UFS_ESTIMATE, RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
            found[match.groupValues[1].uppercase(Locale.US)] = match.groupValues[2]
        }
        val a = found["A"]
        val b = found["B"]
        val preEol = parsePreEol(text)
        if (a != null) return ParsedHealth(listOfNotNull(a, b).map(::normalizeHex), preEol)
        if (b != null) return ParsedHealth(listOf(b).map(::normalizeHex), preEol)
        // Some vendor modules print only two raw codes.
        val codes = Regex("0[xX][0-9a-fA-F]+").findAll(text)
            .map { normalizeHex(it.value) }
            .toList()
        return ParsedHealth(if (codes.size == 2) codes else emptyList(), preEol)
    }

    private fun parsePreEol(text: String): String? {
        val patterns = listOf(
            "(?:Pre[ -]?EOL(?:Info)?|PRE_EOL_INFO|pre_eol_info)\\s*[:=]\\s*(0[xX][0-9a-fA-F]+|[0-9a-fA-F]+)",
            "\\bpre_eol_info\\b\\s+(0[xX][0-9a-fA-F]+|[0-9a-fA-F]+)"
        )
        for (pattern in patterns) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(text) ?: continue
            return match.groupValues[1]
        }
        return null
    }

    /** Older/vendor UFS and eMMC nodes: `life_time*` then `lifetime*`. */
    private fun scanVendorLifetime(mode: AccessMode): ScanResult? {
        for (name in listOf("life_time*", "lifetime*")) {
            val paths = firstLine(
                mode,
                "find /sys/devices/ -type f -name '$name' 2>/dev/null | " +
                    "sed -e ':a;N;\$!ba;s/\\n/ /g'"
            )
            if (paths.isEmpty()) continue
            val values = readCodes(mode, paths)
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
    private fun scanProcUfs(mode: AccessMode): ScanResult? {
        val text = exec(mode, "cat /proc/ufs_health 2>/dev/null", 10)
        if (text.isBlank()) return null
        val parsed = parseUfsHealth(text)
        if (parsed.values.isEmpty() || parsed.values.all { it == "0x00" }) return null
        return ScanResult(
            "/proc/ufs_health",
            R.string.ufs,
            parsed.values,
            listOf("/proc/ufs_health"),
            parsed.preEol
        )
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
        val preEolRaw = p.getString("disk_pre_eol", null)
        val preEol = if (preEolRaw.isNullOrBlank()) null else preEolNode(preEolRaw)

        val groupA: WearGroup
        val groupB: WearGroup

        if (single == null) {
            groupA = if (hex1a != null) {
                WearGroup(
                    title = context.getString(R.string.wear_level_a),
                    nodePath = nodeA,
                    visible = true,
                    one = node(hex1a),
                    two = if (hex2a != null) node(hex2a) else WearNode()
                )
            } else {
                WearGroup()
            }
            groupB = if (hex1b != null) {
                WearGroup(
                    title = context.getString(R.string.wear_level_b),
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
                title = context.getString(R.string.wear_level_a),
                nodePath = singleNode,
                visible = true,
                one = node(single),
                two = WearNode()
            )
            groupB = if (hex1b != null) {
                WearGroup(
                    title = context.getString(R.string.wear_level_b),
                    nodePath = nodeB,
                    visible = true,
                    one = node(hex1b),
                    two = if (hex2b != null) node(hex2b) else WearNode()
                )
            } else {
                WearGroup()
            }
        }

        val items = mutableListOf<WearItem>()
        if (single != null) {
            val singleNode = p.getString("disk_single_node", "").orEmpty()
            items += WearItem(
                title = context.getString(R.string.wear_level_a),
                nodePath = singleNode,
                node = node(single)
            )
        } else {
            if (hex1a != null) {
                items += WearItem(
                    title = context.getString(R.string.wear_level_a),
                    nodePath = nodeA,
                    node = node(hex1a)
                )
            }
            if (hex2a != null) {
                items += WearItem(
                    title = context.getString(R.string.wear_level_a),
                    nodePath = nodeA,
                    node = node(hex2a)
                )
            }
            if (hex1b != null) {
                items += WearItem(
                    title = context.getString(R.string.wear_level_b),
                    nodePath = nodeB,
                    node = node(hex1b)
                )
            }
            if (hex2b != null) {
                items += WearItem(
                    title = context.getString(R.string.wear_level_b),
                    nodePath = nodeB,
                    node = node(hex2b)
                )
            }
        }
        if (preEol != null) {
            items += WearItem(
                title = context.getString(R.string.wear_level_c),
                nodePath = null,
                node = preEol,
                preEol = true
            )
        }
        // Drop duplicate/hidden entries but keep the pre-EOL row.
        val visibleItems = items.filter { it.visible && it.node.visible }

        return WearStats(type, groupA, groupB, preEol, visibleItems)
    }

    /** `p045r1.g.c`: "Storage Health N %". */
    fun healthText(context: Context): String {
        val p = prefs(context)
        val hex = p.getString("disk_hex_1a", null) ?: p.getString("disk_single_hex", null)
        if (hex == null) {
            return context.getString(R.string.storage_health) + " " +
                context.getString(R.string.not_available)
        }
        return context.getString(R.string.storage_health) + " ${percentOf(hex)} %"
    }
}
