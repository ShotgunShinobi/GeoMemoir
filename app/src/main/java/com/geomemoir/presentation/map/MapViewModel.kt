package com.geomemoir.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.data.location.LocationDataSource
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.usecase.category.GetCategoriesUseCase
import com.geomemoir.domain.usecase.place.GetPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    getPlacesUseCase: GetPlacesUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    private val locationDataSource: LocationDataSource
) : ViewModel() {

    private val _activeCategoryId = MutableStateFlow<Long?>(null)
    val activeCategoryId: StateFlow<Long?> = _activeCategoryId

    val places: StateFlow<List<PlaceWithCategory>> = _activeCategoryId
        .flatMapLatest { catId -> getPlacesUseCase(catId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> =
        getCategoriesUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    private val _cameraEvents = MutableSharedFlow<LatLng>()
    val cameraEvents: SharedFlow<LatLng> = _cameraEvents

    private val _pendingLocation = MutableStateFlow<LatLng?>(null)
    val pendingLocation: StateFlow<LatLng?> = _pendingLocation

    fun filterByCategory(categoryId: Long?) {
        _activeCategoryId.value = categoryId
    }

    fun onMapLongPress(latLng: LatLng) {
        _pendingLocation.value = latLng
    }

    fun clearPendingLocation() {
        _pendingLocation.value = null
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            try {
                // Try for up to 3 times with a delay if location is not immediately available
                // (e.g. after toggling GPS)
                var loc: LatLng? = null
                repeat(3) {
                    loc = locationDataSource.getCurrentLocation()
                    if (loc != null) return@repeat
                    kotlinx.coroutines.delay(1000)
                }

                loc?.let {
                    _userLocation.value = it
                    _pendingLocation.value = it
                    _cameraEvents.emit(it)
                }
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
}
