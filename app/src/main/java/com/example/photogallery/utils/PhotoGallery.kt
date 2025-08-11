package com.example.photogallery.utils

import android.app.Application

class PhotoGallery : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}