package com.dororong.rodi.feature.home.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val MAX_CACHED_LOCATION_AGE_MILLIS = 2 * 60 * 1000L
private const val LOCATION_UPDATE_INTERVAL_MILLIS = 3_000L
private const val LOCATION_UPDATE_MIN_INTERVAL_MILLIS = 1_000L
private const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 1f

/** 위치 권한(FINE 또는 COARSE)이 하나라도 허용돼 있는지. */
fun Context.hasLocationPermission(): Boolean {
    fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    return granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
        granted(Manifest.permission.ACCESS_COARSE_LOCATION)
}

/**
 * 현재 위치를 1회 취득한다. 권한이 없거나 위치를 못 받으면 null.
 * 마지막 위치가 있으면 우선 사용하고, 없으면 즉시 측위(getCurrentLocation)를 시도한다.
 */
@SuppressLint("MissingPermission")
suspend fun Context.awaitCurrentLocation(): LatLng? {
    if (!hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(this)

    val last = suspendCancellableCoroutine<Location?> { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc) }
            .addOnFailureListener { cont.resume(null) }
    }
    if (last != null && last.isFreshEnough()) return LatLng.from(last.latitude, last.longitude)

    val cts = CancellationTokenSource()
    return suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLng.from(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}

@SuppressLint("MissingPermission")
fun Context.currentLocationUpdates(): Flow<LatLng> = callbackFlow {
    if (!hasLocationPermission()) {
        close()
        return@callbackFlow
    }

    val client = LocationServices.getFusedLocationProviderClient(this@currentLocationUpdates)
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                trySend(LatLng.from(location.latitude, location.longitude))
            }
        }
    }
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MILLIS)
        .setMinUpdateIntervalMillis(LOCATION_UPDATE_MIN_INTERVAL_MILLIS)
        .setMinUpdateDistanceMeters(LOCATION_UPDATE_MIN_DISTANCE_METERS)
        .build()

    client.lastLocation.addOnSuccessListener { location ->
        location?.let { trySend(LatLng.from(it.latitude, it.longitude)) }
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
