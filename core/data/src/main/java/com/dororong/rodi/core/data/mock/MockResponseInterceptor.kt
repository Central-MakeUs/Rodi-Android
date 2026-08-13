package com.dororong.rodi.core.data.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** [MockResponseRegistry]에 등록된 경로만 200으로 가로챈다. 나머지는 그대로 [Interceptor.Chain.proceed]. */
class MockResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mockJson = MockResponseRegistry.find(request.url.encodedPath) ?: return chain.proceed(request)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("Mocked by MockResponseRegistry")
            .body(mockJson.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
