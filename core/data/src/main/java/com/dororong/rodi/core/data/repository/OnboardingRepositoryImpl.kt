package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toRequest
import com.dororong.rodi.core.data.source.local.datastore.OnboardingPreferences
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val prefs: OnboardingPreferences,
    private val onboardingApi: OnboardingApi,
    private val tokenStore: AuthTokenStore,
) : OnboardingRepository {
    override val profile: Flow<OnboardingProfile> = prefs.profile
    override val isSyncPending: Flow<Boolean> = prefs.isSyncPending
    override val isSyncAuthorized: Flow<Boolean> = prefs.isSyncAuthorized
    override val isInitialFilterTagsApplied: Flow<Boolean> = prefs.isInitialFilterTagsApplied

    override suspend fun saveProfile(profile: OnboardingProfile) = prefs.saveProfile(profile)
    override suspend fun savePendingProfile(profile: OnboardingProfile) = prefs.savePendingProfile(profile)
    override suspend fun authorizeSync() = prefs.authorizeSync()
    override suspend fun clearSyncPending() = prefs.clearSyncPending()
    override suspend fun markInitialFilterTagsApplied() = prefs.markInitialFilterTagsApplied()
    override suspend fun clear() = prefs.clear()

    override suspend fun submit(
        profile: OnboardingProfile,
        level: OnboardingLevel,
    ): OnboardingSubmissionResult {
        val tokens = tokenStore.getTokens()
        if (tokens == null) {
            Timber.w("Onboarding submit skipped: missing auth session.")
            return OnboardingSubmissionResult.Submitted
        }
        authorizeSync()
        val request = profile.toRequest(level)
        Timber.d(
            "Submitting onboarding: level=%s, drivingPeriod=%s, practiceTypes=%d, carType=%s, hasGoal=%s",
            request.level,
            request.drivingPeriod,
            request.practiceTypes.size,
            request.carType,
            request.drivingGoal != null,
        )
        val result = submit(request)
        if (result == OnboardingSubmissionResult.Submitted || result == OnboardingSubmissionResult.AlreadyCompleted) {
            clearSyncPending()
        }
        return result
    }

    /** 토큰 주입과 401 재발급은 OkHttp의 AuthHeaderInterceptor·TokenAuthenticator가 한다. */
    private suspend fun submit(request: OnboardingRequest): OnboardingSubmissionResult = try {
        val response = onboardingApi.submit(request)
        Timber.d(
            "Onboarding submit response: isSuccess=%s, code=%s, message=%s",
            response.isSuccess,
            response.code,
            response.message,
        )
        response.toSubmissionResult()
    } catch (error: CancellationException) {
        throw error
    } catch (error: HttpException) {
        error.toSubmissionResult()
    } catch (error: IOException) {
        Timber.w(error, "Onboarding submit failed due to network error.")
        OnboardingSubmissionResult.RetryableFailure
    } catch (error: Throwable) {
        Timber.e(error, "Onboarding submit failed unexpectedly.")
        OnboardingSubmissionResult.UnexpectedFailure
    }


    private fun ApiEnvelope<*>.toSubmissionResult(): OnboardingSubmissionResult {
        if (isSuccess) return OnboardingSubmissionResult.Submitted
        Timber.w("Onboarding submit rejected: code=%s, message=%s", code, message)
        return when {
            code.contains("409") -> OnboardingSubmissionResult.AlreadyCompleted
            code.contains("400") || code.contains("422") -> OnboardingSubmissionResult.InvalidProfile
            code.contains("401") -> OnboardingSubmissionResult.AuthenticationRequired
            code.contains("403") -> OnboardingSubmissionResult.Forbidden
            code.contains("429") -> OnboardingSubmissionResult.RateLimited
            else -> OnboardingSubmissionResult.UnexpectedFailure
        }
    }

    private fun HttpException.toSubmissionResult(): OnboardingSubmissionResult = when (code()) {
        HTTP_BAD_REQUEST,
        HTTP_UNPROCESSABLE_CONTENT,
        -> OnboardingSubmissionResult.InvalidProfile

        HTTP_UNAUTHORIZED -> OnboardingSubmissionResult.AuthenticationRequired
        HTTP_FORBIDDEN -> OnboardingSubmissionResult.Forbidden
        HTTP_CONFLICT -> OnboardingSubmissionResult.AlreadyCompleted
        HTTP_TOO_MANY_REQUESTS -> OnboardingSubmissionResult.RateLimited
        in HTTP_SERVER_ERROR_RANGE -> OnboardingSubmissionResult.RetryableFailure
        else -> OnboardingSubmissionResult.UnexpectedFailure
    }
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE_CONTENT = 422
private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERROR_RANGE = 500..599
