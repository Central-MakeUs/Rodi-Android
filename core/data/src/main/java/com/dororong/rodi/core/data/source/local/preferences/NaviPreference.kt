package com.dororong.rodi.core.data.source.local.preferences

import android.content.Context
import com.dororong.rodi.core.domain.model.navi.NaviApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NaviPreference @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun getAlways(): NaviApp? {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALWAYS, null) ?: return null
        return NaviApp.entries.firstOrNull { it.key == key }
    }

    fun setAlways(app: NaviApp) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALWAYS, app.key)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "rodi_navi"
        const val KEY_ALWAYS = "navi_always_app"
    }
}
