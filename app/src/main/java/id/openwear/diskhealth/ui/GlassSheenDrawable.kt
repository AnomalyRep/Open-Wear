package id.openwear.diskhealth.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Specular sheen and glowing rim border gradient for v5 Liquid Glass cards.
 * Produces the exact glowing glass reflections, ambient corner highlights,
 * and multi-stop cyber-glass rim border matching reference Image 1.
 */
class GlassSheenDrawable(
    private val cornerRadius: Float,
    private val dark: Boolean
) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val path = Path()
    private val strokePath = Path()

    private var runtimeShader: Any? = null
    private var runtimeShaderFailed = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                runtimeShader = android.graphics.RuntimeShader(AMBIENT_HIGHLIGHT_SHADER)
            } catch (_: Throwable) {
                runtimeShaderFailed = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val rect = RectF(bounds)
        path.reset()
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)

        // 1. Ambient / Specular highlight across the card top
        var usedAgsl = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null && !runtimeShaderFailed) {
            try {
                val shader = runtimeShader as android.graphics.RuntimeShader
                shader.setFloatUniform("size", rect.width(), rect.height())
                shader.setFloatUniform("cornerRadii", cornerRadius, cornerRadius, cornerRadius, cornerRadius)
                shader.setFloatUniform("angle", -0.785398f) // -45 deg (top-left light source)
                shader.setFloatUniform("falloff", 2.2f)
                fillPaint.shader = shader
                fillPaint.alpha = if (dark) (0.35f * 255).toInt() else (0.50f * 255).toInt()
                canvas.drawPath(path, fillPaint)
                fillPaint.shader = null
                usedAgsl = true
            } catch (_: Throwable) {
                runtimeShaderFailed = true
                usedAgsl = false
            }
        }

        if (!usedAgsl) {
            // High-fidelity fallback: diagonal specular reflection gradient
            val topAlpha = if (dark) 0x33 else 0x44
            val bottomAlpha = if (dark) 0x0A else 0x14
            fillPaint.shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    (topAlpha shl 24) or 0x00FFFFFF,
                    0x00FFFFFF,
                    (bottomAlpha shl 24) or 0x00FFFFFF
                ),
                floatArrayOf(0f, 0.40f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, fillPaint)
            fillPaint.shader = null
        }

        // 2. Specular glowing rim border gradient (1.5dp stroke)
        val strokeWidthPx = 4.0f
        strokePaint.strokeWidth = strokeWidthPx
        val halfStroke = strokeWidthPx / 2f
        val strokeRect = RectF(
            rect.left + halfStroke,
            rect.top + halfStroke,
            rect.right - halfStroke,
            rect.bottom - halfStroke
        )
        val strokeRadius = (cornerRadius - halfStroke).coerceAtLeast(0f)
        strokePath.reset()
        strokePath.addRoundRect(strokeRect, strokeRadius, strokeRadius, Path.Direction.CW)

        if (dark) {
            // Glowing neon glass rim: Bright White (top-left) -> Violet -> Cyan -> Deep Blue
            strokePaint.shader = LinearGradient(
                strokeRect.left,
                strokeRect.top,
                strokeRect.right,
                strokeRect.bottom,
                intArrayOf(
                    0xF2FFFFFF.toInt(), // 95% Specular White shine
                    0xCC818CF8.toInt(), // 80% Violet glow
                    0xCC38BDF8.toInt(), // 80% Cyan glow
                    0x990284C7.toInt()  // 60% Blue rim
                ),
                floatArrayOf(0f, 0.30f, 0.70f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            // Light glass rim: Pure White (top) -> Light Sky -> White
            strokePaint.shader = LinearGradient(
                strokeRect.left,
                strokeRect.top,
                strokeRect.right,
                strokeRect.bottom,
                intArrayOf(
                    0xFFFFFFFF.toInt(),
                    0xB3BAE6FD.toInt(),
                    0xE6FFFFFF.toInt()
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(strokePath, strokePaint)
        strokePaint.shader = null
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds, cornerRadius)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        invalidateSelf()
    }

    private companion object {
        const val AMBIENT_HIGHLIGHT_SHADER = """
uniform float2 size;
uniform float4 cornerRadii;
uniform float angle;
uniform float falloff;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    float t = step(0.0, d);
    return half4(t, t, t, 1.0) * intensity;
}
"""
    }
}
