package com.dororong.rodi.feature.home.navi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal fun Context.isPackageInstalled(packageName: String): Boolean = runCatching {
    packageManager.getPackageInfo(packageName, 0)
}.isSuccess

/** Play 스토어 앱이 없는 기기(에뮬레이터 등)는 market 스킴을 처리하지 못하므로 웹 스토어로 폴백한다. */
internal fun Context.openPlayStore(packageName: String) {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).addFlags(flags))
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
                .addFlags(flags),
        )
    }
}
