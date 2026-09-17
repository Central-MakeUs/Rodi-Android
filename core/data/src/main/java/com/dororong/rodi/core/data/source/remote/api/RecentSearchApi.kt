package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchResponse
import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchRegisterRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface RecentSearchApi {
    @GET("members/me/recent-searches")
    suspend fun getRecentSearches(
    ): ApiEnvelope<List<RecentSearchResponse>>

    @POST("members/me/recent-searches")
    suspend fun registerRecentSearch(
        @Body request: RecentSearchRegisterRequest,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me/recent-searches")
    suspend fun deleteAllRecentSearches(
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me/recent-searches/{id}")
    suspend fun deleteRecentSearch(
        @Path("id") id: Long,
    ): ApiEnvelope<JsonObject>
}
