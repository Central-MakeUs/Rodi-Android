package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.mapper.toIssueForm
import com.dororong.rodi.core.data.mapper.toRequest
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ReviewIssueApiContractTest {
    @Test
    fun `비추천 후기 폼을 조회하고 선택 항목을 작성과 수정에 전송한다`() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()
            requests += "${request.method} ${request.url.encodedPath}" to body
            val data = if (request.method == "GET") {
                """{"steps":[{"questions":[{"code":"courseIssues","title":"코스 문제",
                "options":[{"code":"OTHER","label":"기타","order":1,"requiresTextInput":true,
                "textInputMaxLength":30}]}]}]}"""
            } else if (request.method == "POST") """{"reviewId":42}""" else "{}"
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"isSuccess":true,"code":"COMMON_200","message":"성공","data":$data}"""
                    .toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://test.invalid/api/v1/")
            .client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ReviewApi::class.java)

        val form = checkNotNull(checkNotNull(api.getReviewForm(7).data).toIssueForm())
        assertEquals("OTHER", form.options.single().code)
        val draft = ReviewDraft(false, null, null, PracticeMethod.SOLO, null, null, listOf("OTHER"), "공사 중")
        assertEquals(42L, api.createReview(7, draft.toRequest()).data?.reviewId)
        api.updateReview(42, draft.toRequest())

        assertEquals(listOf("GET /api/v1/places/7/review-form", "POST /api/v1/places/7/reviews", "PUT /api/v1/reviews/42"),
            requests.map { it.first })
        for ((_, body) in requests.drop(1)) {
            val fields = json.parseToJsonElement(body).jsonObject
            assertTrue("courseIssues" in fields && "courseIssueDetail" in fields)
            assertFalse("difficulty" in fields || "congestion" in fields)
            assertEquals("[\"OTHER\"]", fields.getValue("courseIssues").toString())
        }
    }
}
