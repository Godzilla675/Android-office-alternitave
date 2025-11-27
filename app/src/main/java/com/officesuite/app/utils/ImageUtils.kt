package com.officesuite.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun rotateBitmapIfNeeded(bitmap: Bitmap, imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)

        if (scale >= 1.0f) {
            return bitmap
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadBitmapFromFile(file: File, maxSize: Int = 2048): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyGrayscaleFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff
                val gray = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
                val newPixel = (0xff shl 24) or (gray shl 16) or (gray shl 8) or gray
                result.setPixel(x, y, newPixel)
            }
        }

        return result
    }

    fun applyContrastEnhancement(bitmap: Bitmap, contrast: Float = 1.5f): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val factor = (259 * (contrast * 255 + 255)) / (255 * (259 - contrast * 255))

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xff
                var red = (pixel shr 16) and 0xff
                var green = (pixel shr 8) and 0xff
                var blue = pixel and 0xff

                red = (factor * (red - 128) + 128).toInt().coerceIn(0, 255)
                green = (factor * (green - 128) + 128).toInt().coerceIn(0, 255)
                blue = (factor * (blue - 128) + 128).toInt().coerceIn(0, 255)

                val newPixel = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                result.setPixel(x, y, newPixel)
            }
        }

        return result
    }
}
