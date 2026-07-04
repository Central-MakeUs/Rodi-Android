package com.dororong.rodi.core.data.network

import com.dororong.rodi.core.domain.AuthException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthErrorMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun httpException(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `maps AUTH_401_5 to InvalidCredential`() {
        val exception = httpException(
            401,
            """{"isSuccess":false,"code":"AUTH_401_5","message":"카카오 토큰이 유효하지 않습니다."}""",
        )

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidCredential)
        assertEquals("카카오 토큰이 유효하지 않습니다.", result.message)
    }

    @Test
    fun `maps COMMON_400 to InvalidRequest`() {
        val exception = httpException(400, """{"isSuccess":false,"code":"COMMON_400","message":"입력값이 올바르지 않습니다."}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidRequest)
    }

    @Test
    fun `maps unknown error code to Unknown`() {
        val exception = httpException(500, """{"isSuccess":false,"code":"COMMON_500","message":"서버 오류"}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.Unknown)
    }

    @Test
    fun `maps IOException to Network`() {
        val result = IOException("연결 실패").toAuthException(json)

        assertTrue(result is AuthException.Network)
    }
}
