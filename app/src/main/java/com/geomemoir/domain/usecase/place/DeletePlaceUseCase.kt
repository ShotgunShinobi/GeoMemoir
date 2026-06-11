package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.repository.PlaceRepository
import javax.inject.Inject

class DeletePlaceUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "Invalid place ID" }
        repository.deletePlace(id)
    }
}
