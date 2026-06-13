package com.geomemoir.domain.repository

import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.OfflineRegion
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.StateFlow

interface OfflineMapRepository {
    val activeDownloads: StateFlow<Map<Long, DownloadProgress>>
    fun getAllRegions(): Flow<List<OfflineRegion>>
    fun downloadRegion(region: OfflineRegion): Flow<DownloadProgress>
    suspend fun pauseDownload(maplibreRegionId: Long)
    suspend fun resumeDownload(maplibreRegionId: Long)
    suspend fun deleteRegion(id: Long)
    suspend fun insertRegion(region: OfflineRegion): Long
}
