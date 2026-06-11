package com.geomemoir.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_region_meta")
data class OfflineRegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "min_lat") val minLat: Double,
    @ColumnInfo(name = "max_lat") val maxLat: Double,
    @ColumnInfo(name = "min_lng") val minLng: Double,
    @ColumnInfo(name = "max_lng") val maxLng: Double,
    @ColumnInfo(name = "min_zoom") val minZoom: Int,
    @ColumnInfo(name = "max_zoom") val maxZoom: Int,
    @ColumnInfo(name = "maplibre_region_id") val maplibreRegionId: Long?,
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long?,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long?,
    val status: String
)
