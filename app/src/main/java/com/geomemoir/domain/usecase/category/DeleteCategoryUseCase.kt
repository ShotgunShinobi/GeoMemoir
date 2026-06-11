package com.geomemoir.domain.usecase.category

import com.geomemoir.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "Invalid category ID" }
        repository.deleteCategory(id)
    }
}
