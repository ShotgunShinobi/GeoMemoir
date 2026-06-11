package com.geomemoir.domain.repository

import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.entity.PlaceWithCategory
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun getAllPlaces(): Flow<List<PlaceWithCategory>>
    fun getPlacesByCategory(categoryId: Long): Flow<List<PlaceWithCategory>>
    fun getPlaceById(id: Long): Flow<PlaceWithCategory?>
    fun searchPlaces(query: String): Flow<List<PlaceWithCategory>>
    suspend fun insertPlace(place: Place): Long
    suspend fun updatePlace(place: Place)
    suspend fun deletePlace(id: Long)
}
