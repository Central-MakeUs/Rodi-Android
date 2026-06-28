package com.cmc.routi.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

    val last = suspendCancellableCoroutine<LatLng?> { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLng.from(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }
    if (last != null) return last

    val cts = CancellationTokenSource()
    return suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLng.from(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}
