package com.dororong.rodi.core.data.navi

import android.content.Context
import com.dororong.rodi.core.domain.NaviApp

object NaviPreference {

    private const val PREFS_NAME = "rodi_navi"
    private const val KEY_ALWAYS = "navi_always_app"

    fun getAlways(context: Context): NaviApp? {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALWAYS, null) ?: return null
        return NaviApp.entries.firstOrNull { it.key == key }
    }

    fun setAlways(context: Context, app: NaviApp) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALWAYS, app.key)
            .apply()
    }
}
