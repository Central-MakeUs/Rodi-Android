package com.dororong.rodi.core.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class PermissionAction {
    RequestSystemPermission,
    OpenAppSettings,
}

typealias LocationPermissionAction = PermissionAction

fun resolvePermissionAction(
    isGranted: Boolean,
    hasRequestedPermission: Boolean,
    shouldShowRationale: Boolean,
): PermissionAction = when {
    isGranted -> PermissionAction.OpenAppSettings
    !hasRequestedPermission || shouldShowRationale -> PermissionAction.RequestSystemPermission
    else -> PermissionAction.OpenAppSettings
}

fun resolveLocationPermissionAction(
    isLocationGranted: Boolean,
    hasRequestedLocationPermission: Boolean,
    shouldShowRationale: Boolean,
): PermissionAction = resolvePermissionAction(isLocationGranted, hasRequestedLocationPermission, shouldShowRationale)

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.hasNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
} else {
    NotificationManagerCompat.from(this).areNotificationsEnabled()
}
