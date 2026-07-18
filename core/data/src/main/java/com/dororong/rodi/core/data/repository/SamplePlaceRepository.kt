package com.dororong.rodi.core.data.repository

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

/** 개발 중 기존 목업 장소를 서버 응답에 추가하는 PlaceRepository decorator. */
class SamplePlaceRepository @Inject constructor(
    private val delegate: PlaceRepositoryImpl,
    private val savedPlaceLocalDataSource: SavedPlaceLocalDataSource,
) : PlaceRepository {
    override suspend fun getCoordinates(): List<PlaceCoordinate> =
        (delegate.getCoordinates() + SamplePlaces.coordinates()).distinctBy(PlaceCoordinate::id)

    override suspend fun getPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        val page = delegate.getPlaces(query, cursor, size)
        if (cursor != null) return page

        val samples = SamplePlaces.summaries(query)
        return page.copy(
            items = (page.items + samples).distinctBy(PlaceSummary::id),
            totalCount = page.totalCount?.plus(samples.size),
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
