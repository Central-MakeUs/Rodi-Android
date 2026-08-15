package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toAuthException
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.member.BlockedMember
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
    private val json: Json,
) : MemberRepository {
    override suspend fun getMyPage(): MyPage = authenticatedRequest { authorization ->
        memberApi.getMyPage(authorization).requireData().toDomain()
    }

    override suspend fun getPracticeRecords(cursor: String?, size: Int): CursorPage<PracticeRecordItem> = authenticatedRequest { authorization ->
        memberApi.getPracticeRecords(authorization, size, cursor).requireData().toDomain()
    }

    override suspend fun getMyReviews(cursor: String?, size: Int): CursorPage<MyReview> = authenticatedRequest { authorization ->
        memberApi.getMyReviews(authorization, size, cursor).requireData().toDomain()
    }

    override suspend fun getBlockedMembers(cursor: String?, size: Int): CursorPage<BlockedMember> = authenticatedRequest { authorization ->
        memberApi.getBlockedMembers(authorization, size, cursor).requireData().toDomain()
    }

    override suspend fun updateDrivingGoal(drivingGoal: String) {
        require(drivingGoal.length <= 30) { "운전 목표는 30자 이하여야 합니다." }
        authenticatedRequest { authorization ->
            memberApi.updateMe(authorization, MemberUpdateRequest(drivingGoal)).requireSuccess()
        }
    }

    override suspend fun updateFilterTags(filterTags: List<PracticeType>) {
        authenticatedRequest { authorization ->
            memberApi.updateFilterTags(
                authorization = authorization,
                request = FilterTagsRequest(filterTags.map(PracticeType::name)),
            ).requireSuccess()
        }
    }

    override suspend fun blockMember(memberId: Long) {
        authenticatedRequest { authorization -> memberApi.blockMember(authorization, memberId).requireSuccess() }
    }

    override suspend fun unblockMember(memberId: Long) {
        authenticatedRequest { authorization -> memberApi.unblockMember(authorization, memberId).requireSuccess() }
    }

    override suspend fun withdraw() {
        authenticatedRequest { authorization -> memberApi.withdraw(authorization).requireSuccess() }
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
        }
    }

    override suspend fun hardDelete() {
        authenticatedRequest { authorization -> memberApi.hardDelete(authorization).requireSuccess() }
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
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
