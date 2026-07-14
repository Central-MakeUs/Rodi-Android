package com.dororong.rodi.core.domain.model.auth

sealed class AuthException(message: String) : Exception(message) {
    class InvalidRequest(message: String) : AuthException(message)
    class InvalidCredential(message: String) : AuthException(message)
    class NotAuthenticated(message: String) : AuthException(message)
    class SessionRevoked(message: String) : AuthException(message)
    class RecoveryExpired(message: String) : AuthException(message)
    class RecoveryNotFound(message: String) : AuthException(message)
    class Network(message: String) : AuthException(message)
    class Unknown(message: String) : AuthException(message)
}
