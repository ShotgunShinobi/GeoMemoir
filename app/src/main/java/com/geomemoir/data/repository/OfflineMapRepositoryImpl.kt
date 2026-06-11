package com.geomemoir.data.repository

import com.geomemoir.data.db.dao.OfflineRegionDao
import com.geomemoir.data.db.mapper.toDomain
import com.geomemoir.data.db.mapper.toEntity
import com.geomemoir.data.map.OfflineMapDataSource
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.DownloadStatus
import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.domain.repository.OfflineMapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMapRepositoryImpl @Inject constructor(
    private val offlineRegionDao: OfflineRegionDao,
    private val offlineMapDataSource: OfflineMapDataSource
) : OfflineMapRepository {

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

                if (progress.isComplete) {
                    offlineRegionDao.updateDownloadStatus(
                        id = region.id,
                        status = DownloadStatus.COMPLETE.name,
                        size = progress.completedBytes,
                        downloadedAt = System.currentTimeMillis()
                    )
                } else if (progress.errorMessage != null) {
                    offlineRegionDao.updateDownloadStatus(
                        id = region.id,
                        status = DownloadStatus.ERROR.name,
                        size = null,
                        downloadedAt = null
                    )
                }
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
        }
    }

    override suspend fun deleteRegion(id: Long) {
        try {
            val region = offlineRegionDao.getRegionById(id).map { it?.toDomain() }.first()
            if (region != null && region.maplibreRegionId != null) {
                offlineMapDataSource.deleteRegion(region.maplibreRegionId)
            }
        } catch (e: Exception) {
            // Ignore if missing, proceed to delete metadata
        }
        offlineRegionDao.deleteById(id)
    }

    override suspend fun insertRegion(region: OfflineRegion): Long {
        return offlineRegionDao.insert(region.toEntity())
    }
}
