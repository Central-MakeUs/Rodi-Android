package com.dororong.rodi.core.data.source.remote.api

import com.dororong.rodi.core.data.di.NetworkModule
import java.util.Collections
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PlaceApiContractTest {
    @Test
    fun `지도 클러스터는 폐기된 좌표 API 대신 범위와 반복 필터 query를 전송한다`() = runTest {
        val requests = Collections.synchronizedList(mutableListOf<Request>())
        val api = api(requests, """{"places":[],"levels":[]}""")

        api.getClusters(37.4, 126.7, 37.7, 127.2, listOf("PARKING", "STRAIGHT"))

        val url = requests.single().url
        assertEquals("/api/v1/places/clusters", url.encodedPath)
        assertEquals("37.4", url.queryParameter("swLat"))
        assertEquals("126.7", url.queryParameter("swLng"))
        assertEquals("37.7", url.queryParameter("neLat"))
        assertEquals("127.2", url.queryParameter("neLng"))
        assertEquals(listOf("PARKING", "STRAIGHT"), url.queryParameterValues("practiceTypes"))
    }

    @Test
    fun `필터 없는 목록은 practiceTypes query를 생략하고 검색은 커서와 필터를 유지한다`() = runTest {
        val requests = Collections.synchronizedList(mutableListOf<Request>())
        val api = api(requests, """{"items":[],"hasNext":false}""")

        api.getPlaces(37.4, 126.7, 37.7, 127.2, 37.55, 126.95, 20, null)
        api.getPlaces(37.4, 126.7, 37.7, 127.2, 37.55, 126.95, 20, "filtered", listOf("PARKING", "STRAIGHT"))
        api.searchPlaces("서울 중구", 37.55, 126.95, 10, "next cursor", listOf("PARKING"))

        assertFalse(requests[0].url.queryParameterNames.contains("practiceTypes"))
        assertEquals(listOf("PARKING", "STRAIGHT"), requests[1].url.queryParameterValues("practiceTypes"))
        assertEquals("filtered", requests[1].url.queryParameter("cursor"))
        val searchUrl = requests[2].url
        assertEquals("/api/v1/places/search", searchUrl.encodedPath)
        assertEquals("서울 중구", searchUrl.queryParameter("keyword"))
        assertEquals("next cursor", searchUrl.queryParameter("cursor"))
        assertEquals(listOf("PARKING"), searchUrl.queryParameterValues("practiceTypes"))
    }

    private fun api(requests: MutableList<Request>, data: String): PlaceApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"isSuccess":true,"code":"COMMON_200","message":"성공","data":$data}"""
                        .toResponseBody("application/json".toMediaType()),
                )
                .build()
        }.build()
        return Retrofit.Builder()
            .baseUrl("https://example.test/api/v1/")
            .client(client)
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PlaceApi::class.java)
    }
}
