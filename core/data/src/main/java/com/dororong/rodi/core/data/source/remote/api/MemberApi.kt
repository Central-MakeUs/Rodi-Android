package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.DELETE
import retrofit2.http.Header

interface MemberApi {
    @DELETE("members/me")
    suspend fun withdraw(@Header("Authorization") authorization: String): ApiEnvelope<JsonObject>
}
