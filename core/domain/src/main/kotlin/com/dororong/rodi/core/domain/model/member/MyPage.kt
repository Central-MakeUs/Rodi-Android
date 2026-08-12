package com.dororong.rodi.core.domain.model.member

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus

data class MyPage(
    val nickname: String,
    val level: OnboardingLevel,
    val recommendationTags: List<String>,
    val drivingGoal: String?,
    val savedPlaceCount: Long,
    val levelProgress: LevelProgress = LevelProgress(),
)

data class LevelProgress(
    val totalDistanceKm: Double = 0.0,
    val currentLevelStartKm: Double = 0.0,
    val nextLevelKm: Double? = null,
    val progressPercent: Int = 0,
)

data class PracticeRecordItem(
    val practiceId: Long,
    val placeId: Long,
    val placeName: String,
    val practiceTypes: List<PracticeType>,
    val visitCount: Int,
    val visitedAt: java.time.Instant?,
    val isVerified: Boolean,
    val hasReview: Boolean,
    val status: PracticeStatus = PracticeStatus.PLANNED,
)

data class MyReview(
    val reviewId: Long,
    val placeId: Long,
    val placeName: String,
    val content: String?,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val isVerifiedVisit: Boolean,
    val createdAt: java.time.Instant,
)

data class BlockedMember(
    val memberId: Long,
    val nickname: String?,
    val blockedAt: java.time.Instant,
)
