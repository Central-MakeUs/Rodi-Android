package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toRequest
import com.dororong.rodi.core.data.source.local.datastore.OnboardingPreferences
import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val prefs: OnboardingPreferences,
    private val onboardingApi: OnboardingApi,
) : OnboardingRepository {
    override val profile: Flow<OnboardingProfile> = prefs.profile

    override suspend fun saveProfile(profile: OnboardingProfile) = prefs.saveProfile(profile)

    override suspend fun submit(profile: OnboardingProfile, level: OnboardingLevel) {
        try {
            val response = onboardingApi.submit(profile.toRequest(level))
            check(response.isSuccess) { response.message.ifBlank { "온보딩 제출에 실패했습니다." } }
        } catch (error: HttpException) {
            if (error.code() != 409) throw error
        }
    }
}
