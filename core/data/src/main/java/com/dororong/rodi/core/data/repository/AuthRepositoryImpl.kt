package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.mapper.toOAuthRequest
import com.dororong.rodi.core.data.source.remote.model.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
    override suspend fun getSession(): AuthSession = AuthSession(
        isLoggedIn = tokenStore.isLoggedIn,
        hasRecentKakaoLogin = tokenStore.hasRecentKakaoLogin,
    )

    override suspend fun loginWithKakao(
        kakaoAccessToken: String,
        onboardingProfile: OnboardingProfile?,
    ): Boolean {
        val envelope = try {
            authApi.oauthLogin(
                "kakao",
                OAuthLoginRequest(
                    credential = kakaoAccessToken,
                    onboardingProfile = onboardingProfile?.toOAuthRequest(),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw e.toAuthException(json)
        }
        if (!envelope.isSuccess) {
            throw AuthException.Unknown(
                envelope.message.ifBlank { "로그인 요청이 실패했습니다." },
            )
        }
        val body = envelope.data ?: throw AuthException.Unknown(
            envelope.message.ifBlank { "응답에 로그인 정보가 없습니다." },
        )
        tokenStore.save(body.accessToken, body.refreshToken)
        return body.isNewMember
    }
}
