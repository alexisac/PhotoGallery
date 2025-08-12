package com.example.photogallery.features.galleryScreen

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                GalleryViewModel(application)
            }
        }
    }
}