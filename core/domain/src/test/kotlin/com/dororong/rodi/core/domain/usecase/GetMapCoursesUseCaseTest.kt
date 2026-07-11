package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetMapCoursesUseCaseTest {
    private val repository = mockk<CourseRepository>()
    private val useCase = GetMapCoursesUseCase(repository)
    private val query = MapViewportQuery(
        northEast = GeoPoint(38.0, 128.0),
        southWest = GeoPoint(37.0, 127.0),
        zoomLevel = 13,
    )

    @Test
    fun `returns repository failure as result`() = runTest {
        val error = IllegalStateException("failed")
        coEvery { repository.getCourses(query) } throws error

        assertSame(error, useCase(query).exceptionOrNull())
    }

    @Test
    fun `rethrows cancellation`() = runTest {
        coEvery { repository.getCourses(query) } throws CancellationException("cancelled")

        assertThrows<CancellationException> {
            useCase(query)
        }
    }
}
