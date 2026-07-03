package com.dororong.rodi.core.domain

interface NaviPreferenceRepository {
    suspend fun getAlways(): NaviApp?
    suspend fun setAlways(app: NaviApp)
}
