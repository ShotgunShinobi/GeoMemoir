package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    operator fun invoke(categoryId: Long? = null): Flow<List<PlaceWithCategory>> =
        if (categoryId == null || categoryId == -1L) repository.getAllPlaces()
        else repository.getPlacesByCategory(categoryId)
}
