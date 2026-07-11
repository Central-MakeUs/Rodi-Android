package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.navi.NaviApp

interface NaviPreferenceRepository {
    suspend fun getAlways(): NaviApp?
    suspend fun setAlways(app: NaviApp)
}
