package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CachedPlaceRepository @Inject constructor(
    private val delegate: PlaceRepositoryImpl,
    private val placeCache: PlaceCacheLocalDataSource,
) : PlaceRepository {
    override suspend fun getCoordinates(): List<PlaceCoordinate> {
        placeCache.deleteSamples()
        return placeCache.coordinates().distinctBy(PlaceCoordinate::id)
    }

    override suspend fun refreshCoordinates(): List<PlaceCoordinate> {
        val coordinates = delegate.getCoordinates().distinctBy(PlaceCoordinate::id)
        placeCache.replaceCoordinates(coordinates)
        return coordinates
    }

    override suspend fun getPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        placeCache.deleteSamples()
        val items = if (cursor == null) {
            placeCache.summaries(query)
                .distinctBy(PlaceSummary::id)
                .take(size)
        } else {
            emptyList()
        }
        return CursorPage(
            items = items,
            hasNext = false,
            nextCursor = null,
            totalCount = null,
        )
    }

    override suspend fun refreshPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        val page = delegate.getPlaces(query, cursor, size)
        placeCache.upsertSummaries(page.items)
        return page.copy(items = page.items.distinctBy(PlaceSummary::id))
    }

    override suspend fun searchPlaces(
        keyword: String,
        origin: GeoPoint,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = delegate.searchPlaces(keyword, origin, cursor, size)

    override suspend fun relatedSearch(
        keyword: String,
        cursor: String?,
        size: Int,
    ): RelatedSearch = delegate.relatedSearch(keyword, cursor, size)

    override suspend fun getPlaceDetail(placeId: Long): PlaceDetail = delegate.getPlaceDetail(placeId)

    override suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary> =
        delegate.getSavedPlaces(cursor, size)

    override suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) =
        delegate.setBookmarked(place, bookmarked)

    override fun observeSavedPlaces(): Flow<List<PlaceSummary>> = delegate.observeSavedPlaces()
}
