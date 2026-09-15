package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.api.CourseApi
import com.dororong.rodi.core.data.source.remote.model.course.CoursePageResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterRequest
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationInputsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationSectionsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseInputSpecResponse
import com.dororong.rodi.core.data.source.remote.model.course.MyCourseItemResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeCategoryResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeFormResponse
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseRegistrationRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `refreshes once when registration form receives unauthorized response`() = runTest {
        val api = mockk<CourseApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(
            AuthTokens("access-old", "refresh", "kakao"),
            AuthTokens("access-new", "refresh-new", "kakao"),
        )
        coEvery { api.getRegistrationForm("Bearer access-old") } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_1",
            message = "만료된 토큰입니다.",
        )
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { api.getRegistrationForm("Bearer access-new") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = formResponse(),
        )
        val repository = CourseRegistrationRepositoryImpl(api, tokenStore, authRepository, json)

        assertEquals(4, repository.getRegistrationForm().maxWaypoints)

        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 1) { api.getRegistrationForm("Bearer access-new") }
    }

    @Test
    fun `registers with bearer token and maps response`() = runTest {
        val api = mockk<CourseApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.registerCourse("Bearer access", any()) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CourseRegisterResponse(42, "PENDING"),
        )
        val repository = CourseRegistrationRepositoryImpl(api, tokenStore, authRepository, json)

        val result = repository.registerCourse(request())

        assertEquals(42L, result.courseId)
        assertEquals(CourseApprovalStatus.PENDING, result.approvalStatus)
        coVerify { api.registerCourse("Bearer access", match { it.waypoints.size == 2 }) }
    }

    @Test
    fun `sends the start waypoint name as course name when none is provided`() = runTest {
        val api = mockk<CourseApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.registerCourse("Bearer access", any()) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CourseRegisterResponse(42, "PENDING"),
        )
        val repository = CourseRegistrationRepositoryImpl(api, tokenStore, authRepository, json)

        repository.registerCourse(request())

        coVerify { api.registerCourse("Bearer access", match { it.name == "출발" }) }
    }

    @Test
    fun `clamps list size and sends selected status and cursor`() = runTest {
        val api = mockk<CourseApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getMyCourses("Bearer access", "REJECTED", 100, "next") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CoursePageResponse(
                items = listOf(MyCourseItemResponse(1, "코스", "REJECTED", "2026-08-15T00:00:00Z")),
                hasNext = false,
                nextCursor = null,
                totalCount = 1,
            ),
        )
        val repository = CourseRegistrationRepositoryImpl(api, tokenStore, authRepository, json)

        val page = repository.getMyCourses(CourseApprovalStatus.REJECTED, "next", 101)

        assertEquals(1, page.items.size)
        assertTrue(page.items.single().approvalStatus == CourseApprovalStatus.REJECTED)
        coVerify { api.getMyCourses("Bearer access", "REJECTED", 100, "next") }
    }

    private fun request() = CourseRegistrationRequest(
        address = "서울특별시 강남구",
        distanceMeters = 1_000,
        waypoints = listOf(
            RegistrationWaypoint(RegistrationWaypointType.START, "출발", "서울특별시 강남구", lat = 37.5, lng = 127.0),
            RegistrationWaypoint(RegistrationWaypointType.DESTINATION, "도착", "서울특별시 강남구", lat = 37.51, lng = 127.01),
        ),
        practiceTypes = listOf("STRAIGHT"),
        description = "소개",
        caution = "주의",
    )

    private fun formResponse() = CourseRegistrationFormResponse(
        maxWaypoints = 4,
        sections = CourseRegistrationSectionsResponse("기본", "카테고리", "유형", "주의", "소개"),
        practiceType = PracticeTypeFormResponse(
            maxSelect = 2,
            maxSelectExceededMessage = "최대 2개",
            categories = listOf(
                PracticeTypeCategoryResponse("basic", "기본", 1, emptyList()),
            ),
        ),
        inputs = CourseRegistrationInputsResponse(
            caution = CourseInputSpecResponse(false, null, 100, "주의"),
            description = CourseInputSpecResponse(true, 1, 100, "소개"),
        ),
    )
}
