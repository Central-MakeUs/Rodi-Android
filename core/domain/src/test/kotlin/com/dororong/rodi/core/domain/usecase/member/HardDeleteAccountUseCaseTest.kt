package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.onboarding.ClearOnboardingDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HardDeleteAccountUseCaseTest {
    @Test
    fun `returns success when repository hard deletes account`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.hardDelete() } returns HardDeleteResult(localCleanupSucceeded = true)
        coEvery { clearOnboardingData() } returns Unit

        val result = HardDeleteAccountUseCase(repository, clearOnboardingData)()

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.hardDelete()
            clearOnboardingData()
        }
    }

    @Test
    fun `does not clear local data when hard delete fails`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.hardDelete() } throws IllegalStateException("server error")

        val result = HardDeleteAccountUseCase(repository, clearOnboardingData)()

        assertTrue(result.isFailure)
        coVerify { repository.hardDelete() }
        coVerify(exactly = 0) { clearOnboardingData() }
    }

    @Test
    fun `propagates cancellation without clearing local data`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.hardDelete() } throws CancellationException("cancelled")

        try {
            HardDeleteAccountUseCase(repository, clearOnboardingData)()
        } catch (_: CancellationException) {
            coVerify { repository.hardDelete() }
            coVerify(exactly = 0) { clearOnboardingData() }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }

    @Test
    fun `still returns success when local onboarding cleanup fails after hard delete`() = runTest {
        // 계정 삭제(되돌릴 수 없음)는 이미 끝났다. 그 뒤 로컬 정리 실패를 "삭제 실패"로
        // 보고하면 사용자가 이미 지워진 계정으로 재시도를 반복하게 된다.
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.hardDelete() } returns HardDeleteResult(localCleanupSucceeded = true)
        coEvery { clearOnboardingData() } throws IllegalStateException("local cleanup failed")

        val result = HardDeleteAccountUseCase(repository, clearOnboardingData)()

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.hardDelete()
            clearOnboardingData()
        }
    }

    @Test
    fun `propagates cancellation from local onboarding cleanup`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.hardDelete() } returns HardDeleteResult(localCleanupSucceeded = true)
        coEvery { clearOnboardingData() } throws CancellationException("cancelled")

        try {
            HardDeleteAccountUseCase(repository, clearOnboardingData)()
        } catch (_: CancellationException) {
            coVerifyOrder {
                repository.hardDelete()
                clearOnboardingData()
            }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
