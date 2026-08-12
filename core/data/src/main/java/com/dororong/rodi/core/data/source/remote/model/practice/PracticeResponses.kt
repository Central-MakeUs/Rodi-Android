package com.dororong.rodi.core.data.source.remote.model.practice

import kotlinx.serialization.Serializable

@Serializable
data class PracticeRegisterResponse(
    val practiceId: Long = 0,
    val status: String = "PLANNED",
    val visitCount: Int = 0,
    val requiredDistanceMeters: Int = 0,
)

@Serializable
data class PracticeVisitRequest(
    val certifiedDistanceMeters: Int? = null,
)

@Serializable
data class PracticeVisitResponse(
    val visitCount: Int = 0,
    val addedCertifiedDistanceMeters: Int = 0,
    val requiredDistanceMeters: Int = 0,
    val isCertifiedNow: Boolean = false,
    val isVerified: Boolean = false,
    val totalDistanceKm: Double = 0.0,
    val levelUp: Boolean = false,
    val newLevel: String? = null,
)

@Serializable
data class PracticeSkipReasonRequest(
    val reason: String = "",
    val detail: String? = null,
)

@Serializable
data class FormResponse(
    val questionId: String = "",
    val type: String = "SINGLE_SELECT",
    val title: String = "",
    val description: String? = null,
    val required: Boolean = false,
    val options: List<FormOptionResponse> = emptyList(),
)

@Serializable
data class FormOptionResponse(
    val code: String = "",
    val label: String = "",
    val order: Int = 0,
    val requiresTextInput: Boolean = false,
    val textInputPlaceholder: String? = null,
    val textInputMaxLength: Int? = null,
)
