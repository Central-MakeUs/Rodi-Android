package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.course.CourseDraft

enum class CourseRegistrationEntryMode {
    Normal,
    ContinueDraft,
    StartFresh,
}

sealed interface CourseRegistrationEntryUiState {
    data object Loading : CourseRegistrationEntryUiState

    data class Ready(val draft: CourseDraft?) : CourseRegistrationEntryUiState
}

internal enum class CourseRegistrationPreflightDecision {
    OpenImmediately,
    ShowResumeDialog,
}
