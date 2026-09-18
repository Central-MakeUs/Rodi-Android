package com.dororong.rodi.feature.home.review.notvisited

import com.dororong.rodi.core.domain.model.practice.SkipReasonForm

data class PracticeSkipReasonUiState(
    val practiceId: Long? = null,
    val form: SkipReasonForm? = null,
    val selectedOptionCode: String? = null,
    val detail: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
)
