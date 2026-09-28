package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.model.practice.Practice
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.model.practice.PracticeVisitResult
import com.dororong.rodi.core.domain.model.practice.SkipReasonForm
import com.dororong.rodi.core.domain.repository.PracticeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PracticeUseCasesTest {
    private val repository = mockk<PracticeRepository>()

    @Test
    fun `연습 등록은 장소 id를 저장소에 넘긴다`() = runTest {
        val practice = Practice(3, PracticeStatus.PLANNED, 0, 0)
        coEvery { repository.register(10) } returns practice

        assertEquals(practice, RegisterPracticeUseCase(repository)(10).getOrThrow())
        coVerify(exactly = 1) { repository.register(10) }
    }

    @Test
    fun `방문 기록은 기본으로 거리 없이 요청한다`() = runTest {
        val result = PracticeVisitResult(1, 0, 0, false, 0.0, false, null)
        coEvery { repository.recordVisit(3, null) } returns result

        assertEquals(result, RecordPracticeVisitUseCase(repository)(3).getOrThrow())
        coVerify(exactly = 1) { repository.recordVisit(3, null) }
    }

    @Test
    fun `미방문 사유 제출은 코드와 상세 내용을 저장소에 넘긴다`() = runTest {
        coEvery { repository.submitSkipReason(3, "OTHER", "사유") } returns Unit

        assertTrue(SubmitSkipReasonUseCase(repository)(3, "OTHER", "사유").isSuccess)
        coVerify(exactly = 1) { repository.submitSkipReason(3, "OTHER", "사유") }
    }

    @Test
    fun `미방문 사유 폼 조회를 저장소에 한 번 위임한다`() = runTest {
        val form = SkipReasonForm("id", "SINGLE_SELECT", "title", null, true, emptyList())
        coEvery { repository.getSkipReasonForm() } returns form

        assertEquals(form, GetSkipReasonFormUseCase(repository)().getOrThrow())
        coVerify(exactly = 1) { repository.getSkipReasonForm() }
    }
}
