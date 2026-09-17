package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageMyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageBlockedMemberItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CourseTutorialCompletionResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface MemberApi {
    @PATCH("members/me/course-tutorial")
    suspend fun completeCourseTutorial(
    ): ApiEnvelope<CourseTutorialCompletionResponse>
    @GET("members/me")
    suspend fun getMyPage(): ApiEnvelope<MyPageResponse>

    @GET("members/me/practices")
    suspend fun getPracticeRecords(
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPagePracticeItemResponse>

    @GET("members/me/reviews")
    suspend fun getMyReviews(
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageMyReviewItemResponse>

    @GET("members/me/blocks")
    suspend fun getBlockedMembers(
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageBlockedMemberItemResponse>

    @PATCH("members/me")
    suspend fun updateMe(
        @Body request: MemberUpdateRequest,
    ): ApiEnvelope<JsonObject>

    @PUT("members/me/filter-tags")
    suspend fun updateFilterTags(
        @Body request: FilterTagsRequest,
    ): ApiEnvelope<JsonObject>

    @POST("members/{memberId}/block")
    suspend fun blockMember(
        @Path("memberId") memberId: Long,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/{memberId}/block")
    suspend fun unblockMember(
        @Path("memberId") memberId: Long,
    ): ApiEnvelope<JsonObject>

    @DELETE("members/me")
    suspend fun withdraw(): ApiEnvelope<JsonObject>

    @DELETE("members/me/hard")
    suspend fun hardDelete(): ApiEnvelope<JsonObject>
}
