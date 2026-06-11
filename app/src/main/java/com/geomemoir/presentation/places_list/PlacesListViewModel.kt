package com.geomemoir.presentation.places_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.domain.usecase.category.GetCategoriesUseCase
import com.geomemoir.domain.usecase.place.GetPlacesUseCase
import com.geomemoir.domain.usecase.place.SearchPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlacesListViewModel @Inject constructor(
    private val getPlacesUseCase: GetPlacesUseCase,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeCategoryId = MutableStateFlow<Long?>(null)
    val activeCategoryId: StateFlow<Long?> = _activeCategoryId

    val categories: StateFlow<List<Category>> =
        getCategoriesUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val places: StateFlow<List<PlaceWithCategory>> = combine(
        _searchQuery,
        _activeCategoryId
    ) { query, catId ->
        Pair(query, catId)
    }.flatMapLatest { (query, catId) ->
        if (query.isBlank()) {
            getPlacesUseCase(catId)
        } else {
            searchPlacesUseCase(query).map { list ->
                if (catId != null) {
                    list.filter { it.place.categoryId == catId }
                } else {
                    list
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(categoryId: Long?) {
        _activeCategoryId.value = categoryId
    }
}
