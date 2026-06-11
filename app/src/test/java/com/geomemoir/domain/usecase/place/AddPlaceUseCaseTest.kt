package com.geomemoir.domain.usecase.place

import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddPlaceUseCaseTest {

    private lateinit var repository: FakePlaceRepository
    private lateinit var addPlaceUseCase: AddPlaceUseCase

    @Before
    fun setUp() {
        repository = FakePlaceRepository()
        addPlaceUseCase = AddPlaceUseCase(repository)
    }

    @Test
    fun invoke_withValidPlace_insertsAndReturnsSuccess() = runTest {
        val place = Place(
            name = "Colosseum",
            latitude = 41.8902,
            longitude = 12.4922,
            dateVisited = 1686399000000L
        )
        repository.insertedId = 101L

        val result = addPlaceUseCase(place)

        assertTrue(result.isSuccess)
        assertEquals(101L, result.getOrNull())
    }

    @Test
    fun invoke_withEmptyName_returnsFailure() = runTest {
        val place = Place(
            name = "  ",
            latitude = 41.8902,
            longitude = 12.4922,
            dateVisited = 1686399000000L
        )

        val result = addPlaceUseCase(place)

        assertTrue(result.isFailure)
        assertEquals("Place name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun invoke_withInvalidLatitude_returnsFailure() = runTest {
        val place = Place(
            name = "Colosseum",
            latitude = 95.0, // Invalid latitude (> 90.0)
            longitude = 12.4922,
            dateVisited = 1686399000000L
        )

        val result = addPlaceUseCase(place)

        assertTrue(result.isFailure)
        assertEquals("Invalid latitude", result.exceptionOrNull()?.message)
    }
}

class FakePlaceRepository : PlaceRepository {
    private val places = mutableListOf<Place>()
    var insertedId = 1L

    override fun getAllPlaces(): Flow<List<PlaceWithCategory>> = flowOf(emptyList())
    override fun getPlacesByCategory(categoryId: Long): Flow<List<PlaceWithCategory>> = flowOf(emptyList())
    override fun getPlaceById(id: Long): Flow<PlaceWithCategory?> = flowOf(null)
    override fun searchPlaces(query: String): Flow<List<PlaceWithCategory>> = flowOf(emptyList())

    override suspend fun insertPlace(place: Place): Long {
        places.add(place)
        return insertedId
    }

    override suspend fun updatePlace(place: Place) {
        val idx = places.indexOfFirst { it.id == place.id }
        if (idx != -1) places[idx] = place
    }

    override suspend fun deletePlace(id: Long) {
        places.removeIf { it.id == id }
    }
}
