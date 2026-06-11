package com.geomemoir.data.repository

import com.geomemoir.data.db.dao.PlaceDao
import com.geomemoir.data.db.mapper.toDomain
import com.geomemoir.data.db.mapper.toEntity
import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val placeDao: PlaceDao
) : PlaceRepository {

    override fun getAllPlaces(): Flow<List<PlaceWithCategory>> {
        return placeDao.getAllPlaces().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPlacesByCategory(categoryId: Long): Flow<List<PlaceWithCategory>> {
        return placeDao.getPlacesByCategory(categoryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPlaceById(id: Long): Flow<PlaceWithCategory?> {
        return placeDao.getPlaceById(id).map { it?.toDomain() }
    }

    override fun searchPlaces(query: String): Flow<List<PlaceWithCategory>> {
        return placeDao.searchPlaces(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertPlace(place: Place): Long {
        return placeDao.insert(place.toEntity())
    }

    override suspend fun updatePlace(place: Place) {
        placeDao.update(place.toEntity())
    }
    override suspend fun deletePlace(id: Long) {
        placeDao.deleteById(id)
    }
}
