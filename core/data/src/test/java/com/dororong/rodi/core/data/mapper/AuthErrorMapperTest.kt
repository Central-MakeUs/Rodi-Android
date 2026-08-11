package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.auth.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
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
    fun `maps AUTH_400_1 to InvalidRequest`() {
        val exception = httpException(400, """{"isSuccess":false,"code":"AUTH_400_1","message":"지원하지 않는 provider입니다."}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidRequest)
        assertEquals("지원하지 않는 provider입니다.", result.message)
    }

    @Test
    fun `maps AUTH_401_4 to SessionRevoked`() {
        val result = ApiEnvelope<Nothing>(
            isSuccess = false,
            code = "AUTH_401_4",
            message = "폐기된 토큰입니다.",
        ).toAuthException()

        assertTrue(result is AuthException.SessionRevoked)
    }

    @Test
    fun `maps recovery errors from envelope`() {
        val expired = ApiEnvelope<Nothing>(false, "MEMBER_409_1", "복구 기한이 지났습니다.").toAuthException()
        val notFound = ApiEnvelope<Nothing>(false, "MEMBER_404_1", "복구 대상이 없습니다.").toAuthException()

        assertTrue(expired is AuthException.RecoveryExpired)
        assertTrue(notFound is AuthException.RecoveryNotFound)
    }

    @Test
    fun `maps unknown error code to Unknown`() {
        val exception = httpException(500, """{"isSuccess":false,"code":"COMMON_500","message":"서버 오류"}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.Unknown)
    }

    @Test
    fun `never leaks deserialization failure text to the user`() {
        val missingField = SerializationException(
            "Field 'nickname' is required for type with serial name 'MyPageResponse', but it was missing",
        )

        val result = missingField.toAuthException(json)

        assertTrue(result is AuthException.Unknown)
        assertEquals("알 수 없는 오류가 발생했습니다.", result.message)
    }

    @Test
    fun `maps IOException to Network`() {
        val result = IOException("연결 실패").toAuthException(json)

        assertTrue(result is AuthException.Network)
    }

    @Test
    fun `rethrows CancellationException instead of mapping it`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("cancelled").toAuthException(json)
        }
    }
}
