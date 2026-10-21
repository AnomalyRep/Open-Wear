package id.openwear.diskhealth.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.AccessMode
import id.openwear.diskhealth.data.HistoryStore
import id.openwear.diskhealth.data.Shell
import id.openwear.diskhealth.ui.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar

/**
 * Onboarding flow:
 * welcome -> access mode -> root (optional) -> permissions -> purchase date
 * -> dashboard. When the purchase date is already known it jumps to the
 * credits page, resolves a missing access mode, and shows the dashboard.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(R.layout.splash_screen)
        Ui.edgeToEdge(this)
        Ui.applyAppearance(this)
        flipper = findViewById(R.id.viewFlipper)
        rootView = findViewById(android.R.id.content)

        if (!prefs().getBoolean("key_agreements", false)) {
            showAgreement()
        } else {
            afterAgreement()
        }
    }

    private fun prefs() =
        getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

    private fun afterAgreement() {
        if (HistoryStore.isPurchaseSet(this)) {
            flipper.displayedChild = 4
            lifecycleScope.launch {
                delay(2000)
                // Existing v4 users may already have a purchase date but no
                // access-mode preference yet. Resolve it off the main thread.
                if (!prefs().contains(AccessMode.KEY)) {
                    val mode = withContext(Dispatchers.IO) {
                        if (Shell.isRooted()) AccessMode.ROOT else AccessMode.NON_ROOT
                    }
                    AccessMode.set(this@SplashActivity, mode)
                }
                openMain()
            }
        } else {
            flipper.displayedChild = 0
            bindOnboarding()
        }
    }

    private fun bindOnboarding() {
        findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            if (prefs().contains(AccessMode.KEY)) {
                proceedWithAccessMode(AccessMode.current(this))
            } else {
                showAccessModeDialog()
            }
        }

        findViewById<MaterialButton>(R.id.btn_grant_root).setOnClickListener {
            lifecycleScope.launch {
                val root = withContext(Dispatchers.IO) { Shell.isRooted(force = true) }
                if (root) {
                    if (flipper.displayedChild == 1) flipper.showNext()
                } else {
                    Ui.dialog(
                        this@SplashActivity,
                        getString(R.string.request_root_access),
                        getString(R.string.access_mode_root_denied),
                        positive = getString(R.string.retry),
                        negative = getString(R.string.access_mode_non_root),
                        root = rootView,
                        onPositive = { flipper.displayedChild = 1 },
                        onNegative = {
                            AccessMode.set(this@SplashActivity, AccessMode.NON_ROOT)
                            flipper.displayedChild = 2
                        },
                        cancelable = false
                    )
                }
            }
        }

        findViewById<MaterialButton>(R.id.btn_grant_permission).setOnClickListener {
            if (hasAllPermissions()) {
                if (flipper.displayedChild == 2) flipper.showNext()
            } else {
                requestPermissionsAndSettings()
            }
        }

        findViewById<MaterialButton>(R.id.btn_select_date).setOnClickListener { showDatePicker() }
    }

    private fun showAccessModeDialog() {
        val items = listOf(
            getString(R.string.access_mode_root),
            getString(R.string.access_mode_non_root)
        )
        var dialog: Dialog? = null
        dialog = Ui.listDialog(
            this,
            getString(R.string.access_mode),
            items,
            rootView
        ) { position ->
            dialog?.dismiss()
            val mode = if (position == 0) AccessMode.ROOT else AccessMode.NON_ROOT
            AccessMode.set(this, mode)
            proceedWithAccessMode(mode)
        }
    }

    private fun proceedWithAccessMode(mode: AccessMode) {
        flipper.displayedChild = if (mode == AccessMode.ROOT) 1 else 2
    }

    private fun hasAllPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) return false
        if (!packageManager.canRequestPackageInstalls()) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return false
        if (checkSelfPermission("android.permission.RECEIVE_BOOT_COMPLETED") != android.content.pm.PackageManager.PERMISSION_GRANTED) return false
        return true
    }

    private fun requestPermissionsAndSettings() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            runCatching {
                startActivity(
                    Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            return
        }
        if (!packageManager.canRequestPackageInstalls()) {
            runCatching {
                startActivity(
                    Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1001)
            return
        }
        if (checkSelfPermission("android.permission.RECEIVE_BOOT_COMPLETED") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.RECEIVE_BOOT_COMPLETED"), 1001)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::flipper.isInitialized && flipper.displayedChild == 2 && hasAllPermissions()) {
            flipper.showNext()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                HistoryStore.initializePurchase(this, selected.timeInMillis)
                openMain()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setTitle(getString(R.string.select_phone_purchase_date))
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        Ui.stopAppearance(this)
        super.onDestroy()
    }

    private fun showAgreement() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agreement, null)
        val scroll = view.findViewById<ScrollView>(R.id.scrollView)
        val message = view.findViewById<TextView>(R.id.dialogMessage)
        val hint = view.findViewById<TextView>(R.id.tvScrollHint)
        val positive = view.findViewById<MaterialButton>(R.id.positiveButton)
        val negative = view.findViewById<MaterialButton>(R.id.negativeButton)

        message.text = loadDisclaimer()
        var scrolled = false
        var timerFinished = false

        fun update() {
            if (scrolled && timerFinished) {
                positive.isEnabled = true
                positive.setText(R.string.agree)
                hint.visibility = View.GONE
            }
        }

        val timer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                positive.isEnabled = false
                positive.text = "${getString(R.string.please_read)} (${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                timerFinished = true
                if (!scrolled) {
                    positive.text = getString(R.string.read_it_untill_latest_paragrap)
                }
                update()
            }
        }
        timer.start()
        positive.isEnabled = false
        positive.text = "${getString(R.string.please_read)} (30s)"

        fun markScrolled() {
            if (scrolled) return
            scrolled = true
            update()
        }

        scroll.viewTreeObserver.addOnScrollChangedListener(object : ViewTreeObserver.OnScrollChangedListener {
            override fun onScrollChanged() {
                val content = scroll.getChildAt(0) ?: return
                if (content.bottom - (scroll.scrollY + scroll.height) <= 10) markScrolled()
            }
        })
        scroll.post {
            val content = scroll.getChildAt(0) ?: return@post
            if (content.bottom - (scroll.scrollY + scroll.height) <= 0) markScrolled()
        }

        val dialog = DialogFactory.create(this, view)
        dialog.setCancelable(false)
        dialog.setOnDismissListener { timer.cancel() }

        positive.setOnClickListener {
            timer.cancel()
            prefs().edit().putBoolean("key_agreements", true).apply()
            dialog.dismiss()
            afterAgreement()
        }
        negative.setOnClickListener {
            finishAffinity()
        }
        dialog.show()
    }

    private fun loadDisclaimer(): CharSequence {
        return try {
            val text = assets.open("disclaimer.html").use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } catch (t: Exception) {
            "Error loading disclaimer: ${t.message}"
        }
    }
}

/** Shared dialog shell; the agreement layout supplies its own card. */
object DialogFactory {
    fun create(activity: android.app.Activity, view: View): android.app.Dialog {
        val dialog = android.app.Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        Ui.styleDialog(view)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.3f)
        val metrics = activity.resources.displayMetrics
        val width = kotlin.math.min(
            (metrics.widthPixels * 0.9f).toInt(),
            (420 * metrics.density).toInt()
        )
        window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
