package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.AuthRepository
import com.dororong.rodi.core.domain.AuthException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginWithKakaoUseCaseTest {

    @Test
    fun `invoke returns success when repository login succeeds`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.loginWithKakao("access-token") } returns true
        val useCase = LoginWithKakaoUseCase(repository)

        val result = useCase("access-token")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        coVerify(exactly = 1) { repository.loginWithKakao("access-token") }
    }

    @Test
    fun `invoke wraps repository failure as Result failure`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.loginWithKakao("access-token") } throws AuthException.InvalidCredential("boom")
        val useCase = LoginWithKakaoUseCase(repository)

        val result = useCase("access-token")

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { repository.loginWithKakao("access-token") }
    }
}
