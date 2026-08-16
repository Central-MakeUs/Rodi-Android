package com.dororong.rodi.feature.home.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.LatLng
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

private const val MAX_CACHED_LOCATION_AGE_MILLIS = 2 * 60 * 1000L
private const val LOCATION_UPDATE_INTERVAL_MILLIS = 3_000L
private const val LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1_000L
private const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 1f
const val INITIAL_LOCATION_TIMEOUT_MILLIS = 5_000L

/**
 * 유효한 현재 위치를 제한 시간 동안 기다린다. 권한이 없거나 제한 시간을 넘기면 null.
 */
@SuppressLint("MissingPermission")
suspend fun Context.awaitCurrentLocation(
    timeoutMillis: Long = INITIAL_LOCATION_TIMEOUT_MILLIS,
): LatLng? {
    if (!hasLocationPermission()) return null
    return withTimeoutOrNull(timeoutMillis) {
        currentLocationUpdates().first()
    }
}

@SuppressLint("MissingPermission")
fun Context.currentLocationUpdates(): Flow<LatLng> =
    rawCurrentLocationUpdates().map { LatLng.from(it.latitude, it.longitude) }

@SuppressLint("MissingPermission")
fun Context.rawCurrentLocationUpdates(): Flow<Location> = callbackFlow {
    if (!hasLocationPermission()) {
        close()
        return@callbackFlow
    }

    val client = LocationServices.getFusedLocationProviderClient(this@rawCurrentLocationUpdates)
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                trySend(location)
            }
        }
    }
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MILLIS)
        .setMinUpdateIntervalMillis(LOCATION_UPDATE_MIN_INTERVAL_MILLIS)
        .setMinUpdateDistanceMeters(LOCATION_UPDATE_MIN_DISTANCE_METERS)
        .build()

    client.lastLocation.addOnSuccessListener { location ->
        location
            ?.takeIf(Location::isFreshEnough)
            ?.let { trySend(it) }
    }
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}

private fun Location.isFreshEnough(): Boolean {
    val ageMillis = if (elapsedRealtimeNanos > 0L) {
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    } else {
        System.currentTimeMillis() - time
    }
    return ageMillis in 0..MAX_CACHED_LOCATION_AGE_MILLIS
}
