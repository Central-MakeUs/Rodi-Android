package com.dororong.rodi.core.domain.model.practice

import com.dororong.rodi.core.common.UserMessageProvider

sealed class PracticeException(
    override val userMessage: String,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause), UserMessageProvider {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Forbidden(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class SkipReasonAlreadySubmitted(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Network(message: String, cause: Throwable? = null) : PracticeException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : PracticeException(message, cause)
}
