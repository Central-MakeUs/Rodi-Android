package com.dororong.rodi.core.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dororong.rodi.core.data.source.local.database.entity.PlaceCoordinateEntity
import com.dororong.rodi.core.data.source.local.database.entity.PlaceSummaryEntity

@Dao
interface PlaceCacheDao {
    @Query("SELECT * FROM place_coordinates ORDER BY id")
    suspend fun getCoordinates(): List<PlaceCoordinateEntity>

    @Query("SELECT * FROM place_summaries WHERE latitude BETWEEN :southLatitude AND :northLatitude AND longitude BETWEEN :westLongitude AND :eastLongitude")
    suspend fun getSummariesInViewport(
        southLatitude: Double,
        northLatitude: Double,
        westLongitude: Double,
        eastLongitude: Double,
    ): List<PlaceSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoordinates(items: List<PlaceCoordinateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummaries(items: List<PlaceSummaryEntity>)

    @Query("DELETE FROM place_coordinates")
    suspend fun deleteCoordinates()

    @Query("DELETE FROM place_coordinates WHERE id < 0")
    suspend fun deleteSampleCoordinates()

    @Query("DELETE FROM place_summaries WHERE id < 0")
    suspend fun deleteSampleSummaries()

    @Transaction
    suspend fun replaceCoordinates(items: List<PlaceCoordinateEntity>) {
        deleteCoordinates()
        upsertCoordinates(items)
    }

    @Transaction
    suspend fun deleteSamples() {
        deleteSampleCoordinates()
        deleteSampleSummaries()
    }

    @Transaction
    suspend fun upsertSummariesWithCoordinates(
        summaries: List<PlaceSummaryEntity>,
        coordinates: List<PlaceCoordinateEntity>,
    ) {
        upsertSummaries(summaries)
        upsertCoordinates(coordinates)
    }

}
