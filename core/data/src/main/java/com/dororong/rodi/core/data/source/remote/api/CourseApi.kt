package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.source.remote.model.course.CoursePageResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterRequest
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationFormResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CourseApi {
    @POST("courses")
    suspend fun registerCourse(
        @Body request: CourseRegisterRequest,
    ): ApiEnvelope<CourseRegisterResponse>

    @GET("members/me/courses")
    suspend fun getMyCourses(
        @Query("status") status: String?,
        @Query("size") size: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<CoursePageResponse>

    @GET("courses/registration-form")
    suspend fun getRegistrationForm(
    ): ApiEnvelope<CourseRegistrationFormResponse>

    @DELETE("courses/{courseId}")
    suspend fun deleteCourse(
        @Path("courseId") courseId: Long,
    ): ApiEnvelope<JsonObject>
}
