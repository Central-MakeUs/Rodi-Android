package com.dororong.rodi.core.ui.permission

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 이 앱의 실시간 업데이트 허용 설정만 연다. 제조사에 따라 그 화면이 없을 수 있어
 * ([Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS]는 Android 16부터) 열리지 않으면
 * 앱 알림 설정 → 앱 상세 설정 순으로 물러난다.
 */
fun Context.openLiveUpdateSystemSettings() {
    val candidates = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            add(
                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
        add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        add(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }
    candidates.firstOrNull { it.resolveActivity(packageManager) != null }
        ?.let(::startActivity)
}
