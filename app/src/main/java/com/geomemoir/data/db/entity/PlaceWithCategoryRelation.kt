package com.geomemoir.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlaceWithCategoryRelation(
    @Embedded val place: PlaceEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
