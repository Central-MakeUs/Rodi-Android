package com.dororong.rodi.core.domain.model.course

import com.dororong.rodi.core.common.UserMessageProvider

sealed class CourseReportException(
    override val userMessage: String,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause), UserMessageProvider {
    class AuthenticationRequired(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
    class OwnCourse(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
    class Unavailable(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
    class Network(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
    class Unexpected(message: String, cause: Throwable? = null) : CourseReportException(message, cause)
}
