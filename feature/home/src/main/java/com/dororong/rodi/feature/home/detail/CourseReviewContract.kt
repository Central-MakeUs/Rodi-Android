package com.dororong.rodi.feature.home.detail

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty

data class CourseReviewUiState(
    val placeId: Long? = null,
    val isGuest: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalCount: Long = 0,
    val recommendCount: Long = 0,
    val selectedLevel: OnboardingLevel = OnboardingLevel.SEED,
    val difficultyCounts: Map<ReviewDifficulty, Long> = emptyMap(),
    val latestReviews: List<Review> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isNextPageLoading: Boolean = false,
)
