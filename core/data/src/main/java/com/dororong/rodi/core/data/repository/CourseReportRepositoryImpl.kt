package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toCourseIssueForm
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.CourseApi
import com.dororong.rodi.core.data.source.remote.model.course.CourseReportRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.course.CourseReportException
import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.repository.CourseReportRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class CourseReportRepositoryImpl @Inject constructor(
    private val api: CourseApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : CourseReportRepository {
    override suspend fun getReportForm(): ReviewIssueForm = authenticatedRequest {
        val response = api.getReportForm()
        if (!response.isSuccess) throw response.toReportException()
        requireNotNull(response.data).toCourseIssueForm()
    }

    override suspend fun reportCourse(courseId: Long, submission: CourseReportSubmission) {
        authenticatedRequest {
            val response = api.reportCourse(
                courseId = courseId,
                request = CourseReportRequest(submission.courseIssues, submission.detail),
            )
            if (!response.isSuccess) throw response.toReportException()
        }
    }

    private suspend fun <T> authenticatedRequest(block: suspend () -> T): T {
        tokenStore.getTokens()?.accessToken
            ?: throw CourseReportException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: CourseReportException) {
            throw error
        } catch (error: HttpException) {
            throw error.toReportException()
        } catch (error: IOException) {
            throw CourseReportException.Network("네트워크 연결을 확인해주세요.", error)
        } catch (error: Throwable) {
            throw CourseReportException.Unexpected(UNEXPECTED_MESSAGE, error)
        }
    }

    private fun HttpException.toReportException(): CourseReportException {
        val body = runCatching {
            json.parseToJsonElement(response()?.errorBody()?.string().orEmpty()).jsonObject
        }.getOrNull()
        val code = body?.get("code")?.jsonPrimitive?.contentOrNull
        val message = body?.get("message")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        return reportException(code ?: code().toString(), message, this)
    }

    private fun ApiEnvelope<*>.toReportException(): CourseReportException =
        reportException(code, message.takeIf(String::isNotBlank), null)

    private fun reportException(code: String, message: String?, cause: Throwable?): CourseReportException =
        when {
            code == CODE_OWN_COURSE -> CourseReportException.OwnCourse(message ?: UNEXPECTED_MESSAGE, cause)
            code.contains("401") -> CourseReportException.AuthenticationRequired(message ?: "로그인이 필요합니다.", cause)
            code.contains("404") -> CourseReportException.Unavailable(message ?: UNEXPECTED_MESSAGE, cause)
            code.contains("400") -> CourseReportException.InvalidRequest(message ?: UNEXPECTED_MESSAGE, cause)
            else -> CourseReportException.Unexpected(UNEXPECTED_MESSAGE, cause)
        }
}

private const val CODE_OWN_COURSE = "COURSE_400_1"
private const val UNEXPECTED_MESSAGE = "신고하지 못했어요."
