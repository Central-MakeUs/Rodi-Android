package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingFormResponse

interface OnboardingApi {
    @GET("members/me/onboarding-form")
    suspend fun getForm(): ApiEnvelope<OnboardingFormResponse>

    @POST("members/me/onboarding")
    suspend fun submit(
        @Body request: OnboardingRequest,
    ): ApiEnvelope<JsonObject>
}
