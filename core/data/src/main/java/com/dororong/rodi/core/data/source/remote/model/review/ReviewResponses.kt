package com.dororong.rodi.core.data.source.remote.model.review

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequest(
    val isRecommended: Boolean,
    val difficulty: String,
    val congestion: String,
    val practiceMethod: String,
    val content: String? = null,
    val caution: String? = null,
)

@Serializable
data class ReportRequest(
    val reason: String,
    val detail: String? = null,
    val detailConsistent: Boolean,
)

@Serializable
data class CursorPageReviewResponse(
    val items: List<ReviewResponse> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class ReviewResponse(
    val reviewId: Long,
    val memberId: Long,
    val nickname: String? = null,
    val practiceMethod: String? = null,
    val content: String? = null,
    val isMine: Boolean,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val isVerifiedVisit: Boolean,
    val createdAt: String,
)

@Serializable
data class ReviewCreatedResponse(
    val reviewId: Long,
)

@Serializable
data class ReviewDetailResponse(
    val reviewId: Long,
    val placeId: Long,
    val placeName: String,
    val isRecommended: Boolean,
    val difficulty: String? = null,
    val congestion: String? = null,
    val practiceMethod: String? = null,
    val content: String? = null,
    val caution: String? = null,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val isVerifiedVisit: Boolean,
    val createdAt: String,
)

@Serializable
data class ReviewSummaryResponse(
    val level: String? = null,
    // 2026-08-13 Swagger 재대조 결과 totalCount는 더 이상 없다 — levelReviewCount(선택 레벨
    // 기준)/totalReviewCount(전체 레벨 합산)로 나뉘었다. levelReviewCount는 아직 UI에서 안 쓴다.
    // topDifficulty(신규, 서버가 동률 처리까지 계산해 내려줌)는 클라이언트가 difficultyCounts로
    // 이미 같은 규칙(동률이면 더 어려운 쪽 우선)을 계산하고 있어 당장은 매핑하지 않는다.
    val levelReviewCount: Long = 0L,
    val totalReviewCount: Long = 0L,
    val recommendCount: Long = 0L,
    val notRecommendCount: Long = 0L,
    val difficultyCounts: Map<String, Long> = emptyMap(),
    val congestionCounts: Map<String, Long> = emptyMap(),
    val levelCounts: Map<String, Long> = emptyMap(),
)

@Serializable
data class ReportFormResponse(
    val questionId: String,
    val title: String,
    val description: String? = null,
    val required: Boolean,
    val options: List<ReportFormOptionResponse> = emptyList(),
)

@Serializable
data class ReportFormOptionResponse(
    val code: String,
    val label: String,
    val order: Int,
    val requiresTextInput: Boolean,
    val textInputPlaceholder: String? = null,
    val textInputMaxLength: Int? = null,
)
