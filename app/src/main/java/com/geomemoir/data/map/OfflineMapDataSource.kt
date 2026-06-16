package com.geomemoir.data.map

import android.content.Context
import android.util.Log
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.OfflineRegion as DomainRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
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

                val observer = createObserver(region.id, maplibreId, region.name)
                mlRegion.setObserver(observer)
                
                // Get initial status immediately
                mlRegion.getStatus(object : MapLibreRegion.OfflineRegionStatusCallback {
                    override fun onStatus(status: OfflineRegionStatus?) {
                        status?.let { observer.onStatusChanged(it) }
                    }
                    override fun onError(error: String?) {
                        trySend(DownloadProgress(region.id, 0, 0, 0, false, maplibreId, error ?: "Unknown error"))
                    }
                })
            }

            override fun onError(error: String) {
                trySend(DownloadProgress(region.id, 0, 0, 0, false, null, error))
                channel.close()
            }
        })
        awaitClose { }
    }.conflate()

    private fun kotlinx.coroutines.channels.ProducerScope<DownloadProgress>.createObserver(
        regionId: Long,
        maplibreId: Long,
        regionName: String
    ) = object : MapLibreRegion.OfflineRegionObserver {
        override fun onStatusChanged(status: OfflineRegionStatus) {
            val isActuallyComplete = status.isComplete || 
                (status.requiredResourceCount > 0 && status.completedResourceCount >= status.requiredResourceCount)
            
            val progress = DownloadProgress(
                regionId      = regionId,
                completedTiles = status.completedResourceCount,
                totalTiles    = status.requiredResourceCount,
                completedBytes = status.completedResourceSize,
                isComplete    = isActuallyComplete,
                maplibreRegionId = maplibreId
            )
            
            trySend(progress)

            if (isActuallyComplete) {
                Log.d("OfflineMapDataSource", "Download complete for region: $regionName")
                channel.close()
            }
        }

        override fun onError(error: OfflineRegionError) {
            Log.e("OfflineMapDataSource", "Region observer error for $regionName: ${error.message}")
            trySend(DownloadProgress(regionId, 0, 0, 0, false, maplibreId, error.message))
            channel.close()
        }

        override fun mapboxTileCountLimitExceeded(limit: Long) {
            Log.e("OfflineMapDataSource", "Tile count limit exceeded for $regionName: $limit")
            trySend(DownloadProgress(regionId, 0, 0, 0, false, maplibreId, "Tile count limit exceeded"))
            channel.close()
        }
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
                    val maplibreId = mlRegion.id
                    Log.d("OfflineMapDataSource", "Region created: $maplibreId for ${region.name}")
                    
                    val observer = createObserver(region.id, maplibreId, region.name)
                    mlRegion.setObserver(observer)
                    mlRegion.setDownloadState(MapLibreRegion.STATE_ACTIVE)
                    
                    // Fetch initial status to jump-start the progress (get total tiles count)
                    mlRegion.getStatus(object : MapLibreRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            status?.let { observer.onStatusChanged(it) }
                        }
                        override fun onError(error: String?) {
                            Log.e("OfflineMapDataSource", "Initial status error: $error")
                        }
                    })
                }
                override fun onError(error: String) {
                    Log.e("OfflineMapDataSource", "Create region error for ${region.name}: $error")
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
            Log.d("OfflineMapDataSource", "Flow stopped for: ${region.name}")
        }
    }.conflate()

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
