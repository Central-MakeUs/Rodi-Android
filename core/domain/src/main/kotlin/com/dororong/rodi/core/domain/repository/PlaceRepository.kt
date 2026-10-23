package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.search.RelatedSearch

interface PlaceRepository {
    suspend fun getClusters(query: PlaceViewportQuery): PlaceMarkerClusters
    suspend fun refreshClusters(query: PlaceViewportQuery): PlaceMarkerClusters = getClusters(query)
    suspend fun getPlaces(query: PlaceViewportQuery, cursor: String?, size: Int): CursorPage<PlaceSummary>
    suspend fun searchPlaces(
        keyword: String,
        origin: GeoPoint,
        cursor: String?,
        size: Int,
        practiceTypes: Set<PracticeType> = emptySet(),
    ): CursorPage<PlaceSummary>
    suspend fun relatedSearch(keyword: String, cursor: String?, size: Int): RelatedSearch
    suspend fun refreshPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = getPlaces(query, cursor, size)
    suspend fun getPlaceDetail(placeId: Long): PlaceDetail
    suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary>
    suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean)
}
