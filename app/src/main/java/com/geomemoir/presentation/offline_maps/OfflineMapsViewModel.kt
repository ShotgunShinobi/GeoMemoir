package com.geomemoir.presentation.offline_maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.domain.entity.BoundingBox
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.domain.usecase.offlinemap.DeleteRegionUseCase
import com.geomemoir.domain.usecase.offlinemap.DownloadRegionUseCase
import com.geomemoir.domain.usecase.offlinemap.GetOfflineRegionsUseCase
import com.geomemoir.domain.repository.OfflineMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineMapsViewModel @Inject constructor(
    private val getRegionsUseCase: GetOfflineRegionsUseCase,
    private val downloadRegionUseCase: DownloadRegionUseCase,
    private val deleteRegionUseCase: DeleteRegionUseCase,
    private val repository: OfflineMapRepository
) : ViewModel() {

    val regions: StateFlow<List<OfflineRegion>> =
        getRegionsUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadProgresses: StateFlow<Map<Long, DownloadProgress>> = repository.activeDownloads

    fun createAndDownloadRegion(name: String, minLat: Double, maxLat: Double, minLng: Double, maxLng: Double, maxZoom: Int) {
        viewModelScope.launch {
            val pendingRegion = OfflineRegion(
                name = name,
                boundingBox = BoundingBox(minLat, maxLat, minLng, maxLng),
                minZoom = 1,
                maxZoom = maxZoom
            )
            val generatedId = downloadRegionUseCase.createPendingRegion(pendingRegion)
            val finalRegion = pendingRegion.copy(id = generatedId)
            
            downloadRegionUseCase(finalRegion).collect { 
                // Progress is now tracked automatically by the repository
            }
        }
    }

    fun deleteRegion(id: Long) {
        viewModelScope.launch {
            deleteRegionUseCase(id)
        }
    }
}
