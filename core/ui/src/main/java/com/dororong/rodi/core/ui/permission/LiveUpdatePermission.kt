package com.dororong.rodi.core.ui.permission

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 시스템이 우리 알림을 실시간 업데이트로 승격해도 되는지. 승격 여부 자체는 제조사 구현에 따라
 * 이 값과 어긋날 수 있어(One UI 8 실측, 2026-09-16) 화면 분기에는 쓰지 않고 진단 로그로만 쓴다.
 */
fun Context.canPostPromotedNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return true
    return getSystemService(NotificationManager::class.java)?.canPostPromotedNotifications() == true
}
