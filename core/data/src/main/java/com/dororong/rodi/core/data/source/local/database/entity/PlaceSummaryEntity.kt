package com.dororong.rodi.core.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_summaries")
data class PlaceSummaryEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val practiceTypes: String,
    val description: String?,
    val distanceMeters: Int?,
    val capacity: Int?,
    val openTime: String?,
)
