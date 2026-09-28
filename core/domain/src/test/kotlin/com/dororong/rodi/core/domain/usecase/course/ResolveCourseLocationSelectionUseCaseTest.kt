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
    fun `저장소가 확정한 검색 결과를 반환한다`() = runTest {
        val resolved = suggestion.copy(point = GeoPoint(37.5495, 126.9139))
        coEvery { repository.resolveSelection(suggestion) } returns resolved

        assertEquals(resolved, useCase(suggestion).getOrThrow())
    }

    @Test
    fun `저장소 실패를 Result 실패로 감싼다`() = runTest {
        coEvery { repository.resolveSelection(suggestion) } throws IllegalStateException("boom")

        assertTrue(useCase(suggestion).exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `취소는 감싸지 않고 다시 던진다`() {
        coEvery { repository.resolveSelection(suggestion) } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking { useCase(suggestion) }
        }
    }
}
