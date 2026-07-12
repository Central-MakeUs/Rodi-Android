package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.preferences.NaviPreference
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.repository.NaviPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NaviPreferenceRepositoryImpl @Inject constructor(
    private val preference: NaviPreference,
) : NaviPreferenceRepository {
    override suspend fun getAlways(): NaviApp? = withContext(Dispatchers.IO) {
        preference.getAlways()
    }

    override suspend fun setAlways(app: NaviApp) = withContext(Dispatchers.IO) {
        preference.setAlways(app)
    }
}
