package com.geomemoir.domain.entity

data class Place(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String? = null,
    val rating: Int? = null,            // 1-5; null = unrated
    val dateVisited: Long,              // epoch ms
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
