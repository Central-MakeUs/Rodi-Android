package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageMyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.CursorPageBlockedMemberItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.BlockedMemberItemResponse
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.LevelProgress
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.member.BlockedMember
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel

fun MyPageResponse.toDomain() = MyPage(
    nickname = nickname,
    level = runCatching { OnboardingLevel.valueOf(level) }
        .getOrElse { throw IllegalArgumentException("Unsupported onboarding level: $level") },
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
    visitedAt = visitedAt?.let(::parseServerTimestamp),
    isVerified = isVerified,
    hasReview = hasReview,
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
