package com.geomemoir.data.map

import android.content.Context
import android.util.Log
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.OfflineRegion as DomainRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion as MapLibreRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMapDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val offlineManager by lazy { OfflineManager.getInstance(context) }

    fun observeRegion(region: DomainRegion, maplibreId: Long): Flow<DownloadProgress> = callbackFlow {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<MapLibreRegion>?) {
                val mlRegion = offlineRegions?.find { it.id == maplibreId }
                if (mlRegion == null) {
                    trySend(DownloadProgress(region.id, 0, 0, 0, false, null, "Region not found in MapLibre"))
                    channel.close()
                    return
                }

                mlRegion.setObserver(object : MapLibreRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        trySend(DownloadProgress(
                            regionId = region.id,
                            completedTiles = status.completedResourceCount,
                            totalTiles = status.requiredResourceCount,
                            completedBytes = status.completedResourceSize,
                            isComplete = status.isComplete,
                            maplibreRegionId = maplibreId
                        ))
                        if (status.isComplete) channel.close()
                    }

                    override fun onError(error: OfflineRegionError) {
                        trySend(DownloadProgress(region.id, 0, 0, 0, false, maplibreId, error.message))
                        channel.close()
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        trySend(DownloadProgress(region.id, 0, 0, 0, false, maplibreId, "Tile count limit exceeded"))
                        channel.close()
                    }
                })
            }

            override fun onError(error: String) {
                trySend(DownloadProgress(region.id, 0, 0, 0, false, null, error))
                channel.close()
            }
        })
        awaitClose { }
    }

    fun downloadRegion(region: DomainRegion): Flow<DownloadProgress> = callbackFlow {
        Log.d("OfflineMapDataSource", "Starting download for region: ${region.name}")
        val definition = OfflineTilePyramidRegionDefinition(
            MapConfig.STYLE_URL,
            LatLngBounds.from(
                region.boundingBox.maxLat, region.boundingBox.maxLng,
                region.boundingBox.minLat, region.boundingBox.minLng
            ),
            region.minZoom.toDouble(),
            region.maxZoom.toDouble(),
            context.resources.displayMetrics.density
        )
        val metadata = region.name.encodeToByteArray()

        offlineManager.createOfflineRegion(definition, metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(mlRegion: MapLibreRegion) {
                    Log.d("OfflineMapDataSource", "Region created: ${mlRegion.id}")
                    val maplibreId = mlRegion.id
                    mlRegion.setObserver(object : MapLibreRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            Log.d("OfflineMapDataSource", "Status changed: ${status.completedResourceCount}/${status.requiredResourceCount}")
                            trySend(DownloadProgress(
                                regionId      = region.id,
                                completedTiles = status.completedResourceCount,
                                totalTiles    = status.requiredResourceCount,
                                completedBytes = status.completedResourceSize,
                                isComplete    = status.isComplete,
                                maplibreRegionId = maplibreId
                            ))
                            if (status.isComplete) {
                                Log.d("OfflineMapDataSource", "Download complete for region: ${region.name}")
                                channel.close()
                            }
                        }
                        override fun onError(error: OfflineRegionError) {
                            Log.e("OfflineMapDataSource", "Region observer error: ${error.message}")
                            trySend(DownloadProgress(
                                regionId = region.id, 0, 0, 0,
                                isComplete = false,
                                maplibreRegionId = maplibreId,
                                errorMessage = error.message
                            ))
                            channel.close()
                        }
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            Log.e("OfflineMapDataSource", "Tile count limit exceeded: $limit")
                            trySend(DownloadProgress(
                                regionId = region.id, 0, 0, 0,
                                isComplete = false,
                                maplibreRegionId = maplibreId,
                                errorMessage = "Tile count limit exceeded ($limit tiles max)"
                            ))
                            channel.close()
                        }
                    })
                    mlRegion.setDownloadState(MapLibreRegion.STATE_ACTIVE)
                }
                override fun onError(error: String) {
                    Log.e("OfflineMapDataSource", "Create region error: $error")
                    trySend(DownloadProgress(
                        regionId = region.id,
                        completedTiles = 0,
                        totalTiles = 0,
                        completedBytes = 0,
                        isComplete = false,
                        errorMessage = error
                    ))
                    channel.close()
                }
            }
        )
        awaitClose { 
            Log.d("OfflineMapDataSource", "Flow collection stopped for: ${region.name}. Background download continues.")
        }
    }

    fun pauseDownload(maplibreRegionId: Long) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<MapLibreRegion>?) {
                offlineRegions?.find { it.id == maplibreRegionId }?.setDownloadState(MapLibreRegion.STATE_INACTIVE)
            }
            override fun onError(error: String) {}
        })
    }

    fun resumeDownload(maplibreRegionId: Long) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<MapLibreRegion>?) {
                offlineRegions?.find { it.id == maplibreRegionId }?.setDownloadState(MapLibreRegion.STATE_ACTIVE)
            }
            override fun onError(error: String) {}
        })
    }

    fun deleteRegion(maplibreRegionId: Long) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<MapLibreRegion>?) {
                offlineRegions?.find { it.id == maplibreRegionId }?.delete(object : MapLibreRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {}
                    override fun onError(error: String) {}
                })
            }
            override fun onError(error: String) {}
        })
    }
}
