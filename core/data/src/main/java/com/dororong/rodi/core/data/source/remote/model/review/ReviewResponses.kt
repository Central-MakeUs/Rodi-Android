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
    val nickname: String,
    val memberLevel: String,
    val isRecommended: Boolean,
    val difficulty: String? = null,
    val congestion: String? = null,
    val practiceMethod: String? = null,
    val content: String? = null,
    val caution: String? = null,
    val isMine: Boolean,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val createdAt: String,
)

@Serializable
data class ReviewCreatedResponse(
    val reviewId: Long,
)

@Serializable
data class ReviewSummaryResponse(
    val level: String? = null,
    // 후기가 0건인 레벨은 서버가 카운트 필드를 아예 생략한다. 기본값 없이 선언하면
    // MissingFieldException이 그대로 스낵바에 노출된다.
    val totalCount: Long = 0L,
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
