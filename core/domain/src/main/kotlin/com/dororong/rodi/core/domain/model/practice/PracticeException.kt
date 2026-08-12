package com.dororong.rodi.core.domain.model.practice

sealed class PracticeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Forbidden(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class SkipReasonAlreadySubmitted(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Network(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : PracticeException(message, cause)
}
