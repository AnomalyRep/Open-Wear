package id.openwear.diskhealth.data

import android.content.Context
import org.json.JSONArray

/** A single timestamped wear estimate, stored as `{"t":...,"v":...}`. */
data class HistoryPoint(val t: Long, val v: Int)

object HistoryStore {

    fun load(context: Context, key: String): MutableList<HistoryPoint> {
        val raw = prefs(context).getString(key, "[]") ?: "[]"
        val result = mutableListOf<HistoryPoint>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(HistoryPoint(obj.getLong("t"), obj.getInt("v")))
            }
        }
        return result
    }

    fun save(context: Context, key: String, points: List<HistoryPoint>) {
        val array = JSONArray()
        for (point in points) {
            array.put(org.json.JSONObject().apply {
                put("t", point.t)
                put("v", point.v)
            })
        }
        prefs(context).edit().putString(key, array.toString()).apply()
    }

    /**
     * Appends a point only when the percentage changed, matching the original.
     * Codes below 1% (0x0b / EOL) are intentionally not recorded.
     */
    fun record(context: Context, key: String, rawHex: String?) {
        if (rawHex.isNullOrBlank()) return
        val points = load(context, key)
        if (points.isEmpty()) return
        val value = WearLevelReader.percentOf(rawHex)
        if (value <= 0 || value == points.last().v) return
        points.add(HistoryPoint(System.currentTimeMillis(), value))
        save(context, key, points)
    }

    fun initializePurchase(context: Context, date: Long) {
        val earliest = if (date > System.currentTimeMillis()) System.currentTimeMillis() else date
        val point = HistoryPoint(earliest, 100)
        save(context, "wear_history_a", listOf(point))
        save(context, "wear_history_b", listOf(point))
        prefs(context).edit().putBoolean("is_purchase_date_set", true).apply()
    }

    fun isPurchaseSet(context: Context): Boolean =
        prefs(context).getBoolean("is_purchase_date_set", false)

    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}
