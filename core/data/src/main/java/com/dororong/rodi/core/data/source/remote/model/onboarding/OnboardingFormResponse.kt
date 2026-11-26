package com.dororong.rodi.core.data.source.remote.model.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingFormResponse(val steps: List<OnboardingFormStepResponse>)

@Serializable
data class OnboardingFormStepResponse(
    val order: Int,
    val skippable: Boolean,
    val title: String,
    val description: String,
    val questions: List<OnboardingQuestionResponse>,
)

@Serializable
data class OnboardingQuestionResponse(
    val code: String,
    val order: Int,
    val type: String,
    val required: Boolean,
    val title: String,
    val description: String? = null,
    val placeholder: String? = null,
    val maxSelect: Int? = null,
    val maxLength: Int? = null,
    val showWhen: OnboardingShowWhenResponse? = null,
    val options: List<OnboardingOptionResponse>? = null,
)

@Serializable
data class OnboardingOptionResponse(
    val code: String,
    val label: String,
    val order: Int,
    val exclusive: Boolean? = null,
    val skipsFollowUps: Boolean? = null,
)

@Serializable
data class OnboardingShowWhenResponse(val question: String, val anyOf: List<String>)
