package com.dororong.rodi.core.domain

sealed class AuthException(message: String) : Exception(message) {
    class InvalidRequest(message: String) : AuthException(message)
    class InvalidCredential(message: String) : AuthException(message)
    class Network(message: String) : AuthException(message)
    class Unknown(message: String) : AuthException(message)
}
