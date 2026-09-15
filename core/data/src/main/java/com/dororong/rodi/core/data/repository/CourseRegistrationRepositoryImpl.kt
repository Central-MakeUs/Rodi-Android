package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.authRequest
import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.mapper.toAuthException as toApiAuthException
import com.dororong.rodi.core.data.mapper.toApprovalStatus
import com.dororong.rodi.core.data.mapper.toData
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.CourseApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.CourseRegistrationResult
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.domain.model.course.validateForSubmission
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CourseRegistrationRepositoryImpl @Inject constructor(
    private val api: CourseApi,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
    private val json: Json,
) : CourseRegistrationRepository {
    override suspend fun getRegistrationForm(): CourseRegistrationForm = authenticatedRequest {
        api.getRegistrationForm(it).requireData().toDomain()
    }

    override suspend fun registerCourse(request: CourseRegistrationRequest): CourseRegistrationResult {
        request.validateForSubmission()
        return authenticatedRequest { api.registerCourse(it, request.toData()).requireData().toDomain() }
    }

    override suspend fun getMyCourses(
        status: CourseApprovalStatus?,
        cursor: String?,
        size: Int,
    ): CursorPage<RegisteredCourse> = authenticatedRequest {
        api.getMyCourses(it, status?.name, size.coerceIn(1, 100), cursor).requireData().toDomain()
    }

    override suspend fun deleteCourse(courseId: Long) {
        authenticatedRequest { api.deleteCourse(it, courseId).requireSuccess() }
    }

    private suspend fun <T> authenticatedRequest(
        canRefresh: Boolean = true,
        block: suspend (String) -> T,
    ): T {
        val accessToken = tokenStore.getTokens()?.accessToken
            ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        return try {
            block("Bearer $accessToken")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val unauthorized = error is HttpException && error.code() == 401 ||
                error is AuthException.NotAuthenticated
            if (unauthorized && canRefresh) {
                authRepository.reissueToken()
                authenticatedRequest(canRefresh = false, block = block)
            } else {
                throw if (error is AuthException) error else error.toAuthException(json)
            }
        }
    }

    private fun <T> ApiEnvelope<T>.requireData(): T {
        if (!isSuccess) throw toAuthException()
        return data ?: throw AuthException.Unknown(message.ifBlank { "응답 데이터가 없습니다." })
    }

    private fun ApiEnvelope<*>.requireSuccess() {
        if (!isSuccess) throw toAuthException()
    }

    private fun ApiEnvelope<*>.toAuthException(): AuthException = when {
        code.contains("401") -> AuthException.NotAuthenticated(message)
        else -> this.toApiAuthException()
    }
}
