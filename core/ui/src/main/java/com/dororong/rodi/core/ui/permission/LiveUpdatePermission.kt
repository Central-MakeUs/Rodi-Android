package com.dororong.rodi.core.ui.permission

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

fun Context.canPostPromotedNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return true
    @Suppress("NewApi")
    return getSystemService(NotificationManager::class.java)
        ?.canPostPromotedNotifications() == true
}

fun Context.openPromotedNotificationSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
        openAppSettings()
        return
    }
    @Suppress("NewApi")
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
        .onFailure { openAppSettings() }
}
