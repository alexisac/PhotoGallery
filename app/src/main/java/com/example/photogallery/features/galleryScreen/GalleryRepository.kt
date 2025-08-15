package com.example.photogallery.features.galleryScreen

import com.example.photogallery.dbRoom.GalleryDAO
import com.example.photogallery.dbRoom.PhotoEntity
import com.example.photogallery.dbRoom.PhotoSubjectMapping
import com.example.photogallery.dbRoom.SubjectEntity
import com.example.photogallery.dbRoom.SubjectWithCount
import kotlinx.coroutines.flow.Flow

class GalleryRepository(private val dao: GalleryDAO) {

    suspend fun upsertPhoto(uri: String, createdAt: Long) {
        return dao.addPhoto(PhotoEntity(uri, createdAt))
    }

    fun subjectsFlow(): Flow<List<SubjectEntity>> {
        return dao.getAllSubjects()
    }

    fun subjectsWithCount(): Flow<List<SubjectWithCount>> {
        return dao.subjectsWithCount()
    }

    suspend fun createSubject(name: String): Long {
        return dao.addSubject(SubjectEntity(name = name))
    }

    suspend fun deleteSubject(id: Long) {
        dao.deleteSubject(id)
    }

    suspend fun setPhotoInSubject(photoUri: String, subjectId: Long, checked: Boolean) {
        if (checked)
            dao.addPhotoSubject(PhotoSubjectMapping(photoUri, subjectId))
        else
            dao.deletePhotoFromSubject(photoUri, subjectId)
    }

    fun subjectIdsForPhoto(photoUri: String): Flow<List<Long>> {
        return dao.getAllSubjectsIdForPhoto(photoUri)
    }

    fun photosForSubject(subjectId: Long): Flow<List<PhotoEntity>> {
        return dao.getAllPhotosForSubject(subjectId)
    }
}