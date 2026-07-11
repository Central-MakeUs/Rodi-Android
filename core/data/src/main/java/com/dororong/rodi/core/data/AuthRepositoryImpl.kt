package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.auth.AuthApi
import com.dororong.rodi.core.data.auth.AuthTokenStore
import com.dororong.rodi.core.data.auth.OAuthOnboardingProfileRequest
import com.dororong.rodi.core.data.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.network.toAuthException
import com.dororong.rodi.core.domain.AuthException
import com.dororong.rodi.core.domain.AuthRepository
import com.dororong.rodi.core.domain.OnboardingProfile
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
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

private fun OnboardingProfile.toOAuthRequest(): OAuthOnboardingProfileRequest =
    OAuthOnboardingProfileRequest(
        nickname = nickname.ifBlank { null },
        drivingPeriod = drivingPeriod?.name,
        recentFrequency = recentFrequency?.name,
        roadExperiences = roadExperiences.map { it.name },
        soloDrivingRange = soloDrivingRange?.name,
        soloParkingLevel = soloParkingLevel?.name,
        practiceSituations = practiceSituations.map { it.name },
        vehicleType = vehicleType?.name,
        goal = goal.ifBlank { null },
    )
