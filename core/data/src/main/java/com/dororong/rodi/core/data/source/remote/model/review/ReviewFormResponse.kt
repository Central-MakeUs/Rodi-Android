package com.dororong.rodi.core.data.source.remote.model.review

import kotlinx.serialization.Serializable

@Serializable
data class ReviewFormResponse(val steps: List<ReviewFormStepResponse>)

@Serializable
data class ReviewFormStepResponse(val questions: List<ReviewFormQuestionResponse>)

@Serializable
data class ReviewFormQuestionResponse(
    val code: String,
    val title: String? = null,
    val options: List<ReviewFormOptionResponse>? = null,
)

@Serializable
data class ReviewFormOptionResponse(
    val code: String,
    val label: String,
    val order: Int,
    val requiresTextInput: Boolean = false,
    val textInputPlaceholder: String? = null,
    val textInputMaxLength: Int? = null,
)
