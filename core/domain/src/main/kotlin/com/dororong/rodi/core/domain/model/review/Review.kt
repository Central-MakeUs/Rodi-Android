package com.dororong.rodi.core.domain.model.review

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import java.time.Instant

enum class ReviewDifficulty {
    VERY_EASY,
    EASY,
    NORMAL,
    HARD,
    VERY_HARD,
}

enum class ReviewCongestion {
    QUIET,
    NORMAL,
    CROWDED,
}

enum class PracticeMethod {
    SOLO,
    WITH_COMPANION,
}

data class Review(
    val reviewId: Long,
    val memberId: Long,
    val nickname: String,
    val memberLevel: OnboardingLevel,
    val isRecommended: Boolean,
    val difficulty: ReviewDifficulty?,
    val congestion: ReviewCongestion?,
    val practiceMethod: PracticeMethod?,
    val content: String?,
    val caution: String?,
    val isMine: Boolean,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val createdAt: Instant,
)

data class ReviewDetail(
    val reviewId: Long,
    val placeId: Long,
    val placeName: String,
    val isRecommended: Boolean,
    val difficulty: ReviewDifficulty?,
    val congestion: ReviewCongestion?,
    val practiceMethod: PracticeMethod?,
    val content: String?,
    val caution: String?,
    val isEditable: Boolean,
    val isHidden: Boolean,
    val isVerifiedVisit: Boolean,
    val createdAt: Instant,
)

data class ReviewDraft(
    val isRecommended: Boolean,
    val difficulty: ReviewDifficulty,
    val congestion: ReviewCongestion,
    val practiceMethod: PracticeMethod,
    val content: String?,
    val caution: String?,
)

data class ReviewSummary(
    val level: OnboardingLevel?,
    val totalCount: Long,
    val recommendCount: Long,
    val notRecommendCount: Long,
    val difficultyCounts: Map<ReviewDifficulty, Long>,
    val congestionCounts: Map<ReviewCongestion, Long>,
    val levelCounts: Map<OnboardingLevel, Long>,
)

sealed interface ReviewLevelFilter {
    data object Mine : ReviewLevelFilter
    data object All : ReviewLevelFilter
    data class Of(val level: OnboardingLevel) : ReviewLevelFilter
}

data class ReportForm(
    val questionId: String,
    val title: String,
    val description: String?,
    val required: Boolean,
    val options: List<ReportFormOption>,
)

data class ReportFormOption(
    val code: String,
    val label: String,
    val order: Int,
    val requiresTextInput: Boolean,
    val textInputPlaceholder: String?,
    val textInputMaxLength: Int?,
)

data class ReportSubmission(
    val reason: String,
    val detail: String?,
    val detailConsistent: Boolean,
)
