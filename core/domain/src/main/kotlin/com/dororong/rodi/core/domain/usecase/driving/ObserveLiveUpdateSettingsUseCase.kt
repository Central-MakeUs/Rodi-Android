package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.repository.LiveUpdateRepository
import javax.inject.Inject

class ObserveLiveUpdateSettingsUseCase @Inject constructor(
    private val repository: LiveUpdateRepository,
) {
    operator fun invoke() = repository.settings
}
