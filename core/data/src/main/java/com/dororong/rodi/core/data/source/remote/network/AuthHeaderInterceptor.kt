package com.dororong.rodi.core.data.source.remote.network

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 보호 API에 액세스 토큰을 붙인다. 토큰이 없으면 헤더 없이 보낸다 — 장소 목록처럼 비로그인도
 * 부를 수 있는 API가 있어서, 여기서 막지 않고 서버 응답(401)으로 판단하게 둔다.
 *
 * OkHttp 인터셉터는 블로킹 호출이라 토큰 조회를 [runBlocking]으로 감싼다. 조회는 메모리 캐시라
 * 첫 호출 외에는 I/O가 없다.
 */
@Singleton
class AuthHeaderInterceptor @Inject constructor(
    private val tokenStore: AuthTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val tokens = runBlocking { tokenStore.getTokens() }
            ?: return chain.proceed(request)
        val authorization = request.header(HEADER_AUTHORIZATION)
        if (authorization != null && authorization != bearer(tokens.accessToken)) return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, bearer(tokens.accessToken))
                .tag(AuthRequestSession::class.java, AuthRequestSession(tokens.sessionId))
                .build(),
        )
    }
}

internal const val HEADER_AUTHORIZATION = "Authorization"

internal fun bearer(accessToken: String): String = "Bearer $accessToken"
