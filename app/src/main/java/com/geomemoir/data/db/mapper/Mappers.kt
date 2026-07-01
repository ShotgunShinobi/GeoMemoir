package com.geomemoir.data.db.mapper

import com.geomemoir.data.db.entity.CategoryEntity
import com.geomemoir.data.db.entity.PlaceEntity
import com.geomemoir.data.db.entity.PlaceWithCategoryRelation
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.entity.Place
import com.geomemoir.domain.entity.PlaceWithCategory

fun PlaceEntity.toDomain() = Place(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    notes = notes,
    rating = rating,
    dateVisited = dateVisited,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Place.toEntity() = PlaceEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    notes = notes,
    rating = rating,
    dateVisited = dateVisited,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt
)

fun PlaceWithCategoryRelation.toDomain() = PlaceWithCategory(
    place = place.toDomain(),
    category = category?.toDomain()
)
