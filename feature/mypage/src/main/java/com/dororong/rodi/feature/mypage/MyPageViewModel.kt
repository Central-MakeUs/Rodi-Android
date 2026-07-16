package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    getOnboardingProfile: GetOnboardingProfileUseCase,
) : ViewModel() {
    val uiState: StateFlow<MyPageUiState> = getOnboardingProfile()
        .map { profile -> MyPageUiState(profile = profile.toMyPageProfile()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MyPageUiState(),
        )
}

private fun OnboardingProfile.toMyPageProfile(): MyPageProfile = MyPageProfile(
    nickname = nickname,
    level = calculateLevel().name.lowercase().replaceFirstChar { it.titlecase() },
    practiceTypes = practiceSituations.map { it.label },
    drivingGoal = goal,
)
