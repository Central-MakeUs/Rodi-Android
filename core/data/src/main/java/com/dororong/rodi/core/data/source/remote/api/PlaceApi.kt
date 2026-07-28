package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceCoordinateResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {
    @GET("places/bookmarks")
    suspend fun getSavedPlaces(
        @Header("Authorization") authorization: String,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPagePlaceResponse>

    @GET("places/coordinates")
    suspend fun getCoordinates(): ApiEnvelope<List<PlaceCoordinateResponse>>

    @GET("places")
    suspend fun getPlaces(
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double,
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPagePlaceResponse>

    @GET("places/search")
    suspend fun searchPlaces(
        @Header("Authorization") authorization: String,
        @Query("keyword") keyword: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPagePlaceResponse>

    @GET("places/{placeId}")
    suspend fun getPlaceDetail(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
    ): ApiEnvelope<PlaceDetailResponse>

    @POST("places/{placeId}/bookmark")
    suspend fun bookmark(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
    ): ApiEnvelope<JsonObject>

    @DELETE("places/{placeId}/bookmark")
    suspend fun unbookmark(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
    ): ApiEnvelope<JsonObject>
}
