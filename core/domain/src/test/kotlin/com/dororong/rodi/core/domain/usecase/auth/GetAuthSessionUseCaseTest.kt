package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAuthSessionUseCaseTest {
    @Test
    fun `returns auth session from repository`() = runTest {
        val repository = mockk<AuthRepository>()
        val expected = AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        coEvery { repository.getSession() } returns expected

        val actual = GetAuthSessionUseCase(repository)()

        assertEquals(expected, actual)
    }
}
