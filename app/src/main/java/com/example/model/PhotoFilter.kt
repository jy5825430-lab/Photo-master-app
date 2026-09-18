package com.example.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min

enum class PhotoFilter(
    val id: String,
    val displayName: String,
    val description: String
) {
    ORIGINAL("original", "Original", "Natural untouched photo"),
    BLACK_AND_WHITE("bw", "B & W", "High-contrast monochrome"),
    SEPIA("sepia", "Sepia", "Warm vintage tone"),
    VINTAGE("vintage", "Vintage", "Warm retro film wash"),
    BLUR("blur", "Blur", "Soft Gaussian-style defocus"),
    SHARPEN("sharpen", "Sharpen", "Crisp edge enhancement"),
    INVERT("invert", "Invert", "Negative inversion art"),
    BRIGHTNESS("brightness", "Brightness", "Vibrant luminous glow"),
    CONTRAST("contrast", "Contrast", "Dramatic dynamic range")
}

data class Adjustments(
    val brightness: Float = 0f,   // -100 to +100
    val contrast: Float = 1f,     // 0.2 to 2.5 (1.0 = normal)
    val saturation: Float = 1f    // 0.0 to 2.0 (1.0 = normal)
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f
}

data class TextOverlay(
    val id: Long = System.currentTimeMillis(),
    val text: String = "Hello",
    val color: Int = android.graphics.Color.WHITE,
    val fontSizeSp: Float = 28f,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val hasBackground: Boolean = true,
    val backgroundColor: Int = android.graphics.Color.argb(160, 0, 0, 0),
    // Normalized coordinates (0.0 to 1.0) relative to photo width & height
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f
)

data class StickerOverlay(
    val id: Long = System.currentTimeMillis(),
    val emoji: String = "✨",
    val sizeSp: Float = 44f,
    // Normalized coordinates (0.0 to 1.0)
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f
)

enum class BlurBackgroundShape {
    RADIAL_VIGNETTE,
    LINEAR_FOCUS,
    FULL_BLUR
}

data class BlurBgSettings(
    val enabled: Boolean = false,
    val blurRadius: Int = 18,
    val focusRadiusRatio: Float = 0.45f, // Area kept clear in center (0.1 to 0.9)
    val shape: BlurBackgroundShape = BlurBackgroundShape.RADIAL_VIGNETTE
)

enum class CropRatio(val displayName: String, val ratioX: Float, val ratioY: Float) {
    FREE("Free", 0f, 0f),
    SQUARE("1:1", 1f, 1f),
    PORTRAIT_4_5("4:5", 4f, 5f),
    STORY_9_16("9:16", 9f, 16f),
    LANDSCAPE_16_9("16:9", 16f, 9f),
    PHOTO_3_4("3:4", 3f, 4f)
}

object FilterProcessor {

    /**
     * Applies filter, adjustments, text, stickers, and background blur to produce the final bitmap.
     */
    fun processFullImage(
        baseBitmap: Bitmap,
        filter: PhotoFilter,
        adjustments: Adjustments,
        blurBg: BlurBgSettings,
        textOverlays: List<TextOverlay>,
        stickerOverlays: List<StickerOverlay>
    ): Bitmap {
        // Step 1: Base Filter
        var current = applyFilter(baseBitmap, filter)

        // Step 2: Adjustments (Brightness, Contrast, Saturation)
        if (!adjustments.isDefault) {
            current = applyAdjustments(current, adjustments)
        }

        // Step 3: Blur background effect if enabled
        if (blurBg.enabled) {
            current = applyBlurBackground(current, blurBg)
        }

        // Step 4: Text & Sticker overlays rendered directly onto the bitmap canvas
        if (textOverlays.isNotEmpty() || stickerOverlays.isNotEmpty()) {
            current = renderOverlays(current, textOverlays, stickerOverlays)
        }

        return current
    }

    /**
     * Applies the named filter to the source bitmap.
     */
    fun applyFilter(source: Bitmap, filter: PhotoFilter): Bitmap {
        return when (filter) {
            PhotoFilter.ORIGINAL -> source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
            PhotoFilter.BLACK_AND_WHITE -> applyColorMatrix(source, getBlackAndWhiteMatrix())
            PhotoFilter.SEPIA -> applyColorMatrix(source, getSepiaMatrix())
            PhotoFilter.VINTAGE -> applyColorMatrix(source, getVintageMatrix())
            PhotoFilter.INVERT -> applyColorMatrix(source, getInvertMatrix())
            PhotoFilter.BRIGHTNESS -> applyColorMatrix(source, getBrightnessMatrix(45f))
            PhotoFilter.CONTRAST -> applyColorMatrix(source, getContrastMatrix(1.4f))
            PhotoFilter.BLUR -> fastBoxBlur(source, 14)
            PhotoFilter.SHARPEN -> applySharpenConvolution(source)
        }
    }

