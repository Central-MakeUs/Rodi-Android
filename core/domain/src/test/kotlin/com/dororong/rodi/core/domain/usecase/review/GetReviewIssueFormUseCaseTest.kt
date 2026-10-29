package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.repository.ReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetReviewIssueFormUseCaseTest {
    private val reviews = mockk<ReviewRepository>()
    private val places = mockk<PlaceRepository>()
    private val form = ReviewIssueForm("코스 문제", listOf(ReviewIssueOption("OTHER", "기타", true, null, 30)))

    @Test
    fun `코스에 새 후기 폼이 있으면 문제 선택 항목을 제공한다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } returns form
        coEvery { places.getPlaceDetail(7) } returns place(PlaceType.COURSE)
        assertEquals(form, GetReviewIssueFormUseCase(reviews, places)(7).getOrThrow())
    }

    @Test
    fun `주차장에는 코스 문제 선택을 적용하지 않는다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } returns form
        coEvery { places.getPlaceDetail(7) } returns place(PlaceType.PARKING)
        assertNull(GetReviewIssueFormUseCase(reviews, places)(7).getOrThrow())
    }

    @Test
    fun `새 후기 폼이 없는 서버는 장소 상세를 추가 조회하지 않는다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } returns null
        assertNull(GetReviewIssueFormUseCase(reviews, places)(7).getOrThrow())
        coVerify(exactly = 0) { places.getPlaceDetail(any()) }
    }

    private fun place(type: PlaceType) = mockk<PlaceDetail>().also { every { it.type } returns type }
}
