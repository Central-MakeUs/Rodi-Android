package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val profile: Flow<OnboardingProfile>
    val isSyncPending: Flow<Boolean>
    val isSyncAuthorized: Flow<Boolean>
    val isInitialFilterTagsApplied: Flow<Boolean>
    suspend fun getForm(): OnboardingForm?
    suspend fun saveProfile(profile: OnboardingProfile)
    suspend fun savePendingProfile(profile: OnboardingProfile)
    suspend fun authorizeSync()
    suspend fun clearSyncPending()
    suspend fun markInitialFilterTagsApplied()
    suspend fun clear()
    suspend fun submit(profile: OnboardingProfile, level: OnboardingLevel): OnboardingSubmissionResult
}
