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
    fun `course puts start and via points into vp keys in order and destination into ep`() {
        val result = KakaoMapLauncher.buildRouteUri(course)

        assertEquals(
            "kakaomap://route?vp=37.5563,126.922&vp2=37.5497,126.914&vp3=37.5477,126.9229" +
                "&ep=37.5572,126.9254&by=car",
            result,
        )
    }

    @Test
    fun `course orders waypoints by sequence regardless of list order`() {
        val shuffled = course.copy(course = courseDetail.copy(waypoints = courseDetail.waypoints.reversed()))

        val result = KakaoMapLauncher.buildRouteUri(shuffled)

        assertEquals(KakaoMapLauncher.buildRouteUri(course), result)
    }

    @Test
    fun `course keeps only the first five via points`() {
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
    fun `parking sends its point as the only destination`() {
        val result = KakaoMapLauncher.buildRouteUri(HomePreviewData.parkingDetail)

        assertEquals("kakaomap://route?ep=37.5568,126.919&by=car", result)
    }

    @Test
    fun `parking ignores via points even when course waypoints are present`() {
        val startAndVia = courseDetail.waypoints.filter { it.type != PlaceWaypointType.DESTINATION }
        val parking = HomePreviewData.parkingDetail.copy(course = courseDetail.copy(waypoints = startAndVia))

        val result = KakaoMapLauncher.buildRouteUri(parking)

        assertEquals("kakaomap://route?ep=37.5568,126.919&by=car", result)
    }

    @Test
    fun `course without waypoints uses place point as destination`() {
        val noWaypoints = course.copy(course = courseDetail.copy(waypoints = emptyList()))

        val result = KakaoMapLauncher.buildRouteUri(noWaypoints)

        assertEquals("kakaomap://route?ep=37.5563,126.922&by=car", result)
    }

    @Test
    fun `coordinate params are latitude first then longitude`() {
        val point = GeoPoint(lat = 35.1, lng = 129.2)
        val place = course.copy(point = point, course = null)

        val result = KakaoMapLauncher.buildRouteUri(place)

        assertTrue(result.contains("ep=35.1,129.2"))
        assertFalse(result.contains("129.2,35.1"))
    }

    private fun waypoint(type: PlaceWaypointType, sequence: Int, lat: Double, lng: Double) =
        PlaceWaypoint(type, sequence, GeoPoint(lat, lng), name = null)
}
