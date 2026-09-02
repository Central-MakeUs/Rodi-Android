package com.dororong.rodi.core.ui.map

import android.content.Context
import androidx.core.content.edit
import com.dororong.rodi.core.domain.model.course.GeoPoint

data class MapCameraSnapshot(
    val center: GeoPoint,
    val zoomLevel: Int,
)

fun Context.lastMapCameraOrNull(): MapCameraSnapshot? {
    val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (
        !preferences.contains(KEY_CENTER_LATITUDE) ||
        !preferences.contains(KEY_CENTER_LONGITUDE) ||
        !preferences.contains(KEY_ZOOM_LEVEL)
    ) {
        return null
    }
    return mapCameraSnapshotOrNull(
        lat = java.lang.Double.longBitsToDouble(
            preferences.getLong(KEY_CENTER_LATITUDE, 0L),
        ),
        lng = java.lang.Double.longBitsToDouble(
            preferences.getLong(KEY_CENTER_LONGITUDE, 0L),
        ),
        zoom = preferences.getInt(KEY_ZOOM_LEVEL, 0),
    )
}

fun Context.saveLastMapCamera(center: GeoPoint, zoomLevel: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putLong(KEY_CENTER_LATITUDE, java.lang.Double.doubleToRawLongBits(center.lat))
        putLong(KEY_CENTER_LONGITUDE, java.lang.Double.doubleToRawLongBits(center.lng))
        putInt(KEY_ZOOM_LEVEL, zoomLevel)
    }
}

internal fun mapCameraSnapshotOrNull(
    lat: Double,
    lng: Double,
    zoom: Int,
): MapCameraSnapshot? {
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0 || zoom <= 0) return null
    return MapCameraSnapshot(
        center = GeoPoint(lat, lng),
        zoomLevel = zoom,
    )
}

private const val PREFS_NAME = "rodi_map_camera"
private const val KEY_CENTER_LATITUDE = "center_latitude"
private const val KEY_CENTER_LONGITUDE = "center_longitude"
private const val KEY_ZOOM_LEVEL = "zoom_level"
