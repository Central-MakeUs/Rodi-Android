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
    fun `실시간 업데이트는 사용자가 끄기 전까지 켜져 있다`() = runTest {
        every { repository.settings } returns flowOf(LiveUpdateSettings())

        assertTrue(ObserveLiveUpdateSettingsUseCase(repository)().first().isEnabled)
    }

    @Test
    fun `앱 설정은 사용자가 둔 값 그대로 저장한다`() = runTest {
        every { repository.settings } returns flowOf(LiveUpdateSettings(isEnabled = false))

        SetLiveUpdateEnabledUseCase(repository)(false)

        coVerify(exactly = 1) { repository.setEnabled(false) }
        assertFalse(ObserveLiveUpdateSettingsUseCase(repository)().first().isEnabled)
    }
}
