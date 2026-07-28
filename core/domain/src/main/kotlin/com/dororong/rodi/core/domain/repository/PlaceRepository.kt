package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.course.GeoPoint
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    suspend fun getCoordinates(): List<PlaceCoordinate>
    suspend fun refreshCoordinates(): List<PlaceCoordinate> = getCoordinates()
    suspend fun getPlaces(query: PlaceViewportQuery, cursor: String?, size: Int): CursorPage<PlaceSummary>
    suspend fun searchPlaces(keyword: String, origin: GeoPoint, cursor: String?, size: Int): CursorPage<PlaceSummary>
    suspend fun refreshPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = getPlaces(query, cursor, size)
    suspend fun getPlaceDetail(placeId: Long): PlaceDetail
    suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary>
    suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean)
    fun observeSavedPlaces(): Flow<List<PlaceSummary>>
}
