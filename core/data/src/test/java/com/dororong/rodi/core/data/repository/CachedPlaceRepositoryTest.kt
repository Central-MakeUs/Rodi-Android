package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CachedPlaceRepositoryTest {
    @Test
    fun `새 범위와 필터는 이전 조건의 클러스터 캐시를 재사용하지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val query = viewportQuery()
        val repository = CachedPlaceRepository(remote, mockk(relaxed = true))
        val clusters = PlaceMarkerClusters(listOf(coordinate(1)), emptyList())
        coEvery { remote.getClusters(query) } returns clusters

        repository.refreshClusters(query)

        assertEquals(clusters, repository.getClusters(query))
        assertEquals(emptyList<PlaceCoordinate>(), repository.getClusters(query.copy(
            practiceTypes = setOf(PracticeType.PARKING),
        )).places)
        assertEquals(emptyList<PlaceCoordinate>(), repository.getClusters(query.copy(
            northEast = GeoPoint(38.5, 127.5),
        )).places)
    }

    @Test
    fun `실패한 클러스터 갱신은 이전 조건의 완전한 캐시를 유지한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val query = viewportQuery()
        val newQuery = query.copy(practiceTypes = setOf(PracticeType.PARKING))
        val clusters = PlaceMarkerClusters(listOf(coordinate(1)), emptyList())
        coEvery { remote.getClusters(query) } returns clusters
        coEvery { remote.getClusters(newQuery) } throws IllegalStateException("서버 오류")
        val repository = CachedPlaceRepository(remote, mockk(relaxed = true))
        repository.refreshClusters(query)

        assertThrows<IllegalStateException> { repository.refreshClusters(newQuery) }

        assertEquals(clusters, repository.getClusters(query))
        assertEquals(emptyList<PlaceCoordinate>(), repository.getClusters(newQuery).places)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `이전 범위의 늦은 클러스터 응답은 최신 범위의 캐시를 덮어쓰지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val oldQuery = viewportQuery()
        val newQuery = oldQuery.copy(practiceTypes = setOf(PracticeType.PARKING))
        val oldResponse = CompletableDeferred<PlaceMarkerClusters>()
        val newClusters = PlaceMarkerClusters(listOf(coordinate(2)), emptyList())
        coEvery { remote.getClusters(oldQuery) } coAnswers { oldResponse.await() }
        coEvery { remote.getClusters(newQuery) } returns newClusters
        val repository = CachedPlaceRepository(remote, mockk(relaxed = true))
        val oldRequest = async { repository.refreshClusters(oldQuery) }
        runCurrent()
        repository.refreshClusters(newQuery)

        oldResponse.complete(PlaceMarkerClusters(listOf(coordinate(1)), emptyList()))
        oldRequest.await()

        assertEquals(newClusters, repository.getClusters(newQuery))
        assertEquals(emptyList<PlaceCoordinate>(), repository.getClusters(oldQuery).places)
    }

    @Test
    fun `목록 조회는 범위 필터와 커서를 서버로 보내고 회원 순서를 유지한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val query = viewportQuery().copy(practiceTypes = setOf(PracticeType.PARKING))
        val serverPage = CursorPage(listOf(summary(3), summary(1)), true, "next", 5L)
        coEvery { remote.getPlaces(query, "previous", 2) } returns serverPage
        val cache = mockk<PlaceCacheLocalDataSource>()
        coEvery { cache.upsertSummaries(serverPage.items) } returns Unit
        val repository = CachedPlaceRepository(remote, cache)

        val page = repository.getPlaces(query, "previous", 2)
        val refreshed = repository.refreshPlaces(query, "previous", 2)

        assertEquals(serverPage, page)
        assertEquals(serverPage, refreshed)
        coVerify(exactly = 2) { remote.getPlaces(query, "previous", 2) }
        coVerify(exactly = 2) { cache.upsertSummaries(serverPage.items) }
    }

    @Test
    fun `첫 페이지의 네트워크 실패는 범위 안 미삭제 캐시를 OR 필터한 뒤 거리순으로 제한한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery().copy(practiceTypes = setOf(PracticeType.PARKING, PracticeType.STRAIGHT))
        val networkError = PlaceException.Network("연결 실패")
        coEvery { remote.getPlaces(query, null, 2) } throws networkError
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } returns listOf(
            summary(1).copy(practiceTypes = listOf(PracticeType.STRAIGHT), distanceFromMeMeters = 500),
            summary(2).copy(practiceTypes = listOf(PracticeType.PARKING), distanceFromMeMeters = 100),
            summary(3).copy(practiceTypes = listOf(PracticeType.LANE_CHANGE), distanceFromMeMeters = 10),
            summary(4).copy(practiceTypes = listOf(PracticeType.PARKING), distanceFromMeMeters = 1, isDeleted = true),
            summary(5).copy(practiceTypes = listOf(PracticeType.PARKING), distanceFromMeMeters = 0,
                point = GeoPoint(39.0, 126.9)),
            summary(6).copy(practiceTypes = listOf(PracticeType.STRAIGHT), distanceFromMeMeters = 200),
        )

        val page = CachedPlaceRepository(remote, cache).getPlaces(query, null, 2)

        assertEquals(listOf(2L, 6L), page.items.map(PlaceSummary::id))
        assertEquals(false, page.hasNext)
        assertEquals(null, page.nextCursor)
        assertEquals(null, page.totalCount)
        coVerify(exactly = 0) { cache.upsertSummaries(any()) }
    }

    @Test
    fun `필터 없는 오프라인 첫 페이지는 캐시의 모든 연습유형을 허용한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        coEvery { remote.getPlaces(query, null, 20) } throws PlaceException.Network("연결 실패")
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } returns listOf(
            summary(1).copy(practiceTypes = listOf(PracticeType.STRAIGHT), distanceFromMeMeters = 200),
            summary(2).copy(practiceTypes = listOf(PracticeType.PARKING), distanceFromMeMeters = 100),
        )

        val page = CachedPlaceRepository(remote, cache).refreshPlaces(query, null, 20)

        assertEquals(listOf(2L, 1L), page.items.map(PlaceSummary::id))
    }

    @Test
    fun `캐시가 비었거나 필터에 맞는 항목이 없으면 원래 네트워크 오류를 유지한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery().copy(practiceTypes = setOf(PracticeType.PARKING))
        val error = PlaceException.Network("연결 실패")
        coEvery { remote.getPlaces(query, null, 20) } throws error
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } returnsMany listOf(
            emptyList(),
            listOf(summary(1).copy(practiceTypes = listOf(PracticeType.STRAIGHT))),
        )
        val repository = CachedPlaceRepository(remote, cache)

        assertSame(error, assertThrows<PlaceException.Network> { repository.getPlaces(query, null, 20) })
        assertSame(error, assertThrows<PlaceException.Network> { repository.getPlaces(query, null, 20) })
    }

    @Test
    fun `다음 페이지 네트워크 실패는 첫 페이지 캐시를 가짜 다음 페이지로 반환하지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        val error = PlaceException.Network("연결 실패")
        coEvery { remote.getPlaces(query, "next", 20) } throws error

        assertSame(error, assertThrows<PlaceException.Network> {
            CachedPlaceRepository(remote, cache).getPlaces(query, "next", 20)
        })
        coVerify(exactly = 0) { cache.summaries(any()) }
    }

    @Test
    fun `권한 서버 계약 오류와 취소는 캐시로 대체하지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        val repository = CachedPlaceRepository(remote, cache)
        val errors = listOf(
            PlaceException.AuthenticationRequired("로그인 필요"),
            PlaceException.NotFound("장소 없음"),
            PlaceException.Unexpected("서버 계약 실패", IllegalArgumentException("필수 필드 누락")),
            CancellationException("cancelled"),
        )
        errors.forEach { error ->
            coEvery { remote.getPlaces(query, null, 20) } throws error
            assertSame(error, assertThrows<Throwable> { repository.getPlaces(query, null, 20) })
        }
        coVerify(exactly = 0) { cache.summaries(any()) }
    }

    @Test
    fun `목록 성공 시 캐시 저장 실패는 서버 결과를 가리지 않고 취소는 재전파한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        val page = CursorPage(listOf(summary(1)), false, null, 1L)
        coEvery { remote.getPlaces(query, null, 20) } returns page
        coEvery { cache.upsertSummaries(page.items) } throws IllegalStateException("저장소 오류")
        val repository = CachedPlaceRepository(remote, cache)
        assertEquals(page, repository.getPlaces(query, null, 20))

        val cancellation = CancellationException("cancelled")
        coEvery { cache.upsertSummaries(page.items) } throws cancellation
        assertSame(cancellation, assertThrows<CancellationException> { repository.getPlaces(query, null, 20) })
    }

    @Test
    fun `오프라인 캐시 읽기 실패는 원래 네트워크 오류를 유지하고 취소는 재전파한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        val networkError = PlaceException.Network("연결 실패")
        coEvery { remote.getPlaces(query, null, 20) } throws networkError
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } throws IllegalStateException("저장소 오류")
        val repository = CachedPlaceRepository(remote, cache)
        assertSame(networkError, assertThrows<PlaceException.Network> { repository.getPlaces(query, null, 20) })

        val cancellation = CancellationException("cancelled")
        coEvery { cache.summaries(query) } throws cancellation
        assertSame(cancellation, assertThrows<CancellationException> { repository.getPlaces(query, null, 20) })
    }

    @Test
    fun `클러스터 네트워크 실패는 완전히 같은 조건의 캐시만 반환한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        val clusters = PlaceMarkerClusters(listOf(coordinate(1)), emptyList())
        coEvery { remote.getClusters(query) } returns clusters
        val repository = CachedPlaceRepository(remote, cache)
        repository.refreshClusters(query)
        val error = PlaceException.Network("연결 실패")
        coEvery { remote.getClusters(any()) } throws error

        assertEquals(clusters, repository.refreshClusters(query))
        assertSame(error, assertThrows<PlaceException.Network> {
            repository.refreshClusters(query.copy(practiceTypes = setOf(PracticeType.PARKING)))
        })
        assertSame(error, assertThrows<PlaceException.Network> {
            repository.refreshClusters(query.copy(northEast = GeoPoint(38.5, 127.5)))
        })
        coVerify(exactly = 0) { cache.replaceCoordinates(any()) }
    }

    @Test
    fun `클러스터의 권한 계약 오류는 같은 조건의 캐시가 있어도 반환하지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val query = viewportQuery()
        val clusters = PlaceMarkerClusters(listOf(coordinate(1)), emptyList())
        coEvery { remote.getClusters(query) } returns clusters
        val repository = CachedPlaceRepository(remote, mockk(relaxed = true))
        repository.refreshClusters(query)
        val errors = listOf(
            PlaceException.AuthenticationRequired("로그인 필요"),
            PlaceException.Unexpected("서버 계약 실패"),
            CancellationException("cancelled"),
        )
        errors.forEach { error ->
            coEvery { remote.getClusters(query) } throws error
            assertSame(error, assertThrows<Throwable> { repository.refreshClusters(query) })
        }
    }

    private fun coordinate(id: Long) = PlaceCoordinate(
        id = id,
        type = PlaceType.COURSE,
        name = "장소 $id",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
    )

    private fun summary(id: Long) = PlaceSummary(
        id = id,
        type = PlaceType.COURSE,
        name = "장소 $id",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
        distanceFromMeMeters = null,
        practiceTypes = emptyList(),
        description = null,
        distanceMeters = null,
        capacity = null,
        openTime = null,
        isDeleted = false,
    )

    private fun viewportQuery() = PlaceViewportQuery(
        southWest = GeoPoint(37.0, 126.0),
        northEast = GeoPoint(38.0, 127.0),
        origin = GeoPoint(37.5, 126.5),
    )
}
