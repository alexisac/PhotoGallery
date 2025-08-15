package com.example.photogallery.dbRoom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(
    entities = [
        PhotoEntity::class,
        SubjectEntity::class,
        PhotoSubjectMapping::class
               ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDB : RoomDatabase() {
    abstract fun dao(): GalleryDAO

    companion object {
        @Volatile private var INSTANCE: GalleryDB? = null
        fun get(context: Context): GalleryDB =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalleryDB::class.java,
                    "gallery.db"
                ).build().also { INSTANCE = it }
            }
    }
}