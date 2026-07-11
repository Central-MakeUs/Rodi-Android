package com.dororong.rodi.core.data.navi

import android.content.Context
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.repository.NaviPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NaviPreferenceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NaviPreferenceRepository {
    override suspend fun getAlways(): NaviApp? = withContext(Dispatchers.IO) {
        NaviPreference.getAlways(context)
    }

    override suspend fun setAlways(app: NaviApp) = withContext(Dispatchers.IO) {
        NaviPreference.setAlways(context, app)
    }
}
