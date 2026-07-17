package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MyPageUiState(
    val profile: MyPageProfile = MyPageProfile(),
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    getOnboardingProfile: GetOnboardingProfileUseCase,
    getCourses: GetCoursesUseCase,
) : ViewModel() {
    val uiState: StateFlow<MyPageUiState> = combine(
        getOnboardingProfile(),
        getCourses.observeSavedCourseIds(),
    ) { profile, savedCourseIds ->
        MyPageUiState(profile = profile.toMyPageProfile(savedCourseIds.size))
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MyPageUiState(),
        )
}

private fun OnboardingProfile.toMyPageProfile(savedCourseCount: Int): MyPageProfile = MyPageProfile(
    nickname = nickname,
    level = calculateLevel(),
    practiceTypes = practiceSituations.map { it.label },
    drivingGoal = goal,
    savedCourseCount = savedCourseCount,
)
