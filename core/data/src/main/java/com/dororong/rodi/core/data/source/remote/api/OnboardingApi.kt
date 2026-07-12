package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface OnboardingApi {
    @POST("members/me/onboarding")
    suspend fun submit(@Body request: OnboardingRequest): ApiEnvelope<JsonObject>
}
