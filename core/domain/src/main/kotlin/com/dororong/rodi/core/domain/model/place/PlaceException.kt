package com.dororong.rodi.core.domain.model.place

sealed class PlaceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class Network(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : PlaceException(message, cause)
}
