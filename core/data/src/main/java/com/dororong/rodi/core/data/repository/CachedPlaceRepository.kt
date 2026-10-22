package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class CachedPlaceRepository @Inject constructor(
    private val delegate: PlaceRepositoryImpl,
    private val placeCache: PlaceCacheLocalDataSource,
) : PlaceRepository {
    private val clusterCacheMutex = Mutex()
    private var clusterCache: Pair<PlaceViewportQuery, PlaceMarkerClusters>? = null
    private var clusterRefreshVersion = 0L

    override suspend fun getClusters(query: PlaceViewportQuery): PlaceMarkerClusters = clusterCacheMutex.withLock {
        clusterCache?.takeIf { it.first == query }?.second ?: PlaceMarkerClusters(emptyList(), emptyList())
    }

    override suspend fun refreshClusters(query: PlaceViewportQuery): PlaceMarkerClusters {
        val cacheQuery = query.copy(practiceTypes = query.practiceTypes.toSet())
        val refreshVersion = clusterCacheMutex.withLock { ++clusterRefreshVersion }
        val clusters = try {
            delegate.getClusters(cacheQuery)
        } catch (error: PlaceException.Network) {
            return clusterCacheMutex.withLock {
                clusterCache?.takeIf { it.first == cacheQuery }?.second ?: throw error
            }
        }
        clusterCacheMutex.withLock {
            if (refreshVersion == clusterRefreshVersion) clusterCache = cacheQuery to clusters
        }
        return clusters
    }

    override suspend fun getPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = loadPlaces(query, cursor, size)

    override suspend fun refreshPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = loadPlaces(query, cursor, size)

    private suspend fun loadPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        val requestQuery = query.copy(practiceTypes = query.practiceTypes.toSet())
        val page = try {
            delegate.getPlaces(requestQuery, cursor, size)
        } catch (error: PlaceException.Network) {
            if (cursor != null) throw error
            return cachedPlaces(requestQuery, size, error)
        }
        try {
            placeCache.upsertSummaries(page.items)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "장소 목록 캐시를 저장하지 못했습니다.")
        }
        return page
    }

    private suspend fun cachedPlaces(
        query: PlaceViewportQuery,
        size: Int,
        networkError: PlaceException.Network,
    ): CursorPage<PlaceSummary> {
        val cached = try {
            placeCache.deleteSamples()
            placeCache.summaries(query)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "오프라인 장소 목록 캐시를 읽지 못했습니다.")
            throw networkError
        }
        val items = cached.asSequence()
            .filterNot(PlaceSummary::isDeleted)
            .filter { it.point.lat in query.southWest.lat..query.northEast.lat }
            .filter { it.point.lng in query.southWest.lng..query.northEast.lng }
            .filter { place -> query.practiceTypes.isEmpty() || place.practiceTypes.any(query.practiceTypes::contains) }
            .distinctBy(PlaceSummary::id)
            .sortedBy { it.distanceFromMeMeters ?: Long.MAX_VALUE }
            .take(size.coerceAtLeast(0))
            .toList()
        if (items.isEmpty()) throw networkError
        return CursorPage(items, hasNext = false, nextCursor = null, totalCount = null)
    }

    override suspend fun searchPlaces(
        keyword: String,
        origin: GeoPoint,
        cursor: String?,
        size: Int,
        practiceTypes: Set<PracticeType>,
    ): CursorPage<PlaceSummary> = delegate.searchPlaces(keyword, origin, cursor, size, practiceTypes)

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
}
