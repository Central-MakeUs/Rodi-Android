package com.dororong.rodi.core.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_coordinates")
data class PlaceCoordinateEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
