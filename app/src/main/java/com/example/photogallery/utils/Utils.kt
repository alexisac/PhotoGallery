package com.example.photogallery.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.core.net.toUri
import com.example.photogallery.model.PhotoFilter
import java.io.File

object Utils {

    fun newImageFile(galleryDir: File, suffix: String, extension: String) =
        File(galleryDir, "img_${System.currentTimeMillis()}$suffix.${extension.lowercase()}")

    fun copyToAppStorage(contentResolver: ContentResolver, galleryDir: File, src: Uri): Uri {
        val dst = newImageFile(galleryDir, suffix = "", extension = "jpg")
        contentResolver.openInputStream(src)?.use { input ->
            dst.outputStream().use { output -> input.copyTo(output) }
        }
        return dst.toUri()
    }

    fun applyFilter(src: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.None) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix(filter))
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun colorMatrix(filter: PhotoFilter): ColorMatrix = when (filter) {
        PhotoFilter.GrayScale -> ColorMatrix().apply { setSaturation(0f) }
        PhotoFilter.Sepia -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
        )
        PhotoFilter.Invert -> ColorMatrix(
            floatArrayOf(
                -1f, 0f,  0f,  0f, 255f,
                0f,-1f, 0f,  0f, 255f,
                0f, 0f,-1f,  0f, 255f,
                0f, 0f, 0f,  1f,   0f
            )
        )
        PhotoFilter.None -> ColorMatrix()
    }
}