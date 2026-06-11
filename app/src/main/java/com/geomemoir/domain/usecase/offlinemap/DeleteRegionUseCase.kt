package com.geomemoir.domain.usecase.offlinemap

import com.geomemoir.domain.repository.OfflineMapRepository
import javax.inject.Inject

class DeleteRegionUseCase @Inject constructor(
    private val repository: OfflineMapRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "Invalid region ID" }
        repository.deleteRegion(id)
    }
}
