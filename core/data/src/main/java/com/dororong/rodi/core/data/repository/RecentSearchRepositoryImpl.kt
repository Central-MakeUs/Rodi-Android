package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.RecentSearchApi
import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchRegisterRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class RecentSearchRepositoryImpl @Inject constructor(
    private val recentSearchApi: RecentSearchApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : RecentSearchRepository {
    override suspend fun getRecentSearches() = authenticatedRequest {
        recentSearchApi.getRecentSearches().requireData().map { it.toDomain() }
    }

    override suspend fun registerRecentSearch(search: RecentSearchRegistration) {
        authenticatedRequest {
            recentSearchApi.registerRecentSearch(
                request = RecentSearchRegisterRequest(
                    type = search.type.name,
                    keyword = search.keyword,
                    placeId = search.placeId,
                ),
            ).requireSuccess()
        }
    }

    override suspend fun deleteAllRecentSearches() {
        authenticatedRequest {
            recentSearchApi.deleteAllRecentSearches().requireSuccess()
        }
    }

    override suspend fun deleteRecentSearch(id: Long) {
        authenticatedRequest {
            recentSearchApi.deleteRecentSearch(id).requireSuccess()
        }
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
        if (!isSuccess) throw asException()
        return data ?: throw AuthException.Unknown(message.ifBlank { "응답 데이터가 없습니다." })
    }

    private fun ApiEnvelope<*>.requireSuccess() {
        if (!isSuccess) throw asException()
    }

    private fun ApiEnvelope<*>.asException(): AuthException =
        if (code.contains("401")) AuthException.NotAuthenticated(message) else toAuthException()
}
