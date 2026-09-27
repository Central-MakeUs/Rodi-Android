package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.data.di.NetworkModule
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AccountRestoreMapperTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `locked account is a login and restore result instead of an unknown error`() {
        val response = json.decodeFromString<SocialLoginResponse>(LOCKED_RESPONSE)

        assertDoesNotThrow { response.toLoginResult() }
        assertDoesNotThrow { response.toAccountRestoreResult() }
    }

    @Test
    fun `locked account carries the server re-registration time as the only date`() {
        val response = json.decodeFromString<SocialLoginResponse>(LOCKED_RESPONSE)
        // 오프셋 없는 서버 시각은 서비스 시간대(KST)로 해석한다.
        val expected = Instant.parse("2026-09-20T03:00:00Z")

        assertEquals(LoginResult.WithdrawalLocked(reRegisterableAt = expected), response.toLoginResult())
        assertEquals(AccountRestoreResult.WithdrawalLocked(reRegisterableAt = expected), response.toAccountRestoreResult())
    }

    @Test
    fun `locked account keeps an offset re-registration time`() {
        val response = SocialLoginResponse(
            status = "WITHDRAWAL_LOCKED",
            isOnboarded = false,
            isCourseTutorialCompleted = false,
            reRegisterableAt = "2026-09-20T12:00:00+09:00",
        )

        assertEquals(
            LoginResult.WithdrawalLocked(reRegisterableAt = Instant.parse("2026-09-20T03:00:00Z")),
            response.toLoginResult(),
        )
    }

    @Test
    fun `locked account stays locked when the re-registration time is missing or unusable`() {
        listOf(null, "invalid").forEach { value ->
            val response = SocialLoginResponse(
                status = "WITHDRAWAL_LOCKED",
                isOnboarded = false,
                isCourseTutorialCompleted = false,
                reRegisterableAt = value,
            )

            assertEquals(LoginResult.WithdrawalLocked(reRegisterableAt = null), response.toLoginResult(), value)
            assertEquals(AccountRestoreResult.WithdrawalLocked(reRegisterableAt = null), response.toAccountRestoreResult(), value)
        }
    }

    @Test
    fun `maps success status to restored result`() {
        val response = SocialLoginResponse(
            status = "SUCCESS",
            isNewMember = false,
            isCourseTutorialCompleted = false,
            isOnboarded = true,
            nickname = "로디",
        )

        val result = response.toAccountRestoreResult()

        assertEquals(AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디"), result)
    }

    @Test
    fun `maps server onboarding state instead of whether the member is new`() {
        val response = SocialLoginResponse(
            status = "SUCCESS",
            accessToken = "access",
            refreshToken = "refresh",
            isNewMember = false,
            isOnboarded = false,
            isCourseTutorialCompleted = false,
            nickname = "로디",
        )

        assertEquals(LoginResult.Success(isOnboarded = false, nickname = "로디"), response.toLoginResult())
        assertEquals(AccountRestoreResult.Restored(isOnboarded = false, nickname = "로디"), response.toAccountRestoreResult())
    }

    @Test
    fun `maps withdrawal pending timestamps to domain result`() {
        val response = SocialLoginResponse(
            status = "WITHDRAWAL_PENDING",
            isCourseTutorialCompleted = false,
            isOnboarded = false,
            withdrawalRequestedAt = "2026-07-13T00:00:00+09:00",
            recoverableUntil = "2026-07-16T00:00:00+09:00",
        )

        val result = response.toAccountRestoreResult()

        assertEquals(
            AccountRestoreResult.WithdrawalPending(
                withdrawalRequestedAt = Instant.parse("2026-07-12T15:00:00Z"),
                recoverableUntil = Instant.parse("2026-07-15T15:00:00Z"),
            ),
            result,
        )
    }

    @Test
    fun `rejects success response without required tokens`() {
        val response = SocialLoginResponse(
            status = "SUCCESS",
            isNewMember = false,
            isCourseTutorialCompleted = null,
            isOnboarded = true,
            nickname = "로디",
        )

        val exception = assertThrows(AuthException.Unknown::class.java) { response.toAuthTokenResponse() }

        assertTrue(exception.message!!.contains("accessToken"))
    }

    @Test
    fun `rejects successful token response without tutorial flag`() {
        val response = SocialLoginResponse(
            status = "SUCCESS",
            accessToken = "access",
            refreshToken = "refresh",
            isNewMember = false,
            isCourseTutorialCompleted = null,
            isOnboarded = true,
            nickname = "로디",
        )

        val exception = assertThrows(AuthException.Unknown::class.java) { response.toAuthTokenResponse() }

        assertTrue(exception.message!!.contains("isCourseTutorialCompleted"))
    }

    @Test
    fun `rejects unsupported restore status`() {
        val response = SocialLoginResponse(status = "LOCKED", isOnboarded = false, isCourseTutorialCompleted = false)

        val exception = assertThrows(AuthException.Unknown::class.java) { response.toAccountRestoreResult() }

        assertTrue(exception.message!!.contains("복구 응답 상태"))
    }

    @Test
    fun `maps offset-less withdrawal timestamps so recovery stays reachable`() {
        val response = SocialLoginResponse(
            status = "WITHDRAWAL_PENDING",
            isCourseTutorialCompleted = false,
            isOnboarded = false,
            withdrawalRequestedAt = "2026-07-13T00:00:00.996642",
            recoverableUntil = "2026-07-16T00:00:00",
        )

        val result = response.toLoginResult()

        assertEquals(
            LoginResult.WithdrawalPending(
                withdrawalRequestedAt = Instant.parse("2026-07-12T15:00:00.996642Z"),
                recoverableUntil = Instant.parse("2026-07-15T15:00:00Z"),
            ),
            result,
        )
    }

    @Test
    fun `keeps withdrawal pending result when timestamps are unusable`() {
        val response = SocialLoginResponse(
            status = "WITHDRAWAL_PENDING",
            isCourseTutorialCompleted = false,
            isOnboarded = false,
            withdrawalRequestedAt = "invalid",
            recoverableUntil = null,
        )

        val result = response.toLoginResult()

        assertEquals(LoginResult.WithdrawalPending(null, null), result)
    }

    private companion object {
        // 서버 계약(2026-09-27 Swagger): 200 + status=WITHDRAWAL_LOCKED, 토큰·닉네임은 null, reRegisterableAt만 온다.
        const val LOCKED_RESPONSE = """{"status":"WITHDRAWAL_LOCKED","accessToken":null,"refreshToken":null,""" +
            """"isNewMember":false,"isOnboarded":false,"isCourseTutorialCompleted":false,"nickname":null,""" +
            """"withdrawalRequestedAt":"2026-09-10T12:00:00","recoverableUntil":null,"reRegisterableAt":"2026-09-20T12:00:00"}"""
    }
}
