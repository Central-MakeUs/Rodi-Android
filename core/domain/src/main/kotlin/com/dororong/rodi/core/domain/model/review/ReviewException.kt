package com.dororong.rodi.core.domain.model.review

sealed class ReviewException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Forbidden(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class LevelRequired(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class LevelChanged(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Network(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : ReviewException(message, cause)
}
