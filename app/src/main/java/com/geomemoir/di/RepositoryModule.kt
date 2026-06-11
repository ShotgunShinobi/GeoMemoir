package com.geomemoir.di

import com.geomemoir.data.repository.CategoryRepositoryImpl
import com.geomemoir.data.repository.OfflineMapRepositoryImpl
import com.geomemoir.data.repository.PlaceRepositoryImpl
import com.geomemoir.domain.repository.CategoryRepository
import com.geomemoir.domain.repository.OfflineMapRepository
import com.geomemoir.domain.repository.PlaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindOfflineMapRepository(impl: OfflineMapRepositoryImpl): OfflineMapRepository
}
