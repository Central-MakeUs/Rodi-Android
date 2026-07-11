package com.dororong.rodi.core.data.auth

import com.dororong.rodi.core.data.network.ApiEnvelope
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("auth/oauth/{provider}")
    suspend fun oauthLogin(
        @Path("provider") provider: String,
        @Body request: OAuthLoginRequest,
    ): ApiEnvelope<AuthTokenResponse>
}

@Serializable
data class OAuthLoginRequest(
    val credential: String,
    val onboardingProfile: OAuthOnboardingProfileRequest? = null,
)

@Serializable
data class OAuthOnboardingProfileRequest(
    val nickname: String? = null,
    val drivingPeriod: String? = null,
    val recentFrequency: String? = null,
    val roadExperiences: List<String> = emptyList(),
    val soloDrivingRange: String? = null,
    val soloParkingLevel: String? = null,
    val practiceSituations: List<String> = emptyList(),
    val vehicleType: String? = null,
    val goal: String? = null,
)

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean,
)
