package com.dororong.rodi.core.data.source.remote.model.member

import kotlinx.serialization.Serializable

@Serializable
data class MyPageResponse(
    val nickname: String,
    val level: String,
    val recommendationTags: List<String>,
    val drivingGoal: String? = null,
    val savedPlaceCount: Long,
    val levelProgress: LevelProgressResponse,
)

@Serializable
data class LevelProgressResponse(
    val totalDistanceKm: Double,
    val currentLevelStartKm: Double,
    val nextLevelKm: Double? = null,
    val progressPercent: Int,
)

@Serializable
data class CursorPagePracticeItemResponse(
    val items: List<PracticeItemResponse>,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class PracticeItemResponse(
    val practiceId: Long,
    val placeId: Long,
    val placeName: String,
    val practiceTypes: List<String>,
    val status: String,
    val visitCount: Int,
    val lastActivityAt: String? = null,
    val isVerified: Boolean = false,
    val hasReview: Boolean,
)

@Serializable
data class CursorPageMyReviewItemResponse(
    val items: List<MyReviewItemResponse>,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class MyReviewItemResponse(
    val reviewId: Long,
    val placeId: Long,
    val placeName: String,
    val content: String? = null,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val isVerifiedVisit: Boolean,
    val createdAt: String,
)

@Serializable
data class CursorPageBlockedMemberItemResponse(
    val items: List<BlockedMemberItemResponse>,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class BlockedMemberItemResponse(
    val memberId: Long,
    val nickname: String? = null,
    val blockedAt: String,
)

@Serializable
data class MemberUpdateRequest(
    val drivingGoal: String,
)

@Serializable
data class FilterTagsRequest(
    val filterTags: List<String>,
)

@Serializable
data class CourseTutorialCompletionResponse(
    val courseTutorialCompletedAt: String,
)
