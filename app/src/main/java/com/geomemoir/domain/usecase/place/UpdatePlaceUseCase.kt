package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.repository.PlaceRepository
import javax.inject.Inject

class UpdatePlaceUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(place: Place): Result<Unit> = runCatching {
        require(place.id > 0) { "Invalid place ID" }
        require(place.name.isNotBlank()) { "Place name cannot be empty" }
        require(place.latitude in -90.0..90.0) { "Invalid latitude" }
        require(place.longitude in -180.0..180.0) { "Invalid longitude" }
        repository.updatePlace(place)
    }
}
