package com.geomemoir.presentation.place

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.repository.PlaceRepository
import com.geomemoir.domain.usecase.category.GetCategoriesUseCase
import com.geomemoir.domain.usecase.place.AddPlaceUseCase
import com.geomemoir.domain.usecase.place.UpdatePlaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceFormState(
    val id: Long? = null,
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val rating: Int = 0, // 0 = unrated
    val dateVisited: Long = System.currentTimeMillis(),
    val categoryId: Long? = null,
    // Validation
    val nameError: String? = null,
    val locationError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class AddEditPlaceViewModel @Inject constructor(
    private val addPlaceUseCase: AddPlaceUseCase,
    private val updatePlaceUseCase: UpdatePlaceUseCase,
    private val placeRepository: PlaceRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        getCategoriesUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _formState = MutableStateFlow(PlaceFormState())
    val formState: StateFlow<PlaceFormState> = _formState

    init {
        val latStr: String? = savedStateHandle["lat"]
        val lngStr: String? = savedStateHandle["lng"]
        val placeId: Long? = savedStateHandle["placeId"]

        val lat = latStr?.toDoubleOrNull()
        val lng = lngStr?.toDoubleOrNull()

        if (placeId != null && placeId != -1L) {
            viewModelScope.launch {
                placeRepository.getPlaceById(placeId).firstOrNull()?.let { item ->
                    _formState.value = PlaceFormState(
                        id = item.place.id,
                        name = item.place.name,
                        latitude = item.place.latitude,
                        longitude = item.place.longitude,
                        notes = item.place.notes ?: "",
                        rating = item.place.rating ?: 0,
                        dateVisited = item.place.dateVisited,
                        categoryId = item.place.categoryId
                    )
                }
            }
        } else if (lat != null && lng != null) {
            _formState.update { it.copy(latitude = lat, longitude = lng) }
        }
    }

    fun onNameChange(value: String) =
        _formState.update { it.copy(name = value, nameError = null) }

    fun onNotesChange(value: String) = _formState.update { it.copy(notes = value) }
    fun onRatingChange(value: Int) = _formState.update { it.copy(rating = value) }
    fun onDateChange(epochMs: Long) = _formState.update { it.copy(dateVisited = epochMs) }
    fun onCategoryChange(id: Long?) = _formState.update { it.copy(categoryId = id) }

    fun save() {
        val s = _formState.value
        if (s.name.isBlank()) {
            _formState.update { it.copy(nameError = "Name required") }
            return
        }
        if (s.latitude == null || s.longitude == null) {
            _formState.update { it.copy(locationError = "Location required") }
            return
        }
        _formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val place = Place(
                id = s.id ?: 0,
                name = s.name.trim(),
                latitude = s.latitude,
                longitude = s.longitude,
                notes = s.notes.ifBlank { null },
                rating = if (s.rating == 0) null else s.rating,
                dateVisited = s.dateVisited,
                categoryId = s.categoryId
            )
            val result = if (s.id == null) addPlaceUseCase(place)
            else updatePlaceUseCase(place)
            _formState.update { it.copy(isSaving = false, saveSuccess = result.isSuccess) }
        }
    }
}
