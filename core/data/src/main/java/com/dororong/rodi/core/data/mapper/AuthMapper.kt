package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.auth.OAuthOnboardingProfileRequest
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile

fun OnboardingProfile.toOAuthRequest(): OAuthOnboardingProfileRequest =
    OAuthOnboardingProfileRequest(
        nickname = nickname.ifBlank { null },
        drivingPeriod = drivingPeriod?.name,
        recentFrequency = recentFrequency?.name,
        roadExperiences = roadExperiences.map { it.name },
        soloDrivingRange = soloDrivingRange?.name,
        soloParkingLevel = soloParkingLevel?.name,
        practiceSituations = practiceSituations.map { it.name },
        vehicleType = vehicleType?.name,
        goal = goal.ifBlank { null },
    )
