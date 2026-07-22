package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH

interface MemberApi {
    @GET("members/me")
    suspend fun getMyPage(@Header("Authorization") authorization: String): ApiEnvelope<MyPageResponse>

    @PATCH("members/me")
    suspend fun updateMe(
        @Header("Authorization") authorization: String,
        @Body request: MemberUpdateRequest,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me")
    suspend fun withdraw(@Header("Authorization") authorization: String): ApiEnvelope<JsonObject>
}
