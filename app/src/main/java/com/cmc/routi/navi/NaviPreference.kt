package com.cmc.routi.navi

import android.content.Context

enum class NaviApp(val key: String, val label: String) {
    KAKAOMAP("kakaomap", "카카오맵"),
    KAKAONAVI("kakaonavi", "카카오내비"),
}

object NaviPreference {

    private const val PREFS_NAME = "routi_navi"
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
