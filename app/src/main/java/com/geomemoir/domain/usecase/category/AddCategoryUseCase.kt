package com.geomemoir.domain.usecase.category

import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Long> = runCatching {
        require(category.name.isNotBlank()) { "Category name cannot be empty" }
        require(category.colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Invalid hex color format" }
        repository.insertCategory(category)
    }
}
