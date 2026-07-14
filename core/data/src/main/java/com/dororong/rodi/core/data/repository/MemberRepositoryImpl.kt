package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.authRequest
import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.MemberRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : MemberRepository {
    override suspend fun withdraw() {
        val tokens = tokenStore.getTokens() ?: throw AuthException.NotAuthenticated("로그인 세션이 없습니다.")
        val response = json.authRequest { memberApi.withdraw("Bearer ${tokens.accessToken}") }
        if (!response.isSuccess) throw response.toAuthException()
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
        }
    }
}
