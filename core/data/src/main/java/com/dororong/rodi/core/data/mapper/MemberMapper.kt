package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageMyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageBlockedMemberItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.BlockedMemberItemResponse
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.LevelProgress
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.member.BlockedMember
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import timber.log.Timber

fun MyPageResponse.toDomain() = MyPage(
    nickname = nickname,
    level = level.toOnboardingLevel(),
    recommendationTags = recommendationTags,
    drivingGoal = drivingGoal,
    savedPlaceCount = savedPlaceCount,
    levelProgress = LevelProgress(
        totalDistanceKm = levelProgress.totalDistanceKm,
        currentLevelStartKm = levelProgress.currentLevelStartKm,
        nextLevelKm = levelProgress.nextLevelKm,
        progressPercent = levelProgress.progressPercent,
    ),
)

fun CursorPagePracticeItemResponse.toDomain() = CursorPage(
    items = items.map(PracticeItemResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

fun PracticeItemResponse.toDomain() = PracticeRecordItem(
    practiceId = practiceId,
    placeId = placeId,
    placeName = placeName,
    practiceTypes = practiceTypes.mapNotNull { value -> PracticeType.entries.firstOrNull { it.name == value } },
    visitCount = visitCount,
    visitedAt = lastActivityAt?.let(::parseServerTimestamp),
    isVerified = isVerified,
    hasReview = hasReview,
    status = status.toPracticeStatus(),
)

fun CursorPageMyReviewItemResponse.toDomain() = CursorPage(
    items = items.map(MyReviewItemResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

fun MyReviewItemResponse.toDomain() = MyReview(
    reviewId = reviewId,
    placeId = placeId,
    placeName = placeName,
    content = content,
    isEditable = isEditable,
    isHidden = isHidden,
    isVerifiedVisit = isVerifiedVisit,
    createdAt = parseServerTimestamp(createdAt),
)

fun CursorPageBlockedMemberItemResponse.toDomain() = CursorPage(
    items = items.map(BlockedMemberItemResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

fun BlockedMemberItemResponse.toDomain() = BlockedMember(
    memberId = memberId,
    nickname = nickname,
    blockedAt = parseServerTimestamp(blockedAt),
)

/**
 * 레벨은 프로필 전체의 표시를 좌우하는 제어 값이라 모르는 값을 SEED로 덮지 않는다.
 * 덮으면 서버가 레벨을 추가했을 때 사용자에게 틀린 레벨을 조용히 보여주게 된다.
 */
private fun String.toOnboardingLevel(): OnboardingLevel =
    OnboardingLevel.entries.firstOrNull { it.name == this }
        ?: run {
            Timber.w("Unknown member level value: %s", this)
            throw AuthException.Unknown("프로필을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.")
        }
