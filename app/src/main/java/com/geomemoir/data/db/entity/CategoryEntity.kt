package com.geomemoir.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "icon_name") val iconName: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
