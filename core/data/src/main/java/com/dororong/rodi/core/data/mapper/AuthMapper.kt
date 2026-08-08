package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.auth.AuthTokenResponse
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun SocialLoginResponse.toAccountRestoreResult(): AccountRestoreResult = when (status) {
    STATUS_SUCCESS -> AccountRestoreResult.Restored(
        isNewMember = requireField(isNewMember, "isNewMember"),
        nickname = requireField(nickname?.takeIf { it.isNotBlank() }, "nickname"),
    )
    STATUS_WITHDRAWAL_PENDING -> AccountRestoreResult.WithdrawalPending(
        withdrawalRequestedAt = parseDateTime(withdrawalRequestedAt, "withdrawalRequestedAt"),
        recoverableUntil = parseDateTime(recoverableUntil, "recoverableUntil"),
    )
    else -> throw AuthException.Unknown("복구 응답 상태를 처리할 수 없습니다.")
}

fun SocialLoginResponse.toLoginResult(): LoginResult = when (status) {
    STATUS_SUCCESS -> LoginResult.Success(
        isNewMember = requireField(isNewMember, "isNewMember"),
        nickname = requireField(nickname?.takeIf { it.isNotBlank() }, "nickname"),
    )
    STATUS_WITHDRAWAL_PENDING -> LoginResult.WithdrawalPending(
        withdrawalRequestedAt = parseDateTime(withdrawalRequestedAt, "withdrawalRequestedAt"),
        recoverableUntil = parseDateTime(recoverableUntil, "recoverableUntil"),
    )
    else -> throw AuthException.Unknown("로그인 응답 상태를 처리할 수 없습니다.")
}

fun SocialLoginResponse.toAuthTokenResponse(): AuthTokenResponse = AuthTokenResponse(
    accessToken = requireField(accessToken?.takeIf { it.isNotBlank() }, "accessToken"),
    refreshToken = requireField(refreshToken?.takeIf { it.isNotBlank() }, "refreshToken"),
    isNewMember = requireField(isNewMember, "isNewMember"),
)

private fun parseDateTime(value: String?, field: String) = try {
    OffsetDateTime.parse(requireField(value, field)).toInstant()
} catch (_: DateTimeParseException) {
    throw AuthException.Unknown("인증 응답의 $field 값이 올바르지 않습니다.")
}

private fun <T> requireField(value: T?, field: String): T =
    value ?: throw AuthException.Unknown("인증 응답에 $field 값이 없습니다.")

private const val STATUS_SUCCESS = "SUCCESS"
private const val STATUS_WITHDRAWAL_PENDING = "WITHDRAWAL_PENDING"
