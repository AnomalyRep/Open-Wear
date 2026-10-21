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
import com.google.android.material.textfield.TextInputEditText
import id.openwear.diskhealth.BuildConfig
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.AccessMode
import id.openwear.diskhealth.data.BatteryInfoReader
import id.openwear.diskhealth.data.Shell
import id.openwear.diskhealth.ui.DialogFactory
import id.openwear.diskhealth.ui.Ui
import java.net.HttpURLConnection
import java.net.URL

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
        Ui.applyAppearance(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .commit()
        }
    }

    override fun onDestroy() {
        Ui.stopAppearance(this)
        super.onDestroy()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private fun prefs() =
            requireContext().getSharedPreferences(
                requireContext().packageName + "_preferences",
                Context.MODE_PRIVATE
            )

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            listView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            val themeKey = getString(R.string.key_theme)
            val themePref = findPreference<Preference>(themeKey)
            themePref?.summary = currentTheme()
            themePref?.setOnPreferenceClickListener {
                showThemeDialog()
                true
            }

            findPreference<Preference>(getString(R.string.key_access_mode))?.apply {
                summary = getString(AccessMode.current(requireContext()).titleRes)
                setOnPreferenceClickListener {
                    showAccessModeDialog()
                    true
                }
            }
            findPreference<Preference>(getString(R.string.key_battery_capacity))?.apply {
                updateBatterySummary(this)
                setOnPreferenceClickListener {
                    showBatteryCapacityDialog()
                    true
                }
            }
            findPreference<Preference>(getString(R.string.key_appearance))?.apply {
                summary = appearanceLabel()
                setOnPreferenceClickListener {
                    showAppearanceDialog()
                    true
                }
            }
            findPreference<Preference>(getString(R.string.key_background_style))?.apply {
                summary = backgroundLabel()
                setOnPreferenceClickListener {
                    showBackgroundDialog()
                    true
                }
            }
            findPreference<Preference>(getString(R.string.key_check_updates))?.setOnPreferenceClickListener {
                checkForUpdates()
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

        private fun showAccessModeDialog() {
            val context = requireContext()
            val root = requireActivity().findViewById<View>(android.R.id.content)
            val current = AccessMode.current(context)
            val labels = listOf(
                getString(R.string.access_mode_root),
                getString(R.string.access_mode_non_root)
            )
            var dialogRef: android.app.Dialog? = null
            val dialog = Ui.listDialog(
                context,
                getString(R.string.access_mode),
                labels,
                root
            ) { position ->
                dialogRef?.dismiss()
                if (position == 0) {
                    Thread {
                        val rooted = Shell.isRooted(force = true)
                        Handler(Looper.getMainLooper()).post {
                            if (!isAdded) return@post
                            if (rooted) {
                                setAccessMode(AccessMode.ROOT)
                            } else {
                                setAccessMode(AccessMode.NON_ROOT)
                                Ui.dialog(
                                    context,
                                    getString(R.string.access_mode),
                                    getString(R.string.access_mode_root_denied),
                                    positive = getString(R.string.close),
                                    root = root
                                )
                            }
                        }
                    }.start()
                } else {
                    setAccessMode(AccessMode.NON_ROOT)
                }
            }
            dialogRef = dialog
            // Show the current selection as a small subtitle in the title.
            if (current == AccessMode.ROOT) {
                dialog.findViewById<TextView>(R.id.dialogTitle)?.text =
                    getString(R.string.access_mode) + "  (" + getString(R.string.access_mode_root) + ")"
            }
        }

        private fun setAccessMode(mode: AccessMode) {
            AccessMode.set(requireContext(), mode)
            findPreference<Preference>(getString(R.string.key_access_mode))?.summary =
                getString(mode.titleRes)
            Ui.toast(requireContext(), getString(R.string.access_mode_changed))
        }

        private fun updateBatterySummary(preference: Preference) {
            val custom = BatteryInfoReader.customCapacity(requireContext())
            preference.summary = if (custom > 0) {
                "$custom mAh"
            } else {
                getString(R.string.system_default)
            }
        }

        private fun showBatteryCapacityDialog() {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input, null)
            val input = view.findViewById<TextInputEditText>(R.id.inputCapacity)
            val layout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.inputLayout)
            view.findViewById<TextView>(R.id.dialogTitle).text = getString(R.string.edit_battery_capacity)
            layout.hint = getString(R.string.battery_capacity_hint)
            view.findViewById<TextView>(R.id.inputHint).text = getString(R.string.battery_health_calc_desc)
            val custom = BatteryInfoReader.customCapacity(requireContext())
            if (custom > 0) input.setText(custom.toString())

            val dialog = DialogFactory.create(requireActivity(), view)
            val negative = view.findViewById<MaterialButton>(R.id.negativeButton)
            val positive = view.findViewById<MaterialButton>(R.id.positiveButton)
            negative.setText(R.string.reset_battery_capacity)
            positive.setText(R.string.save)
            negative.setOnClickListener {
                BatteryInfoReader.setCustomCapacity(requireContext(), 0)
                findPreference<Preference>(getString(R.string.key_battery_capacity))?.let {
                    updateBatterySummary(it)
                }
                dialog.dismiss()
            }
            positive.setOnClickListener {
                val value = input.text?.toString()?.trim()?.toIntOrNull()
                if (value == null || value !in 100..30_000) {
                    layout.error = "100 - 30000 mAh"
                    return@setOnClickListener
                }
                BatteryInfoReader.setCustomCapacity(requireContext(), value)
                findPreference<Preference>(getString(R.string.key_battery_capacity))?.let {
                    updateBatterySummary(it)
                }
                Ui.toast(requireContext(), getString(R.string.battery_capacity_saved))
                dialog.dismiss()
            }
            dialog.show()
        }

        private fun appearanceLabel(): String = when (prefs().getString(getString(R.string.key_appearance), "md3")) {
            "liquid" -> getString(R.string.appearance_liquid_glass)
            else -> getString(R.string.appearance_md3_expressive)
        }

        private fun backgroundLabel(): String = when (prefs().getString(getString(R.string.key_background_style), "flat")) {
            "orbs" -> getString(R.string.background_orbs)
            else -> getString(R.string.background_flat)
        }

        private fun showAppearanceDialog() {
            showChoice(
                getString(R.string.title_appearance),
                listOf(
                    "md3" to getString(R.string.appearance_md3_expressive),
                    "liquid" to getString(R.string.appearance_liquid_glass)
                )
            ) { value ->
                prefs().edit().putString(getString(R.string.key_appearance), value).apply()
                findPreference<Preference>(getString(R.string.key_appearance))?.summary = appearanceLabel()
                Ui.applyAppearance(requireActivity())
            }
        }

        private fun showBackgroundDialog() {
            showChoice(
                getString(R.string.title_background_style),
                listOf(
                    "flat" to getString(R.string.background_flat),
                    "orbs" to getString(R.string.background_orbs)
                )
            ) { value ->
                prefs().edit().putString(getString(R.string.key_background_style), value).apply()
                findPreference<Preference>(getString(R.string.key_background_style))?.summary = backgroundLabel()
                Ui.applyAppearance(requireActivity())
            }
        }

        private fun showChoice(title: String, values: List<Pair<String, String>>, onSelect: (String) -> Unit) {
            val root = requireActivity().findViewById<View>(android.R.id.content)
            var dialog: android.app.Dialog? = null
            dialog = Ui.listDialog(
                requireContext(),
                title,
                values.map { it.second },
                root
            ) { position ->
                dialog?.dismiss()
                onSelect(values.getOrElse(position) { values.first() }.first)
            }
        }

        private fun checkForUpdates() {
            val context = requireContext()
            val root = requireActivity().findViewById<View>(android.R.id.content)
            Thread {
                val latest = fetchLatestTag()
                Handler(Looper.getMainLooper()).post {
                    if (!isAdded) return@post
                    when {
                        latest == null -> Ui.dialog(
                            context,
                            getString(R.string.check_updates),
                            getString(R.string.update_error),
                            positive = getString(R.string.close),
                            root = root
                        )
                        latest.trimStart('v', 'V') == BuildConfig.VERSION_NAME -> Ui.dialog(
                            context,
                            getString(R.string.check_updates),
                            getString(R.string.update_latest) + "\n\nv" + BuildConfig.VERSION_NAME,
                            positive = getString(R.string.close),
                            root = root
                        )
                        else -> Ui.dialog(
                            context,
                            getString(R.string.check_updates),
                            getString(R.string.update_available) + "\n\n" + latest,
                            positive = getString(R.string.github_source),
                            negative = getString(R.string.close),
                            root = root,
                            onPositive = { openUrl(GITHUB_RELEASES_URL) }
                        )
                    }
                }
            }.start()
        }

        private fun fetchLatestTag(): String? = runCatching {
            val connection = URL(GITHUB_API_LATEST).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "OpenWear")
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val text = connection.inputStream.bufferedReader().readText()
                Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

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
            const val GITHUB_RELEASES_URL = "https://github.com/AnomalyRep/Open-Wear/releases"
            const val GITHUB_API_LATEST =
                "https://api.github.com/repos/AnomalyRep/Open-Wear/releases/latest"

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
