package com.example.photogallery.dbRoom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDAO {

    // Photos
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhoto(photo: PhotoEntity)

    // Subjects
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addSubject(subject: SubjectEntity): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    // da toate subiectele cu nr de poze asignate
    // subiectele sunt sortate alfabetic
    @Query("""
        SELECT s.id, s.name, COUNT(ps.photoUri) AS count
        FROM subjects s
        LEFT JOIN photo_subject ps ON s.id = ps.subjectId
        GROUP BY s.id
        ORDER BY s.name COLLATE NOCASE
    """)
    fun subjectsWithCount(): Flow<List<SubjectWithCount>>

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    //relatia many-to-many   Photo <-> Subject
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhotoSubject(link: PhotoSubjectMapping)

    // sterg o asignare intre o poza si un subiect
    @Query("DELETE FROM photo_subject WHERE photoUri = :photoUri AND subjectId = :subjectId")
    suspend fun deletePhotoFromSubject(photoUri: String, subjectId: Long)

    @Query("SELECT subjectId FROM photo_subject WHERE photoUri = :photoUri")
    fun getAllSubjectsIdForPhoto(photoUri: String): Flow<List<Long>>

    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_subject ps ON ps.photoUri = p.uri
        WHERE ps.subjectId = :subjectId
        ORDER BY p.createdAt DESC
    """)
    fun getAllPhotosForSubject(subjectId: Long): Flow<List<PhotoEntity>>
}