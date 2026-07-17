package com.dororong.rodi.feature.mypage

import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onboarding profile is displayed on my page`() = runTest(testDispatcher) {
        val profile = OnboardingProfile(
            nickname = "로디",
            practiceSituations = listOf(PracticeSituation.PARKING, PracticeSituation.LANE_CHANGE),
            goal = "강남에서 운전하기",
        )
        val repository = mockk<OnboardingRepository> {
            every { this@mockk.profile } returns flowOf(profile)
        }
        val courseRepository = mockk<CourseRepository> {
            every { observeSavedCourseIds() } returns flowOf(setOf(1, 2))
            every { getCourses() } returns emptyList()
        }

        val viewModel = MyPageViewModel(
            getOnboardingProfile = GetOnboardingProfileUseCase(repository),
            getCourses = GetCoursesUseCase(courseRepository),
        )
        advanceUntilIdle()

        assertEquals("로디", viewModel.uiState.value.profile.nickname)
        assertEquals(OnboardingLevel.SEED, viewModel.uiState.value.profile.level)
        assertEquals(listOf("주차", "차선변경"), viewModel.uiState.value.profile.practiceTypes)
        assertEquals("강남에서 운전하기", viewModel.uiState.value.profile.drivingGoal)
        assertEquals(2, viewModel.uiState.value.profile.savedCourseCount)
    }
}
