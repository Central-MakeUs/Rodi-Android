package com.dororong.rodi.navi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.dororong.rodi.model.Course
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption

/**
 * 카카오내비 앱에 코스(출발→경유→목적)를 전달해 길 안내를 시작한다.
 *
 * [NaviClient.navigateIntent]로 Intent를 생성해 startActivity로 실행.
 * KakaoSdk.init() 완료 후 사용 가능 (RoutiApplication.onCreate 참고).
 * 카카오내비 미설치 시 Play Store 설치 페이지로 이동.
 * 카카오내비도 현재 GPS를 출발지로 사용하므로 origin + waypoints를 경유지(viaList)로 전달.
 */
object KakaoNaviLauncher {

    private const val KAKAONAVI_PACKAGE = "com.locnall.KimGiSa"
    private const val MARKET_URL = "market://details?id=$KAKAONAVI_PACKAGE"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$KAKAONAVI_PACKAGE"

    fun launch(context: Context, course: Course) {
        if (!NaviClient.instance.isKakaoNaviInstalled(context)) {
            openInstallPage(context)
            return
        }

        val destination = Location(
            name = course.destination.name,
            x = "%.6f".format(course.destination.lng),
            y = "%.6f".format(course.destination.lat),
        )

        val viaList = (listOf(course.origin) + course.waypointPoints).map { p ->
            Location(name = p.name, x = "%.6f".format(p.lng), y = "%.6f".format(p.lat))
        }

        val intent = NaviClient.instance.navigateIntent(
            destination = destination,
            option = NaviOption(coordType = CoordType.WGS84),
            viaList = viaList,
        )
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInstallPage(context: Context) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, MARKET_URL.toUri()).addFlags(flags))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri()).addFlags(flags))
        }
    }
}
