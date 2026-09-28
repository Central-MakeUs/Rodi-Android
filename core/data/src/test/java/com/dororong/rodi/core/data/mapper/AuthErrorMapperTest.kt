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
    fun `AUTH_401_5는 InvalidCredential로 매핑한다`() {
        val exception = httpException(
            401,
            """{"isSuccess":false,"code":"AUTH_401_5","message":"카카오 토큰이 유효하지 않습니다."}""",
        )

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidCredential)
        assertEquals("카카오 토큰이 유효하지 않습니다.", result.message)
    }

    @Test
    fun `COMMON_400은 InvalidRequest로 매핑한다`() {
        val exception = httpException(400, """{"isSuccess":false,"code":"COMMON_400","message":"입력값이 올바르지 않습니다."}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidRequest)
    }

    @Test
    fun `AUTH_400_1은 InvalidRequest로 매핑한다`() {
        val exception = httpException(400, """{"isSuccess":false,"code":"AUTH_400_1","message":"지원하지 않는 provider입니다."}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.InvalidRequest)
        assertEquals("지원하지 않는 provider입니다.", result.message)
    }

    @Test
    fun `AUTH_401_4는 SessionRevoked로 매핑한다`() {
        val result = ApiEnvelope<Nothing>(
            isSuccess = false,
            code = "AUTH_401_4",
            message = "폐기된 토큰입니다.",
        ).toAuthException()

        assertTrue(result is AuthException.SessionRevoked)
    }

    @Test
    fun `응답 봉투의 복구 오류 코드를 복구 예외로 매핑한다`() {
        val expired = ApiEnvelope<Nothing>(false, "MEMBER_409_1", "복구 기한이 지났습니다.").toAuthException()
        val notFound = ApiEnvelope<Nothing>(false, "MEMBER_404_1", "복구 대상이 없습니다.").toAuthException()

        assertTrue(expired is AuthException.RecoveryExpired)
        assertTrue(notFound is AuthException.RecoveryNotFound)
    }

    @Test
    fun `알 수 없는 오류 코드는 Unknown으로 매핑한다`() {
        val exception = httpException(500, """{"isSuccess":false,"code":"COMMON_500","message":"서버 오류"}""")

        val result = exception.toAuthException(json)

        assertTrue(result is AuthException.Unknown)
    }

    @Test
    fun `역직렬화 실패 문구를 사용자에게 노출하지 않는다`() {
        val missingField = SerializationException(
            "Field 'nickname' is required for type with serial name 'MyPageResponse', but it was missing",
        )

        val result = missingField.toAuthException(json)

        assertTrue(result is AuthException.Unknown)
        assertEquals("알 수 없는 오류가 발생했습니다.", result.message)
    }

    @Test
    fun `IOException은 Network로 매핑한다`() {
        val result = IOException("연결 실패").toAuthException(json)

        assertTrue(result is AuthException.Network)
    }

    @Test
    fun `CancellationException은 매핑하지 않고 다시 던진다`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("cancelled").toAuthException(json)
        }
    }
}
