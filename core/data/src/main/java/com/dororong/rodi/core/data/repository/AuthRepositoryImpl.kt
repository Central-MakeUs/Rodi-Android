package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.mapper.authRequest
import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.mapper.toAccountRestoreResult
import com.dororong.rodi.core.data.mapper.toAuthTokenResponse
import com.dororong.rodi.core.data.mapper.toLoginResult
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.KAKAO_PROVIDER
import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.remote.model.auth.LogoutRequest
import com.dororong.rodi.core.data.source.remote.model.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.TokenRefreshRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
    private val practiceRecordPresenceCache: PracticeRecordPresenceCache,
    private val practiceSessionRepository: PracticeSessionRepository,
) : AuthRepository {
    private val refreshMutex = Mutex()
    private val sessionExpired = MutableStateFlow(false)

    override suspend fun getSession(): AuthSession {
        val tokens = tokenStore.getTokens()
        val recentProvider = tokens?.provider ?: tokenStore.getRecentProvider()
        return AuthSession(
            isLoggedIn = tokens != null,
            hasRecentKakaoLogin = recentProvider == KAKAO_PROVIDER,
        )
    }

    override suspend fun loginWithKakao(kakaoAccessToken: String): LoginResult {
        val envelope = request {
            authApi.oauthLogin(
                "kakao",
                OAuthLoginRequest(credential = kakaoAccessToken),
            )
        }
        val body = envelope.requireData()
        val result = body.toLoginResult()
        if (result is LoginResult.Success) {
            val tokens = body.toAuthTokenResponse()
            saveTokens(tokens.accessToken, tokens.refreshToken)
        }
        return result
    }

    override suspend fun reissueToken() {
        val requestedRefreshToken = tokenStore.getTokens()?.refreshToken
            ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")

        refreshMutex.withLock {
            val currentTokens = tokenStore.getTokens()
                ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
            if (currentTokens.refreshToken != requestedRefreshToken) return

            try {
                val body = refreshRequest(currentTokens.refreshToken)
                saveTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    invalidatePracticeRecordCache = false,
                )
            } catch (exception: AuthException.SessionRevoked) {
                try {
                    clearTokens()
                } catch (clearException: CancellationException) {
                    throw clearException
                } catch (clearException: Throwable) {
                    exception.addSuppressed(clearException)
                }
                sessionExpired.value = true
                throw exception
            }
        }
    }

    override fun observeSessionExpiration(): Flow<Boolean> = sessionExpired.asStateFlow()

    override suspend fun restoreWithKakao(credential: String): AccountRestoreResult {
        val body = request { authApi.restore("kakao", SocialLoginRequest(credential)) }.requireData()
        val result = body.toAccountRestoreResult()
        if (result is AccountRestoreResult.Restored) {
            val tokens = body.toAuthTokenResponse()
            saveTokens(tokens.accessToken, tokens.refreshToken)
        }
        return result
    }

    override suspend fun logout() {
        val tokens = tokenStore.getTokens() ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        request { authApi.logout(LogoutRequest(tokens.refreshToken)) }.requireSuccess()
        clearTokens()
    }

    private suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        invalidatePracticeRecordCache: Boolean = true,
    ) {
        if (!tokenStore.save(accessToken, refreshToken, KAKAO_PROVIDER)) {
            throw AuthException.Unknown("로그인 정보를 안전하게 저장하지 못했습니다.")
        }
        if (invalidatePracticeRecordCache) {
            clearPracticeSessionSafely()
            practiceRecordPresenceCache.clear()
        }
        sessionExpired.value = false
    }

    private suspend fun clearPracticeSessionSafely() {
        try {
            practiceSessionRepository.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            // A stale local session must not make a successful authentication fail.
        }
    }

    private suspend fun clearTokens() {
        practiceSessionRepository.clear()
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
        }
        practiceRecordPresenceCache.clear()
    }

    private suspend fun <T> request(block: suspend () -> T): T = json.authRequest(block)

    private suspend fun refreshRequest(refreshToken: String) = try {
        authApi.reissue(TokenRefreshRequest(refreshToken)).requireRefreshData()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: HttpException) {
        if (exception.code() == HTTP_UNAUTHORIZED) {
            throw AuthException.SessionRevoked(SESSION_EXPIRED_MESSAGE)
        }
        throw exception.toAuthException(json)
    } catch (exception: AuthException) {
        throw exception
    } catch (exception: Throwable) {
        throw exception.toAuthException(json)
    }

    private fun <T> ApiEnvelope<T>.requireRefreshData(): T = when {
        isSuccess -> data ?: throw AuthException.Unknown(message.ifBlank { "응답 데이터가 없습니다." })
        code.contains(HTTP_UNAUTHORIZED.toString()) -> throw AuthException.SessionRevoked(
            message.ifBlank { SESSION_EXPIRED_MESSAGE },
        )
        else -> throw toAuthException()
    }

    private fun <T> ApiEnvelope<T>.requireData(): T {
        if (!isSuccess) throw toAuthException()
        return data ?: throw AuthException.Unknown(message.ifBlank { "응답 데이터가 없습니다." })
    }

    private fun ApiEnvelope<*>.requireSuccess() {
        if (!isSuccess) throw toAuthException()
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val SESSION_EXPIRED_MESSAGE = "로그인 정보가 만료되었습니다."
    }
}
