package com.dororong.rodi.core.domain.model.review

import com.dororong.rodi.core.common.UserMessageProvider

sealed class ReviewException(
    override val userMessage: String,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause), UserMessageProvider {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Forbidden(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : ReviewException(message, cause)

    /** 주차장처럼 새 후기와 '별로에요' 평가를 받지 않는 장소다. 기존 후기는 '추천해요'로만 수정할 수 있다. */
    class PlaceNotReviewable(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class NotFound(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class LevelRequired(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class LevelChanged(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Network(message: String, cause: Throwable? = null) : ReviewException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : ReviewException(message, cause)
}
