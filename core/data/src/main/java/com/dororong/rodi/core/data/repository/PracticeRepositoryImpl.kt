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
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.PracticeRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class PracticeRepositoryImpl @Inject constructor(
    private val api: PracticeApi,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
    private val practiceRecordPresenceCache: PracticeRecordPresenceCache,
) : PracticeRepository {
    override suspend fun register(placeId: Long): Practice = authenticatedRequest { authorization ->
        api.register(authorization, placeId).requireData().toDomain()
    }

    override suspend fun recordVisit(
        practiceId: Long,
        certifiedDistanceMeters: Int?,
    ): PracticeVisitResult = authenticatedRequest { authorization ->
        api.recordVisit(
            authorization = authorization,
            practiceId = practiceId,
            request = PracticeVisitRequest(certifiedDistanceMeters),
        ).requireData().toDomain().also {
            practiceRecordPresenceCache.set(true)
        }
    }

    override suspend fun submitSkipReason(practiceId: Long, reason: String, detail: String?) {
        authenticatedRequest { authorization ->
            api.submitSkipReason(
                authorization = authorization,
                practiceId = practiceId,
                request = PracticeSkipReasonRequest(reason, detail),
            ).requireSuccess()
        }
    }

    override suspend fun getSkipReasonForm(): SkipReasonForm = authenticatedRequest { authorization ->
        api.getSkipReasonForm(authorization).requireData().toDomain()
    }

    private suspend fun <T> authenticatedRequest(
        canRefresh: Boolean = true,
        block: suspend (String) -> T,
    ): T {
        val accessToken = tokenStore.getTokens()?.accessToken
            ?: throw PracticeException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block("Bearer $accessToken")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val mapped = error.toPracticeException()
            if (mapped is PracticeException.AuthenticationRequired && canRefresh) {
                try {
                    authRepository.reissueToken()
                } catch (refreshError: CancellationException) {
                    throw refreshError
                } catch (refreshError: Throwable) {
                    throw refreshError.toPracticeException()
                }
                authenticatedRequest(canRefresh = false, block = block)
            } else {
                throw mapped
            }
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
