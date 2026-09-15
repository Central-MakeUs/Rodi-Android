package com.dororong.rodi.feature.home.navi

import android.content.Context
import android.content.Intent
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import java.util.Locale

/**
 * 카카오내비 앱에 코스(출발→경유→목적)를 전달해 길 안내를 시작한다.
 *
 * [NaviClient.navigateIntent]로 Intent를 생성해 startActivity로 실행.
 * KakaoSdk.init() 완료 후 사용 가능 (RodiApplication.onCreate 참고).
 * 카카오내비 미설치 시 Play Store 설치 페이지로 이동.
 * 카카오내비도 현재 GPS를 출발지로 사용하므로 origin + waypoints를 경유지(viaList)로 전달.
 */
object KakaoNaviLauncher {

    private const val KAKAONAVI_PACKAGE = "com.locnall.KimGiSa"

    fun launch(context: Context, place: PlaceDetail) {
        if (!NaviClient.instance.isKakaoNaviInstalled(context)) {
            openInstallPage(context)
            return
        }

        val courseWaypoints = place.course?.waypoints.orEmpty().sortedBy { it.sequence }
        val destinationPoint = courseWaypoints
            .firstOrNull { it.type == PlaceWaypointType.DESTINATION }
        val destination = Location(
            name = destinationPoint?.name ?: place.name,
            x = (destinationPoint?.point?.lng ?: place.point.lng).toNaviCoordinate(),
            y = (destinationPoint?.point?.lat ?: place.point.lat).toNaviCoordinate(),
        )

        val viaList = if (place.type == PlaceType.PARKING) {
            emptyList()
        } else {
            courseWaypoints
                .filter { it.type == PlaceWaypointType.START || it.type == PlaceWaypointType.VIA }
                .map { point ->
                Location(
                    name = point.name.orEmpty(),
                    x = point.point.lng.toNaviCoordinate(),
                    y = point.point.lat.toNaviCoordinate(),
                )
            }
        }

        val intent = NaviClient.instance.navigateIntent(
            destination = destination,
            option = NaviOption(coordType = CoordType.WGS84),
            viaList = viaList,
        )
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * 내비 선택 시트의 설치 여부 표시용. 실제 실행 직전 판정은 [launch]가 [NaviClient]로 따로 한다.
     */
    fun isInstalled(context: Context): Boolean = context.isPackageInstalled(KAKAONAVI_PACKAGE)

    fun openInstallPage(context: Context) = context.openPlayStore(KAKAONAVI_PACKAGE)
}

internal fun Double.toNaviCoordinate(): String = String.format(Locale.US, "%.6f", this)
