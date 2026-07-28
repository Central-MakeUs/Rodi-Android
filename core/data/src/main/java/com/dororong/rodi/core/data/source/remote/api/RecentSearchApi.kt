package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface RecentSearchApi {
    @GET("members/me/recent-searches")
    suspend fun getRecentSearches(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<List<RecentSearchResponse>>

    @DELETE("members/me/recent-searches")
    suspend fun deleteAllRecentSearches(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me/recent-searches/{id}")
    suspend fun deleteRecentSearch(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    ): ApiEnvelope<JsonObject>
}