    /**
     * Applies Adjustments (Brightness, Contrast, Saturation) via a combined ColorMatrix.
     */
    fun applyAdjustments(source: Bitmap, adjustments: Adjustments): Bitmap {
        val cm = ColorMatrix()

        // Saturation
        cm.setSaturation(adjustments.saturation)

        // Contrast & Brightness
        // Formula: scale = contrast; offset = (1 - contrast) * 128 / 255 * 255 + brightness
        val scale = adjustments.contrast
        val translate = (1f - scale) * 128f + adjustments.brightness

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        return applyColorMatrix(source, cm)
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun getBlackAndWhiteMatrix(): ColorMatrix {
        return ColorMatrix().apply { setSaturation(0f) }
    }

    private fun getSepiaMatrix(): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
        )
    }

    private fun getVintageMatrix(): ColorMatrix {
        // Warm nostalgic crossover: slight green-yellow push in shadows, rose warmth in highlights
        return ColorMatrix(
            floatArrayOf(
                1.15f, 0.05f, 0.05f, 0f, 15f,
                0.05f, 1.05f, 0.05f, 0f, 8f,
                -0.10f, -0.05f, 0.85f, 0f, 25f,
                0f,     0f,     0f,    1f, 0f
            )
        )
    }

    private fun getInvertMatrix(): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                -1f, 0f,  0f,  0f, 255f,
                0f, -1f,  0f,  0f, 255f,
                0f,  0f, -1f,  0f, 255f,
                0f,  0f,  0f,  1f, 0f
            )
        )
    }

    private fun getBrightnessMatrix(brightnessBoost: Float): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessBoost,
                0f, 1f, 0f, 0f, brightnessBoost,
                0f, 0f, 1f, 0f, brightnessBoost,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun getContrastMatrix(contrast: Float): ColorMatrix {
        val scale = contrast
        val translate = (1f - scale) * 128f
        return ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Multi-pass fast stack/box blur
     */
    fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val w = source.width
        val h = source.height
        val pix = IntArray(w * h)
        source.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        var yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (offset in -radius..radius) {
                p = pix[yi + min(wm, max(offset, 0))]
                sir = stack[offset + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(offset)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (offset > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curY == 0) {
                    vmin[curX] = min(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (offset in -radius..radius) {
                yi = max(0, yp) + curX
                sir = stack[offset + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(offset)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (offset > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (offset < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curX == 0) {
                    vmin[curY] = min(curY + r1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurred.setPixels(pix, 0, w, 0, 0, w, h)
        return blurred
    }

    /**
     * Fast 3x3 Sharpen Kernel Convolution:
     * [  0, -1,  0 ]
     * [ -1,  5, -1 ]
     * [  0, -1,  0 ]
     */
    private fun applySharpenConvolution(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        val output = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 1 until h - 1) {
            val yPrev = (y - 1) * w
            val yCurr = y * w
            val yNext = (y + 1) * w

            for (x in 1 until w - 1) {
                val center = pixels[yCurr + x]
                val top = pixels[yPrev + x]
                val bottom = pixels[yNext + x]
                val left = pixels[yCurr + x - 1]
                val right = pixels[yCurr + x + 1]

                val a = (center ushr 24) and 0xff

                val r = 5 * ((center ushr 16) and 0xff) -
                        ((top ushr 16) and 0xff) -
                        ((bottom ushr 16) and 0xff) -
                        ((left ushr 16) and 0xff) -
                        ((right ushr 16) and 0xff)

                val g = 5 * ((center ushr 8) and 0xff) -
                        ((top ushr 8) and 0xff) -
                        ((bottom ushr 8) and 0xff) -
                        ((left ushr 8) and 0xff) -
                        ((right ushr 8) and 0xff)

                val b = 5 * (center and 0xff) -
                        (top and 0xff) -
                        (bottom and 0xff) -
                        (left and 0xff) -
                        (right and 0xff)

                val clampedR = r.coerceIn(0, 255)
                val clampedG = g.coerceIn(0, 255)
                val clampedB = b.coerceIn(0, 255)

                output[yCurr + x] = (a shl 24) or (clampedR shl 16) or (clampedG shl 8) or clampedB
            }
        }

        val sharpened = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(output, 0, w, 0, 0, w, h)
        return sharpened
    }

    /**
     * Applies portrait bokeh/vignette background blur keeping the center subject sharp.
     */
    fun applyBlurBackground(source: Bitmap, settings: BlurBgSettings): Bitmap {
        val blurredBg = fastBoxBlur(source, settings.blurRadius)
        val w = source.width
        val h = source.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw blurred background base
        canvas.drawBitmap(blurredBg, 0f, 0f, null)

        // Mask clear foreground onto canvas with a soft radial falloff
        val centerX = w / 2f
        val centerY = h / 2f
        val maxDim = max(w, h).toFloat()
        val innerRadius = maxDim * settings.focusRadiusRatio
        val outerRadius = maxDim * (settings.focusRadiusRatio + 0.35f)

        // Draw clear source image on top using alpha mask or radial gradient mask
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX, centerY, outerRadius,
                intArrayOf(
                    android.graphics.Color.argb(255, 255, 255, 255),
                    android.graphics.Color.argb(230, 255, 255, 255),
                    android.graphics.Color.argb(0, 255, 255, 255)
                ),
                floatArrayOf(0f, innerRadius / outerRadius, 1f),
                Shader.TileMode.CLAMP
            )
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
        }

        // Create overlay of original bitmap with transparency mask
        val clearOverlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val overlayCanvas = Canvas(clearOverlay)
        overlayCanvas.drawBitmap(source, 0f, 0f, null)

        // Invert shader logic: we keep center sharp
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX, centerY, outerRadius,
                intArrayOf(
                    android.graphics.Color.BLACK,
                    android.graphics.Color.BLACK,
                    android.graphics.Color.TRANSPARENT
                ),
                floatArrayOf(0f, innerRadius / outerRadius, 1f),
                Shader.TileMode.CLAMP
            )
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        }
        overlayCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)

        // Draw the masked sharp center on top of the blurred background
        canvas.drawBitmap(clearOverlay, 0f, 0f, null)

        return result
    }

    /**
     * Renders text overlays and sticker emojis directly onto a bitmap.
     */
    fun renderOverlays(
        base: Bitmap,
        texts: List<TextOverlay>,
        stickers: List<StickerOverlay>
    ): Bitmap {
        val result = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width.toFloat()
        val h = result.height.toFloat()
        val densityFactor = (w / 380f).coerceAtLeast(1.0f) // scale text cleanly with photo resolution

        // Draw Stickers
        for (sticker in stickers) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sticker.sizeSp * densityFactor
                textAlign = Paint.Align.CENTER
            }
            val x = sticker.normalizedX * w
            val y = sticker.normalizedY * h
            canvas.drawText(sticker.emoji, x, y, paint)
        }

        // Draw Text Overlays
        for (textOverlay in texts) {
            val style = when {
                textOverlay.isBold && textOverlay.isItalic -> Typeface.BOLD_ITALIC
                textOverlay.isBold -> Typeface.BOLD
                textOverlay.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val tf = Typeface.create(Typeface.DEFAULT, style)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textOverlay.color
                textSize = textOverlay.fontSizeSp * densityFactor
                typeface = tf
                textAlign = Paint.Align.CENTER
            }

            val x = textOverlay.normalizedX * w
            val y = textOverlay.normalizedY * h

            val bounds = Rect()
            paint.getTextBounds(textOverlay.text, 0, textOverlay.text.length, bounds)

            if (textOverlay.hasBackground) {
                val paddingH = 14f * densityFactor
                val paddingV = 8f * densityFactor
                val bgRect = RectF(
                    x - (bounds.width() / 2f) - paddingH,
                    y - bounds.height() - paddingV,
                    x + (bounds.width() / 2f) + paddingH,
                    y + paddingV
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textOverlay.backgroundColor
                }
                canvas.drawRoundRect(bgRect, 10f * densityFactor, 10f * densityFactor, bgPaint)
            }

            canvas.drawText(textOverlay.text, x, y, paint)
        }

        return result
    }
}
