package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.auth.AuthApi
import com.dororong.rodi.core.data.auth.AuthTokenStore
import com.dororong.rodi.core.data.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.network.toAuthException
import com.dororong.rodi.core.domain.AuthException
import com.dororong.rodi.core.domain.AuthRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String): Boolean {
        val envelope = runCatching { authApi.oauthLogin("kakao", OAuthLoginRequest(kakaoAccessToken)) }
            .getOrElse { throw it.toAuthException(json) }
        val body = envelope.data ?: throw AuthException.Unknown("응답에 로그인 정보가 없습니다.")
        tokenStore.save(body.accessToken, body.refreshToken)
        return body.isNewMember
    }
}
