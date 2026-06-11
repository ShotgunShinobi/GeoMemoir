package com.geomemoir.domain.usecase.offlinemap

import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.domain.repository.OfflineMapRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadRegionUseCase @Inject constructor(
    private val repository: OfflineMapRepository
) {
    operator fun invoke(region: OfflineRegion): Flow<DownloadProgress> {
        return repository.downloadRegion(region)
    }

    suspend fun createPendingRegion(region: OfflineRegion): Long {
        return repository.insertRegion(region)
    }
}
