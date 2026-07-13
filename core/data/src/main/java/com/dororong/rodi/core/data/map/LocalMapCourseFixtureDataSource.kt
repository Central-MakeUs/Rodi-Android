package com.dororong.rodi.core.data.map

import com.dororong.rodi.core.data.SampleCourses
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.MapViewportQuery
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMapCourseFixtureDataSource @Inject constructor() : MapCourseDataSource {
    private val courses by lazy { SampleCourses.RODI_COURSES + createNQueenCourses() }

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

    private fun createNQueenCourses(): List<Course> = N_QUEEN_BOARD_ORIGINS.flatMapIndexed { boardIndex, origin ->
        N_QUEEN_COLUMNS.mapIndexed { row, column ->
            val template = SampleCourses.RODI_COURSES[(boardIndex * N_QUEEN_COLUMNS.size + row) % SampleCourses.RODI_COURSES.size]
            val isParking = template.isParking
            template.reposition(
                id = N_QUEEN_ID_START + boardIndex * N_QUEEN_COLUMNS.size + row,
                name = "N-퀸 검증 ${if (isParking) "주차장" else "코스"} ${boardIndex + 1}-${row + 1}",
                nickname = "N-퀸 ${boardIndex + 1}-${row + 1}",
                lat = origin.lat + row * BOARD_LAT_STEP,
                lng = origin.lng + column * BOARD_LNG_STEP,
            )
        }
    }

    private fun Course.reposition(
        id: Int,
        name: String,
        nickname: String,
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
            region = "seoul",
            areaName = "N-퀸 검증 구역",
            source = "local-n-queens-spike",
            sourceUrl = "",
            waypoints = waypoints.map { point ->
                point.copy(
                    name = if (point.type == origin.type) "$nickname 출발" else point.name,
                    lat = point.lat + latOffset,
                    lng = point.lng + lngOffset,
                    address = "N-퀸 검증 구역",
                )
            },
        )
    }

    private data class BoardOrigin(val lat: Double, val lng: Double)

    private companion object {
        const val N_QUEEN_ID_START = 20_000
        const val BOARD_LAT_STEP = 0.006
        const val BOARD_LNG_STEP = 0.008

        val N_QUEEN_COLUMNS = listOf(0, 4, 7, 5, 2, 6, 1, 3)
        val N_QUEEN_BOARD_ORIGINS = listOf(
            BoardOrigin(37.455, 126.835),
            BoardOrigin(37.505, 126.910),
            BoardOrigin(37.555, 126.975),
            BoardOrigin(37.605, 127.035),
            BoardOrigin(37.495, 127.105),
        )
    }
}
