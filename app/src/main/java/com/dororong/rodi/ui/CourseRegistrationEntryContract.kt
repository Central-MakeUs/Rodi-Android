package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.course.CourseDraft

enum class CourseRegistrationEntryMode {
    Normal,
    ContinueDraft,
    StartFresh,
}

sealed interface CourseRegistrationEntryState {
    data object Loading : CourseRegistrationEntryState

    data class Ready(val draft: CourseDraft?) : CourseRegistrationEntryState
}

internal enum class CourseRegistrationPreflightDecision {
    OpenImmediately,
    ShowResumeDialog,
}
