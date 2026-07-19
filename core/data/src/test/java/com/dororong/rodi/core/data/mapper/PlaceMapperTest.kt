package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.place.CourseDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.WaypointResponse
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.model.place.PracticeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceMapperTest {
    @Test
    fun `detail mapper keeps long id and sorts waypoints`() {
        val result = PlaceDetailResponse(
            id = 4_294_967_296,
            type = "COURSE",
            name = "한강 코스",
            address = "서울 마포구",
            lat = 37.5,
            lng = 126.9,
            practiceTypes = listOf("STRAIGHT", "FUTURE_TYPE"),
            course = CourseDetailResponse(
                distanceMeters = 3200,
                waypoints = listOf(
                    WaypointResponse("DESTINATION", 2, 37.6, 127.0),
                    WaypointResponse("START", 0, 37.5, 126.9),
                    WaypointResponse("VIA", 1, 37.55, 126.95),
                ),
            ),
        ).toDomain()

        assertEquals(4_294_967_296, result.id)
        assertEquals(PlaceType.COURSE, result.type)
        assertEquals(listOf(PracticeType.STRAIGHT), result.practiceTypes)
        assertEquals(
            listOf(PlaceWaypointType.START, PlaceWaypointType.VIA, PlaceWaypointType.DESTINATION),
            result.course?.waypoints?.map { it.type },
        )
    }
}
