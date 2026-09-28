package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.OnboardingPreferences
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OnboardingRepositoryImplTest {
    @Test
    fun `초기화는 로컬 저장소에 위임한다`() = runTest {
        val prefs = preferences()
        val repository = repository(mockk(), mockk(), prefs = prefs)

        repository.clear()

        coVerify { prefs.clear() }
    }

    @Test
    fun `온보딩을 제출하면 요청을 보내고 동기화 대기를 해제한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val prefs = preferences()
        coEvery { tokenStore.getTokens() } returns tokens("access-token")
        coEvery { onboardingApi.submit(any()) } returns successResponse()
        val repository = repository(onboardingApi, tokenStore, prefs = prefs)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify {
            onboardingApi.submit(
                match { it.drivingPeriod == "MONTHS_1_2" && it.level == "ROOKIE" },
            )
        }
        coVerify { prefs.authorizeSync() }
        coVerify { prefs.clearSyncPending() }
    }

    @Test
    fun `세션이 없으면 API 없이 로컬에서 제출을 완료한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val prefs = preferences()
        coEvery { tokenStore.getTokens() } returns null
        val repository = repository(onboardingApi, tokenStore, prefs = prefs)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify(exactly = 0) { onboardingApi.submit(any()) }
        coVerify(exactly = 0) { prefs.authorizeSync() }
        coVerify(exactly = 0) { prefs.clearSyncPending() }
    }

    @Test
    fun `이미 온보딩한 회원의 충돌 응답은 완료로 처리한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any()) } throws httpException(409)
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.AlreadyCompleted, result)
    }

    @Test
    fun `실패 응답 봉투를 오류 코드별 결과로 매핑한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any()) } returns ApiEnvelope<JsonObject>(
            isSuccess = false,
            code = "COMMON_400",
            message = "잘못된 요청입니다.",
        )
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.InvalidProfile, result)
    }

    @Test
    fun `실패 응답 봉투의 인증 실패와 권한 없음과 요청 제한과 기타 오류를 각각 매핑한다`() = runTest {
        val cases = listOf(
            "COMMON_401" to OnboardingSubmissionResult.AuthenticationRequired,
            "COMMON_403" to OnboardingSubmissionResult.Forbidden,
            "COMMON_429" to OnboardingSubmissionResult.RateLimited,
            "COMMON_500" to OnboardingSubmissionResult.UnexpectedFailure,
        )

        cases.forEach { (code, expected) ->
            val onboardingApi = mockk<OnboardingApi>()
            val tokenStore = mockk<AuthTokenStore>()
            coEvery { tokenStore.getTokens() } returns tokens()
            coEvery { onboardingApi.submit(any()) } returns ApiEnvelope<JsonObject>(
                isSuccess = false,
                code = code,
                message = "실패",
            )
            val repository = repository(onboardingApi, tokenStore)

            assertEquals(expected, repository.submit(profile(), OnboardingLevel.ROOKIE))
        }
    }

    @Test
    fun `토큰 재발급이 실패하면 로그인이 필요하다는 결과를 반환한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any()) } throws httpException(401)
        coEvery { authRepository.reissueToken() } throws AuthException.SessionRevoked("refresh failed")
        val repository = repository(onboardingApi, tokenStore, authRepository)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.AuthenticationRequired, result)
        coVerify(exactly = 1) { onboardingApi.submit(any()) }
    }

    @Test
    fun `클라이언트 오류와 서버 오류와 요청 제한을 각각 매핑한다`() = runTest {
        val cases = listOf(
            400 to OnboardingSubmissionResult.InvalidProfile,
            403 to OnboardingSubmissionResult.Forbidden,
            429 to OnboardingSubmissionResult.RateLimited,
            500 to OnboardingSubmissionResult.RetryableFailure,
        )

        cases.forEach { (statusCode, expected) ->
            val onboardingApi = mockk<OnboardingApi>()
            val tokenStore = mockk<AuthTokenStore>()
            coEvery { tokenStore.getTokens() } returns tokens()
            coEvery { onboardingApi.submit(any()) } throws httpException(statusCode)
            val repository = repository(onboardingApi, tokenStore)

            assertEquals(expected, repository.submit(profile(), OnboardingLevel.ROOKIE))
        }
    }

    @Test
    fun `네트워크 오류는 재시도 가능한 결과로 매핑한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any()) } throws IOException("offline")
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.RetryableFailure, result)
    }

    @Test
    fun `온보딩 제출 중 취소를 그대로 전파한다`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any()) } throws CancellationException("cancelled")
        val repository = repository(onboardingApi, tokenStore)

        assertThrowsSuspend<CancellationException> {
            repository.submit(profile(), OnboardingLevel.ROOKIE)
        }
    }

    private fun repository(
        onboardingApi: OnboardingApi,
        tokenStore: AuthTokenStore,
        authRepository: AuthRepository = mockk(),
        prefs: OnboardingPreferences = preferences(),
    ): OnboardingRepositoryImpl {
        return OnboardingRepositoryImpl(prefs, onboardingApi, tokenStore)
    }

    private fun preferences(): OnboardingPreferences = mockk(relaxed = true) {
        every { profile } returns emptyFlow()
        every { isSyncPending } returns emptyFlow()
        every { isSyncAuthorized } returns emptyFlow()
    }

    private fun profile() = OnboardingProfile(
        drivingPeriod = DrivingPeriod.MONTHS_1_2,
    )

    private fun tokens(accessToken: String = "access-token") =
        AuthTokens(accessToken, "refresh-token", "kakao")

    private fun successResponse(): ApiEnvelope<JsonObject> = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
    )

    private fun httpException(statusCode: Int) = HttpException(
        Response.error<Unit>(
            statusCode,
            """{"code":"COMMON_$statusCode","message":"실패"}"""
                .toResponseBody("application/json".toMediaType()),
        ),
    )
}
