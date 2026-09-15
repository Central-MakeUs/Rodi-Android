package com.dororong.rodi.core.ui.permission

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

fun Context.canPostPromotedNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return true
    return getSystemService(NotificationManager::class.java)?.canPostPromotedNotifications() == true
}

/**
 * 실시간 업데이트(Live Updates) 허용 설정은 앱별 프로모션 설정 화면에 있다.
 * 제조사 설정 앱이 이 화면을 제공하지 않으면 앱 알림 설정, 그것도 없으면 앱 정보 화면으로 보낸다.
 */
fun Context.openPromotedNotificationSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
        tryStartActivity(appSettingsIntent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS))
    ) {
        return
    }
    if (tryStartActivity(appSettingsIntent(Settings.ACTION_APP_NOTIFICATION_SETTINGS))) return
    openAppSettings()
}

private fun Context.appSettingsIntent(action: String): Intent =
    Intent(action)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun Context.tryStartActivity(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
