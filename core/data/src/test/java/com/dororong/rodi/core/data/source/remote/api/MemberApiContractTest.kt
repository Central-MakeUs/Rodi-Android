package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.di.NetworkModule
import com.dororong.rodi.core.data.mapper.toApiValue
import com.dororong.rodi.core.data.source.remote.model.member.SortTagsRequest
import com.dororong.rodi.core.domain.model.place.PracticeType
import java.util.Collections
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MemberApiContractTest {
    @Test
    fun `정렬 태그 저장은 sort-tags 경로와 sortTags JSON만 전송하고 우선순위를 보존한다`() = runTest {
        val requests = Collections.synchronizedList(mutableListOf<Pair<Request, String>>())
        val json = NetworkModule.provideJson()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val body = Buffer().also { chain.request().body?.writeTo(it) }.readUtf8()
            requests += chain.request() to body
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"isSuccess":true,"code":"COMMON_200","message":"성공","data":{}}"""
                        .toResponseBody("application/json".toMediaType()),
                )
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl("https://example.test/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MemberApi::class.java)
        val orderedTags = listOf(PracticeType.LEFT_RIGHT_TURN, PracticeType.PARKING, PracticeType.U_TURN)

        api.updateSortTags(SortTagsRequest(orderedTags.map(PracticeType::toApiValue)))

        val (request, body) = requests.single()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/members/me/sort-tags", request.url.encodedPath)
        val payload = json.parseToJsonElement(body).jsonObject
        assertEquals(setOf("sortTags"), payload.keys)
        assertEquals(
            listOf("LEFT_RIGHT_TURN", "PARKING", "U_TURN"),
            payload.getValue("sortTags").jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
