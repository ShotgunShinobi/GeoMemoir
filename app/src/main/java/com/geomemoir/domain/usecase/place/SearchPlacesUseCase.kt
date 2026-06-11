package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    operator fun invoke(query: String): Flow<List<PlaceWithCategory>> {
        return repository.searchPlaces(query.trim())
    }
}
