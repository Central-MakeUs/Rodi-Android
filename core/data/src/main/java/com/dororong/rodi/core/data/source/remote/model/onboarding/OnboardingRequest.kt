package com.dororong.rodi.core.data.source.remote.model.onboarding

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingRequest(
    val drivingPeriod: String,
    val recentFrequency: String? = null,
    val roadExperiences: List<String>? = null,
    val soloDrivingRange: String? = null,
    val soloParkingLevel: String? = null,
    val level: String,
    val practiceTypes: List<String> = emptyList(),
    val carType: String? = null,
    val drivingGoal: String? = null,
)
