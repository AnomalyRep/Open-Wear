package id.openwear.diskhealth.data

import android.content.Context
import androidx.annotation.StringRes
import id.openwear.diskhealth.R

/**
 * v5 access mode contract.
 *
 * Root mode keeps the v4 behaviour (su / libsu style privileged reads and raw
 * partition backup). Non-root mode reads only world-readable sysfs nodes and
 * disables block-level operations.
 */
enum class AccessMode(
    val value: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
) {
    ROOT(
        "root",
        R.string.access_mode_root,
        R.string.access_mode_root_desc
    ),
    NON_ROOT(
        "non_root",
        R.string.access_mode_non_root,
        R.string.access_mode_non_root_desc
    );

    companion object {
        const val KEY = "key_access_mode"

        fun fromValue(value: String?): AccessMode =
            if (value?.trim()?.lowercase() == NON_ROOT.value) NON_ROOT else ROOT

        fun current(context: Context): AccessMode {
            val prefs = context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
            )
            val saved = prefs.getString(KEY, null)
            if (saved != null) return fromValue(saved)
            // Existing v4 installs keep root behaviour; a fresh install is
            // defaulted to root on rooted devices and non-root otherwise.
            return if (Shell.isRooted()) ROOT else NON_ROOT
        }

        fun isRoot(context: Context): Boolean = current(context) == ROOT

        fun set(context: Context, mode: AccessMode) {
            context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
            ).edit().putString(KEY, mode.value).apply()
        }
    }
}
