package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.repository.LiveUpdateRepository
import javax.inject.Inject

class SetLiveUpdateEnabledUseCase @Inject constructor(
    private val repository: LiveUpdateRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}
