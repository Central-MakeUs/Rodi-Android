package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.PlaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchPlacesUseCaseTest {
    @Test
    fun `검색어를 정규화한 뒤 다음 페이지 커서와 필터를 유지한다`() = runTest {
        val repository = mockk<PlaceRepository>()
        val origin = GeoPoint(37.5, 126.9)
        val filter = setOf(PracticeType.PARKING, PracticeType.STRAIGHT)
        coEvery { repository.searchPlaces("서울 중구", origin, "next", 10, filter) } returns
            CursorPage<PlaceSummary>(emptyList(), false, null, null)

        val result = SearchPlacesUseCase(repository)("  서울 중구  ", origin, "next", 10, filter)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.searchPlaces("서울 중구", origin, "next", 10, filter) }
    }
}
