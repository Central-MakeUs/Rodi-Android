package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.RecentSearchApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class RecentSearchRepositoryImpl @Inject constructor(
    private val recentSearchApi: RecentSearchApi,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
    private val json: Json,
) : RecentSearchRepository {
    override suspend fun getRecentSearches(): List<RecentSearch> = authenticatedRequest { authorization ->
        recentSearchApi.getRecentSearches(authorization).requireData().map { response ->
            RecentSearch(id = response.id, keyword = response.keyword)
        }
    }

    override suspend fun deleteAllRecentSearches() {
        authenticatedRequest { authorization ->
            recentSearchApi.deleteAllRecentSearches(authorization).requireSuccess()
        }
    }

    override suspend fun deleteRecentSearch(id: Long) {
        authenticatedRequest { authorization ->
            recentSearchApi.deleteRecentSearch(authorization, id).requireSuccess()
        }
    }

    private suspend fun <T> authenticatedRequest(
        canRefresh: Boolean = true,
        block: suspend (String) -> T,
    ): T {
        val token = tokenStore.getTokens()?.accessToken
            ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        return try {
            block("Bearer $token")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val isUnauthorized = error is AuthException.NotAuthenticated ||
                (error is HttpException && error.code() == 401)
            if (isUnauthorized && canRefresh) {
                authRepository.reissueToken()
                authenticatedRequest(canRefresh = false, block = block)
            } else {
                throw if (error is AuthException) error else error.toAuthException(json)
            }
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
