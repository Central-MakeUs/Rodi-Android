package com.dororong.rodi.feature.home.map

import android.content.Context
import androidx.core.content.edit
import com.kakao.vectormap.LatLng

internal const val DEFAULT_ZOOM = 13
private const val HOME_PREFS = "rodi_home_prefs"
private const val KEY_HAS_LOADED_MAP = "has_loaded_map"
internal val SEOUL = LatLng.from(37.5665, 126.9780)
internal var hasLoadedMapInSession = false

internal enum class MapScreenState {
    Loading,
    Ready,
    NetworkError,
    /** 네트워크는 정상인데 지도 SDK 자체가 초기화·렌더링에 실패한 경우. */
    Error,
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
