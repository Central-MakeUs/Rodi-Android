package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HardDeleteAccountUseCaseTest {
    @Test
    fun `저장소가 보고한 로컬 정리 결과를 그대로 반환한다`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.hardDelete() } returns HardDeleteResult(localCleanupSucceeded = false)

        val result = HardDeleteAccountUseCase(repository)()

        assertEquals(HardDeleteResult(localCleanupSucceeded = false), result.getOrNull())
    }

    @Test
    fun `즉시 삭제가 실패하면 실패를 반환한다`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.hardDelete() } throws IllegalStateException("server error")

        val result = HardDeleteAccountUseCase(repository)()

        assertTrue(result.isFailure)
    }

    @Test
    fun `즉시 삭제 중 취소를 그대로 전파한다`() = runTest {
        val repository = mockk<MemberRepository>()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.hardDelete() } throws cancellation

        try {
            HardDeleteAccountUseCase(repository)()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
