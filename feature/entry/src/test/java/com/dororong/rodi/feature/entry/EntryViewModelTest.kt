package com.dororong.rodi.feature.entry

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.model.entry.EntryProgressStep
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import com.dororong.rodi.core.domain.usecase.entry.GetEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.entry.SaveEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SaveOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.entry.SetEntryCompletedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryViewModelTest {

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
    fun `initial step is terms`() {
        val viewModel = testViewModel()

        assertEquals(EntryStep.TERMS, viewModel.step)
    }

    @Test
    fun `restores saved entry step and onboarding selections`() = runTest(testDispatcher) {
        val viewModel = testViewModel(
            savedProgress = EntryProgress(
                step = EntryProgressStep.PREFERENCE,
                serviceTermsChecked = true,
                privacyTermsChecked = true,
                locationTermsChecked = true,
            ),
            savedProfile = OnboardingProfile(
                nickname = "로디",
                drivingPeriod = DrivingPeriod.MONTHS_1_2,
                recentFrequency = RecentDrivingFrequency.WEEKLY_1,
                roadExperiences = listOf(RoadExperience.SOLO),
                soloDrivingRange = SoloDrivingRange.FAMILIAR_ROAD,
                soloParkingLevel = SoloParkingLevel.FAMILIAR_SPOT,
                practiceSituations = listOf(PracticeSituation.PARKING, PracticeSituation.LANE_CHANGE),
                vehicleType = VehicleType.SUV,
                goal = "주차 연습",
            ),
        )

        advanceUntilIdle()

        assertTrue(viewModel.isRestored)
        assertEquals(EntryStep.PREFERENCE, viewModel.step)
        assertTrue(viewModel.serviceTermsChecked)
        assertTrue(viewModel.privacyTermsChecked)
        assertTrue(viewModel.locationTermsChecked)
        assertEquals("로디", viewModel.nickname)
        assertEquals(DrivingPeriod.MONTHS_1_2, viewModel.drivingPeriod)
        assertEquals(RecentDrivingFrequency.WEEKLY_1, viewModel.recentFrequency)
        assertEquals(listOf(RoadExperience.SOLO), viewModel.roadExperiences)
        assertEquals(SoloDrivingRange.FAMILIAR_ROAD, viewModel.soloDrivingRange)
        assertEquals(SoloParkingLevel.FAMILIAR_SPOT, viewModel.soloParkingLevel)
        assertEquals(listOf(PracticeSituation.PARKING, PracticeSituation.LANE_CHANGE), viewModel.practiceSituations)
        assertEquals(VehicleType.SUV, viewModel.vehicleType)
        assertEquals("주차 연습", viewModel.goal)
    }

    @Test
    fun `restores completed profile at precautions without another submission`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            savedProgress = EntryProgress(step = EntryProgressStep.PRECAUTIONS),
        )

        advanceUntilIdle()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        coVerify(exactly = 0) { saveOnboardingProfileUseCase.submit(any(), any()) }
    }

    @Test
    fun `next moves through onboarding, precautions, location and stays at location`() {
        val viewModel = testViewModel()

        viewModel.next()
        assertEquals(EntryStep.NICKNAME, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.CAREER, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.PREFERENCE, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.LOCATION, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.LOCATION, viewModel.step)
    }

    @Test
    fun `guest browse skips onboarding between terms and precautions`() = runTest(testDispatcher) {
        val viewModel = testViewModel(mode = EntryMode.GUEST_BROWSE)
        advanceUntilIdle()

        viewModel.next()
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        viewModel.next()
        assertEquals(EntryStep.LOCATION, viewModel.step)
    }

    @Test
    fun `guest browse restores legacy onboarding step at precautions`() = runTest(testDispatcher) {
        val viewModel = testViewModel(
            mode = EntryMode.GUEST_BROWSE,
            savedProgress = EntryProgress(step = EntryProgressStep.PREFERENCE),
        )

        advanceUntilIdle()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `guest sign up starts at nickname and cannot return to terms`() = runTest(testDispatcher) {
        val viewModel = testViewModel(
            mode = EntryMode.GUEST_SIGN_UP,
            savedProgress = EntryProgress(step = EntryProgressStep.NICKNAME),
        )
        advanceUntilIdle()

        assertEquals(EntryStep.NICKNAME, viewModel.step)
        assertFalse(viewModel.back())
        assertEquals(EntryStep.NICKNAME, viewModel.step)
    }

    @Test
    fun `back moves through previous steps and stops at precautions`() {
        val viewModel = testViewModel()

        assertFalse(viewModel.back())
        assertEquals(EntryStep.TERMS, viewModel.step)

        viewModel.next()
        assertTrue(viewModel.back())
        assertEquals(EntryStep.TERMS, viewModel.step)

        viewModel.next()
        viewModel.next()
        viewModel.next()
        viewModel.next()
        viewModel.next()
        assertEquals(EntryStep.LOCATION, viewModel.step)

        assertTrue(viewModel.back())
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)

        assertFalse(viewModel.back())
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `openWebView stores url and moves to webview step`() {
        val viewModel = testViewModel()

        viewModel.openWebView("https://example.com/terms")

        assertEquals("https://example.com/terms", viewModel.webViewUrl)
        assertEquals(EntryStep.TERMS_WEBVIEW, viewModel.step)
    }

    @Test
    fun `step and gate selections are saved when changed`() = runTest(testDispatcher) {
        val saveEntryProgressUseCase = testSaveEntryProgressUseCase()
        val viewModel = testViewModel(saveEntryProgressUseCase = saveEntryProgressUseCase)
        advanceUntilIdle()

        viewModel.setAllTermsChecked(true)
        viewModel.next()
        advanceUntilIdle()

        coVerify {
            saveEntryProgressUseCase(
                match {
                    it.step == EntryProgressStep.NICKNAME &&
                        it.serviceTermsChecked &&
                        it.privacyTermsChecked &&
                        it.locationTermsChecked
                },
            )
        }
    }

    @Test
    fun `setAllTermsChecked updates only terms checkboxes`() {
        val viewModel = testViewModel()
        viewModel.toggleLicense()
        viewModel.toggleCompanion()
        viewModel.togglePrecautionAgreement()

        viewModel.setAllTermsChecked(true)

        assertTrue(viewModel.serviceTermsChecked)
        assertTrue(viewModel.privacyTermsChecked)
        assertTrue(viewModel.locationTermsChecked)
        assertTrue(viewModel.licenseChecked)
        assertTrue(viewModel.companionChecked)
        assertTrue(viewModel.precautionAgreementChecked)
    }

    @Test
    fun `toggleServiceTerms flips only service terms`() {
        val viewModel = testViewModel()

        viewModel.toggleServiceTerms()

        assertTrue(viewModel.serviceTermsChecked)
        assertFalse(viewModel.privacyTermsChecked)
        assertFalse(viewModel.locationTermsChecked)
        assertFalse(viewModel.licenseChecked)
        assertFalse(viewModel.companionChecked)
        assertFalse(viewModel.precautionAgreementChecked)
    }

    @Test
    fun `toggleLicense flips only license`() {
        val viewModel = testViewModel()

        viewModel.toggleLicense()

        assertFalse(viewModel.serviceTermsChecked)
        assertFalse(viewModel.privacyTermsChecked)
        assertFalse(viewModel.locationTermsChecked)
        assertTrue(viewModel.licenseChecked)
        assertFalse(viewModel.companionChecked)
        assertFalse(viewModel.precautionAgreementChecked)
    }

    @Test
    fun `nickname is generated only once`() {
        val viewModel = testViewModel()

        viewModel.next()
        val nickname = viewModel.nickname
        viewModel.next()

        assertTrue(nickname.isNotBlank())
        assertEquals(nickname, viewModel.nickname)
    }

    @Test
    fun `long driving period completes career step without detail questions`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.YEARS_3_9)

        assertTrue(viewModel.isCareerStepValid)
        assertEquals(null, viewModel.recentFrequency)
        assertEquals(emptyList<RoadExperience>(), viewModel.roadExperiences)
    }

    @Test
    fun `long driving period completes navigator analysis from career step`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        advanceUntilIdle()

        viewModel.next()
        viewModel.next()
        viewModel.selectDrivingPeriod(DrivingPeriod.YEARS_3_9)
        viewModel.continueAfterCareer()

        assertEquals(EntryStep.CAREER, viewModel.step)
        assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.state.value.onboardingAnalysisState)

        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(OnboardingLevel.NAVIGATOR, viewModel.state.value.onboardingLevel)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.state.value.onboardingAnalysisState)

        viewModel.continueAfterOnboardingAnalysis()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertFalse(viewModel.back())
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `short driving period requires recent frequency and road experience`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.toggleRoadExperience(RoadExperience.WITH_COMPANION)
        assertTrue(viewModel.isCareerStepValid)
    }

    @Test
    fun `short driving period continues to preference after career`() = runTest(testDispatcher) {
        val viewModel = testViewModel()
        advanceUntilIdle()

        viewModel.next()
        viewModel.next()
        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        viewModel.toggleRoadExperience(RoadExperience.WITH_COMPANION)

        viewModel.continueAfterCareer()

        assertEquals(EntryStep.PREFERENCE, viewModel.step)
        assertEquals(null, viewModel.state.value.onboardingAnalysisState)
    }

    @Test
    fun `solo road experience among multiple selections requires conditional answers and clears them when removed`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        viewModel.toggleRoadExperience(RoadExperience.WITH_COMPANION)
        viewModel.toggleRoadExperience(RoadExperience.SOLO)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectSoloDrivingRange(SoloDrivingRange.FAMILIAR_ROAD)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectSoloParkingLevel(SoloParkingLevel.FAMILIAR_SPOT)
        assertTrue(viewModel.isCareerStepValid)

        viewModel.toggleRoadExperience(RoadExperience.SOLO)
        assertEquals(null, viewModel.soloDrivingRange)
        assertEquals(null, viewModel.soloParkingLevel)
        assertEquals(listOf(RoadExperience.WITH_COMPANION), viewModel.roadExperiences)
        assertTrue(viewModel.isCareerStepValid)
    }

    @Test
    fun `goal is limited to thirty characters`() {
        val viewModel = testViewModel()

        viewModel.updateGoal("1234567890123456789012345678901")

        assertEquals("123456789012345678901234567890", viewModel.goal)
    }

    @Test
    fun `practice situations are limited to three and fourth selection is ignored`() {
        val viewModel = testViewModel()

        viewModel.togglePracticeSituation(PracticeSituation.U_TURN)
        viewModel.togglePracticeSituation(PracticeSituation.PARKING)
        viewModel.togglePracticeSituation(PracticeSituation.LANE_CHANGE)
        viewModel.togglePracticeSituation(PracticeSituation.INTERSECTION)

        assertEquals(
            listOf(PracticeSituation.U_TURN, PracticeSituation.PARKING, PracticeSituation.LANE_CHANGE),
            viewModel.practiceSituations,
        )

        viewModel.togglePracticeSituation(PracticeSituation.PARKING)
        assertEquals(listOf(PracticeSituation.U_TURN, PracticeSituation.LANE_CHANGE), viewModel.practiceSituations)
    }

    @Test
    fun `preference next requires situation but not vehicle or goal`() {
        val viewModel = testViewModel()

        assertFalse(viewModel.isPreferenceNextEnabled)

        viewModel.togglePracticeSituation(PracticeSituation.U_TURN)
        assertTrue(viewModel.isPreferenceNextEnabled)

        viewModel.selectVehicleType(VehicleType.SUV)
        assertTrue(viewModel.isPreferenceNextEnabled)
    }

    @Test
    fun `onboarding selections are saved when changed`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        advanceUntilIdle()

        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        viewModel.toggleRoadExperience(RoadExperience.SOLO)
        viewModel.selectSoloDrivingRange(SoloDrivingRange.FAMILIAR_ROAD)
        viewModel.selectSoloParkingLevel(SoloParkingLevel.FAMILIAR_SPOT)
        viewModel.togglePracticeSituation(PracticeSituation.PARKING)
        viewModel.selectVehicleType(VehicleType.SUV)
        viewModel.updateGoal("주차 연습")
        advanceUntilIdle()

        coVerify {
            saveOnboardingProfileUseCase(
                match {
                    it.drivingPeriod == DrivingPeriod.MONTHS_1_2 &&
                        it.recentFrequency == RecentDrivingFrequency.WEEKLY_1 &&
                        it.roadExperiences == listOf(RoadExperience.SOLO) &&
                        it.soloDrivingRange == SoloDrivingRange.FAMILIAR_ROAD &&
                        it.soloParkingLevel == SoloParkingLevel.FAMILIAR_SPOT &&
                        it.practiceSituations == listOf(PracticeSituation.PARKING) &&
                        it.vehicleType == VehicleType.SUV &&
                        it.goal == "주차 연습"
                },
            )
        }
    }

    @Test
    fun `onboarding analysis shows result only after three seconds`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val saveEntryProgressUseCase = testSaveEntryProgressUseCase()
        val viewModel = testViewModel(
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            saveEntryProgressUseCase = saveEntryProgressUseCase,
        )
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        runCurrent()
        advanceTimeBy(2_999)
        runCurrent()

        assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.state.value.onboardingAnalysisState)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.state.value.onboardingAnalysisState)
        coVerify(exactly = 1) { saveOnboardingProfileUseCase.saveForSubmission(any()) }
        coVerify(exactly = 1) { saveEntryProgressUseCase(any()) }

        viewModel.continueAfterOnboardingAnalysis()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(null, viewModel.state.value.onboardingAnalysisState)
    }

    @Test
    fun `guest sign up completes entry when analysis result is confirmed`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            mode = EntryMode.GUEST_SIGN_UP,
            savedProgress = EntryProgress(step = EntryProgressStep.PREFERENCE),
            setEntryCompletedUseCase = setEntryCompletedUseCase,
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
        )
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns
            OnboardingSubmissionResult.Submitted
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        advanceTimeBy(3_000)
        runCurrent()

        viewModel.effect.test {
            viewModel.continueAfterOnboardingAnalysis()
            advanceUntilIdle()

            coVerify(exactly = 1) { setEntryCompletedUseCase() }
            assertEquals(EntryEffect.CompleteEntry, awaitItem())
        }
    }

    @Test
    fun `onboarding analysis emits failure only after three seconds when local save fails`() =
        runTest(testDispatcher) {
            val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
            val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
            coEvery { saveOnboardingProfileUseCase.saveForSubmission(any()) } throws IllegalStateException("failed")
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.startOnboardingAnalysis()
                runCurrent()
                advanceTimeBy(2_999)
                runCurrent()

                assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.state.value.onboardingAnalysisState)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()

                assertEquals(null, viewModel.state.value.onboardingAnalysisState)
                assertEquals(
                    EntryEffect.ShowSubmissionError(
                        message = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                        canRetry = true,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `onboarding analysis treats already completed submission as success`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.AlreadyCompleted
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.state.value.onboardingAnalysisState)
    }

    @Test
    fun `onboarding analysis shows input error without retry action`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.InvalidProfile
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(null, viewModel.state.value.onboardingAnalysisState)
            assertEquals(
                EntryEffect.ShowSubmissionError("입력 정보를 확인해주세요.", canRetry = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `onboarding analysis asks user to wait after rate limit without retry action`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.RateLimited
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(null, viewModel.state.value.onboardingAnalysisState)
            assertEquals(
                EntryEffect.ShowSubmissionError(
                    message = "요청이 많아요. 잠시 기다린 뒤 다시 시도해주세요.",
                    canRetry = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `onboarding analysis ignores duplicate submit while analyzing`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        viewModel.startOnboardingAnalysis()
        advanceTimeBy(3_000)
        runCurrent()

        coVerify(exactly = 1) { saveOnboardingProfileUseCase.submit(any(), any()) }
    }

    @Test
    fun `finish stores entry completion and emits completion effect`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            setEntryCompletedUseCase = setEntryCompletedUseCase,
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
        )
        coEvery { setEntryCompletedUseCase() } returns Unit
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        coEvery { saveOnboardingProfileUseCase.saveForSubmission(any()) } returns Unit
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        advanceUntilIdle()
        viewModel.effect.test {
            viewModel.finish()
            advanceUntilIdle()

            coVerify(exactly = 1) { setEntryCompletedUseCase() }
            assertEquals(EntryEffect.CompleteEntry, awaitItem())
        }
    }

    @Test
    fun `finish does not invoke callback when use case throws`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            setEntryCompletedUseCase = setEntryCompletedUseCase,
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
        )
        coEvery { setEntryCompletedUseCase() } throws IllegalStateException("failed")
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        advanceUntilIdle()
        viewModel.effect.test {
            viewModel.finish()
            advanceUntilIdle()

            coVerify(exactly = 1) { setEntryCompletedUseCase() }
            expectNoEvents()
        }
    }

    @Test
    fun `finish does not invoke callback when use case is cancelled`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            setEntryCompletedUseCase = setEntryCompletedUseCase,
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
        )
        coEvery { setEntryCompletedUseCase() } throws CancellationException("cancelled")
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        advanceUntilIdle()
        viewModel.effect.test {
            viewModel.finish()
            advanceUntilIdle()

            coVerify(exactly = 1) { setEntryCompletedUseCase() }
            expectNoEvents()
        }
    }

    private fun testViewModel(
        setEntryCompletedUseCase: SetEntryCompletedUseCase = testSetEntryCompletedUseCase(),
        saveOnboardingProfileUseCase: SaveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase(),
        getEntryProgressUseCase: GetEntryProgressUseCase = testGetEntryProgressUseCase(),
        saveEntryProgressUseCase: SaveEntryProgressUseCase = testSaveEntryProgressUseCase(),
        getOnboardingProfileUseCase: GetOnboardingProfileUseCase = testGetOnboardingProfileUseCase(),
        savedProgress: EntryProgress = EntryProgress(),
        savedProfile: OnboardingProfile = OnboardingProfile(),
        mode: EntryMode = EntryMode.AUTHENTICATED,
    ): EntryViewModel {
        coEvery { setEntryCompletedUseCase() } returns Unit
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        coEvery { saveOnboardingProfileUseCase.saveForSubmission(any()) } returns Unit
        val progressWithMode = savedProgress.copy(mode = mode)
        every { getEntryProgressUseCase() } returns flowOf(progressWithMode)
        coEvery { saveEntryProgressUseCase(any()) } returns Unit
        every { getOnboardingProfileUseCase() } returns flowOf(savedProfile)
        return EntryViewModel(
            setEntryCompletedUseCase = setEntryCompletedUseCase,
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            getEntryProgressUseCase = getEntryProgressUseCase,
            saveEntryProgressUseCase = saveEntryProgressUseCase,
            getOnboardingProfileUseCase = getOnboardingProfileUseCase,
        )
    }

    private fun testSetEntryCompletedUseCase(): SetEntryCompletedUseCase =
        mockk()

    private fun testSaveOnboardingProfileUseCase(): SaveOnboardingProfileUseCase =
        mockk()

    private fun testGetEntryProgressUseCase(): GetEntryProgressUseCase =
        mockk()

    private fun testSaveEntryProgressUseCase(): SaveEntryProgressUseCase =
        mockk()

    private fun testGetOnboardingProfileUseCase(): GetOnboardingProfileUseCase =
        mockk()

}

