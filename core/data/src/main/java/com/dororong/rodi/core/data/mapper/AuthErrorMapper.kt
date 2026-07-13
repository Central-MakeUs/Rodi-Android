package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
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
        (parsed?.code ?: "").toAuthException(
            message = parsed?.message,
            fallbackMessage = "인증 요청이 거부되었습니다.",
        )
    }
    is IOException -> AuthException.Network("네트워크 연결을 확인해주세요.")
    else -> AuthException.Unknown(message ?: "알 수 없는 오류가 발생했습니다.")
}

fun ApiEnvelope<*>.toAuthException(): AuthException = code.toAuthException(
    message = message,
    fallbackMessage = "인증 요청에 실패했습니다.",
)

private fun String.toAuthException(message: String?, fallbackMessage: String): AuthException {
    val resolvedMessage = message?.ifBlank { null } ?: fallbackMessage
    return when (this) {
        CODE_INVALID_KAKAO_TOKEN -> AuthException.InvalidCredential(resolvedMessage)
        CODE_COMMON_BAD_REQUEST, CODE_UNSUPPORTED_PROVIDER -> AuthException.InvalidRequest(resolvedMessage)
        CODE_REUSED_REFRESH_TOKEN -> AuthException.SessionRevoked(resolvedMessage)
        CODE_RECOVERY_EXPIRED -> AuthException.RecoveryExpired(resolvedMessage)
        CODE_RECOVERY_NOT_FOUND -> AuthException.RecoveryNotFound(resolvedMessage)
        else -> AuthException.Unknown(resolvedMessage)
    }
}

private const val CODE_INVALID_KAKAO_TOKEN = "AUTH_401_5"
private const val CODE_COMMON_BAD_REQUEST = "COMMON_400"
private const val CODE_UNSUPPORTED_PROVIDER = "AUTH_400_1"
private const val CODE_REUSED_REFRESH_TOKEN = "AUTH_401_4"
private const val CODE_RECOVERY_EXPIRED = "MEMBER_409_1"
private const val CODE_RECOVERY_NOT_FOUND = "MEMBER_404_1"
