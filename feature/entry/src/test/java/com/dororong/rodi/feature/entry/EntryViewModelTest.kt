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
import com.dororong.rodi.core.domain.usecase.onboarding.ApplyInitialFilterTagsUseCase
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
    fun `첫 단계는 약관이다`() {
        val viewModel = testViewModel()

        assertEquals(EntryStep.TERMS, viewModel.step)
    }

    @Test
    fun `저장된 진입 단계와 온보딩 선택을 복원한다`() = runTest(testDispatcher) {
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
    fun `완성된 프로필은 다시 제출하지 않고 주의사항 단계로 복원한다`() = runTest(testDispatcher) {
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
    fun `다음을 누르면 온보딩과 주의사항과 위치 단계를 거쳐 위치 단계에 머문다`() {
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
    fun `둘러보기는 약관 다음 온보딩을 건너뛰고 주의사항으로 간다`() = runTest(testDispatcher) {
        val viewModel = testViewModel(mode = EntryMode.GUEST_BROWSE)
        advanceUntilIdle()

        viewModel.next()
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        viewModel.next()
        assertEquals(EntryStep.LOCATION, viewModel.step)
    }

    @Test
    fun `둘러보기는 예전 온보딩 단계가 저장돼 있어도 주의사항으로 복원한다`() = runTest(testDispatcher) {
        val viewModel = testViewModel(
            mode = EntryMode.GUEST_BROWSE,
            savedProgress = EntryProgress(step = EntryProgressStep.PREFERENCE),
        )

        advanceUntilIdle()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `둘러보기 가입은 닉네임부터 시작하고 약관으로 돌아갈 수 없다`() = runTest(testDispatcher) {
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
    fun `뒤로가기는 이전 단계로 가고 주의사항에서 멈춘다`() {
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
    fun `openWebView는 URL을 저장하고 웹뷰 단계로 이동한다`() {
        val viewModel = testViewModel()

        viewModel.openWebView("https://example.com/terms")

        assertEquals("https://example.com/terms", viewModel.webViewUrl)
        assertEquals(EntryStep.TERMS_WEBVIEW, viewModel.step)
    }

    @Test
    fun `단계와 확인 항목이 바뀌면 저장한다`() = runTest(testDispatcher) {
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
    fun `setAllTermsChecked로 약관을 모두 선택해도 이미 선택한 확인 항목은 유지된다`() {
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
    fun `toggleServiceTerms는 서비스 약관만 바꾼다`() {
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
    fun `toggleLicense는 면허 항목만 바꾼다`() {
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
    fun `닉네임은 한 번만 생성한다`() {
        val viewModel = testViewModel()

        viewModel.next()
        val nickname = viewModel.nickname
        viewModel.next()

        assertTrue(nickname.isNotBlank())
        assertEquals(nickname, viewModel.nickname)
    }

    @Test
    fun `운전 경력이 길면 세부 질문 없이 경력 단계를 마친다`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.YEARS_3_9)

        assertTrue(viewModel.isCareerStepValid)
        assertEquals(null, viewModel.recentFrequency)
        assertEquals(emptyList<RoadExperience>(), viewModel.roadExperiences)
    }

    @Test
    fun `운전 경력이 길면 경력 단계에서 내비게이터 분석을 마친다`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        advanceUntilIdle()

        viewModel.next()
        viewModel.next()
        viewModel.selectDrivingPeriod(DrivingPeriod.YEARS_3_9)
        viewModel.continueAfterCareer()

        assertEquals(EntryStep.CAREER, viewModel.step)
        assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.uiState.value.onboardingAnalysisState)

        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(OnboardingLevel.NAVIGATOR, viewModel.uiState.value.onboardingLevel)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.uiState.value.onboardingAnalysisState)

        viewModel.continueAfterOnboardingAnalysis()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertFalse(viewModel.back())
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `운전 경력이 짧으면 최근 운전 빈도와 도로 경험이 필요하다`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.toggleRoadExperience(RoadExperience.WITH_COMPANION)
        assertTrue(viewModel.isCareerStepValid)
    }

    @Test
    fun `운전 경력이 짧으면 경력 단계 다음 선호 단계로 간다`() = runTest(testDispatcher) {
        val viewModel = testViewModel()
        advanceUntilIdle()

        viewModel.next()
        viewModel.next()
        viewModel.selectDrivingPeriod(DrivingPeriod.MONTHS_1_2)
        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        viewModel.toggleRoadExperience(RoadExperience.WITH_COMPANION)

        viewModel.continueAfterCareer()

        assertEquals(EntryStep.PREFERENCE, viewModel.step)
        assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
    }

    @Test
    fun `여러 도로 경험 중 단독 운전이 있으면 조건부 답변이 필요하고 빼면 답변을 지운다`() {
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
    fun `목표는 30자까지만 입력된다`() {
        val viewModel = testViewModel()

        viewModel.updateGoal("1234567890123456789012345678901")

        assertEquals("123456789012345678901234567890", viewModel.goal)
    }

    @Test
    fun `연습 상황은 세 개까지이고 네 번째 선택은 무시한다`() {
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
    fun `선호 단계의 다음 버튼은 연습 상황만 필요하고 차종과 목표는 필요 없다`() {
        val viewModel = testViewModel()

        assertFalse(viewModel.isPreferenceNextEnabled)

        viewModel.togglePracticeSituation(PracticeSituation.U_TURN)
        assertTrue(viewModel.isPreferenceNextEnabled)

        viewModel.selectVehicleType(VehicleType.SUV)
        assertTrue(viewModel.isPreferenceNextEnabled)
    }

    @Test
    fun `온보딩 선택이 바뀌면 저장한다`() = runTest(testDispatcher) {
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
    fun `온보딩 분석 결과는 3초가 지난 뒤에만 보여준다`() = runTest(testDispatcher) {
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

        assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.uiState.value.onboardingAnalysisState)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.uiState.value.onboardingAnalysisState)
        coVerify(exactly = 1) { saveOnboardingProfileUseCase.saveForSubmission(any()) }
        coVerify(exactly = 1) { saveEntryProgressUseCase(any()) }

        viewModel.continueAfterOnboardingAnalysis()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
    }

    @Test
    fun `둘러보기 가입은 분석 결과를 확인하면 진입을 완료한다`() = runTest(testDispatcher) {
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
    fun `로컬 저장이 실패해도 온보딩 분석 실패는 3초가 지난 뒤에 알린다`() =
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

                assertEquals(OnboardingAnalysisState.ANALYZING, viewModel.uiState.value.onboardingAnalysisState)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()

                assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
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
    fun `이미 완료된 제출은 온보딩 분석에서 성공으로 처리한다`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.AlreadyCompleted
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
        assertEquals(OnboardingAnalysisState.RESULT, viewModel.uiState.value.onboardingAnalysisState)
    }

    @Test
    fun `온보딩 분석은 계산한 레벨로 초기 필터 태그를 적용한다`() = runTest(testDispatcher) {
        val applyInitialFilterTags = testApplyInitialFilterTagsUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            applyInitialFilterTagsUseCase = applyInitialFilterTags,
        )
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        coEvery { applyInitialFilterTags(OnboardingLevel.SEED) } returns Result.success(Unit)
        advanceUntilIdle()

        viewModel.startOnboardingAnalysis()
        advanceTimeBy(3_000)
        runCurrent()

        coVerify(exactly = 1) { applyInitialFilterTags(OnboardingLevel.SEED) }
    }

    @Test
    fun `초기 필터 태그 적용이 실패하면 현재 단계에 머문다`() = runTest(testDispatcher) {
        val applyInitialFilterTags = testApplyInitialFilterTagsUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            applyInitialFilterTagsUseCase = applyInitialFilterTags,
        )
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        coEvery { applyInitialFilterTags(OnboardingLevel.SEED) } returns Result.failure(IllegalStateException("failed"))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(EntryStep.TERMS, viewModel.step)
            assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
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
    fun `초기 필터 태그 적용이 취소되면 오류를 보내지 않는다`() = runTest(testDispatcher) {
        val applyInitialFilterTags = testApplyInitialFilterTagsUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(
            saveOnboardingProfileUseCase = saveOnboardingProfileUseCase,
            applyInitialFilterTagsUseCase = applyInitialFilterTags,
        )
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        coEvery { applyInitialFilterTags(OnboardingLevel.SEED) } throws CancellationException("cancelled")
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(EntryStep.TERMS, viewModel.step)
            expectNoEvents()
        }
    }

    @Test
    fun `입력 오류는 다시 시도 버튼 없이 알린다`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.InvalidProfile
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
            assertEquals(
                EntryEffect.ShowSubmissionError("입력 정보를 확인해주세요.", canRetry = false),
                awaitItem(),
            )
        }
    }

    @Test
    fun `요청 제한에 걸리면 다시 시도 버튼 없이 기다리라고 알린다`() = runTest(testDispatcher) {
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        val viewModel = testViewModel(saveOnboardingProfileUseCase = saveOnboardingProfileUseCase)
        coEvery { saveOnboardingProfileUseCase.submit(any(), any()) } returns OnboardingSubmissionResult.RateLimited
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.startOnboardingAnalysis()
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.onboardingAnalysisState)
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
    fun `분석 중 중복 제출은 무시한다`() = runTest(testDispatcher) {
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
    fun `완료하면 진입 완료를 저장하고 완료 효과를 보낸다`() = runTest(testDispatcher) {
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
    fun `진입 완료 저장이 실패하면 완료 효과를 보내지 않는다`() = runTest(testDispatcher) {
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
    fun `진입 완료 저장이 취소되면 완료 효과를 보내지 않는다`() = runTest(testDispatcher) {
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
        applyInitialFilterTagsUseCase: ApplyInitialFilterTagsUseCase = testApplyInitialFilterTagsUseCase(),
        savedProgress: EntryProgress = EntryProgress(),
        savedProfile: OnboardingProfile = OnboardingProfile(),
        mode: EntryMode = EntryMode.AUTHENTICATED,
    ): EntryViewModel {
        coEvery { setEntryCompletedUseCase() } returns Unit
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        coEvery { saveOnboardingProfileUseCase.saveForSubmission(any()) } returns Unit
        coEvery { applyInitialFilterTagsUseCase(any()) } returns Result.success(Unit)
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
            applyInitialFilterTagsUseCase = applyInitialFilterTagsUseCase,
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

    private fun testApplyInitialFilterTagsUseCase(): ApplyInitialFilterTagsUseCase =
        mockk()

}

private val EntryViewModel.isRestored: Boolean get() = uiState.value.isRestored
private val EntryViewModel.step: EntryStep get() = uiState.value.step
private val EntryViewModel.webViewUrl: String get() = uiState.value.webViewUrl
private val EntryViewModel.serviceTermsChecked: Boolean get() = uiState.value.serviceTermsChecked
private val EntryViewModel.privacyTermsChecked: Boolean get() = uiState.value.privacyTermsChecked
private val EntryViewModel.locationTermsChecked: Boolean get() = uiState.value.locationTermsChecked
private val EntryViewModel.licenseChecked: Boolean get() = uiState.value.licenseChecked
private val EntryViewModel.companionChecked: Boolean get() = uiState.value.companionChecked
private val EntryViewModel.precautionAgreementChecked: Boolean get() = uiState.value.precautionAgreementChecked
private val EntryViewModel.nickname: String get() = uiState.value.nickname
private val EntryViewModel.drivingPeriod: DrivingPeriod? get() = uiState.value.drivingPeriod
private val EntryViewModel.recentFrequency: RecentDrivingFrequency? get() = uiState.value.recentFrequency
private val EntryViewModel.roadExperiences: List<RoadExperience> get() = uiState.value.roadExperiences
private val EntryViewModel.soloDrivingRange: SoloDrivingRange? get() = uiState.value.soloDrivingRange
private val EntryViewModel.soloParkingLevel: SoloParkingLevel? get() = uiState.value.soloParkingLevel
private val EntryViewModel.practiceSituations: List<PracticeSituation> get() = uiState.value.practiceSituations
private val EntryViewModel.vehicleType: VehicleType? get() = uiState.value.vehicleType
private val EntryViewModel.goal: String get() = uiState.value.goal
private val EntryViewModel.isCareerStepValid: Boolean get() = uiState.value.isCareerStepValid
private val EntryViewModel.isPreferenceNextEnabled: Boolean get() = uiState.value.isPreferenceNextEnabled
