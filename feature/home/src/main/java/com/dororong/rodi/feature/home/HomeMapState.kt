package com.dororong.rodi.feature.home

import android.content.Context
import androidx.core.content.edit
import com.kakao.vectormap.LatLng

internal const val DEFAULT_ZOOM = 13
private const val HOME_PREFS = "rodi_home_prefs"
private const val KEY_HAS_LOADED_MAP = "has_loaded_map"
internal val SEOUL = LatLng.from(37.5563, 126.9220)
internal var hasLoadedMapInSession = false

internal enum class MapScreenState {
    Loading,
    Ready,
    NetworkError,
}

internal fun Context.hasLoadedMapBefore(): Boolean =
    getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_LOADED_MAP, false)

internal fun Context.markMapLoaded() {
    getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
        .edit {
            putBoolean(KEY_HAS_LOADED_MAP, true)
        }
}
