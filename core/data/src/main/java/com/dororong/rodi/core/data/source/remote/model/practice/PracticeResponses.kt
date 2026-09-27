package com.dororong.rodi.core.data.source.remote.model.practice

import kotlinx.serialization.Serializable

@Serializable
data class PracticeRegisterResponse(
    val practiceId: Long,
    val status: String,
    val visitCount: Int,
    val requiredDistanceMeters: Int,
)

@Serializable
data class PracticeVisitRequest(
    val certifiedDistanceMeters: Int? = null,
)

@Serializable
data class PracticeVisitResponse(
    val visitCount: Int,
    val addedCertifiedDistanceMeters: Int,
    val requiredDistanceMeters: Int,
    val isCertifiedNow: Boolean,
    val isVerified: Boolean = false,
    val totalDistanceKm: Double,
    val levelUp: Boolean,
    val newLevel: String? = null,
)

@Serializable
data class PracticeSkipReasonRequest(
    val reason: String = "",
    val detail: String? = null,
)

@Serializable
data class FormResponse(
    val questionId: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val required: Boolean,
    val options: List<FormOptionResponse>,
)

@Serializable
data class FormOptionResponse(
    val code: String,
    val label: String,
    val order: Int,
    val requiresTextInput: Boolean,
    val textInputPlaceholder: String? = null,
    val textInputMaxLength: Int? = null,
)
