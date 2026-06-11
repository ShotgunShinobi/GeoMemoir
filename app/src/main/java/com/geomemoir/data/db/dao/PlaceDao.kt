package com.geomemoir.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.geomemoir.data.db.entity.PlaceEntity
import com.geomemoir.data.db.entity.PlaceWithCategoryRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Transaction
    @Query("SELECT * FROM places ORDER BY date_visited DESC")
    fun getAllPlaces(): Flow<List<PlaceWithCategoryRelation>>

    @Transaction
    @Query("SELECT * FROM places WHERE category_id = :categoryId ORDER BY date_visited DESC")
    fun getPlacesByCategory(categoryId: Long): Flow<List<PlaceWithCategoryRelation>>

    @Transaction
    @Query("SELECT * FROM places WHERE id = :id")
    fun getPlaceById(id: Long): Flow<PlaceWithCategoryRelation?>

    @Transaction
    @Query("""
        SELECT * FROM places
        WHERE name LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%'
        ORDER BY date_visited DESC
    """)
    fun searchPlaces(q: String): Flow<List<PlaceWithCategoryRelation>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: PlaceEntity): Long

    @Update
    suspend fun update(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: Long)
}
