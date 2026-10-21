package id.openwear.diskhealth.ui

import android.app.Application
import android.content.Intent
import id.openwear.diskhealth.BuildConfig
import id.openwear.diskhealth.ui.activity.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application entry point. Installs the same crash reporter as v4.0.1
 * (`p040p1.c`): format the stack trace, open [CrashActivity] in a fresh task,
 * then terminate the process.
 */
class Applications : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        versionName = BuildConfig.VERSION_NAME

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (!handlingCrash.compareAndSet(false, true)) {
                android.os.Process.killProcess(android.os.Process.myPid())
                Runtime.getRuntime().exit(10)
                return@setDefaultUncaughtExceptionHandler
            }

            val trace = StringWriter().also { writer ->
                throwable.printStackTrace(PrintWriter(writer))
            }.toString()

            try {
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .putExtra("error_details", trace)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } catch (_: Throwable) {
                // If the crash screen cannot start, fall through to termination.
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            Runtime.getRuntime().exit(10)
        }
    }

    companion object {
        lateinit var instance: Applications
            private set

        var versionName: String = ""
            private set

        private val handlingCrash = AtomicBoolean(false)
    }
}
