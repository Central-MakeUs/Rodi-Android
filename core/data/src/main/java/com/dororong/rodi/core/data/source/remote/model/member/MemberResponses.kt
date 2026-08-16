package com.dororong.rodi.core.data.source.remote.model.member

import kotlinx.serialization.Serializable

@Serializable
data class MyPageResponse(
    val nickname: String = "",
    val level: String = "",
    val recommendationTags: List<String> = emptyList(),
    val drivingGoal: String? = null,
    val savedPlaceCount: Long = 0,
    val levelProgress: LevelProgressResponse = LevelProgressResponse(),
)

@Serializable
data class LevelProgressResponse(
    val totalDistanceKm: Double = 0.0,
    val currentLevelStartKm: Double = 0.0,
    val nextLevelKm: Double? = null,
    val progressPercent: Int = 0,
)

@Serializable
data class CursorPagePracticeItemResponse(
    val items: List<PracticeItemResponse> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class PracticeItemResponse(
    val practiceId: Long,
    val placeId: Long,
    val placeName: String,
    val practiceTypes: List<String> = emptyList(),
    val status: String = "PLANNED",
    val visitCount: Int = 0,
    val lastActivityAt: String? = null,
    val isVerified: Boolean = false,
    val hasReview: Boolean = false,
)

@Serializable
data class CursorPageMyReviewItemResponse(
    val items: List<MyReviewItemResponse> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class MyReviewItemResponse(
    val reviewId: Long,
    val placeId: Long,
    val placeName: String,
    val content: String? = null,
    val isEditable: Boolean = false,
    val isHidden: Boolean = false,
    val isVerifiedVisit: Boolean = false,
    val createdAt: String,
)

@Serializable
data class CursorPageBlockedMemberItemResponse(
    val items: List<BlockedMemberItemResponse> = emptyList(),
    val hasNext: Boolean = false,
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
