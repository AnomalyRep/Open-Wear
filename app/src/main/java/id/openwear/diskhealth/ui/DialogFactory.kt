package id.openwear.diskhealth.ui

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.Window

/**
 * Shared dialog factory for custom layout dialogs.
 * Applies size constraints, transparent window, and Liquid Glass styling.
 */
object DialogFactory {
    fun create(activity: Activity, view: View): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        Ui.styleDialog(view)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.3f)
        val metrics = activity.resources.displayMetrics
        val width = kotlin.math.min((metrics.widthPixels * 0.9f).toInt(), (420 * metrics.density).toInt())
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
