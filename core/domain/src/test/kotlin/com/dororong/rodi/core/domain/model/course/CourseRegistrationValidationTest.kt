package com.dororong.rodi.core.domain.model.course

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class CourseRegistrationValidationTest {
    @Test
    fun `순서가 맞는 실제 경로 요청은 검증을 통과한다`() {
        assertDoesNotThrow { validRequest().validateForSubmission() }
    }

    @Test
    fun `실제 경로가 없는 요청은 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            validRequest().copy(distanceMeters = 0).validateForSubmission()
        }
    }

    @Test
    fun `경유지 순서가 어긋나거나 연습 유형이 중복되면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            validRequest().copy(
                waypoints = validRequest().waypoints.reversed(),
            ).validateForSubmission()
        }
        assertThrows(IllegalArgumentException::class.java) {
            validRequest().copy(practiceTypes = listOf("STRAIGHT", "STRAIGHT")).validateForSubmission()
        }
    }

    private fun validRequest() = CourseRegistrationRequest(
        address = "서울특별시 강남구",
        distanceMeters = 8200,
        waypoints = listOf(
            RegistrationWaypoint(RegistrationWaypointType.START, "출발", "서울특별시 강남구", lat = 37.5, lng = 127.0),
            RegistrationWaypoint(RegistrationWaypointType.VIA, "경유", "서울특별시 강남구", lat = 37.51, lng = 127.01),
            RegistrationWaypoint(RegistrationWaypointType.DESTINATION, "도착", "서울특별시 강남구", lat = 37.52, lng = 127.02),
        ),
        practiceTypes = listOf("STRAIGHT", "INTERSECTION"),
        description = "직선 구간이 길어요.",
        caution = "차량을 주의하세요.",
    )
}
