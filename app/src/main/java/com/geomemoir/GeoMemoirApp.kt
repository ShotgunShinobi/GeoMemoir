package com.geomemoir

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class GeoMemoirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre
        MapLibre.getInstance(this)
    }
}
