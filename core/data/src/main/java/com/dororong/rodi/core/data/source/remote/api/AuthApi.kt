package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.auth.AuthTokenResponse
import com.dororong.rodi.core.data.source.remote.model.auth.LogoutRequest
import com.dororong.rodi.core.data.source.remote.model.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.data.source.remote.model.auth.TokenRefreshRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("auth/oauth/{provider}")
    suspend fun oauthLogin(
        @Path("provider") provider: String,
        @Body request: OAuthLoginRequest,
    ): ApiEnvelope<SocialLoginResponse>

    @POST("auth/token/refresh")
    suspend fun reissue(@Body request: TokenRefreshRequest): ApiEnvelope<AuthTokenResponse>

    @POST("auth/oauth/{provider}/restore")
    suspend fun restore(
        @Path("provider") provider: String,
        @Body request: SocialLoginRequest,
    ): ApiEnvelope<SocialLoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): ApiEnvelope<JsonObject>
}
