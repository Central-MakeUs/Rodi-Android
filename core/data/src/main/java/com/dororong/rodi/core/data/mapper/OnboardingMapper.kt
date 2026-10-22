package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingRequest
import com.dororong.rodi.core.domain.model.onboarding.answerCode
import com.dororong.rodi.core.domain.model.onboarding.hasValidRoadExperiences
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile

fun OnboardingProfile.toRequest(level: OnboardingLevel): OnboardingRequest {
    require(hasValidRoadExperiences) { "NONE must be selected alone" }
    return OnboardingRequest(
        drivingPeriod = requireNotNull(drivingPeriod).answerCode(),
        recentFrequency = recentFrequency?.answerCode(),
        roadExperiences = roadExperiences.takeIf { it.isNotEmpty() }?.map { it.answerCode() },
        soloDrivingRange = soloDrivingRange?.answerCode(),
        soloParkingLevel = soloParkingLevel?.answerCode(),
        level = when (level) {
            OnboardingLevel.SEED -> "SEED"
            OnboardingLevel.ROOKIE -> "ROOKIE"
            OnboardingLevel.OWNER -> "OWNER"
            OnboardingLevel.EXPLORER -> "EXPLORER"
            OnboardingLevel.NAVIGATOR -> "NAVIGATOR"
        },
        practiceTypes = practiceSituations.map { it.answerCode() },
        carType = vehicleType?.answerCode(),
        drivingGoal = goal.ifBlank { null },
    )
}
