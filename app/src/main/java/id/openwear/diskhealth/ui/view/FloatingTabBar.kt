package id.openwear.diskhealth.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import id.openwear.diskhealth.R

/**
 * Floating bottom pill tab bar matching reference v5.0.0 FloatingTabBarKt.
 * Compact wrap-content pill (~195dp), 52dp height, 44dp indicator height,
 * animated indicator offset and width matching Compose spring animation.
 */
class FloatingTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class TabItem(val title: String, val iconRes: Int)

    private val tabs = listOf(
        TabItem(context.getString(R.string.tab_wear_benchmark), R.drawable.ic_disk_health),
        TabItem(context.getString(R.string.tab_storage_backup), R.drawable.ic_storage)
    )

    private var selectedIndex = 0
    private var indicatorLeft = 0f
    private var indicatorRight = 0f
    private var targetIndicatorLeft = 0f
    private var targetIndicatorRight = 0f
    private var animator: ValueAnimator? = null

    var onTabSelectedListener: ((Int) -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textSize = dp(13.5f)
        try {
            val tf = context.resources.getFont(R.font.sanfranciscoprorounded)
            typeface = Typeface.create(tf, Typeface.BOLD)
        } catch (_: Throwable) {
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private val bgRect = RectF()
    private val indicatorRect = RectF()
    private val tabBounds = mutableListOf<RectF>()

    init {
        isClickable = true
    }

    fun setSelectedTab(index: Int, animate: Boolean = true) {
        if (index !in tabs.indices || (index == selectedIndex && animator == null)) return
        selectedIndex = index
        if (animate && width > 0) {
            computeTargetIndicator()
            startIndicatorAnimation()
        } else {
            computeTargetIndicator()
            indicatorLeft = targetIndicatorLeft
            indicatorRight = targetIndicatorRight
            invalidate()
        }
    }

    fun getSelectedTab(): Int = selectedIndex

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun isLiquid(): Boolean {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        return prefs.getString(context.getString(R.string.key_appearance), "md3") == "liquid"
    }

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun resolveThemeColor(attr: Int, defaultColor: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else {
            defaultColor
        }
    }

    private fun getSelectedTabWidth(tab: TabItem): Float {
        val iconSize = dp(20f)
        val spacing = dp(8f)
        val textWidth = textPaint.measureText(tab.title)
        val hPadding = dp(16f) * 2f
        return iconSize + spacing + textWidth + hPadding
    }

    private fun getUnselectedTabWidth(): Float {
        val iconSize = dp(20f)
        val hPadding = dp(14f) * 2f
        return iconSize + hPadding
    }

    private fun computeDesiredWidth(): Float {
        val innerPadding = dp(4f) * 2f
        var maxTotal = 0f
        for (sel in tabs.indices) {
            var total = innerPadding
            for (i in tabs.indices) {
                total += if (i == sel) getSelectedTabWidth(tabs[i]) else getUnselectedTabWidth()
            }
            if (total > maxTotal) maxTotal = total
        }
        return maxTotal
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = dp(52f).toInt()
        val desiredWidth = (computeDesiredWidth() + 0.5f).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val finalWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(widthSize, desiredWidth)
            else -> desiredWidth
        }
        setMeasuredDimension(finalWidth, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgRect.set(dp(1f), dp(1f), w - dp(1f), h - dp(1f))
        layoutTabs()
        computeTargetIndicator()
        indicatorLeft = targetIndicatorLeft
        indicatorRight = targetIndicatorRight
    }

    private fun layoutTabs() {
        tabBounds.clear()
        val count = tabs.size
        if (count == 0 || width <= 0) return

        val innerPadding = dp(4f)
        val usableWidth = width - (innerPadding * 2f)

        val selW = getSelectedTabWidth(tabs[selectedIndex])
        val unselW = getUnselectedTabWidth()
        val sumW = selW + unselW * (count - 1)
        val scale = if (sumW > 0f) usableWidth / sumW else 1f

        var currentLeft = innerPadding
        for (i in 0 until count) {
            val rawW = if (i == selectedIndex) selW else unselW
            val tabW = rawW * scale
            tabBounds.add(RectF(currentLeft, innerPadding, currentLeft + tabW, height - innerPadding))
            currentLeft += tabW
        }
    }

    private fun computeTargetIndicator() {
        layoutTabs()
        if (selectedIndex in tabBounds.indices) {
            val target = tabBounds[selectedIndex]
            targetIndicatorLeft = target.left
            targetIndicatorRight = target.right
        }
    }

    private fun startIndicatorAnimation() {
        animator?.cancel()
        val startLeft = indicatorLeft
        val startRight = indicatorRight
        val endLeft = targetIndicatorLeft
        val endRight = targetIndicatorRight

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val frac = va.animatedFraction
                indicatorLeft = startLeft + (endLeft - startLeft) * frac
                indicatorRight = startRight + (endRight - startRight) * frac
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dark = isDark()
        val liquid = isLiquid()

        val primaryColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimary, 0xFF0284C7.toInt())
        val surfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface, if (dark) 0xFF1E293B.toInt() else 0xFFFFFFFF.toInt())
        val surfaceVariant = resolveThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, if (dark) 0xFF334155.toInt() else 0xFFE2E8F0.toInt())
        val onSurfaceVariant = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, if (dark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())

        val pillRadius = height / 2f

        // Draw pill background
        if (liquid) {
            bgPaint.color = if (dark) 0xF216132A.toInt() else 0xF2F8FAFC.toInt()
            strokePaint.strokeWidth = dp(1.2f)
            strokePaint.color = if (dark) 0x9938BDF8.toInt() else 0x990284C7.toInt()
        } else {
            bgPaint.color = surfaceColor
            strokePaint.strokeWidth = dp(1f)
            strokePaint.color = surfaceVariant
        }
        canvas.drawRoundRect(bgRect, pillRadius, pillRadius, bgPaint)
        canvas.drawRoundRect(bgRect, pillRadius, pillRadius, strokePaint)

        // Specular highlight for liquid glass
        if (liquid) {
            val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height * 0.5f,
                    if (dark) 0x33FFFFFF else 0x66FFFFFF,
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            val sheenRect = RectF(bgRect.left + dp(1f), bgRect.top + dp(1f), bgRect.right - dp(1f), bgRect.top + dp(14f))
            canvas.drawRoundRect(sheenRect, pillRadius, pillRadius, sheenPaint)
        }

        // Draw indicator
        val indicatorPadding = dp(4f)
        val indicatorTop = indicatorPadding
        val indicatorBottom = height - indicatorPadding
        val indicatorRadius = (indicatorBottom - indicatorTop) / 2f
        indicatorRect.set(indicatorLeft, indicatorTop, indicatorRight, indicatorBottom)

        if (liquid) {
            indicatorPaint.color = if (dark) 0x4D0284C7.toInt() else 0x4DE0F2FE.toInt()
            indicatorStrokePaint.strokeWidth = dp(1f)
            indicatorStrokePaint.color = if (dark) 0xCC38BDF8.toInt() else 0xCC0284C7.toInt()
        } else {
            indicatorPaint.color = if (dark) 0x3338BDF8.toInt() else 0x260284C7.toInt()
            indicatorStrokePaint.strokeWidth = dp(1f)
            indicatorStrokePaint.color = primaryColor
        }
        canvas.drawRoundRect(indicatorRect, indicatorRadius, indicatorRadius, indicatorPaint)
        canvas.drawRoundRect(indicatorRect, indicatorRadius, indicatorRadius, indicatorStrokePaint)

        // Draw tabs
        val iconSize = dp(20f).toInt()
        for (i in tabs.indices) {
            val tab = tabs[i]
            val isSelected = (i == selectedIndex)
            val bounds = tabBounds.getOrNull(i) ?: continue

            val iconColor = if (isSelected) primaryColor else onSurfaceVariant
            val drawable = ContextCompat.getDrawable(context, tab.iconRes)?.mutate()?.let { d ->
                val wrapped = DrawableCompat.wrap(d)
                DrawableCompat.setTint(wrapped, iconColor)
                wrapped
            }

            if (isSelected) {
                // Selected: icon (20dp) + 6dp gap + text (13.5sp bold)
                val label = tab.title
                textPaint.color = primaryColor
                val textWidth = textPaint.measureText(label)
                val spacing = dp(8f)
                val totalContentWidth = iconSize + spacing + textWidth
                val startX = bounds.centerX() - (totalContentWidth / 2f)

                if (drawable != null) {
                    val iconTop = (bounds.centerY() - (iconSize / 2f)).toInt()
                    drawable.setBounds(startX.toInt(), iconTop, (startX + iconSize).toInt(), iconTop + iconSize)
                    drawable.draw(canvas)
                }

                val fontMetrics = textPaint.fontMetrics
                val textY = bounds.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(label, startX + iconSize + spacing, textY, textPaint)
            } else {
                // Unselected: icon (20dp) centered
                if (drawable != null) {
                    val iconLeft = (bounds.centerX() - (iconSize / 2f)).toInt()
                    val iconTop = (bounds.centerY() - (iconSize / 2f)).toInt()
                    drawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    drawable.draw(canvas)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            for (i in tabBounds.indices) {
                if (tabBounds[i].contains(x, event.y)) {
                    if (i != selectedIndex) {
                        setSelectedTab(i, animate = true)
                        onTabSelectedListener?.invoke(i)
                    }
                    return true
                }
            }
        }
        return true
    }
}
