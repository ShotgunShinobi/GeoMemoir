package com.geomemoir.domain.entity

data class OfflineRegion(
    val id: Long = 0,
    val name: String,
    val boundingBox: BoundingBox,
    val minZoom: Int = 1,
    val maxZoom: Int = 15,
    val maplibreRegionId: Long? = null,
    val downloadedAt: Long? = null,
    val sizeBytes: Long? = null,
    val status: DownloadStatus = DownloadStatus.PENDING
)

data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
)

enum class DownloadStatus { PENDING, DOWNLOADING, COMPLETE, ERROR, PAUSED }

data class DownloadProgress(
    val regionId: Long,
    val completedTiles: Long,
    val totalTiles: Long,
    val completedBytes: Long,
    val isComplete: Boolean,
    val maplibreRegionId: Long? = null,
    val errorMessage: String? = null
) {
    val percentage: Int get() =
        if (totalTiles == 0L) 0 else ((completedTiles * 100) / totalTiles).toInt()
}
