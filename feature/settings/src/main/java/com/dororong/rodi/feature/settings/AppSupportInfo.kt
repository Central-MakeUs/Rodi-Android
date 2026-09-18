package com.dororong.rodi.feature.settings

import android.content.Context
import android.os.Build

/**
 * 문의 메일이나 버그 제보에 붙일 정보. 문의를 받을 때마다 버전·기기를 되묻지 않으려고 만든다.
 * 개인 정보는 담지 않는다 — 닉네임·계정 식별자는 넣지 말 것.
 */
internal fun appSupportInfo(context: Context, appVersion: String): String {
    val versionCode = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    }.getOrNull()
    val version = if (versionCode == null) appVersion else "$appVersion ($versionCode)"
    return buildString {
        appendLine("앱 버전: $version")
        appendLine("기기: ${Build.MANUFACTURER} ${Build.MODEL}")
        append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }
}
