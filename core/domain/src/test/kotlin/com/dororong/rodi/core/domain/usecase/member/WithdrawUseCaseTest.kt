package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WithdrawUseCaseTest {
    @Test
    fun `저장소 탈퇴가 성공하면 성공을 반환한다`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } returns Unit

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `저장소 탈퇴가 실패하면 실패를 반환한다`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } throws IllegalStateException("server error")

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isFailure)
    }

    @Test
    fun `저장소 탈퇴 중 취소를 그대로 전파한다`() = runTest {
        val repository = mockk<MemberRepository>()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.withdraw() } throws cancellation

        try {
            WithdrawUseCase(repository)()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
