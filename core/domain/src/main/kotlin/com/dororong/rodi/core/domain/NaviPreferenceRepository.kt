package com.dororong.rodi.core.domain

interface NaviPreferenceRepository {
    fun getAlways(): NaviApp?
    fun setAlways(app: NaviApp)
}
