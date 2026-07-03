package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import javax.inject.Inject

class GetNaviAlwaysUseCase @Inject constructor(
    private val naviPreferenceRepository: NaviPreferenceRepository,
) {
    suspend operator fun invoke(): NaviApp? = naviPreferenceRepository.getAlways()
}
