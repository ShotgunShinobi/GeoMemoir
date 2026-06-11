package com.geomemoir.data.repository

import com.geomemoir.data.db.dao.CategoryDao
import com.geomemoir.data.db.mapper.toDomain
import com.geomemoir.data.db.mapper.toEntity
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCategoryById(id: Long): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { it?.toDomain() }
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
}
