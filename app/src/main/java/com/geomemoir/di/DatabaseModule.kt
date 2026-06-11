package com.geomemoir.di

import android.content.Context
import androidx.room.Room
import com.geomemoir.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "geomemoir.db")
            .build()

    @Provides
    fun providePlaceDao(db: AppDatabase) = db.placeDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase) = db.categoryDao()

    @Provides
    fun provideOfflineRegionDao(db: AppDatabase) = db.offlineRegionDao()
}
