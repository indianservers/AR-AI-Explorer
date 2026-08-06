package com.indianservers.aiexplorer.input

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object SampledImageLoader {
    const val DEFAULT_MAX_DIMENSION = 1600

    fun calculateSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int = DEFAULT_MAX_DIMENSION,
        maxHeight: Int = DEFAULT_MAX_DIMENSION,
    ): Int {
        if (width <= 0 || height <= 0 || maxWidth <= 0 || maxHeight <= 0) return 1
        var sampleSize = 1
        while (
            width / sampleSize > maxWidth ||
            height / sampleSize > maxHeight
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun decode(
        resolver: ContentResolver,
        uri: Uri,
        maxWidth: Int = DEFAULT_MAX_DIMENSION,
        maxHeight: Int = DEFAULT_MAX_DIMENSION,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    fun readCompressedJpeg(
        resolver: ContentResolver,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = 88,
    ): ByteArray? {
        val bitmap = decode(resolver, uri, maxDimension, maxDimension) ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), output)) {
                    return null
                }
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
