package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolveCourseLocationSelectionUseCaseTest {
    private val repository = mockk<CourseLocationRepository>()
    private val useCase = ResolveCourseLocationSelectionUseCase(repository)
    private val suggestion = CourseLocationSuggestion(
        id = "kakao-1",
        title = "합정역",
        address = "서울 마포구 합정동",
        point = null,
        kind = CourseLocationKind.PLACE,
    )

    @Test
    fun `returns the resolved suggestion from the repository`() = runTest {
        val resolved = suggestion.copy(point = GeoPoint(37.5495, 126.9139))
        coEvery { repository.resolveSelection(suggestion) } returns resolved

        assertEquals(resolved, useCase(suggestion).getOrThrow())
    }

    @Test
    fun `wraps repository failure as Result failure`() = runTest {
        coEvery { repository.resolveSelection(suggestion) } throws IllegalStateException("boom")

        assertTrue(useCase(suggestion).exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `rethrows cancellation instead of wrapping it`() {
        coEvery { repository.resolveSelection(suggestion) } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking { useCase(suggestion) }
        }
    }
}
