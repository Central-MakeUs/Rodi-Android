package com.dororong.rodi.core.data.navi

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface NaviPreferenceRepository {
    fun getAlways(): NaviApp?
    fun setAlways(app: NaviApp)
}

class NaviPreferenceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NaviPreferenceRepository {
    override fun getAlways(): NaviApp? = NaviPreference.getAlways(context)
    override fun setAlways(app: NaviApp) = NaviPreference.setAlways(context, app)
}
