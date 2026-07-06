package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.auth.AuthApi
import com.dororong.rodi.core.data.auth.AuthTokenStore
import com.dororong.rodi.core.data.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.network.toAuthException
import com.dororong.rodi.core.domain.AuthException
import com.dororong.rodi.core.domain.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String): Boolean {
        val envelope = try {
            authApi.oauthLogin("kakao", OAuthLoginRequest(kakaoAccessToken))
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
