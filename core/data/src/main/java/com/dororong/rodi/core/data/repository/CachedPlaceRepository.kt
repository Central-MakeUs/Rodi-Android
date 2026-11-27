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
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class CachedPlaceRepository @Inject constructor(
    private val delegate: PlaceRepositoryImpl,
    private val placeCache: PlaceCacheLocalDataSource,
    private val clock: Clock = Clock.systemDefaultZone(),
) : PlaceRepository {
    private val detailCacheMutex = Mutex()
    private val detailCache = HashMap<Long, CachedDetail>()
    private val detailGenerations = HashMap<Long, Long>()

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

    /**
     * 같은 장소를 짧은 시간 안에 다시 열면 서버를 다시 부르지 않는다. 저장 상태를 바꾸면 버린다.
     * 저장 상태 변경과 겹친 조회는 바뀌기 전 값을 받았을 수 있어 캐시에 남기지 않는다.
     * 저장 상태는 사용자마다 다르므로 로그아웃·재로그인 뒤에는 이전 세션의 값을 쓰지 않는다.
     */
    override suspend fun getPlaceDetail(placeId: Long): PlaceDetail {
        val sessionId = delegate.currentSessionId()
        val now = clock.millis()
        val generation = detailCacheMutex.withLock {
            detailCache[placeId]
                ?.takeIf { it.sessionId == sessionId && now - it.cachedAt < DETAIL_CACHE_TTL_MILLIS }
                ?.let { return it.detail }
            detailGenerations[placeId] ?: 0L
        }
        val detail = delegate.getPlaceDetail(placeId)
        detailCacheMutex.withLock {
            if ((detailGenerations[placeId] ?: 0L) == generation) {
                detailCache[placeId] = CachedDetail(sessionId, clock.millis(), detail)
            }
        }
        return detail
    }

    override suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary> =
        delegate.getSavedPlaces(cursor, size)

    /** 서버 반영 뒤 로컬 저장이 실패하거나 취소돼도, 겹친 조회가 바뀌기 전 값을 캐시에 남기지 않게 한다. */
    override suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) {
        detailCacheMutex.withLock { detailCache.remove(place.id) }
        try {
            delegate.setBookmarked(place, bookmarked)
        } finally {
            withContext(NonCancellable) {
                detailCacheMutex.withLock {
                    detailGenerations[place.id] = (detailGenerations[place.id] ?: 0L) + 1
                    detailCache.remove(place.id)
                }
            }
        }
    }
}

private data class CachedDetail(
    val sessionId: String?,
    val cachedAt: Long,
    val detail: PlaceDetail,
)

private const val DETAIL_CACHE_TTL_MILLIS = 5 * 60 * 1000L
