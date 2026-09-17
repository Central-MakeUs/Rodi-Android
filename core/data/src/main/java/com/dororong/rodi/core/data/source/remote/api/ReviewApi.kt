package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.review.CursorPageReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewCreatedResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewDetailResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {
    @GET("places/{placeId}/reviews")
    suspend fun getReviews(
        @Path("placeId") placeId: Long,
        @Query("level") level: String?,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageReviewResponse>

    @POST("places/{placeId}/reviews")
    suspend fun createReview(
        @Path("placeId") placeId: Long,
        @Body request: ReviewRequest,
    ): ApiEnvelope<ReviewCreatedResponse>

    @GET("places/{placeId}/reviews/summary")
    suspend fun getSummary(
        @Path("placeId") placeId: Long,
        @Query("level") level: String?,
    ): ApiEnvelope<ReviewSummaryResponse>

    @GET("reviews/{reviewId}")
    suspend fun getReview(
        @Path("reviewId") reviewId: Long,
    ): ApiEnvelope<ReviewDetailResponse>

    @PUT("reviews/{reviewId}")
    suspend fun updateReview(
        @Path("reviewId") reviewId: Long,
        @Body request: ReviewRequest,
    ): ApiEnvelope<JsonObject>

    @DELETE("reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("reviewId") reviewId: Long,
    ): ApiEnvelope<JsonObject>

    @POST("reviews/{reviewId}/report")
    suspend fun reportReview(
        @Path("reviewId") reviewId: Long,
        @Body request: ReportRequest,
    ): ApiEnvelope<JsonObject>

    @GET("reviews/report-form")
    suspend fun getReportForm(
    ): ApiEnvelope<ReportFormResponse>
}
