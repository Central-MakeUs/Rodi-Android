package com.dororong.rodi.core.domain.model.practice

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel

data class Practice(
    val practiceId: Long,
    val status: PracticeStatus,
    val visitCount: Int,
    val requiredDistanceMeters: Int,
)

enum class PracticeStatus {
    PLANNED,
    VISITED,
    NOT_VISITED,
}

data class PracticeVisitResult(
    val visitCount: Int,
    val addedCertifiedDistanceMeters: Int,
    val requiredDistanceMeters: Int,
    val isCertifiedNow: Boolean,
    val isVerified: Boolean,
    val totalDistanceKm: Double,
    val levelUp: Boolean,
    val newLevel: OnboardingLevel?,
)

data class SkipReasonForm(
    val questionId: String,
    val type: String,
    val title: String,
    val description: String?,
    val required: Boolean,
    val options: List<SkipReasonOption>,
)

data class SkipReasonOption(
    val code: String,
    val label: String,
    val order: Int,
    val requiresTextInput: Boolean,
    val textInputPlaceholder: String?,
    val textInputMaxLength: Int?,
)
