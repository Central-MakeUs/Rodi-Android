package com.dororong.rodi.core.data.network

import com.dororong.rodi.core.domain.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class AuthErrorBody(val code: String = "", val message: String = "")

fun Throwable.toAuthException(json: Json): AuthException = when (this) {
    is CancellationException -> throw this
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let { runCatching { json.decodeFromString<AuthErrorBody>(it) }.getOrNull() }
        val message = parsed?.message?.ifBlank { null } ?: "로그인 요청이 거부되었습니다."
        when (parsed?.code) {
            "AUTH_401_5" -> AuthException.InvalidCredential(message)
            "COMMON_400", "AUTH_400_1" -> AuthException.InvalidRequest(message)
            else -> AuthException.Unknown(message)
        }
    }
    is IOException -> AuthException.Network("네트워크 연결을 확인해주세요.")
    else -> AuthException.Unknown(message ?: "알 수 없는 오류가 발생했습니다.")
}
