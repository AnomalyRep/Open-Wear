package id.openwear.diskhealth.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import id.openwear.diskhealth.BuildConfig
import id.openwear.diskhealth.R

/**
 * Custom credit card preference from v4.0.1
 * `id.vern.wearlevelinsight.fragments.CreditsPreference`.
 *
 * The preference inflates `R.layout.preference_credits`; tapping it opens the
 * contributors list with the same four names as the original.
 */
class CreditsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {

    init {
        layoutResource = R.layout.preference_credits
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val view = holder.itemView
        view.isClickable = true
        view.isFocusable = true

        view.findViewById<TextView>(R.id.tv_header_openwear)?.text = "Open"
        view.findViewById<TextView>(R.id.tv_header_dev)?.text = "Wear"
        view.findViewById<TextView>(R.id.tv_header_projects)?.text = "Projects"
        view.findViewById<TextView>(R.id.tv_app_name)?.text = context.getString(R.string.app_name)
        view.findViewById<TextView>(R.id.tv_app_tagline)?.text = context.getString(R.string.tagline)
        view.findViewById<TextView>(R.id.appversion)?.text =
            context.getString(R.string.version_format, context.packageName, BuildConfig.VERSION_NAME)

        view.setOnClickListener {
            Ui.listDialog(
                context,
                context.getString(R.string.contributors),
                listOf(
                    "OpenWear (Developer)",
                    "@osyawicane for Bahasa Indonesia Translation",
                    "@danyyok for Russian Translation",
                    "@illya114 for Ukrainian Translation"
                ),
                view.rootView
            )
        }
    }
}
