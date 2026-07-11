package com.dororong.rodi.core.domain.usecase.navi

import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.repository.NaviPreferenceRepository
import javax.inject.Inject

class SetNaviAlwaysUseCase @Inject constructor(
    private val naviPreferenceRepository: NaviPreferenceRepository,
) {
    suspend operator fun invoke(app: NaviApp) = naviPreferenceRepository.setAlways(app)
}
