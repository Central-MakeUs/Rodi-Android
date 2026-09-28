package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CachedPlaceRepositoryTest {
    @Test
    fun `좌표는 서버 캐시를 반환하기 전에 옛 샘플 데이터를 지운다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val coordinates = listOf(coordinate(1), coordinate(1), coordinate(2))
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.coordinates() } returns coordinates

        val result = CachedPlaceRepository(remote, cache).getCoordinates()

        assertEquals(listOf(1L, 2L), result.map(PlaceCoordinate::id))
        coVerify { cache.deleteSamples() }
        coVerify(exactly = 0) { remote.getCoordinates() }
    }

    @Test
    fun `좌표 갱신은 중복을 뺀 서버 데이터로 캐시를 한 번에 교체한다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val server = listOf(coordinate(2), coordinate(2), coordinate(3))
        coEvery { remote.getCoordinates() } returns server
        coEvery { cache.replaceCoordinates(any()) } returns Unit

        val result = CachedPlaceRepository(remote, cache).refreshCoordinates()

        assertEquals(listOf(2L, 3L), result.map(PlaceCoordinate::id))
        coVerify { cache.replaceCoordinates(match { it.map(PlaceCoordinate::id) == listOf(2L, 3L) }) }
    }

    @Test
    fun `캐시된 화면 영역 조회는 음수 샘플 id를 반환하지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } returns emptyList()

        val page = CachedPlaceRepository(remote, cache).getPlaces(query, null, 20)

        assertEquals(emptyList<PlaceSummary>(), page.items)
        coVerify { cache.deleteSamples() }
    }

    @Test
    fun `캐시된 화면 영역은 첫 페이지 크기를 제한하고 다음 커서에서 다시 보내지 않는다`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val query = viewportQuery()
        coEvery { cache.deleteSamples() } returns Unit
        coEvery { cache.summaries(query) } returns listOf(summary(1), summary(1), summary(2), summary(3))

        val firstPage = CachedPlaceRepository(remote, cache).getPlaces(query, null, 2)
        val nextPage = CachedPlaceRepository(remote, cache).getPlaces(query, "server-cursor", 2)

        assertEquals(listOf(1L, 2L), firstPage.items.map(PlaceSummary::id))
        assertEquals(emptyList<PlaceSummary>(), nextPage.items)
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
