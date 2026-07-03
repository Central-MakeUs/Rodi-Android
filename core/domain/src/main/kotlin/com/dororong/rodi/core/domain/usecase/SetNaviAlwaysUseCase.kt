package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import javax.inject.Inject

class SetNaviAlwaysUseCase @Inject constructor(
    private val naviPreferenceRepository: NaviPreferenceRepository,
) {
    suspend operator fun invoke(app: NaviApp) = naviPreferenceRepository.setAlways(app)
}
