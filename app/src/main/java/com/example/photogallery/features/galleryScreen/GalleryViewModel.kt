package com.example.photogallery.features.galleryScreen

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.photogallery.model.PhotoFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images

    private val galleryDir: File by lazy {
        File(app.filesDir, "myGalleryAppPictures").apply { mkdirs() }
    }

    init {
        loadImages()
    }

    fun loadImages() {
        val uris = galleryDir
            .listFiles { file -> file.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.toUri() }
            ?: emptyList()
        _images.value = uris
    }

    fun addUris(uris: List<Uri>) {
        if(uris.isEmpty()) return

        val persisted = uris.map { src -> copyToAppStorage(src) }
        _images.update { it + persisted }
    }

    private fun copyToAppStorage(src: Uri): Uri {
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val destination = File(galleryDir, fileName)

        app.contentResolver.openInputStream(src)?.use {input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination.toUri()
    }

    fun duplicatePhoto(
        index: Int,
        onResult: (Int) -> Unit = {}
    ) {
        val current = _images.value
        if (index !in current.indices) return
        val srcUri = current[index]

        viewModelScope.launch(Dispatchers.IO) {
            val srcFile = File(requireNotNull(srcUri.path) { "Invalid uri path: $srcUri" })
            val ext = srcFile.extension.ifEmpty { "jpg" }
            val dst = File(galleryDir, "img_${System.currentTimeMillis()}.$ext")

            srcFile.inputStream().use { input ->
                dst.outputStream().use { output -> input.copyTo(output) }
            }
            val dstUri = dst.toUri()

            // pun noua poza la inceputul listei
            withContext(Dispatchers.Main) {
                _images.update { listOf(dstUri) + it }
                onResult(0)
            }
        }
    }

    fun saveFilter(
        index: Int,
        filter: PhotoFilter,
        onResult: (Int) -> Unit
    ){
        val current = _images.value
        if(index !in current.indices) return
        val srcUri = current[index]

        viewModelScope.launch(Dispatchers.IO) {
            // Citeste bitmap-ul sursa
            val srcPath = requireNotNull(srcUri.path) { "Invalid uri path: $srcUri" }
            val srcBitmap = BitmapFactory.decodeFile(srcPath) ?: return@launch

            // aplic filtrul
            val filtered = applyFilter(srcBitmap, filter)

            // salvez in acelasi fisier
            val dst = File(galleryDir, "img_${System.currentTimeMillis()}_filtered.jpg")
            dst.outputStream().use { out ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val dstUri = dst.toUri()

            // pun noua poza la inceputul listei
            withContext(Dispatchers.Main) {
                _images.update { listOf(dstUri) + it }
                onResult(0)
            }
        }
    }

    private fun applyFilter(src: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.None) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matrix = when (filter) {
            PhotoFilter.GrayScale -> ColorMatrix().apply { setSaturation(0f) }
            PhotoFilter.Sepia -> ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            ))
            PhotoFilter.Invert -> ColorMatrix(floatArrayOf(
                -1f, 0f,  0f,  0f, 255f,
                0f,-1f, 0f,  0f, 255f,
                0f, 0f,-1f,  0f, 255f,
                0f, 0f, 0f,  1f,   0f
            ))
            PhotoFilter.None -> ColorMatrix()
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                GalleryViewModel(application)
            }
        }
    }
}