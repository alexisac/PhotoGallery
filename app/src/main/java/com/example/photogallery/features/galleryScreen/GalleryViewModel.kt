package com.example.photogallery.features.galleryScreen

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
            ?.sortedBy { it.lastModified() }
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



    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                GalleryViewModel(application)
            }
        }
    }
}