package com.geomemoir.domain.usecase.category

import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddCategoryUseCaseTest {

    private lateinit var repository: FakeCategoryRepository
    private lateinit var addCategoryUseCase: AddCategoryUseCase

    @Before
    fun setUp() {
        repository = FakeCategoryRepository()
        addCategoryUseCase = AddCategoryUseCase(repository)
    }

    @Test
    fun invoke_withValidCategory_insertsAndReturnsSuccess() = runTest {
        val category = Category(
            name = "Museums",
            colorHex = "#AB47BC"
        )
        repository.insertedId = 55L

        val result = addCategoryUseCase(category)

        assertTrue(result.isSuccess)
        assertEquals(55L, result.getOrNull())
    }

    @Test
    fun invoke_withInvalidColorHex_returnsFailure() = runTest {
        val category = Category(
            name = "Museums",
            colorHex = "AB47BC" // Missing '#' prefix
        )

        val result = addCategoryUseCase(category)

        assertTrue(result.isFailure)
        assertEquals("Invalid hex color format", result.exceptionOrNull()?.message)
    }
}

class FakeCategoryRepository : CategoryRepository {
    private val categories = mutableListOf<Category>()
    var insertedId = 1L

    override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
    override fun getCategoryById(id: Long): Flow<Category?> = flowOf(categories.find { it.id == id })

    override suspend fun insertCategory(category: Category): Long {
        categories.add(category)
        return insertedId
    }

    override suspend fun updateCategory(category: Category) {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx != -1) categories[idx] = category
    }

    override suspend fun deleteCategory(id: Long) {
        categories.removeIf { it.id == id }
    }
}
