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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
    private val refreshMutex = Mutex()

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
                val body = request { authApi.reissue(TokenRefreshRequest(currentTokens.refreshToken)) }.requireData()
                saveTokens(body.accessToken, body.refreshToken)
            } catch (exception: AuthException.SessionRevoked) {
                try {
                    clearTokens()
                } catch (clearException: CancellationException) {
                    throw clearException
                } catch (clearException: Throwable) {
                    exception.addSuppressed(clearException)
                }
                throw exception
            }
        }
    }

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

    private suspend fun saveTokens(accessToken: String, refreshToken: String) {
        if (!tokenStore.save(accessToken, refreshToken, KAKAO_PROVIDER)) {
            throw AuthException.Unknown("로그인 정보를 안전하게 저장하지 못했습니다.")
        }
    }

    private suspend fun clearTokens() {
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
        }
    }

    private suspend fun <T> request(block: suspend () -> T): T = json.authRequest(block)

    private fun <T> ApiEnvelope<T>.requireData(): T {
        if (!isSuccess) throw toAuthException()
        return data ?: throw AuthException.Unknown(message.ifBlank { "응답 데이터가 없습니다." })
    }

    private fun ApiEnvelope<*>.requireSuccess() {
        if (!isSuccess) throw toAuthException()
    }
}
