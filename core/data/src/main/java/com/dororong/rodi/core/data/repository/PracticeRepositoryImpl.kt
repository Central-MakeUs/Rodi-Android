package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.PracticeApi
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeSkipReasonRequest
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.practice.Practice
import com.dororong.rodi.core.domain.model.practice.PracticeException
import com.dororong.rodi.core.domain.model.practice.PracticeVisitResult
import com.dororong.rodi.core.domain.model.practice.SkipReasonForm
import com.dororong.rodi.core.domain.repository.PracticeRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class PracticeRepositoryImpl @Inject constructor(
    private val api: PracticeApi,
    private val tokenStore: AuthTokenStore,
    private val practiceRecordPresenceCache: PracticeRecordPresenceCache,
) : PracticeRepository {
    override suspend fun register(placeId: Long): Practice = authenticatedRequest {
        api.register(placeId).requireData().toDomain()
    }

    override suspend fun recordVisit(
        practiceId: Long,
        certifiedDistanceMeters: Int?,
    ): PracticeVisitResult = authenticatedRequest {
        api.recordVisit(
            practiceId = practiceId,
            request = PracticeVisitRequest(certifiedDistanceMeters),
        ).requireData().toDomain().also {
            practiceRecordPresenceCache.set(true)
        }
    }

    override suspend fun submitSkipReason(practiceId: Long, reason: String, detail: String?) {
        authenticatedRequest {
            api.submitSkipReason(
                practiceId = practiceId,
                request = PracticeSkipReasonRequest(reason, detail),
            ).requireSuccess()
        }
    }

    override suspend fun getSkipReasonForm(): SkipReasonForm = authenticatedRequest {
        api.getSkipReasonForm().requireData().toDomain()
    }

    /**
     * 토큰 주입과 401 재발급은 OkHttp의 AuthHeaderInterceptor·TokenAuthenticator가 한다.
     * 여기서는 로그인 여부만 확인하고, 남은 실패를 도메인 예외로 바꾼다.
     */
    private suspend fun <T> authenticatedRequest(block: suspend () -> T): T {
        tokenStore.getTokens()?.accessToken ?: throw PracticeException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw error.toPracticeException()
        }
    }

    private fun <T> ApiEnvelope<T>.requireData(): T {
        if (!isSuccess) throw toPracticeException()
        return data ?: throw PracticeException.Unexpected("$code: ${message.ifBlank { "응답 데이터가 없습니다." }}")
    }

    private fun ApiEnvelope<*>.requireSuccess() {
        if (!isSuccess) throw toPracticeException()
    }

    private fun ApiEnvelope<*>.toPracticeException(): PracticeException = when {
        code.contains("400") -> PracticeException.InvalidRequest(message)
        code.contains("401") -> PracticeException.AuthenticationRequired(message)
        code.contains("403") -> PracticeException.Forbidden(message)
        code.contains("404") -> PracticeException.NotFound(message)
        code.contains("409") -> PracticeException.SkipReasonAlreadySubmitted(message)
        else -> PracticeException.Unexpected("$code: ${message.ifBlank { "연습 요청에 실패했습니다." }}")
    }

    private fun Throwable.toPracticeException(): PracticeException = when (this) {
        is PracticeException -> this
        is HttpException -> when (code()) {
            400 -> PracticeException.InvalidRequest(message(), this)
            401 -> PracticeException.AuthenticationRequired(message(), this)
            403 -> PracticeException.Forbidden(message(), this)
            404 -> PracticeException.NotFound(message(), this)
            409 -> PracticeException.SkipReasonAlreadySubmitted(message(), this)
            else -> PracticeException.Unexpected(message(), this)
        }
        is IOException -> PracticeException.Network("네트워크 연결을 확인해주세요.", this)
        is AuthException -> PracticeException.AuthenticationRequired(message ?: "로그인이 필요합니다.", this)
        else -> PracticeException.Unexpected("연습 요청에 실패했습니다.", this)
    }
}
