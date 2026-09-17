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
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CourseRegistrationRepositoryImpl @Inject constructor(
    private val api: CourseApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : CourseRegistrationRepository {
    override suspend fun getRegistrationForm(): CourseRegistrationForm = authenticatedRequest {
        api.getRegistrationForm().requireData().toDomain()
    }

    override suspend fun registerCourse(request: CourseRegistrationRequest): CourseRegistrationResult =
        authenticatedRequest { api.registerCourse(request.toData()).requireData().toDomain() }

    override suspend fun getMyCourses(
        status: CourseApprovalStatus?,
        cursor: String?,
        size: Int,
    ): CursorPage<RegisteredCourse> = authenticatedRequest {
        api.getMyCourses(status?.name, size.coerceIn(1, 100), cursor).requireData().toDomain()
    }

    override suspend fun deleteCourse(courseId: Long) {
        authenticatedRequest { api.deleteCourse(courseId).requireSuccess() }
    }

    /**
     * 토큰 주입과 401 재발급은 OkHttp의 AuthHeaderInterceptor·TokenAuthenticator가 한다.
     * 여기서는 로그인 여부만 확인하고, 남은 실패를 도메인 예외로 바꾼다.
     */
    private suspend fun <T> authenticatedRequest(block: suspend () -> T): T {
        tokenStore.getTokens()?.accessToken ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw if (error is AuthException) error else error.toAuthException(json)
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
