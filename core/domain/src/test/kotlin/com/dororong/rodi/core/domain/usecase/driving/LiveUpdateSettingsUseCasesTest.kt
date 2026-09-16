package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.driving.LiveUpdateSettings
import com.dororong.rodi.core.domain.repository.LiveUpdateRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiveUpdateSettingsUseCasesTest {
    private val repository = mockk<LiveUpdateRepository>(relaxUnitFun = true)

    @Test
    fun `live updates are on until the user turns them off`() = runTest {
        every { repository.settings } returns flowOf(LiveUpdateSettings())

        assertTrue(ObserveLiveUpdateSettingsUseCase(repository)().first().isEnabled)
    }

    @Test
    fun `the app setting is stored as the user left it`() = runTest {
        every { repository.settings } returns flowOf(LiveUpdateSettings(isEnabled = false))

        SetLiveUpdateEnabledUseCase(repository)(false)

        coVerify(exactly = 1) { repository.setEnabled(false) }
        assertFalse(ObserveLiveUpdateSettingsUseCase(repository)().first().isEnabled)
    }
}
