package com.example.photogallery.utils

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Api {
    // Turn off windows firewall
    private val URL = "192.168.100.42:8080"
    private val HTTP_URL = "http://$URL"
    val preferences = "PhotoGalleryPreferences"

    private var gson = GsonBuilder().create()

    val retrofit = Retrofit.Builder()
        .baseUrl(HTTP_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}