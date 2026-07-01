package com.dororong.rodi.navi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CoursePoint
import androidx.core.net.toUri

/**
 * 코스(출발→경유→목적)를 **카카오맵 앱**으로 보내 지도 위에 경로선을 표시한다.
 *
 * URL Scheme: `kakaomap://route?vp=위도,경도&vp2=...&ep=위도,경도&by=car`
 * (https://apis.map.kakao.com/android_v2/docs/api-guide/urlscheme/)
 *
 * 카카오맵은 sp(출발지)를 지정해도 현재 GPS로 덮어쓰므로, 코스 출발지를 경로에 포함하려면
 * 첫 번째 vp 로 넣는다. 경유지(vp~vp5)는 최대 5개.
 */
object KakaoMapLauncher {

    private const val MAX_VIA_COUNT = 5
    private const val KAKAO_MAP_PACKAGE = "net.daum.android.map"
    private const val MARKET_URL = "market://details?id=$KAKAO_MAP_PACKAGE"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$KAKAO_MAP_PACKAGE"

    fun launch(context: Context, course: Course) {
        val intent = Intent(Intent.ACTION_VIEW, buildRouteUri(course).toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "카카오맵이 설치되어 있지 않아 설치 페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
            openInstallPage(context)
        }
    }

    fun openInstallPage(context: Context) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, MARKET_URL.toUri()).addFlags(flags))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri()).addFlags(flags))
        }
    }

    /** 출발지(vp) → 경유지(vp2..) → 목적지(ep). 주차장은 목적지만 전달한다. */
    internal fun buildRouteUri(course: Course): String {
        val params = buildList {
            if (!course.isParking) {
                val viaPoints = (listOf(course.origin) + course.waypointPoints).take(MAX_VIA_COUNT)
                viaPoints.forEachIndexed { i, point ->
                    val key = if (i == 0) "vp" else "vp${i + 1}" // vp, vp2, vp3, vp4, vp5
                    add("$key=${point.toMapParam()}")
                }
            }
            add("ep=${course.destination.toMapParam()}")
            add("by=car")
        }
        return "kakaomap://route?${params.joinToString("&")}"
    }

    /** 카카오맵 좌표 파라미터는 **위도,경도** 순서. */
    private fun CoursePoint.toMapParam(): String = "$lat,$lng"
}
