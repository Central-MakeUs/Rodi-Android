package com.dororong.rodi.core.ui.permission

enum class LocationPermissionAction {
    RequestSystemPermission,
    OpenAppSettings,
}

fun resolveLocationPermissionAction(
    isLocationGranted: Boolean,
    hasRequestedLocationPermission: Boolean,
    shouldShowRationale: Boolean,
): LocationPermissionAction = when {
    isLocationGranted -> LocationPermissionAction.OpenAppSettings
    !hasRequestedLocationPermission || shouldShowRationale -> LocationPermissionAction.RequestSystemPermission
    else -> LocationPermissionAction.OpenAppSettings
}
