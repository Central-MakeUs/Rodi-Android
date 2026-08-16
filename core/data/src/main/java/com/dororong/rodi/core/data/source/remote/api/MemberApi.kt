package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageMyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageBlockedMemberItemResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface MemberApi {
    @GET("members/me")
    suspend fun getMyPage(@Header("Authorization") authorization: String): ApiEnvelope<MyPageResponse>

    @GET("members/me/practices")
    suspend fun getPracticeRecords(
        @Header("Authorization") authorization: String,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPagePracticeItemResponse>

    @GET("members/me/reviews")
    suspend fun getMyReviews(
        @Header("Authorization") authorization: String,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageMyReviewItemResponse>

    @GET("members/me/blocks")
    suspend fun getBlockedMembers(
        @Header("Authorization") authorization: String,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageBlockedMemberItemResponse>

    @PATCH("members/me")
    suspend fun updateMe(
        @Header("Authorization") authorization: String,
        @Body request: MemberUpdateRequest,
    ): ApiEnvelope<JsonObject>

    @PUT("members/me/filter-tags")
    suspend fun updateFilterTags(
        @Header("Authorization") authorization: String,
        @Body request: FilterTagsRequest,
    ): ApiEnvelope<JsonObject>

    @POST("members/{memberId}/block")
    suspend fun blockMember(
        @Header("Authorization") authorization: String,
        @Path("memberId") memberId: Long,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/{memberId}/block")
    suspend fun unblockMember(
        @Header("Authorization") authorization: String,
        @Path("memberId") memberId: Long,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me")
    suspend fun withdraw(@Header("Authorization") authorization: String): ApiEnvelope<JsonObject>

    @DELETE("members/me/hard")
    suspend fun hardDelete(@Header("Authorization") authorization: String): ApiEnvelope<JsonObject>
}
