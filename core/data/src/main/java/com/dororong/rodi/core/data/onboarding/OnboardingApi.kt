package com.dororong.rodi.core.data.onboarding

import com.dororong.rodi.core.data.network.ApiEnvelope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface OnboardingApi {
    @POST("members/me/onboarding")
    suspend fun submit(@Body request: OnboardingRequest): ApiEnvelope<JsonObject>
}

@Serializable
data class OnboardingRequest(
    val drivingPeriod: String,
    val recentFrequency: String? = null,
    val roadExperiences: List<String>? = null,
    val soloDrivingRange: String? = null,
    val soloParkingLevel: String? = null,
    val level: String,
    val practiceTypes: List<String> = emptyList(),
    val carType: String? = null,
    val drivingGoal: String? = null,
)
