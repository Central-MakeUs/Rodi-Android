package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.course.CourseInputSpecResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationInputsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationSectionsResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeCategoryResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeItemResponse
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CourseRegistrationMapperTest {
    @Test
    fun `uses the provided course name when present`() {
        val data = registrationRequest(name = "우리 동네 연습 코스").toData()

        assertEquals("우리 동네 연습 코스", data.name)
    }

    @Test
    fun `falls back to the start waypoint name when course name is blank`() {
        val data = registrationRequest(name = "  ").toData()

        assertEquals("출발", data.name)
    }

    @Test
    fun `sorts dynamic registration categories and practice types by server order`() {
        val result = form().toDomain()

        assertEquals(listOf("basic", "advanced"), result.categories.map { it.code })
        assertEquals(listOf("STRAIGHT", "INTERSECTION"), result.categories.first().practiceTypes.map { it.code })
        assertEquals(3, result.practiceTypeMaxSelect)
        assertEquals(10, result.descriptionInput.minLength)
    }

    @Test
    fun `maps approval status strictly`() {
        assertEquals(CourseApprovalStatus.PENDING, "PENDING".toApprovalStatus())
        assertThrows(IllegalStateException::class.java) { "UNKNOWN".toApprovalStatus() }
    }

    private fun form() = CourseRegistrationFormResponse(
        maxWaypoints = 5,
        sections = CourseRegistrationSectionsResponse("기본정보", "카테고리", "연습유형", "주의", "소개"),
        practiceType = PracticeTypeFormResponse(
            maxSelect = 3,
            maxSelectExceededMessage = "최대 3개까지 선택할 수 있어요.",
            categories = listOf(
                PracticeTypeCategoryResponse(
                    code = "advanced",
                    label = "고급",
                    order = 2,
                    practiceTypes = listOf(PracticeTypeItemResponse("INTERSECTION", "교차로", 2)),
                ),
                PracticeTypeCategoryResponse(
                    code = "basic",
                    label = "기초",
                    order = 1,
                    practiceTypes = listOf(
                        PracticeTypeItemResponse("INTERSECTION", "교차로", 2),
                        PracticeTypeItemResponse("STRAIGHT", "직선", 1),
                    ),
                ),
            ),
        ),
        inputs = CourseRegistrationInputsResponse(
            caution = CourseInputSpecResponse(false, null, 100, "주의사항"),
            description = CourseInputSpecResponse(true, 10, 30, "한줄 소개"),
        ),
    )

    private fun registrationRequest(name: String?) = CourseRegistrationRequest(
        address = "서울특별시 강남구",
        distanceMeters = 1_000,
        waypoints = listOf(
            RegistrationWaypoint(RegistrationWaypointType.START, "출발", "서울특별시 강남구", lat = 37.5, lng = 127.0),
            RegistrationWaypoint(RegistrationWaypointType.DESTINATION, "도착", "서울특별시 강남구", lat = 37.51, lng = 127.01),
        ),
        practiceTypes = listOf("STRAIGHT"),
        description = "소개",
        caution = "주의",
        name = name,
    )
}
