package com.geomemoir.data.db.mapper

import com.geomemoir.data.db.entity.CategoryEntity
import com.geomemoir.data.db.entity.OfflineRegionEntity
import com.geomemoir.data.db.entity.PlaceEntity
import com.geomemoir.data.db.entity.PlaceWithCategoryRelation
import com.geomemoir.domain.entity.BoundingBox
import com.geomemoir.domain.entity.Category
import com.geomemoir.domain.entity.DownloadStatus
import com.geomemoir.domain.entity.OfflineRegion
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

fun OfflineRegionEntity.toDomain() = OfflineRegion(
    id = id,
    name = name,
    boundingBox = BoundingBox(minLat, maxLat, minLng, maxLng),
    minZoom = minZoom,
    maxZoom = maxZoom,
    maplibreRegionId = maplibreRegionId,
    downloadedAt = downloadedAt,
    sizeBytes = sizeBytes,
    status = try {
        DownloadStatus.valueOf(status)
    } catch (e: Exception) {
        DownloadStatus.PENDING
    }
)

fun OfflineRegion.toEntity() = OfflineRegionEntity(
    id = id,
    name = name,
    minLat = boundingBox.minLat,
    maxLat = boundingBox.maxLat,
    minLng = boundingBox.minLng,
    maxLng = boundingBox.maxLng,
    minZoom = minZoom,
    maxZoom = maxZoom,
    maplibreRegionId = maplibreRegionId,
    downloadedAt = downloadedAt,
    sizeBytes = sizeBytes,
    status = status.name
)