private val EntryViewModel.isRestored: Boolean get() = state.value.isRestored
private val EntryViewModel.step: EntryStep get() = state.value.step
private val EntryViewModel.webViewUrl: String get() = state.value.webViewUrl
private val EntryViewModel.serviceTermsChecked: Boolean get() = state.value.serviceTermsChecked
private val EntryViewModel.privacyTermsChecked: Boolean get() = state.value.privacyTermsChecked
private val EntryViewModel.locationTermsChecked: Boolean get() = state.value.locationTermsChecked
private val EntryViewModel.licenseChecked: Boolean get() = state.value.licenseChecked
private val EntryViewModel.companionChecked: Boolean get() = state.value.companionChecked
private val EntryViewModel.precautionAgreementChecked: Boolean get() = state.value.precautionAgreementChecked
private val EntryViewModel.nickname: String get() = state.value.nickname
private val EntryViewModel.drivingPeriod: DrivingPeriod? get() = state.value.drivingPeriod
private val EntryViewModel.recentFrequency: RecentDrivingFrequency? get() = state.value.recentFrequency
private val EntryViewModel.roadExperiences: List<RoadExperience> get() = state.value.roadExperiences
private val EntryViewModel.soloDrivingRange: SoloDrivingRange? get() = state.value.soloDrivingRange
private val EntryViewModel.soloParkingLevel: SoloParkingLevel? get() = state.value.soloParkingLevel
private val EntryViewModel.practiceSituations: List<PracticeSituation> get() = state.value.practiceSituations
private val EntryViewModel.vehicleType: VehicleType? get() = state.value.vehicleType
private val EntryViewModel.goal: String get() = state.value.goal
private val EntryViewModel.isCareerStepValid: Boolean get() = state.value.isCareerStepValid
private val EntryViewModel.isPreferenceNextEnabled: Boolean get() = state.value.isPreferenceNextEnabled
