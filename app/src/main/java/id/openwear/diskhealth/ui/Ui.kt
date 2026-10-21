package id.openwear.diskhealth.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import id.openwear.diskhealth.R
import java.util.WeakHashMap
import kotlin.math.min

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
        val controller = androidx.core.view.WindowInsetsControllerCompat(activity.window, root)
        controller.isAppearanceLightStatusBars = !night
        controller.isAppearanceLightNavigationBars = !night
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isStatusBarContrastEnforced = false
            activity.window.isNavigationBarContrastEnforced = false
        }
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

    private class CardStyle(
        val background: ColorStateList?,
        val strokeWidth: Int,
        val strokeColor: Int,
        val elevation: Float,
        val cardElevation: Float,
        val hadForeground: Boolean
    )

    private val cardStyles = WeakHashMap<MaterialCardView, CardStyle>()
    private val appBarTints = WeakHashMap<View, ColorStateList?>()
    private val wallpaperCache = HashMap<Int, Bitmap>()

    /** Applies the v5 appearance/background choice to an activity root. */
    fun applyAppearance(activity: Activity) {
        val prefs = activity.getSharedPreferences(
            activity.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
        val appearance = prefs.getString(activity.getString(R.string.key_appearance), "md3")
        val background = prefs.getString(activity.getString(R.string.key_background_style), "flat")
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        val isDark = isDark(activity)
        val liquid = appearance == "liquid"
        val animated = background == "orbs"

        (root.background as? android.graphics.drawable.Animatable)?.stop()
        when {
            animated -> {
                root.background = AnimatedOrbsDrawable(isDark, true).also { it.start() }
            }
            liquid -> {
                root.background = referenceWallpaper(activity, isDark)
                    ?: AnimatedOrbsDrawable(isDark, false)
            }
            else -> {
                root.setBackgroundColor(surfaceVariantColor(activity))
            }
        }

        applyAppBar(activity, liquid || animated)
        forEachCard(root) { styleActivityCard(it, liquid, isDark) }
    }

    /** Re-applies the active appearance to a newly attached view subtree. */
    fun styleView(view: View) {
        val context = view.context
        val liquid = isLiquid(context)
        val dark = isDark(context)
        forEachCard(view) { styleActivityCard(it, liquid, dark) }
    }

    /** Stops an animated background before the activity is destroyed. */
    fun stopAppearance(activity: Activity) {
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        (root.background as? android.graphics.drawable.Animatable)?.stop()
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
        val density = context.resources.displayMetrics.density
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
        val minHeight = (180 * density).toInt()
        val maxHeight = (400 * density).toInt()
        listView.layoutParams = listView.layoutParams.also {
            it.height = height.coerceIn(minHeight, maxHeight)
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
        styleDialog(view)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.3f)
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val width = min((metrics.widthPixels * 0.9f).toInt(), (420 * metrics.density).toInt())
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    /** Applies the v5 Liquid Glass dialog surface when that appearance is active. */
    fun styleDialog(view: View) {
        val context = view.context
        if (!isLiquid(context)) return
        val dark = isDark(context)
        forEachCard(view) { card -> styleDialogCard(card, dark) }
    }

    private fun styleActivityCard(card: MaterialCardView, liquid: Boolean, dark: Boolean) {
        val original = if (cardStyles.containsKey(card)) {
            cardStyles[card]!!
        } else {
            CardStyle(
                background = card.cardBackgroundColor,
                strokeWidth = card.strokeWidth,
                strokeColor = card.strokeColor,
                elevation = card.elevation,
                cardElevation = card.cardElevation,
                hadForeground = card.foreground != null
            ).also { cardStyles[card] = it }
        }

        if (!liquid) {
            card.setCardBackgroundColor(original.background)
            card.strokeWidth = original.strokeWidth
            card.setStrokeColor(original.strokeColor)
            card.elevation = original.elevation
            card.cardElevation = original.cardElevation
            card.foreground = if (original.hadForeground) {
                selectableItemBackground(card.context)
            } else {
                null
            }
            return
        }

        // Keep the transparent credits card untouched.
        val background = card.cardBackgroundColor
        if (Color.alpha(background.defaultColor) == 0) return

        val preferenceRow = card.tag == "preference_row"
        paintGlassCard(
            card = card,
            dark = dark,
            dialog = false,
            sheen = !preferenceRow
        )
    }

    private fun styleDialogCard(card: MaterialCardView, dark: Boolean) {
        card.radius = dp(card.context, 32).toFloat()
        paintGlassCard(card = card, dark = dark, dialog = true, sheen = true)
    }

    private fun paintGlassCard(
        card: MaterialCardView,
        dark: Boolean,
        dialog: Boolean,
        sheen: Boolean
    ) {
        val fill = when {
            dialog && dark -> 0xB31E293B.toInt()
            dialog -> 0xF2F8FAFC.toInt()
            dark -> 0x1AFFFFFF
            else -> 0x47FFFFFF
        }
        val stroke = when {
            dialog && dark -> 0x80FFFFFF.toInt()
            dialog -> 0xCCFFFFFF.toInt()
            dark -> 0x66FFFFFF
            else -> 0x99FFFFFF.toInt()
        }
        val ripple = if (dark) 0x33FFFFFF else 0x1F000000

        card.setCardBackgroundColor(ColorStateList.valueOf(fill))
        card.strokeWidth = dp(card.context, 1)
        card.setStrokeColor(ColorStateList.valueOf(stroke))
        card.cardElevation = 0f
        card.elevation = 0f
        if (sheen) {
            card.foreground = RippleDrawable(
                ColorStateList.valueOf(ripple),
                GlassSheenDrawable(card.radius, dark),
                null
            )
        }
    }

    private fun isLiquid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
        return prefs.getString(context.getString(R.string.key_appearance), "md3") == "liquid"
    }

    private fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun selectableItemBackground(context: Context): Drawable? {
        val value = android.util.TypedValue()
        return if (context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                value,
                true
            ) && value.resourceId != 0
        ) {
            ContextCompat.getDrawable(context, value.resourceId)
        } else {
            null
        }
    }

    private fun referenceWallpaper(context: Context, dark: Boolean): Drawable? {
        val name = if (dark) "bg_flat_dark" else "bg_flat_light"
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        val bitmap = wallpaperCache[id] ?: try {
            BitmapFactory.decodeResource(
                context.resources,
                id,
                BitmapFactory.Options().apply { inScaled = false }
            )?.also { wallpaperCache[id] = it }
        } catch (_: Throwable) {
            null
        } ?: return null
        return CenterCropDrawable(bitmap)
    }

    private fun surfaceVariantColor(context: Context): Int {
        val value = android.util.TypedValue()
        return if (context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceVariant,
                value,
                true
            )
        ) {
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId)
            else value.data
        } else {
            Color.TRANSPARENT
        }
    }

    private fun applyAppBar(activity: Activity, clear: Boolean) {
        val appBar = activity.findViewById<View>(R.id.appbar) ?: return
        if (!appBarTints.containsKey(appBar)) {
            appBarTints[appBar] = appBar.backgroundTintList
        }
        appBar.backgroundTintList = if (clear) {
            ColorStateList.valueOf(Color.TRANSPARENT)
        } else {
            appBarTints[appBar]
        }
    }

    private fun forEachCard(view: View, block: (MaterialCardView) -> Unit) {
        if (view is MaterialCardView) block(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                forEachCard(view.getChildAt(index), block)
            }
        }
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
