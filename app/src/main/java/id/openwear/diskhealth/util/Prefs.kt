package id.openwear.diskhealth.util

import android.content.Context

/**
 * Same storage contract as the original `q3.a` helper: values live in the
 * default preference file under the keys below.
 */
object Prefs {
    const val KEY_HEX_1A = "key_hex1a"
    const val KEY_HEX_2A = "key_hex2a"
    const val KEY_HEX_1B = "key_hex1b"
    const val KEY_HEX_2B = "key_hex2b"
    const val KEY_DISCLAIMER_ACCEPTED = "key_disclaimer_accepted"

    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun get(context: Context, key: String): String = prefs(context).getString(key, "").orEmpty()

    fun put(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        prefs(context).getBoolean(key, default)

    fun putBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }
}
