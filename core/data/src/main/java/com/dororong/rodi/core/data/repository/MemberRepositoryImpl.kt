package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.common.graphemeLength
import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
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
import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
    private val json: Json,
    private val practiceRecordPresenceCache: PracticeRecordPresenceCache,
    private val practiceSessionRepository: PracticeSessionRepository,
) : MemberRepository {
    override suspend fun completeCourseTutorial() {
        authenticatedRequest { authorization ->
            memberApi.completeCourseTutorial(authorization).requireData()
        }
        if (!tokenStore.markCourseTutorialCompleted()) {
            throw AuthException.Unknown("튜토리얼 완료 상태를 저장하지 못했습니다.")
        }
    }

    override suspend fun getMyPage(): MyPage = authenticatedRequest { authorization ->
        memberApi.getMyPage(authorization).requireData().toDomain()
    }

    override suspend fun getPracticeRecords(cursor: String?, size: Int): CursorPage<PracticeRecordItem> = authenticatedRequest { authorization ->
        if (cursor == null) {
            practiceRecordPresenceCache.withRefresh {
                memberApi.getPracticeRecords(authorization, size, cursor).requireData().toDomain()
                    .also { page -> updatePracticeRecordPresence(page, canProveAbsence = true) }
            }
        } else {
            memberApi.getPracticeRecords(authorization, size, cursor).requireData().toDomain()
                .also { page -> updatePracticeRecordPresence(page, canProveAbsence = false) }
        }
    }

    override suspend fun hasPracticeRecords(): Boolean = practiceRecordPresenceCache.getOrLoadOrNull {
        authenticatedRequest { authorization ->
            var cursor: String? = null
            var hasVisitedRecord = false
            var reachedEnd = false
            var pageCount = 0
            while (pageCount < MAX_PRACTICE_PRESENCE_PAGES) {
                pageCount++
                val page = memberApi.getPracticeRecords(
                    authorization = authorization,
                    size = PRACTICE_PRESENCE_PAGE_SIZE,
                    cursor = cursor,
                ).requireData().toDomain()
                updatePracticeRecordPresence(page, canProveAbsence = cursor == null)
                if (page.items.any { it.status == PracticeStatus.VISITED }) {
                    hasVisitedRecord = true
                    break
                }

                val nextCursor = page.nextCursor
                if (!page.hasNext || nextCursor == null || nextCursor == cursor) {
                    reachedEnd = true
                    break
                }
                cursor = nextCursor
            }
            if (hasVisitedRecord || reachedEnd) hasVisitedRecord else null
        }
    } ?: false

    private fun updatePracticeRecordPresence(
        page: CursorPage<PracticeRecordItem>,
        canProveAbsence: Boolean,
    ) {
        when {
            page.items.any { it.status == PracticeStatus.VISITED } -> practiceRecordPresenceCache.set(true)
            canProveAbsence && !page.hasNext -> practiceRecordPresenceCache.set(false)
        }
    }

    override suspend fun getMyReviews(cursor: String?, size: Int): CursorPage<MyReview> = authenticatedRequest { authorization ->
        memberApi.getMyReviews(authorization, size, cursor).requireData().toDomain()
    }

    override suspend fun getBlockedMembers(cursor: String?, size: Int): CursorPage<BlockedMember> = authenticatedRequest { authorization ->
        memberApi.getBlockedMembers(authorization, size, cursor).requireData().toDomain()
    }

    override suspend fun updateDrivingGoal(drivingGoal: String) {
        require(drivingGoal.graphemeLength() <= 30) { "운전 목표는 30자 이하여야 합니다." }
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
        practiceSessionRepository.clear()
        tokenStore.clearCourseRegistrationData()
        if (!tokenStore.clear()) {
            throw AuthException.Unknown("로그인 정보를 안전하게 삭제하지 못했습니다.")
        }
        practiceRecordPresenceCache.clear()
    }

    override suspend fun hardDelete(): HardDeleteResult {
        authenticatedRequest { authorization -> memberApi.hardDelete(authorization).requireSuccess() }
        var localCleanupSucceeded = true
        try {
            practiceSessionRepository.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            localCleanupSucceeded = false
        }
        val tokensCleared = try {
            tokenStore.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            false
        }
        if (!tokensCleared) {
            localCleanupSucceeded = false
        }
        try {
            tokenStore.clearCourseRegistrationData()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            localCleanupSucceeded = false
        }
        practiceRecordPresenceCache.clear()
        return HardDeleteResult(localCleanupSucceeded = localCleanupSucceeded)
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

private const val PRACTICE_PRESENCE_PAGE_SIZE = 20
private const val MAX_PRACTICE_PRESENCE_PAGES = 5
