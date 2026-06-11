package com.geomemoir.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.geomemoir.data.db.dao.CategoryDao
import com.geomemoir.data.db.dao.OfflineRegionDao
import com.geomemoir.data.db.dao.PlaceDao
import com.geomemoir.data.db.entity.CategoryEntity
import com.geomemoir.data.db.entity.OfflineRegionEntity
import com.geomemoir.data.db.entity.PlaceEntity

@Database(
    entities = [
        PlaceEntity::class,
        CategoryEntity::class,
        OfflineRegionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun offlineRegionDao(): OfflineRegionDao
}
