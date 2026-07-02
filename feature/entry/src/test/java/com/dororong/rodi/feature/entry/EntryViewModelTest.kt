package com.dororong.rodi.feature.entry

import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    fun `next moves from location to terms to precautions and stays there`() {
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())

        viewModel.next()
        assertEquals(EntryStep.TERMS, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)

        viewModel.next()
        assertEquals(EntryStep.PRECAUTIONS, viewModel.step)
    }

    @Test
    fun `back moves through previous steps and returns false at location`() {
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())

        assertFalse(viewModel.back())
        assertEquals(EntryStep.LOCATION, viewModel.step)

        viewModel.next()
        assertTrue(viewModel.back())
        assertEquals(EntryStep.LOCATION, viewModel.step)

        viewModel.next()
        viewModel.next()
        assertTrue(viewModel.back())
        assertEquals(EntryStep.TERMS, viewModel.step)

        viewModel.openWebView("https://example.com")
        assertTrue(viewModel.back())
        assertEquals(EntryStep.TERMS, viewModel.step)
    }

    @Test
    fun `openWebView stores url and moves to webview step`() {
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())

        viewModel.openWebView("https://example.com/terms")

        assertEquals("https://example.com/terms", viewModel.webViewUrl)
        assertEquals(EntryStep.TERMS_WEBVIEW, viewModel.step)
    }

    @Test
    fun `setAllTermsChecked updates only terms checkboxes`() {
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())
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
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())

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
        val viewModel = EntryViewModel(testSetEntryCompletedUseCase())

        viewModel.toggleLicense()

        assertFalse(viewModel.serviceTermsChecked)
        assertFalse(viewModel.privacyTermsChecked)
        assertFalse(viewModel.locationTermsChecked)
        assertTrue(viewModel.licenseChecked)
        assertFalse(viewModel.companionChecked)
        assertFalse(viewModel.precautionAgreementChecked)
    }

    @Test
    fun `complete stores entry completion and invokes callback`() = runTest(testDispatcher) {
        val setEntryCompletedUseCase = testSetEntryCompletedUseCase()
        coEvery { setEntryCompletedUseCase() } returns Unit
        val viewModel = EntryViewModel(setEntryCompletedUseCase)
        var done = false

        viewModel.complete { done = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { setEntryCompletedUseCase() }
        assertTrue(done)
    }

    private fun testSetEntryCompletedUseCase(): SetEntryCompletedUseCase =
        mockk()
}
