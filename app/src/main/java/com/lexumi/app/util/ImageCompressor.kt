package com.lexumi.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Compresses a picked image down to at most [maxBytes], first by lowering
 * JPEG quality and, if that's not enough, by shrinking the dimensions too.
 * Returns the file it wrote, or null if the image couldn't be read at all.
 */
object ImageCompressor {

    fun compressToFile(context: Context, uri: Uri, destination: File, maxBytes: Int = 100 * 1024): File? {
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null

        var bitmap = original
        var quality = 90

        fun currentBytes(bmp: Bitmap, q: Int): ByteArrayOutputStream {
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, q, stream)
            return stream
        }

        var stream = currentBytes(bitmap, quality)

        // Step 1: lower JPEG quality first (cheap, preserves detail best).
        while (stream.size() > maxBytes && quality > 35) {
            quality -= 10
            stream = currentBytes(bitmap, quality)
        }

        // Step 2: if quality alone isn't enough, shrink the dimensions too.
        while (stream.size() > maxBytes && (bitmap.width > 120 || bitmap.height > 120)) {
            val newWidth = (bitmap.width * 0.8f).toInt().coerceAtLeast(120)
            val newHeight = (bitmap.height * 0.8f).toInt().coerceAtLeast(120)
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            quality = 80
            stream = currentBytes(bitmap, quality)
            while (stream.size() > maxBytes && quality > 35) {
                quality -= 10
                stream = currentBytes(bitmap, quality)
            }
        }

        destination.writeBytes(stream.toByteArray())
        return destination
    }
}
