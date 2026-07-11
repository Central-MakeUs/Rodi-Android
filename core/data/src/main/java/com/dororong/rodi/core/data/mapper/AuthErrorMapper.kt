package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.auth.AuthException
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
        val parsed = runCatching {
            response()?.errorBody()?.string()?.let { json.decodeFromString<AuthErrorBody>(it) }
        }.getOrNull()
        val message = parsed?.message?.ifBlank { null } ?: "로그인 요청이 거부되었습니다."
        when (parsed?.code) {
            CODE_INVALID_KAKAO_TOKEN -> AuthException.InvalidCredential(message)
            CODE_COMMON_BAD_REQUEST, CODE_UNSUPPORTED_PROVIDER -> AuthException.InvalidRequest(message)
            else -> AuthException.Unknown(message)
        }
    }
    is IOException -> AuthException.Network("네트워크 연결을 확인해주세요.")
    else -> AuthException.Unknown(message ?: "알 수 없는 오류가 발생했습니다.")
}

private const val CODE_INVALID_KAKAO_TOKEN = "AUTH_401_5"
private const val CODE_COMMON_BAD_REQUEST = "COMMON_400"
private const val CODE_UNSUPPORTED_PROVIDER = "AUTH_400_1"
