package com.dororong.rodi.core.data.map

import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.MapViewportQuery
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSyntheticMapCourseDataSource @Inject constructor() : MapCourseDataSource {
    private val courses: List<Course> by lazy(::createCourses)

    override suspend fun getCourses(query: MapViewportQuery): List<Course> {
        val northEast = query.northEast
        val southWest = query.southWest
        return courses.filter { course ->
            val start = course.startWaypoint
            start.lat in southWest.lat..northEast.lat &&
                    start.lng in southWest.lng..northEast.lng
        }
    }

    internal fun allCourses(): List<Course> = courses

    private fun createCourses(): List<Course> {
        val template = SampleCourses.RODI_COURSES.first { !it.isParking }
        return REGION_CENTERS.flatMapIndexed { regionIndex, center ->
            val count = if (center.region == "seoul") SEOUL_COURSE_COUNT else REGION_COURSE_COUNT
            val columns = if (center.region == "seoul") 12 else 4
            val rows = count / columns
            List(count) { index ->
                val column = index % columns
                val row = index / columns
                val lat = center.lat + (row - (rows - 1) / 2.0) * center.spacing
                val lng = center.lng + (column - (columns - 1) / 2.0) * center.spacing
                template.reposition(
                    id = SYNTHETIC_ID_START + regionIndex * 100 + index,
                    name = "${center.areaName} 검증 코스 ${index + 1}",
                    nickname = "${center.areaName} ${index + 1}",
                    region = center.region,
                    areaName = center.areaName,
                    lat = lat,
                    lng = lng,
                )
            }
        }
    }

    private fun Course.reposition(
        id: Int,
        name: String,
        nickname: String,
        region: String,
        areaName: String,
        lat: Double,
        lng: Double,
    ): Course {
        val origin = startWaypoint
        val latOffset = lat - origin.lat
        val lngOffset = lng - origin.lng
        return copy(
            id = id,
            courseName = name,
            courseNickname = nickname,
            region = region,
            areaName = areaName,
            source = "local-clustering-spike",
            sourceUrl = "",
            waypoints = waypoints.map { point ->
                point.copy(
                    name = if (point.type == origin.type) "$nickname 출발" else point.name,
                    lat = point.lat + latOffset,
                    lng = point.lng + lngOffset,
                    address = "$areaName 합성 데이터",
                )
            },
        )
    }

    private data class RegionCenter(
        val region: String,
        val areaName: String,
        val lat: Double,
        val lng: Double,
        val spacing: Double,
    )

    private companion object {
        const val SYNTHETIC_ID_START = 10_000
        const val SEOUL_COURSE_COUNT = 108
        const val REGION_COURSE_COUNT = 12

        val REGION_CENTERS = listOf(
            RegionCenter("seoul", "서울", 37.5563, 126.9220, 0.009),
            RegionCenter("busan", "부산", 35.1796, 129.0756, 0.025),
            RegionCenter("daegu", "대구", 35.8714, 128.6014, 0.025),
            RegionCenter("incheon", "인천", 37.4563, 126.7052, 0.025),
            RegionCenter("gwangju", "광주", 35.1595, 126.8526, 0.025),
            RegionCenter("daejeon", "대전", 36.3504, 127.3845, 0.025),
            RegionCenter("ulsan", "울산", 35.5384, 129.3114, 0.025),
            RegionCenter("sejong", "세종", 36.4800, 127.2890, 0.025),
            RegionCenter("gyeonggi", "수원", 37.2636, 127.0286, 0.025),
            RegionCenter("gangwon", "춘천", 37.8813, 127.7298, 0.025),
            RegionCenter("chungnam", "홍성", 36.6010, 126.6600, 0.025),
            RegionCenter("chungbuk", "청주", 36.6424, 127.4890, 0.025),
            RegionCenter("jeonnam", "무안", 34.9904, 126.4817, 0.025),
            RegionCenter("jeonbuk", "전주", 35.8242, 127.1480, 0.025),
            RegionCenter("gyeongnam", "창원", 35.2279, 128.6811, 0.025),
            RegionCenter("gyeongbuk", "안동", 36.5684, 128.7294, 0.025),
            RegionCenter("jeju", "제주", 33.4996, 126.5312, 0.025),
        )
    }
}
