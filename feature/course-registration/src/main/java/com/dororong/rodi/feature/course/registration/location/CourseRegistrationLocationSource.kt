package com.dororong.rodi.feature.course.registration.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val LOCATION_TIMEOUT_MILLIS = 5_000L
private const val MAX_CACHED_LOCATION_AGE_MILLIS = 2 * 60 * 1000L
private const val LOCATION_UPDATE_INTERVAL_MILLIS = 3_000L
private const val LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1_000L
private const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 1f

/** Registration-owned adapter using the same fused-location freshness semantics as the home map. */
@SuppressLint("MissingPermission")
suspend fun Context.awaitRegistrationCurrentLocation(
    timeoutMillis: Long = LOCATION_TIMEOUT_MILLIS,
): GeoPoint? {
    if (!hasLocationPermission()) return null
    return withTimeoutOrNull(timeoutMillis) {
        registrationCurrentLocationUpdates().first()
    }
}

@SuppressLint("MissingPermission")
fun Context.registrationCurrentLocationUpdates(): Flow<GeoPoint> = callbackFlow {
    if (!hasLocationPermission()) {
        close()
        return@callbackFlow
    }

    val client = LocationServices.getFusedLocationProviderClient(this@registrationCurrentLocationUpdates)
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                trySend(GeoPoint(location.latitude, location.longitude))
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
            ?.let { trySend(GeoPoint(it.latitude, it.longitude)) }
    }
    client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}

private fun Location.isFreshEnough(): Boolean {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    val ageMillis = if (elapsedRealtimeNanos > 0L) {
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
    } else {
        System.currentTimeMillis() - time
    }
    return ageMillis in 0..MAX_CACHED_LOCATION_AGE_MILLIS
}
