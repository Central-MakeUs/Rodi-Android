package com.cmc.routi.directions

import android.util.Log
import com.cmc.routi.BuildConfig
import com.cmc.routi.model.Course
import com.cmc.routi.model.CoursePoint
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 카카오모빌리티 [자동차 길찾기 REST API](https://developers.kakaomobility.com/docs/navi-api/directions/)
 * 를 호출해 출발→경유→목적지의 **실제 도로 경로 좌표**를 가져온다.
 *
 * REST API 키가 없거나 호출이 실패하면 출발·경유·목적지를 잇는 **직선**으로 폴백한다.
 * (지도에 항상 무언가는 그려지도록)
 */
object KakaoDirectionsClient {

    private const val TAG = "KakaoDirections"
    private const val BASE_URL = "https://apis-navi.kakaomobility.com/v1/directions"
    private const val MAX_API_WAYPOINTS = 4

    /**
     * @param points              전체 폴리라인 좌표 (지도 렌더용)
     * @param isRealRoute         true=도로 경로, false=직선 폴백
     * @param totalDistanceMeters 전체 주행 거리(미터)
     * @param snappedPoints       각 입력 지점이 실제로 스냅된 도로 위 좌표
     */
    data class RouteResult(
        val points: List<LatLng>,
        val isRealRoute: Boolean,
        val totalDistanceMeters: Int = 0,
        val snappedPoints: List<LatLng> = emptyList(),
    )

    suspend fun getRoute(course: Course): RouteResult =
        getRoute(course.origin, course.waypointPoints, course.destination)

    suspend fun getRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult = withContext(Dispatchers.IO) {
        val straightPts = (listOf(origin) + waypoints + destination).map { LatLng.from(it.lat, it.lng) }
        fun straightFallback() = RouteResult(
            points = straightPts,
            isRealRoute = false,
            totalDistanceMeters = straightPts.zipWithNext().sumOf { (a, b) -> haversineMeters(a, b) }.roundToInt(),
        )

        val restKey = BuildConfig.KAKAO_REST_API_KEY
        if (restKey.isBlank()) {
            Log.w(TAG, "REST API 키가 없어 직선으로 폴백합니다.")
            return@withContext straightFallback()
        }
        if (waypoints.size > MAX_API_WAYPOINTS) {
            Log.w(TAG, "경유지가 API 제한을 초과해 직선으로 폴백합니다.")
            return@withContext straightFallback()
        }
        runCatching { requestDirections(origin, waypoints, destination, restKey) }
            .getOrElse {
                Log.w(TAG, "길찾기 실패, 직선으로 폴백: ${it.message}")
                straightFallback()
            }
    }

    private fun requestDirections(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
        restKey: String,
    ): RouteResult {
        val url = buildUrl(origin, waypoints, destination)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "KakaoAK $restKey")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) error("HTTP $code")

            val parsed = parseRouteData(body)
            if (parsed.points.isEmpty()) error("경로 좌표가 비어 있음")
            return parsed
        } finally {
            conn.disconnect()
        }
    }

    private fun buildUrl(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): String {
        fun CoursePoint.param() = "$lng,$lat" // origin/destination 은 "경도,위도"
        val params = buildList {
            add("origin=${origin.param().enc()}")
            add("destination=${destination.param().enc()}")
            if (waypoints.isNotEmpty()) {
                val way = waypoints.joinToString("|") { it.param() }
                add("waypoints=${way.enc()}")
            }
            add("priority=RECOMMEND")
            add("car_fuel=GASOLINE")
        }
        return "$BASE_URL?${params.joinToString("&")}"
    }

    /**
     * routes[0].sections[] 을 파싱해 전체 폴리라인 좌표 + 각 구간 스냅 좌표 + 총 거리 추출.
     */
    private fun parseRouteData(body: String): RouteResult {
        val routes = JSONObject(body).optJSONArray("routes") ?: return RouteResult(emptyList(), false)
        if (routes.length() == 0) return RouteResult(emptyList(), false)
        val route = routes.getJSONObject(0)
        val resultCode = route.optInt("result_code", -1)
        if (resultCode != 0) error("result_code=$resultCode ${route.optString("result_msg")}")

        val allPoints = mutableListOf<LatLng>()
        val snappedPoints = mutableListOf<LatLng>()
        var totalDistance = 0

        val sections = route.optJSONArray("sections") ?: return RouteResult(emptyList(), false)
        for (s in 0 until sections.length()) {
            val section = sections.getJSONObject(s)
            totalDistance += section.optInt("distance", 0)
            val roads = section.optJSONArray("roads") ?: continue

            var sectionStart: LatLng? = null
            var sectionEnd: LatLng? = null
            for (r in 0 until roads.length()) {
                val vertexes = roads.getJSONObject(r).optJSONArray("vertexes") ?: continue
                var i = 0
                while (i + 1 < vertexes.length()) {
                    val lng = vertexes.getDouble(i)
                    val lat = vertexes.getDouble(i + 1)
                    val pt = LatLng.from(lat, lng)
                    allPoints.add(pt)
                    if (sectionStart == null) sectionStart = pt
                    sectionEnd = pt
                    i += 2
                }
            }
            if (sectionStart != null && sectionEnd != null) {
                // 첫 구간만 시작점 추가, 이후엔 끝점만 (이전 구간 끝 == 이번 구간 시작)
                if (s == 0) snappedPoints.add(sectionStart)
                snappedPoints.add(sectionEnd)
            }
        }

        return RouteResult(
            points = allPoints,
            isRealRoute = true,
            totalDistanceMeters = totalDistance,
            snappedPoints = snappedPoints,
        )
    }

    /** 두 좌표 간 직선 거리(미터). Haversine 공식. */
    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val x = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        return 2 * r * asin(sqrt(x))
    }

    private fun String.enc(): String = URLEncoder.encode(this, "UTF-8")
}
