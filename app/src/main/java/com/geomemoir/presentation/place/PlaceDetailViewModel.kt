package com.geomemoir.presentation.place

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.repository.PlaceRepository
import com.geomemoir.domain.usecase.place.DeletePlaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val deletePlaceUseCase: DeletePlaceUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val placeId: Long = savedStateHandle.get<Long>("placeId") ?: -1L

    val placeDetails: StateFlow<PlaceWithCategory?> = placeRepository.getPlaceById(placeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun deletePlace(onComplete: () -> Unit) {
        viewModelScope.launch {
            deletePlaceUseCase(placeId)
            onComplete()
        }
    }
}
