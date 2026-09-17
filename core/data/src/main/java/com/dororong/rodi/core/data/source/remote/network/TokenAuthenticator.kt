package com.dororong.rodi.core.data.source.remote.network

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.domain.repository.AuthRepository
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401을 받으면 토큰을 재발급하고 그 요청만 한 번 더 보낸다.
 *
 * 동시에 여러 요청이 401을 받아도 재발급은 한 번만 일어난다 — [AuthRepository.reissueToken]이
 * Mutex와 "들고 있던 refreshToken이 이미 바뀌었으면 재발급하지 않는다" 가드를 갖고 있고,
 * 뒤늦게 들어온 쪽은 갱신된 토큰을 그대로 쓴다.
 *
 * [AuthRepository]를 [Lazy]로 받는 이유는 순환 의존 때문이다 —
 * OkHttpClient → Authenticator → AuthRepository → AuthApi → Retrofit → OkHttpClient.
 * AuthApi는 인증이 붙지 않은 클라이언트로 만들지만, 그래프 구성 시점의 순환도 함께 끊는다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val authRepository: Lazy<AuthRepository>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.retryCount() >= MAX_RETRY) return null
        val failedToken = response.request.header(HEADER_AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)

        return runBlocking {
            // 다른 요청이 이미 재발급을 끝냈으면 그 토큰으로 바로 재시도한다. 이 비교를 재발급
            // 뒤에 두면, 갱신된 refreshToken을 기준으로 삼아 한 번 더 재발급이 돌아간다.
            val current = tokenStore.getTokens()?.accessToken
            if (current != null && current != failedToken) {
                return@runBlocking response.request.retryWith(current)
            }

            try {
                authRepository.get().reissueToken()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // 재발급이 실패하면 원래 401을 그대로 돌려준다. 세션 만료 처리는 AuthRepository가 한다.
                return@runBlocking null
            }
            val refreshed = tokenStore.getTokens()?.accessToken ?: return@runBlocking null
            if (refreshed == failedToken) return@runBlocking null

            response.request.retryWith(refreshed)
        }
    }

    private fun Request.retryWith(accessToken: String): Request = newBuilder()
        .header(HEADER_AUTHORIZATION, bearer(accessToken))
        .build()

    private fun Response.retryCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRY = 2
        const val BEARER_PREFIX = "Bearer "
    }
}
