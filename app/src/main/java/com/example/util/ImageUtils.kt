package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.CropRatio
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

object ImageUtils {

    /**
     * Decodes and scales a Bitmap from a content Uri, correcting for EXIF orientation.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= maxDimension ||
                options.outHeight / (sampleSize * 2) >= maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val orientation = getExifOrientation(context, uri)
            rotateBitmap(decodedBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a Bitmap from resource ID with scaling.
     */
    fun loadBitmapFromResource(context: Context, resId: Int, maxDimension: Int = 2048): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)

            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= maxDimension ||
                options.outHeight / (sampleSize * 2) >= maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeResource(context.resources, resId, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a square thumbnail for filter preview tabs.
     */
    fun createThumbnail(bitmap: Bitmap, size: Int = 128): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val minEdge = min(width, height)
        val xOffset = (width - minEdge) / 2
        val yOffset = (height - minEdge) / 2
        val square = Bitmap.createBitmap(bitmap, xOffset, yOffset, minEdge, minEdge)
        return Bitmap.createScaledBitmap(square, size, size, true)
    }

    /**
     * Rotates a bitmap by a multiple of 90 degrees.
     */
    fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Flips a bitmap horizontally or vertically.
     */
    fun flipImage(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        val matrix = Matrix().apply { postScale(sx, sy) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops a bitmap according to target aspect ratio (centered crop).
     */
    fun cropToRatio(bitmap: Bitmap, ratio: CropRatio): Bitmap {
        if (ratio == CropRatio.FREE) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val targetAspect = ratio.ratioX / ratio.ratioY
        val currentAspect = w.toFloat() / h.toFloat()

        var cropW = w
        var cropH = h

        if (currentAspect > targetAspect) {
            // Source is wider than target: trim width
            cropW = (h * targetAspect).toInt()
        } else {
            // Source is taller than target: trim height
            cropH = (w / targetAspect).toInt()
        }

        val startX = ((w - cropW) / 2).coerceAtLeast(0)
        val startY = ((h - cropH) / 2).coerceAtLeast(0)

        return Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
    }

    /**
     * Saves the bitmap to the public Android Pictures/PhotoMaster gallery folder using MediaStore.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Result<Uri> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "PhotoMaster_$timestamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoMaster")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Could not create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { stream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                if (!compressed) {
                    resolver.delete(imageUri, null, null)
                    return Result.failure(Exception("Failed to compress image"))
                }
            } ?: run {
                resolver.delete(imageUri, null, null)
                return Result.failure(Exception("Could not open output stream for saving image"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Saves bitmap to a temporary cache file and creates an ACTION_SEND share intent.
     */
    fun shareBitmap(context: Context, bitmap: Bitmap): Intent? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "photomaster_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                val exifInterface = ExifInterface(stream)
                exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }
}
