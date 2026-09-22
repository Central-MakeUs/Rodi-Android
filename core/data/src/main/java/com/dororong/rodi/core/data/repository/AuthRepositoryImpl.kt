package com.dororong.rodi.core.data.repository

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
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
    private val sessionCoordinator: AuthSessionCoordinator,
) : AuthRepository {
    private val refreshMutex = Mutex()

    override suspend fun getSession(): AuthSession {
        val tokens = tokenStore.getTokens()
        val recentProvider = tokens?.provider ?: tokenStore.getRecentProvider()
        return AuthSession(
            isLoggedIn = tokens != null,
            hasRecentKakaoLogin = recentProvider == KAKAO_PROVIDER,
            isCourseTutorialCompleted = tokens?.isCourseTutorialCompleted == true,
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
            sessionCoordinator.start(tokens.accessToken, tokens.refreshToken, tokens.isCourseTutorialCompleted)
        }
        return result
    }

    override suspend fun reissueToken(expectedSessionId: String?, expectedAccessToken: String?) {
        val requested = tokenStore.getTokens()
            ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        if (expectedSessionId != null && requested.sessionId != expectedSessionId) return
        if (expectedAccessToken != null && requested.accessToken != expectedAccessToken) return

        refreshMutex.withLock {
            val currentTokens = tokenStore.getTokens()
                ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
            if (currentTokens.sessionId != requested.sessionId || currentTokens.refreshToken != requested.refreshToken) return

            try {
                val body = refreshRequest(currentTokens.refreshToken)
                sessionCoordinator.rotate(currentTokens, body.accessToken, body.refreshToken, body.isCourseTutorialCompleted)
            } catch (exception: AuthException.SessionRevoked) {
                try {
                    if (!sessionCoordinator.expire(currentTokens)) return
                } catch (clearException: CancellationException) {
                    throw clearException
                } catch (clearException: Throwable) {
                    exception.addSuppressed(clearException)
                }
                throw exception
            }
        }
    }

    override fun observeSessionExpiration(): Flow<Boolean> = sessionCoordinator.observeExpiration()

    override fun observeSignOut(): Flow<Unit> = sessionCoordinator.observeSignOut()

    override suspend fun restoreWithKakao(credential: String): AccountRestoreResult {
        val body = request { authApi.restore("kakao", SocialLoginRequest(credential)) }.requireData()
        val result = body.toAccountRestoreResult()
        if (result is AccountRestoreResult.Restored) {
            val tokens = body.toAuthTokenResponse()
            sessionCoordinator.start(tokens.accessToken, tokens.refreshToken, tokens.isCourseTutorialCompleted)
        }
        return result
    }

    override suspend fun logout() {
        val tokens = tokenStore.getTokens() ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        request { authApi.logout(LogoutRequest(tokens.refreshToken)) }.requireSuccess()
        sessionCoordinator.signOut(tokens)
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
