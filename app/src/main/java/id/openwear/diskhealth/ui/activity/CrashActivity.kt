package id.openwear.diskhealth.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import id.openwear.diskhealth.R
import id.openwear.diskhealth.ui.Ui

/**
 * Crash screen from v4.0.1 `CrashActivity`.
 *
 * Receives the full stack trace through the `error_details` extra. Copy puts
 * it on the clipboard; Report opens the OpenWear GitHub issue tracker after
 * copying.
 */
class CrashActivity : AppCompatActivity() {

    override fun onDestroy() {
        Ui.stopAppearance(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(R.layout.activity_crash)
        Ui.edgeToEdge(this)
        Ui.applyAppearance(this)

        val details = intent.getStringExtra("error_details") ?: "Unknown Error"
        findViewById<TextView>(R.id.tv_error_log).text = details

        findViewById<MaterialButton>(R.id.btnCopy).setOnClickListener {
            copy(details)
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnReport).setOnClickListener {
            copy(details)
            Toast.makeText(this, "Log copied! Opening GitHub...", Toast.LENGTH_SHORT).show()
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/AnomalyRep/Open-Wear/issues")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open GitHub", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            finishAffinity()
            Runtime.getRuntime().exit(0)
        }
    }

    private fun copy(text: String) {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Crash Log", text))
    }
}
