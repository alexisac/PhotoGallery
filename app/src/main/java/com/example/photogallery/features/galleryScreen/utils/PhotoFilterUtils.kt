package com.example.photogallery.features.galleryScreen.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.photogallery.model.PhotoFilter

object PhotoFilterUtils {
    fun toColorFilterOrNull(filter: PhotoFilter): ColorFilter? {
        val matrix = when (filter) {
            PhotoFilter.None      -> return null
            PhotoFilter.GrayScale -> ColorMatrix().apply { setToSaturation(0f) }
            PhotoFilter.Sepia     -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f,     0f,     0f, 1f, 0f
                )
            )
            PhotoFilter.Invert    -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f,  0f, 255f,
                     0f,-1f, 0f,  0f, 255f,
                     0f, 0f,-1f,  0f, 255f,
                     0f, 0f, 0f,  1f,   0f
                )
            )
        }
        return ColorFilter.colorMatrix(matrix)
    }
}

fun PhotoFilter.toColorFilterOrNull(): ColorFilter? =
    PhotoFilterUtils.toColorFilterOrNull(this)