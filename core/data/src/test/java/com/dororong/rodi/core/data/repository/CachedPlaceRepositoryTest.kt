package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CachedPlaceRepositoryTest {
    @Test
    fun `coordinates return local cache before requesting server`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val saved = mockk<SavedPlaceLocalDataSource>()
        val localCoordinates = listOf(coordinate(1))
        coEvery { cache.seedSamplesIfEmpty() } returns Unit
        coEvery { cache.coordinates() } returns localCoordinates
        val repository = CachedPlaceRepository(remote, cache, saved)

        val result = repository.getCoordinates()

        assertEquals(localCoordinates, result)
        coVerify(exactly = 0) { remote.getCoordinates() }
    }

    @Test
    fun `coordinate refresh stores server result and returns refreshed cache`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val saved = mockk<SavedPlaceLocalDataSource>()
        val serverCoordinates = listOf(coordinate(2))
        coEvery { remote.getCoordinates() } returns serverCoordinates
        coEvery { cache.upsertCoordinates(serverCoordinates) } returns Unit
        coEvery { cache.seedSamplesIfEmpty() } returns Unit
        coEvery { cache.coordinates() } returns serverCoordinates
        val repository = CachedPlaceRepository(remote, cache, saved)

        val result = repository.refreshCoordinates()

        assertEquals(serverCoordinates, result)
        coVerify(exactly = 1) { cache.upsertCoordinates(serverCoordinates) }
    }

    @Test
    fun `server coordinate replaces only a sample at the same type and point`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val saved = mockk<SavedPlaceLocalDataSource>()
        val samePoint = GeoPoint(37.5, 126.9)
        val coordinates = listOf(
            coordinate(-1, samePoint),
            coordinate(10, samePoint),
            coordinate(11, samePoint),
            coordinate(-2, GeoPoint(37.6, 126.9)),
        )
        coEvery { cache.seedSamplesIfEmpty() } returns Unit
        coEvery { cache.coordinates() } returns coordinates

        val result = CachedPlaceRepository(remote, cache, saved).getCoordinates()

        assertEquals(listOf(10L, 11L, -2L), result.map(PlaceCoordinate::id))
    }

    @Test
    fun `server summary replaces the matching sample in viewport cache`() = runTest {
        val remote = mockk<PlaceRepositoryImpl>()
        val cache = mockk<PlaceCacheLocalDataSource>()
        val saved = mockk<SavedPlaceLocalDataSource>()
        val query = viewportQuery()
        val samePoint = GeoPoint(37.5, 126.9)
        coEvery { cache.seedSamplesIfEmpty() } returns Unit
        coEvery { cache.summaries(query) } returns listOf(
            summary(-1, samePoint),
            summary(10, samePoint),
            summary(-2, GeoPoint(37.6, 126.9)),
        )

        val result = CachedPlaceRepository(remote, cache, saved).getPlaces(query, null, 20)

        assertEquals(listOf(10L, -2L), result.items.map(PlaceSummary::id))
    }

    private fun coordinate(id: Long, point: GeoPoint = GeoPoint(37.5, 126.9)) = PlaceCoordinate(
        id = id,
        type = PlaceType.COURSE,
        name = "장소 $id",
        address = "서울",
        point = point,
    )

    private fun summary(id: Long, point: GeoPoint) = PlaceSummary(
        id = id,
        type = PlaceType.COURSE,
        name = "장소 $id",
        address = "서울",
        point = point,
        distanceFromMeMeters = 100,
        practiceTypes = listOf(PracticeType.STRAIGHT),
        description = null,
        distanceMeters = 1_000,
        capacity = null,
        openTime = null,
    )

    private fun viewportQuery() = PlaceViewportQuery(
        southWest = GeoPoint(37.0, 126.0),
        northEast = GeoPoint(38.0, 127.0),
        origin = GeoPoint(37.5, 126.5),
    )
}
