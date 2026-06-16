package com.geomemoir

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineManager

@HiltAndroidApp
class GeoMemoirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre
        MapLibre.getInstance(this)
        
        // Increase tile limit for offline regions (default is 6000)
        OfflineManager.getInstance(this).setOfflineMapboxTileCountLimit(50_000)
    }
}
