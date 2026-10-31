package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.domain.repository.ReviewRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetReviewIssueFormUseCaseTest {
    private val reviews = mockk<ReviewRepository>()
    private val form = ReviewIssueForm("코스 문제", listOf(ReviewIssueOption("OTHER", "기타", true, null, 30)))

    @Test
    fun `서버가 주차장에도 문제 선택 폼을 제공하면 그대로 사용한다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } returns form
        assertEquals(form, GetReviewIssueFormUseCase(reviews)(7).getOrThrow())
    }

    @Test
    fun `새 후기 폼이 없는 서버에서는 기존 입력 흐름을 유지한다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } returns null
        assertNull(GetReviewIssueFormUseCase(reviews)(7).getOrThrow())
    }

    @Test
    fun `조회 실패는 기존 폼으로 대체하지 않고 실패로 전달한다`() = runTest {
        val error = IllegalStateException("form unavailable")
        coEvery { reviews.getReviewIssueForm(7) } throws error
        assertSame(error, GetReviewIssueFormUseCase(reviews)(7).exceptionOrNull())
    }

    @Test
    fun `조회 취소는 실패 결과로 감싸지 않고 전파한다`() = runTest {
        coEvery { reviews.getReviewIssueForm(7) } throws CancellationException()
        assertThrows<CancellationException> { GetReviewIssueFormUseCase(reviews)(7) }
    }
}
