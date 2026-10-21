package id.openwear.diskhealth.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Draws a bitmap with the same center-crop behavior as the v5
 * `drawFlatWallpaper` helper.
 */
class CenterCropDrawable(private val bitmap: Bitmap) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dest = RectF()

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty || bitmap.width <= 0 || bitmap.height <= 0) return
        val scale = maxOf(
            bounds.width().toFloat() / bitmap.width,
            bounds.height().toFloat() / bitmap.height
        )
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val left = bounds.left + (bounds.width() - width) / 2f
        val top = bounds.top + (bounds.height() - height) / 2f
        dest.set(left, top, left + width, top + height)
        canvas.drawBitmap(bitmap, null, dest, paint)
    }

    override fun getIntrinsicWidth(): Int = bitmap.width

    override fun getIntrinsicHeight(): Int = bitmap.height

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        invalidateSelf()
    }
}
