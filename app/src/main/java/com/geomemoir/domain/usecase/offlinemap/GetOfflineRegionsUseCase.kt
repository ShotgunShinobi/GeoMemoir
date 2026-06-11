package com.geomemoir.domain.usecase.offlinemap

import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.domain.repository.OfflineMapRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOfflineRegionsUseCase @Inject constructor(
    private val repository: OfflineMapRepository
) {
    operator fun invoke(): Flow<List<OfflineRegion>> {
        return repository.getAllRegions()
    }
}
