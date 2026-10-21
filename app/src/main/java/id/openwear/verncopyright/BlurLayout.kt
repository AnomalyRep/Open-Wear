package id.openwear.verncopyright

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Lightweight stand-in for the original `id.openwear.verncopyright.BlurLayout`
 * (which wraps the third-party "Blurry" renderer). On API 31+ the overlay is
 * blurred with RenderEffect; on older devices a scrim keeps the same visual
 * hierarchy. Only ever used as a full-screen loading / splash overlay.
 */
class BlurLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
        setBackgroundColor(0x00000000)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                setRenderEffect(
                    RenderEffect.createBlurEffect(22f, 22f, Shader.TileMode.CLAMP)
                )
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(0x99000000.toInt())
        super.draw(canvas)
    }
}
