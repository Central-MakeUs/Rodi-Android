package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.database.PlaceCacheLocalDataSource
import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceType
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

    private fun coordinate(id: Long) = PlaceCoordinate(
        id = id,
        type = PlaceType.COURSE,
        name = "장소 $id",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
    )
}
