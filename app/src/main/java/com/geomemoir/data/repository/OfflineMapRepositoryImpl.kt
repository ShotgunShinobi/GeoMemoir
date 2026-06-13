package com.geomemoir.data.repository

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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

    init {
        // Automatically resume observing regions that were downloading
        repositoryScope.launch {
            offlineRegionDao.getAllRegions().first().forEach { entity ->
                if (entity.status == DownloadStatus.DOWNLOADING.name && entity.maplibreRegionId != null) {
                    observeExistingDownload(entity.toDomain(), entity.maplibreRegionId)
                }
            }
        }
    }

    private fun observeExistingDownload(region: OfflineRegion, maplibreId: Long) {
        offlineMapDataSource.observeRegion(region, maplibreId)
            .onEach { progress ->
                updateProgress(progress)
            }
            .launchIn(repositoryScope)
    }

    private suspend fun updateProgress(progress: DownloadProgress) {
        _activeDownloads.update { it + (progress.regionId to progress) }
        
        if (progress.isComplete) {
            offlineRegionDao.updateDownloadStatus(
                id = progress.regionId,
                status = DownloadStatus.COMPLETE.name,
                size = progress.completedBytes,
                downloadedAt = System.currentTimeMillis()
            )
            _activeDownloads.update { it - progress.regionId }
        } else if (progress.errorMessage != null) {
            offlineRegionDao.updateDownloadStatus(
                id = progress.regionId,
                status = DownloadStatus.ERROR.name,
                size = null,
                downloadedAt = null
            )
            _activeDownloads.update { it - progress.regionId }
        }
    }

    override fun getAllRegions(): Flow<List<OfflineRegion>> {
        return offlineRegionDao.getAllRegions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun downloadRegion(region: OfflineRegion): Flow<DownloadProgress> {
        return offlineMapDataSource.downloadRegion(region)
            .onStart {
                offlineRegionDao.updateDownloadStatus(
                    id = region.id,
                    status = DownloadStatus.DOWNLOADING.name,
                    size = null,
                    downloadedAt = null
                )
            }
            .onEach { progress ->
                if (progress.maplibreRegionId != null) {
                    offlineRegionDao.updateMaplibreId(region.id, progress.maplibreRegionId)
                }
                updateProgress(progress)
            }
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
            observeExistingDownload(entity.toDomain(), maplibreRegionId)
        }
    }

    override suspend fun deleteRegion(id: Long) {
        try {
            val region = offlineRegionDao.getRegionById(id).first()
            if (region != null && region.maplibreRegionId != null) {
                offlineMapDataSource.deleteRegion(region.maplibreRegionId)
            }
        } catch (e: Exception) {
            // Ignore if missing, proceed to delete metadata
        }
        offlineRegionDao.deleteById(id)
        _activeDownloads.update { it - id }
    }

    override suspend fun insertRegion(region: OfflineRegion): Long {
        return offlineRegionDao.insert(region.toEntity())
    }
}
