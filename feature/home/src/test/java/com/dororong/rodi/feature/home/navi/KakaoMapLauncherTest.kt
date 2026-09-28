package com.dororong.rodi.feature.home.navi

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.feature.home.HomePreviewData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KakaoMapLauncherTest {
    private val course = HomePreviewData.courseDetail
    private val courseDetail = requireNotNull(course.course)

    @Test
    fun `코스는 출발지와 경유지를 순서대로 vp 키에 넣고 도착지를 ep에 넣는다`() {
        val result = KakaoMapLauncher.buildRouteUri(course)

        assertEquals(
            "kakaomap://route?vp=37.5563,126.922&vp2=37.5497,126.914&vp3=37.5477,126.9229" +
                "&ep=37.5572,126.9254&by=car",
            result,
        )
    }

    @Test
    fun `코스 경유지는 목록 순서와 관계없이 순번대로 정렬한다`() {
        val shuffled = course.copy(course = courseDetail.copy(waypoints = courseDetail.waypoints.reversed()))

        val result = KakaoMapLauncher.buildRouteUri(shuffled)

        assertEquals(KakaoMapLauncher.buildRouteUri(course), result)
    }

    @Test
    fun `코스는 경유지를 처음 다섯 개까지만 넣는다`() {
        val waypoints = listOf(waypoint(PlaceWaypointType.START, 0, 37.0, 127.0)) +
            listOf(37.1, 37.2, 37.3, 37.4, 37.5, 37.6).mapIndexed { i, lat ->
                waypoint(PlaceWaypointType.VIA, i + 1, lat, 127.0)
            } +
            waypoint(PlaceWaypointType.DESTINATION, 7, 38.0, 128.0)
        val manyVia = course.copy(course = courseDetail.copy(waypoints = waypoints))

        val result = KakaoMapLauncher.buildRouteUri(manyVia)

        assertEquals(
            "kakaomap://route?vp=37.0,127.0&vp2=37.1,127.0&vp3=37.2,127.0&vp4=37.3,127.0" +
                "&vp5=37.4,127.0&ep=38.0,128.0&by=car",
            result,
        )
        assertFalse(result.contains("vp6="))
    }

    @Test
    fun `주차장은 자기 좌표만 도착지로 보낸다`() {
        val result = KakaoMapLauncher.buildRouteUri(HomePreviewData.parkingDetail)

        assertEquals("kakaomap://route?ep=37.5568,126.919&by=car", result)
    }

    @Test
    fun `주차장은 코스 경유지가 있어도 무시한다`() {
        val startAndVia = courseDetail.waypoints.filter { it.type != PlaceWaypointType.DESTINATION }
        val parking = HomePreviewData.parkingDetail.copy(course = courseDetail.copy(waypoints = startAndVia))

        val result = KakaoMapLauncher.buildRouteUri(parking)

        assertEquals("kakaomap://route?ep=37.5568,126.919&by=car", result)
    }

    @Test
    fun `경유지가 없는 코스는 장소 좌표를 도착지로 쓴다`() {
        val noWaypoints = course.copy(course = courseDetail.copy(waypoints = emptyList()))

        val result = KakaoMapLauncher.buildRouteUri(noWaypoints)

        assertEquals("kakaomap://route?ep=37.5563,126.922&by=car", result)
    }

    @Test
    fun `좌표 파라미터는 위도 다음 경도 순서다`() {
        val point = GeoPoint(lat = 35.1, lng = 129.2)
        val place = course.copy(point = point, course = null)

        val result = KakaoMapLauncher.buildRouteUri(place)

        assertTrue(result.contains("ep=35.1,129.2"))
        assertFalse(result.contains("129.2,35.1"))
    }

    @Test
    fun `현재 위치는 sp에 넣고 코스 출발지는 vp에 넣는다`() {
        val result = KakaoMapLauncher.buildRouteUri(course, origin = GeoPoint(lat = 37.5, lng = 127.0))

        assertEquals(
            "kakaomap://route?sp=37.5,127.0&vp=37.5563,126.922&vp2=37.5497,126.914&vp3=37.5477,126.9229" +
                "&ep=37.5572,126.9254&by=car",
            result,
        )
    }

    @Test
    fun `주차장은 현재 위치와 도착지만 보낸다`() {
        val result = KakaoMapLauncher.buildRouteUri(HomePreviewData.parkingDetail, origin = GeoPoint(lat = 37.5, lng = 127.0))

        assertEquals("kakaomap://route?sp=37.5,127.0&ep=37.5568,126.919&by=car", result)
    }

    private fun waypoint(type: PlaceWaypointType, sequence: Int, lat: Double, lng: Double) =
        PlaceWaypoint(type, sequence, GeoPoint(lat, lng), name = null)
}
