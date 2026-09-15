package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoAddressSearchResponse
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoCoordinateAddressResponse
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoKeywordSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocalApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("size") size: Int = 15,
    ): KakaoKeywordSearchResponse

    @GET("v2/local/search/address.json")
    suspend fun searchAddress(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("size") size: Int = 15,
    ): KakaoAddressSearchResponse

    @GET("v2/local/geo/coord2address.json")
    suspend fun reverseGeocode(
        @Header("Authorization") authorization: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("input_coord") inputCoordinate: String = "WGS84",
    ): KakaoCoordinateAddressResponse
}
