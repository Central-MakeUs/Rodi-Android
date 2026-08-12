package com.dororong.rodi.core.domain.model.auth

import com.dororong.rodi.core.common.UserMessageProvider

sealed class AuthException(
    override val userMessage: String,
) : Exception(userMessage), UserMessageProvider {
    class InvalidRequest(message: String) : AuthException(message)
    class InvalidCredential(message: String) : AuthException(message)
    class NotAuthenticated(message: String) : AuthException(message)
    class SessionRevoked(message: String) : AuthException(message)
    class RecoveryExpired(message: String) : AuthException(message)
    class RecoveryNotFound(message: String) : AuthException(message)
    class Network(message: String) : AuthException(message)
    class Unknown(message: String) : AuthException(message)
}
