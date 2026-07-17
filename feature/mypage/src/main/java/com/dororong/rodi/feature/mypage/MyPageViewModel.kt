package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
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

private fun OnboardingProfile.toMyPageProfile(savedCourseCount: Int): MyPageProfile {
    val level = calculateLevel()
    return MyPageProfile(
        nickname = nickname,
        level = level,
        practiceTypes = level.recommendedProfileActivities,
        drivingGoal = goal,
        savedCourseCount = savedCourseCount,
    )
}

private val OnboardingLevel.recommendedProfileActivities: List<String>
    get() = when (this) {
        OnboardingLevel.SEED -> listOf("차선변경", "교차로", "주차")
        OnboardingLevel.ROOKIE -> listOf("유턴", "차선변경", "주차", "교차로")
        OnboardingLevel.OWNER -> listOf("고속도로", "합류", "다차로주행")
        OnboardingLevel.EXPLORER -> listOf("비보호 좌회전", "회전교차로", "코너링")
        OnboardingLevel.NAVIGATOR -> listOf("코스등록", "리뷰 작성", "추천 코스")
    }
