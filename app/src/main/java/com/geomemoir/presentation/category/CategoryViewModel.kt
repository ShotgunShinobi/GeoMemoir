package com.geomemoir.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.usecase.category.AddCategoryUseCase
import com.geomemoir.domain.usecase.category.DeleteCategoryUseCase
import com.geomemoir.domain.usecase.category.GetCategoriesUseCase
import com.geomemoir.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        getCategoriesUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            addCategoryUseCase(Category(name = name.trim(), colorHex = colorHex))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            updateCategoryUseCase(category)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            deleteCategoryUseCase(id)
        }
    }
}
