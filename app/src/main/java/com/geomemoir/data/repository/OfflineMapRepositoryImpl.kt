package com.geomemoir.data.repository

import android.util.Log
import com.geomemoir.data.db.dao.OfflineRegionDao
import com.geomemoir.data.db.mapper.toDomain
import com.geomemoir.data.db.mapper.toEntity
import com.geomemoir.data.map.OfflineMapDataSource
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.DownloadStatus
import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.domain.repository.OfflineMapRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMapRepositoryImpl @Inject constructor(
    private val offlineRegionDao: OfflineRegionDao,
    private val offlineMapDataSource: OfflineMapDataSource
) : OfflineMapRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeDownloads = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    override val activeDownloads: StateFlow<Map<Long, DownloadProgress>> = _activeDownloads.asStateFlow()

    private val activeFlows = mutableMapOf<Long, Flow<DownloadProgress>>()
    private val lastDbUpdateProgress = mutableMapOf<Long, Int>()

    init {
        // Automatically resume observing regions that were downloading
        repositoryScope.launch {
            offlineRegionDao.getAllRegions().first().forEach { entity ->
                if (entity.status == DownloadStatus.DOWNLOADING.name && entity.maplibreRegionId != null) {
                    Log.d("OfflineMapRepository", "Resuming observation for region: ${entity.name}")
                    getOrCreateDownloadFlow(entity.toDomain())
                }
            }
        }
    }

    private fun getOrCreateDownloadFlow(region: OfflineRegion): Flow<DownloadProgress> {
        return activeFlows.getOrPut(region.id) {
            val maplibreId = region.maplibreRegionId
            val sourceFlow = if (maplibreId != null) {
                offlineMapDataSource.observeRegion(region, maplibreId)
            } else {
                offlineMapDataSource.downloadRegion(region)
            }

            sourceFlow
                .onEach { progress ->
                    if (progress.maplibreRegionId != null) {
                        offlineRegionDao.updateMaplibreId(region.id, progress.maplibreRegionId)
                    }
                    updateProgress(progress)
                }
                .onCompletion {
                    activeFlows.remove(region.id)
                    lastDbUpdateProgress.remove(region.id)
                    Log.d("OfflineMapRepository", "Flow completed for region: ${region.name}")
                }
                .shareIn(repositoryScope, SharingStarted.Eagerly, replay = 1)
        }
    }

    private suspend fun updateProgress(progress: DownloadProgress) {
        // UI Update is immediate
        _activeDownloads.update { it + (progress.regionId to progress) }
        
        val currentPercent = progress.percentage
        val lastUpdate = lastDbUpdateProgress[progress.regionId] ?: -1
        
        // Throttled DB update: only every 5% or on completion/error
        val shouldUpdateDb = progress.isComplete || 
                progress.errorMessage != null || 
                currentPercent >= lastUpdate + 5

        if (shouldUpdateDb) {
            lastDbUpdateProgress[progress.regionId] = currentPercent
            
            val status = when {
                progress.errorMessage != null -> DownloadStatus.ERROR
                progress.isComplete -> DownloadStatus.COMPLETE
                else -> DownloadStatus.DOWNLOADING
            }

            Log.d("OfflineMapRepository", "Updating DB for region ${progress.regionId}: $currentPercent% ($status)")
            
            offlineRegionDao.updateDownloadStatus(
                id = progress.regionId,
                status = status.name,
                size = if (progress.isComplete) progress.completedBytes else null,
                downloadedAt = if (progress.isComplete) System.currentTimeMillis() else null
            )

            if (progress.isComplete || progress.errorMessage != null) {
                _activeDownloads.update { it - progress.regionId }
            }
        }
    }

    override fun getAllRegions(): Flow<List<OfflineRegion>> {
        return offlineRegionDao.getAllRegions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun downloadRegion(region: OfflineRegion): Flow<DownloadProgress> {
        return getOrCreateDownloadFlow(region)
    }

    override suspend fun pauseDownload(maplibreRegionId: Long) {
        offlineMapDataSource.pauseDownload(maplibreRegionId)
        val entity = offlineRegionDao.getRegionByMaplibreId(maplibreRegionId)
        if (entity != null) {
            offlineRegionDao.updateDownloadStatus(
                id = entity.id,
                status = DownloadStatus.PAUSED.name,
                size = entity.sizeBytes,
                downloadedAt = entity.downloadedAt
            )
            _activeDownloads.update { it - entity.id }
            activeFlows.remove(entity.id)
            lastDbUpdateProgress.remove(entity.id)
        }
    }

    override suspend fun resumeDownload(maplibreRegionId: Long) {
        offlineMapDataSource.resumeDownload(maplibreRegionId)
        val entity = offlineRegionDao.getRegionByMaplibreId(maplibreRegionId)
        if (entity != null) {
            offlineRegionDao.updateDownloadStatus(
                id = entity.id,
                status = DownloadStatus.DOWNLOADING.name,
                size = entity.sizeBytes,
                downloadedAt = entity.downloadedAt
            )
            getOrCreateDownloadFlow(entity.toDomain())
        }
    }

    override suspend fun deleteRegion(id: Long) {
        try {
            val region = offlineRegionDao.getRegionById(id).first()
            if (region != null && region.maplibreRegionId != null) {
                offlineMapDataSource.deleteRegion(region.maplibreRegionId)
            }
        } catch (e: Exception) {
            // Ignore
        }
        offlineRegionDao.deleteById(id)
        _activeDownloads.update { it - id }
        activeFlows.remove(id)
        lastDbUpdateProgress.remove(id)
    }

    override suspend fun insertRegion(region: OfflineRegion): Long {
        return offlineRegionDao.insert(region.toEntity())
    }
}
