package id.openwear.diskhealth.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import id.openwear.diskhealth.R

object Ui {

    /**
     * Edge-to-edge setup matching v4 `p042q0.a.e`: transparent status bar and
     * top padding on the content root equal to the status-bar inset.
     */
    fun edgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val root = activity.findViewById<View>(android.R.id.content)
        val night = (activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowInsetsControllerCompat(activity.window, root)
            .isAppearanceLightStatusBars = !night
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val modes = context.resources.getStringArray(R.array.theme_modes)
        val saved = prefs.getString(context.getString(R.string.key_theme), modes.firstOrNull())
        val index = modes.indexOfFirst { it == saved }
        val mode = when (index) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun toast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Custom card dialog matching `dialog_layout`. Positive/negative labels are
     * hidden when null. A blur render effect is applied to `root` on Android 12+.
     */
    fun dialog(
        context: Context,
        title: String?,
        message: String,
        positive: String? = null,
        negative: String? = null,
        root: View? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null,
        cancelable: Boolean = true
    ): Dialog {
        blur(root)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null)
        val titleView = view.findViewById<TextView>(R.id.dialogTitle)
        if (title == null) titleView.visibility = View.GONE else titleView.text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message
        val positiveButton = view.findViewById<MaterialButton>(R.id.positiveButton)
        val negativeButton = view.findViewById<MaterialButton>(R.id.negativeButton)

        if (positive == null) {
            positiveButton.visibility = View.GONE
        } else {
            positiveButton.text = positive
        }
        if (negative == null) {
            negativeButton.visibility = View.GONE
        } else {
            negativeButton.text = negative
        }

        val dialog = create(context, view)
        dialog.setCancelable(cancelable)
        dialog.setOnDismissListener { clearBlur(root) }
        positiveButton.setOnClickListener {
            dialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({ onPositive?.invoke() }, 150)
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({ onNegative?.invoke() }, 150)
        }
        dialog.show()
        return dialog
    }

    fun listDialog(
        context: Context,
        title: String,
        items: List<String>,
        root: View? = null,
        onClick: ((Int) -> Unit)? = null
    ): Dialog {
        blur(root)
        val view = LayoutInflater.from(context).inflate(R.layout.list_dialog_layout, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        val listView = view.findViewById<ListView>(R.id.listView)
        val adapter = ArrayAdapter(context, R.layout.item_list, R.id.tv_file_name, items)
        listView.adapter = adapter
        if (onClick != null) {
            listView.setOnItemClickListener { _, _, position, _ -> onClick(position) }
        }

        // Match v4 `n.i`: cap the list at 400dp and measure each row.
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.widthPixels,
            View.MeasureSpec.UNSPECIFIED
        )
        var height = 0
        for (index in 0 until adapter.count) {
            val row = adapter.getView(index, null, listView)
            row.measure(widthSpec, 0)
            height += row.measuredHeight
        }
        height += (adapter.count - 1) * listView.dividerHeight
        val maxHeight = (400 * context.resources.displayMetrics.density).toInt()
        listView.layoutParams = listView.layoutParams.also {
            it.height = if (height > maxHeight) maxHeight else height
        }

        val dialog = create(context, view)
        dialog.setOnDismissListener { clearBlur(root) }
        val negative = view.findViewById<MaterialButton>(R.id.negativeButton)
        negative.setText(R.string.close)
        negative.setOnClickListener { dialog.dismiss() }
        view.findViewById<MaterialButton>(R.id.positiveButton).visibility = View.GONE
        dialog.show()
        return dialog
    }

    private fun create(context: Context, view: View): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.3f)
        val metrics: DisplayMetrics = context.resources.displayMetrics
        window?.setLayout((metrics.widthPixels * 0.9).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    fun blur(root: View?) {
        if (root == null || Build.VERSION.SDK_INT < 31) return
        try {
            root.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR))
        } catch (_: Exception) {
        }
    }

    /** Removes the blur applied by [blur], matching `p040p1.b` cleanup. */
    fun clearBlur(root: View?) {
        if (root == null || Build.VERSION.SDK_INT < 31) return
        try {
            root.setRenderEffect(null)
        } catch (_: Exception) {
        }
    }
}
