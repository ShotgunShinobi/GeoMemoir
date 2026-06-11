package com.geomemoir.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.geomemoir.data.db.entity.OfflineRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRegionDao {
    @Query("SELECT * FROM offline_region_meta ORDER BY id DESC")
    fun getAllRegions(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_region_meta WHERE id = :id")
    fun getRegionById(id: Long): Flow<OfflineRegionEntity?>

    @Query("SELECT * FROM offline_region_meta WHERE maplibre_region_id = :maplibreRegionId")
    suspend fun getRegionByMaplibreId(maplibreRegionId: Long): OfflineRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(region: OfflineRegionEntity): Long

    @Update
    suspend fun update(region: OfflineRegionEntity)

    @Query("DELETE FROM offline_region_meta WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE offline_region_meta SET status = :status, size_bytes = :size, downloaded_at = :downloadedAt WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: String, size: Long?, downloadedAt: Long?)

    @Query("UPDATE offline_region_meta SET maplibre_region_id = :maplibreId WHERE id = :id")
    suspend fun updateMaplibreId(id: Long, maplibreId: Long)
}
