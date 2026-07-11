package com.dororong.rodi.core.data.source.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class OAuthOnboardingProfileRequest(
    val nickname: String? = null,
    val drivingPeriod: String? = null,
    val recentFrequency: String? = null,
    val roadExperiences: List<String> = emptyList(),
    val soloDrivingRange: String? = null,
    val soloParkingLevel: String? = null,
    val practiceSituations: List<String> = emptyList(),
    val vehicleType: String? = null,
    val goal: String? = null,
)
