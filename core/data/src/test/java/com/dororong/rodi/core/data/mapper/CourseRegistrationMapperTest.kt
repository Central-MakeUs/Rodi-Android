package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.course.CourseInputSpecResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationInputsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationSectionsResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeCategoryResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeItemResponse
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CourseRegistrationMapperTest {
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
}
