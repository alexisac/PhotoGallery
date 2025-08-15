package com.example.photogallery.features.galleryScreen

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.photogallery.dbRoom.GalleryDB
import com.example.photogallery.dbRoom.SubjectEntity
import com.example.photogallery.dbRoom.SubjectWithCount
import com.example.photogallery.features.galleryScreen.utils.GalleryFileUtils
import com.example.photogallery.model.PhotoFilter
import com.example.photogallery.utils.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val galleryDir: File by lazy {
        File(app.filesDir, Strings.GALLERY_DIR_NAME).apply { mkdirs() }
    }

    private val repo: GalleryRepository = GalleryRepository(GalleryDB.get(app).dao())
    val subjectsFlow: Flow<List<SubjectEntity>> = repo.subjectsFlow()
    val subjectsWithCount: Flow<List<SubjectWithCount>> = repo.subjectsWithCount()

    init {
        viewModelScope.launch { loadImages() }
    }

    suspend fun loadImages() = withContext(Dispatchers.IO) {
        val files = galleryDir
            .listFiles { file -> file.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val uris = files.map { it.toUri() }
        _images.value = uris

        // add photo in local DB
        files.forEach { f ->
            repo.upsertPhoto(
                uri = f.toUri().toString(),
                createdAt = f.lastModified()
            )
        }
    }

    // acum e safe impotriva erorilor
    // daca vor fi multe poze e safe, nu blocheaza thread-ul principal
    fun addUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val persisted = uris.mapNotNull { src ->
                runCatching { GalleryFileUtils.copyToAppStorage(app.contentResolver, galleryDir, src) }.getOrNull()
            }
            if (persisted.isNotEmpty()) {
                // add photo in local DB
                persisted.forEach{ uri ->
                    repo.upsertPhoto(uri.toString(), File(requireNotNull(uri.path)).lastModified())
                }
                withContext(Dispatchers.Main) {
                    _images.update { persisted + it }
                }
            }
        }
    }

    fun duplicatePhoto(
        index: Int,
        onResult: (Int) -> Unit = {}
    ) {
        val current = _images.value
        if (index !in current.indices) return
        val srcUri = current[index]

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val srcFile = File(requireNotNull(srcUri.path) { "Invalid uri path: $srcUri" })
                val extension = srcFile.extension.ifEmpty { "jpg" }
                val dst = GalleryFileUtils.newImageFile(galleryDir, suffix = "", extension = extension)

                srcFile.inputStream().use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                }
                dst.toUri()
            }.onSuccess { newUri ->
                // add ptoho in DB
                repo.upsertPhoto(newUri.toString(), File(requireNotNull(newUri.path)).lastModified())

                withContext(Dispatchers.Main) {
                    insertAtTop(newUri)
                    onResult(0)
                }
            }
        }
    }

    fun saveFilter(
        index: Int,
        filter: PhotoFilter,
        onResult: (Int) -> Unit = {}
    ){
        val current = _images.value
        if(index !in current.indices) return
        val srcUri = current[index]

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Citeste bitmap-ul sursa
                val srcPath = requireNotNull(srcUri.path) { "Invalid uri path: $srcUri" }
                val srcBitmap = BitmapFactory.decodeFile(srcPath) ?: return@launch

                // aplic filtrul
                val filtered = GalleryFileUtils.applyFilter(srcBitmap, filter)

                // salvez in acelasi fisier
                val dst = GalleryFileUtils.newImageFile(galleryDir, suffix = "_filtered", extension = "jpg")
                dst.outputStream().use { out ->
                    filtered.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                dst.toUri()
            }.getOrNull()?.let { newUri ->
                //add photo in DB
                repo.upsertPhoto(newUri.toString(), File(requireNotNull(newUri.path)).lastModified())

                withContext(Dispatchers.Main) {
                    insertAtTop(newUri)
                    onResult(0)
                }
            }
        }
    }

    fun createSubject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repo.createSubject(trimmed) }
            }
            result.onFailure {
                _errorMessage.value = "Subject \"$trimmed\" already exist!"
            }
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.deleteSubject(id) }
        }
    }

    fun setPhotoInSubject(
        photoUri: Uri,
        subjectId: Long,
        checked: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setPhotoInSubject(
                photoUri.toString(),
                subjectId,
                checked
            )
        }
    }

    fun subjectIdsForPhoto(photoUri: Uri): Flow<Set<Long>> =
        repo.subjectIdsForPhoto(photoUri.toString()).map { it.toSet() }

    fun photosForSubject(subjectId: Long): Flow<List<Uri>> =
        repo.photosForSubject(subjectId).map { list ->
            list.map { Uri.parse(it.uri) }
        }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun insertAtTop(uri: Uri) {
        _images.update { listOf(uri) + it }
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