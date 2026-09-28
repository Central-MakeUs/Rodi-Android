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
    fun `재가입 대기 계정은 알 수 없는 오류가 아니라 로그인과 복구 결과로 매핑한다`() {
        val response = json.decodeFromString<SocialLoginResponse>(LOCKED_RESPONSE)

        assertDoesNotThrow { response.toLoginResult() }
        assertDoesNotThrow { response.toAccountRestoreResult() }
    }

    @Test
    fun `재가입 대기 계정은 서버의 재가입 가능 시각만 날짜로 전달한다`() {
        val response = json.decodeFromString<SocialLoginResponse>(LOCKED_RESPONSE)
        // 오프셋 없는 서버 시각은 서비스 시간대(KST)로 해석한다.
        val expected = Instant.parse("2026-09-20T03:00:00Z")

        assertEquals(LoginResult.WithdrawalLocked(reRegisterableAt = expected), response.toLoginResult())
        assertEquals(AccountRestoreResult.WithdrawalLocked(reRegisterableAt = expected), response.toAccountRestoreResult())
    }

    @Test
    fun `재가입 대기 계정은 오프셋이 있는 재가입 가능 시각을 그대로 해석한다`() {
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
    fun `재가입 가능 시각이 없거나 해석할 수 없어도 재가입 대기 상태를 유지한다`() {
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
    fun `성공 상태를 복구 완료 결과로 매핑한다`() {
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
    fun `신규 회원 여부가 아니라 서버의 온보딩 완료 여부를 매핑한다`() {
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
    fun `탈퇴 유예 시각을 도메인 결과로 매핑한다`() {
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
    fun `토큰이 없는 성공 응답은 거부한다`() {
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
    fun `튜토리얼 완료 여부가 없는 토큰 응답은 거부한다`() {
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
    fun `지원하지 않는 복구 상태는 거부한다`() {
        val response = SocialLoginResponse(status = "LOCKED", isOnboarded = false, isCourseTutorialCompleted = false)

        val exception = assertThrows(AuthException.Unknown::class.java) { response.toAccountRestoreResult() }

        assertTrue(exception.message!!.contains("복구 응답 상태"))
    }

    @Test
    fun `오프셋 없는 탈퇴 시각도 매핑해 복구 화면에 진입할 수 있다`() {
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
    fun `탈퇴 시각을 해석할 수 없어도 탈퇴 유예 결과를 유지한다`() {
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
