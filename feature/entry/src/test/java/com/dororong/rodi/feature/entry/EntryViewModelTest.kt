package com.dororong.rodi.feature.entry

import com.dororong.rodi.core.domain.DrivingPeriod
import com.dororong.rodi.core.domain.PracticeSituation
import com.dororong.rodi.core.domain.RecentDrivingFrequency
import com.dororong.rodi.core.domain.RoadExperience
import com.dororong.rodi.core.domain.SoloDrivingRange
import com.dororong.rodi.core.domain.SoloParkingLevel
import com.dororong.rodi.core.domain.VehicleType
import com.dororong.rodi.core.domain.usecase.SaveOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    fun `back moves through previous steps and returns false at terms`() {
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

        assertTrue(viewModel.back())
        assertEquals(EntryStep.PREFERENCE, viewModel.step)

        assertTrue(viewModel.back())
        assertEquals(EntryStep.CAREER, viewModel.step)

        assertTrue(viewModel.back())
        assertEquals(EntryStep.NICKNAME, viewModel.step)

        viewModel.openWebView("https://example.com")
        assertTrue(viewModel.back())
        assertEquals(EntryStep.TERMS, viewModel.step)
    }

    @Test
    fun `openWebView stores url and moves to webview step`() {
        val viewModel = testViewModel()

        viewModel.openWebView("https://example.com/terms")

        assertEquals("https://example.com/terms", viewModel.webViewUrl)
        assertEquals(EntryStep.TERMS_WEBVIEW, viewModel.step)
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

        viewModel.ensureNicknameGenerated()
        val nickname = viewModel.nickname
        viewModel.ensureNicknameGenerated()

        assertTrue(nickname.isNotBlank())
        assertEquals(nickname, viewModel.nickname)
    }

    @Test
    fun `solo road experience requires conditional answers and clears them when changed`() {
        val viewModel = testViewModel()

        viewModel.selectDrivingPeriod(DrivingPeriod.MONTH_1_TO_3)
        viewModel.selectRecentFrequency(RecentDrivingFrequency.WEEKLY_1)
        viewModel.selectRoadExperience(RoadExperience.SOLO)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectSoloDrivingRange(SoloDrivingRange.FAMILIAR_ROAD)
        assertFalse(viewModel.isCareerStepValid)

        viewModel.selectSoloParkingLevel(SoloParkingLevel.FAMILIAR_SPOT)
        assertTrue(viewModel.isCareerStepValid)

        viewModel.selectRoadExperience(RoadExperience.WITH_COMPANION)
        assertEquals(null, viewModel.soloDrivingRange)
        assertEquals(null, viewModel.soloParkingLevel)
        assertTrue(viewModel.isCareerStepValid)
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
    fun `complete stores entry completion and invokes callback`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        coEvery { setEntryCompletedUseCase() } returns Unit
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        val viewModel = EntryViewModel(setEntryCompletedUseCase, saveOnboardingProfileUseCase)
        var done = false

        viewModel.complete { done = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { saveOnboardingProfileUseCase(any()) }
        coVerify(exactly = 1) { setEntryCompletedUseCase() }
        assertTrue(done)
    }

    @Test
    fun `complete does not invoke callback when use case throws`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        coEvery { setEntryCompletedUseCase() } throws IllegalStateException("failed")
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        val viewModel = EntryViewModel(setEntryCompletedUseCase, saveOnboardingProfileUseCase)
        var done = false

        viewModel.complete { done = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { saveOnboardingProfileUseCase(any()) }
        coVerify(exactly = 1) { setEntryCompletedUseCase() }
        assertFalse(done)
    }

    @Test
    fun `complete does not invoke callback when use case is cancelled`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        coEvery { setEntryCompletedUseCase() } throws CancellationException("cancelled")
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        val viewModel = EntryViewModel(setEntryCompletedUseCase, saveOnboardingProfileUseCase)
        var done = false

        viewModel.complete { done = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { setEntryCompletedUseCase() }
        assertFalse(done)
    }

    private fun testViewModel(): EntryViewModel {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        val saveOnboardingProfileUseCase = testSaveOnboardingProfileUseCase()
        coEvery { setEntryCompletedUseCase() } returns Unit
        coEvery { saveOnboardingProfileUseCase(any()) } returns Unit
        return EntryViewModel(setEntryCompletedUseCase, saveOnboardingProfileUseCase)
    }

    private fun testSetEntryCompletedUseCase(): SetEntryCompletedUseCase =
        mockk()

    private fun testSaveOnboardingProfileUseCase(): SaveOnboardingProfileUseCase =
        mockk()
}
