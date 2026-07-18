package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.data.source.local.sample.SamplePlaces
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Room 캐시를 먼저 읽고 서버 응답으로 갱신하는 PlaceRepository decorator. */
class CachedPlaceRepository @Inject constructor(
    private val delegate: PlaceRepositoryImpl,
    private val placeCache: PlaceCacheLocalDataSource,
    private val savedPlaceLocalDataSource: SavedPlaceLocalDataSource,
) : PlaceRepository {
    override suspend fun getCoordinates(): List<PlaceCoordinate> {
        placeCache.seedSamplesIfEmpty()
        return placeCache.coordinates()
    }

    override suspend fun refreshCoordinates(): List<PlaceCoordinate> {
        val coordinates = delegate.getCoordinates()
        placeCache.upsertCoordinates(coordinates)
        return getCoordinates()
    }

    override suspend fun getPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        placeCache.seedSamplesIfEmpty()
        return CursorPage(
            items = placeCache.summaries(query),
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
        return page.copy(
            items = placeCache.summaries(query),
        )
    }

    override suspend fun getPlaceDetail(placeId: Long): PlaceDetail =
        SamplePlaces.detail(placeId) ?: delegate.getPlaceDetail(placeId)

    override suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) {
        if (SamplePlaces.isSamplePlace(place.id)) {
            savedPlaceLocalDataSource.setBookmarked(place, bookmarked)
        } else {
            delegate.setBookmarked(place, bookmarked)
        }
    }

    override fun observeSavedPlaces(): Flow<List<PlaceSummary>> = delegate.observeSavedPlaces()
}
