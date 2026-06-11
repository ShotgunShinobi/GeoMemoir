package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.repository.PlaceRepository
import javax.inject.Inject

class AddPlaceUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(place: Place): Result<Long> = runCatching {
        require(place.name.isNotBlank()) { "Place name cannot be empty" }
        require(place.latitude in -90.0..90.0) { "Invalid latitude" }
        require(place.longitude in -180.0..180.0) { "Invalid longitude" }
        repository.insertPlace(place)
    }
}
