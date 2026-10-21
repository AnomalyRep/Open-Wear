package id.openwear.diskhealth.ui.activity

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.button.MaterialButton
import id.openwear.diskhealth.R
import id.openwear.diskhealth.ui.DialogFactory
import id.openwear.diskhealth.ui.Ui

/**
 * Settings screen, ported from v4.0.1 `SettingsActivity` + `o1.c`.
 *
 * The screen is a real `PreferenceFragmentCompat`; the XML is the original
 * v4 `res/xml/settings.xml` with the custom credits preference rebased onto
 * this package.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(R.layout.activity_settings)
        Ui.edgeToEdge(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private fun prefs() =
            requireContext().getSharedPreferences(
                requireContext().packageName + "_preferences",
                Context.MODE_PRIVATE
            )

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            val themeKey = getString(R.string.key_theme)
            val themePref = findPreference<Preference>(themeKey)
            themePref?.summary = currentTheme()
            themePref?.setOnPreferenceClickListener {
                showThemeDialog()
                true
            }

            findPreference<Preference>(getString(R.string.key_getprefs))?.setOnPreferenceClickListener {
                showSavedPreferences()
                true
            }
            findPreference<Preference>(getString(R.string.key_dependency))?.setOnPreferenceClickListener {
                showDependencies()
                true
            }
            findPreference<Preference>(getString(R.string.key_website))?.apply {
                summary = GITHUB_URL
                setOnPreferenceClickListener {
                    openUrl(GITHUB_URL)
                    true
                }
            }
            // Branding row only: explicitly non-clickable, no Telegram intent.
            findPreference<Preference>(getString(R.string.key_telegram))?.apply {
                isSelectable = false
                summary = getString(R.string.telegram_summary)
            }
        }

        private fun currentTheme(): String {
            val values = resources.getStringArray(R.array.theme_values)
            val saved = prefs().getString(getString(R.string.key_theme), values.firstOrNull())
            return saved ?: values.firstOrNull().orEmpty()
        }

        private fun showThemeDialog() {
            val values = resources.getStringArray(R.array.theme_values)
            val modes = resources.getStringArray(R.array.theme_modes)
            val saved = currentTheme()
            val checked = values.indexOfFirst { it == saved }.coerceAtLeast(0)

            val view = LayoutInflater.from(requireContext())
                .inflate(R.layout.list_dialog_layout, null)
            view.findViewById<TextView>(R.id.dialogTitle).setText(R.string.title_choose_theme)
            val list = view.findViewById<ListView>(R.id.listView)
            list.choiceMode = ListView.CHOICE_MODE_SINGLE
            list.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                modes
            )
            list.setItemChecked(checked, true)

            view.findViewById<MaterialButton>(R.id.positiveButton).visibility = View.GONE
            val dialog = DialogFactory.create(requireActivity(), view)
            view.findViewById<MaterialButton>(R.id.negativeButton).setOnClickListener {
                dialog.dismiss()
            }
            list.setOnItemClickListener { _, _, position, _ ->
                dialog.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    applyTheme(values.getOrElse(position) { values.first() }, position)
                }, 150)
            }
            dialog.show()
        }

        private fun applyTheme(value: String, index: Int) {
            prefs().edit().putString(getString(R.string.key_theme), value).apply()
            findPreference<Preference>(getString(R.string.key_theme))?.summary = value
            val mode = when (index) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        private fun showSavedPreferences() {
            val items = prefs().all.entries.map { (key, value) -> "$key: $value" }
                .ifEmpty { listOf(getString(R.string.saved_preferences)) }
            Ui.listDialog(
                requireContext(),
                getString(R.string.saved_preferences),
                items,
                requireActivity().findViewById(android.R.id.content)
            ) { position ->
                copy(R.string.preference, items[position])
            }
        }

        private fun showDependencies() {
            val dependencies = DEPENDENCIES
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { "\u2022 $it" }
                .toList()
            Ui.listDialog(
                requireContext(),
                getString(R.string.dependencies),
                dependencies,
                requireActivity().findViewById(android.R.id.content)
            ) { position ->
                copy(R.string.library_1, dependencies[position])
            }
        }

        private fun copy(labelRes: Int, text: String) {
            val manager = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText(getString(labelRes), text))
            Ui.toast(requireContext(), "Copied: $text")
        }

        private fun openUrl(url: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                Ui.toast(requireContext(), "Could not open link")
            }
        }

        private companion object {
            const val GITHUB_URL = "https://github.com/AnomalyRep/Open-Wear"

            /** Copied verbatim from the v4.0.1 dependency preference. */
            const val DEPENDENCIES =
                "androidx.appcompat:appcompat:1.7.1\n" +
                    "androidx.browser:browser:1.10.0-alpha02\n" +
                    "androidx.constraintlayout:constraintlayout:2.2.1\n" +
                    "androidx.lifecycle:lifecycle-runtime-ktx:2.10.0\n" +
                    "androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0\n" +
                    "androidx.preference:preference:1.2.1\n" +
                    "com.github.PhilJay:MPAndroidChart:v3.1.0\n" +
                    "com.github.topjohnwu.libsu:core:6.0.0\n" +
                    "com.github.topjohnwu.libsu:service:6.0.0\n" +
                    "com.google.android.material:material:1.14.0-alpha09\n" +
                    "io.coil-kt:coil:2.7.0\n" +
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2\n" +
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
        }
    }
}
