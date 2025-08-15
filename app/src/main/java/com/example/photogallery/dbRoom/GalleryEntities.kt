package com.example.photogallery.dbRoom

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val uri: String,
    val createdAt: Long
)

@Entity(tableName = "subjects",
    indices = [Index(
        value = ["name"],
        unique = true
    )])
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// relatia many-to-many   Photos <-> Subjects
@Entity(
    tableName = "photo_subject",
    primaryKeys = ["photoUri", "subjectId"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["uri"],
            childColumns = ["photoUri"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)

// DTO ce reprezinta entitatea din tabelul PhotoSubject de mai ssu
data class PhotoSubjectMapping(
    val photoUri: String,
    val subjectId: Long
)

// DTO pentru a sti cate poze are fiecare subiect
data class SubjectWithCount(
    val id: Long,
    val name: String,
    val count: Int
)