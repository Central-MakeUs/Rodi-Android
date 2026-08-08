package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.review.CursorPageReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewCreatedResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {
    @GET("places/{placeId}/reviews")
    suspend fun getReviews(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
        @Query("level") level: String?,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CursorPageReviewResponse>

    @POST("places/{placeId}/reviews")
    suspend fun createReview(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
        @Body request: ReviewRequest,
    ): ApiEnvelope<ReviewCreatedResponse>

    @GET("places/{placeId}/reviews/summary")
    suspend fun getSummary(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long,
        @Query("level") level: String?,
    ): ApiEnvelope<ReviewSummaryResponse>

    @PUT("reviews/{reviewId}")
    suspend fun updateReview(
        @Header("Authorization") authorization: String,
        @Path("reviewId") reviewId: Long,
        @Body request: ReviewRequest,
    ): ApiEnvelope<JsonObject>

    @DELETE("reviews/{reviewId}")
    suspend fun deleteReview(
        @Header("Authorization") authorization: String,
        @Path("reviewId") reviewId: Long,
    ): ApiEnvelope<JsonObject>

    @POST("reviews/{reviewId}/report")
    suspend fun reportReview(
        @Header("Authorization") authorization: String,
        @Path("reviewId") reviewId: Long,
        @Body request: ReportRequest,
    ): ApiEnvelope<JsonObject>

    @GET("reviews/report-form")
    suspend fun getReportForm(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<ReportFormResponse>
}
