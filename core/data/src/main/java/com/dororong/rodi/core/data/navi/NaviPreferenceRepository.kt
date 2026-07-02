package com.dororong.rodi.core.data.navi

import android.content.Context
import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NaviPreferenceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NaviPreferenceRepository {
    override fun getAlways(): NaviApp? = NaviPreference.getAlways(context)
    override fun setAlways(app: NaviApp) = NaviPreference.setAlways(context, app)
}
