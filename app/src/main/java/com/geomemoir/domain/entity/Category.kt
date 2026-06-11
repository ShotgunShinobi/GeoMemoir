package com.geomemoir.domain.entity

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#607D8B",
    val iconName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
