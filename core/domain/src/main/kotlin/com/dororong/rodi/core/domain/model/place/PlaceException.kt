package com.dororong.rodi.core.domain.model.place

import com.dororong.rodi.core.common.UserMessageProvider

sealed class PlaceException(
    override val userMessage: String,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause), UserMessageProvider {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class Network(message: String, cause: Throwable? = null) : PlaceException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : PlaceException(message, cause)
}
