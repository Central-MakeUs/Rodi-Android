package com.dororong.rodi.core.data

import android.content.Context
import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : OnboardingRepository {
    private val prefs = OnboardingPreferences(context)

    override suspend fun saveProfile(profile: OnboardingProfile) = prefs.saveProfile(profile)
}
