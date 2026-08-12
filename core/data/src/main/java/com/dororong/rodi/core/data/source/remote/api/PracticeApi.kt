package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeSkipReasonRequest
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitRequest
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PracticeApi {
    @POST("places/{placeId}/practices")
    suspend fun register(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
    ): ApiEnvelope<PracticeRegisterResponse>

    @POST("practices/{practiceId}/visits")
    suspend fun recordVisit(
        @Header("Authorization") authorization: String,
        @Path("practiceId") practiceId: Long,
        @Body request: PracticeVisitRequest,
    ): ApiEnvelope<PracticeVisitResponse>

    @POST("practices/{practiceId}/skip-reason")
    suspend fun submitSkipReason(
        @Header("Authorization") authorization: String,
        @Path("practiceId") practiceId: Long,
        @Body request: PracticeSkipReasonRequest,
    ): ApiEnvelope<JsonObject>

    @GET("practices/skip-reason-form")
    suspend fun getSkipReasonForm(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<FormResponse>

    @DELETE("practices/{practiceId}")
    suspend fun delete(
        @Header("Authorization") authorization: String,
        @Path("practiceId") practiceId: Long,
    ): ApiEnvelope<JsonObject>
}
