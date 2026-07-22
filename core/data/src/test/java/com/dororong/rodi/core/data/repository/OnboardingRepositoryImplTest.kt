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
    fun `submit sends bearer access token and onboarding request`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val prefs = preferences()
        coEvery { tokenStore.getTokens() } returns tokens("access-token")
        coEvery { onboardingApi.submit("Bearer access-token", any()) } returns successResponse()
        val repository = repository(onboardingApi, tokenStore, prefs = prefs)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify {
            onboardingApi.submit(
                "Bearer access-token",
                match { it.drivingPeriod == "MONTHS_1_3" && it.level == "ROOKIE" },
            )
        }
        coVerify { prefs.authorizeSync() }
        coVerify { prefs.clearSyncPending() }
    }

    @Test
    fun `submit skips api without a session and completes locally`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val prefs = preferences()
        coEvery { tokenStore.getTokens() } returns null
        val repository = repository(onboardingApi, tokenStore, prefs = prefs)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify(exactly = 0) { onboardingApi.submit(any(), any()) }
        coVerify(exactly = 0) { prefs.authorizeSync() }
        coVerify(exactly = 0) { prefs.clearSyncPending() }
    }

    @Test
    fun `submit treats already-onboarded conflict as completed`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } throws httpException(409)
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.AlreadyCompleted, result)
    }

    @Test
    fun `submit maps unsuccessful response envelope by error code`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } returns ApiEnvelope<JsonObject>(
            isSuccess = false,
            code = "COMMON_400",
            message = "잘못된 요청입니다.",
        )
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.InvalidProfile, result)
    }

    @Test
    fun `submit maps unsuccessful response envelope auth forbidden rate limit and unexpected errors`() = runTest {
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
            coEvery { onboardingApi.submit(any(), any()) } returns ApiEnvelope<JsonObject>(
                isSuccess = false,
                code = code,
                message = "실패",
            )
            val repository = repository(onboardingApi, tokenStore)

            assertEquals(expected, repository.submit(profile(), OnboardingLevel.ROOKIE))
        }
    }

    @Test
    fun `submit refreshes expired access token once and retries`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(tokens("access-old"), tokens("access-new"))
        coEvery { onboardingApi.submit("Bearer access-old", any()) } throws httpException(401)
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { onboardingApi.submit("Bearer access-new", any()) } returns successResponse()
        val repository = repository(onboardingApi, tokenStore, authRepository)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 1) { onboardingApi.submit("Bearer access-new", any()) }
    }

    @Test
    fun `submit requires login when token refresh fails`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } throws httpException(401)
        coEvery { authRepository.reissueToken() } throws AuthException.SessionRevoked("refresh failed")
        val repository = repository(onboardingApi, tokenStore, authRepository)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.AuthenticationRequired, result)
        coVerify(exactly = 1) { onboardingApi.submit(any(), any()) }
    }

    @Test
    fun `submit returns retryable result when token refresh is offline`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } throws httpException(401)
        coEvery { authRepository.reissueToken() } throws AuthException.Network("offline")
        val repository = repository(onboardingApi, tokenStore, authRepository)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.RetryableFailure, result)
    }

    @Test
    fun `submit maps client server and rate limit errors`() = runTest {
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
            coEvery { onboardingApi.submit(any(), any()) } throws httpException(statusCode)
            val repository = repository(onboardingApi, tokenStore)

            assertEquals(expected, repository.submit(profile(), OnboardingLevel.ROOKIE))
        }
    }

    @Test
    fun `submit maps network failure to retryable result`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } throws IOException("offline")
        val repository = repository(onboardingApi, tokenStore)

        val result = repository.submit(profile(), OnboardingLevel.ROOKIE)

        assertEquals(OnboardingSubmissionResult.RetryableFailure, result)
    }

    @Test
    fun `submit propagates cancellation`() = runTest {
        val onboardingApi = mockk<OnboardingApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { onboardingApi.submit(any(), any()) } throws CancellationException("cancelled")
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
        return OnboardingRepositoryImpl(prefs, onboardingApi, tokenStore, authRepository)
    }

    private fun preferences(): OnboardingPreferences = mockk(relaxed = true) {
        every { profile } returns emptyFlow()
        every { isSyncPending } returns emptyFlow()
        every { isSyncAuthorized } returns emptyFlow()
    }

    private fun profile() = OnboardingProfile(
        drivingPeriod = DrivingPeriod.MONTH_1_TO_3,
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
