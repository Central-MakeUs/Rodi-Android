package com.dororong.rodi.core.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 권한을 영구 거부하면 시스템 권한 창이 더 이상 뜨지 않는다(launch가 즉시 거부로 끝난다).
 * 이때는 사용자가 직접 켤 수 있도록 앱 설정 화면으로 보낸다.
 */
fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.hasNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
} else {
    NotificationManagerCompat.from(this).areNotificationsEnabled()
}
