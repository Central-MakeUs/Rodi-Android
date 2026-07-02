package com.dororong.rodi.feature.home

import app.cash.turbine.test
import com.dororong.rodi.core.data.navi.NaviApp
import com.dororong.rodi.core.data.navi.NaviPreferenceRepository
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseFeatures
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.RodiItemType
import com.dororong.rodi.core.domain.RouteResult
import com.dororong.rodi.core.domain.Waypoint
import com.dororong.rodi.core.domain.WaypointType
import com.dororong.rodi.core.domain.usecase.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.GetRouteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state contains courses from use case`() {
        val courses = listOf(testCourse())
        val viewModel = createViewModel(courses = courses)

        assertEquals(courses, viewModel.state.value.courses)
    }

    @Test
    fun `onCourseClick selects course and stores route on success`() = runTest(testDispatcher) {
        val course = testCourse()
        val routeResult = testRouteResult()
        val getRouteUseCase = mockk<GetRouteUseCase>()
        coEvery { getRouteUseCase(course) } returns Result.success(routeResult)
        val viewModel = createViewModel(courses = listOf(course), getRouteUseCase = getRouteUseCase)

        viewModel.state.test {
            assertEquals(null, awaitItem().selectedCourseId)

            viewModel.onIntent(HomeIntent.OnCourseClick(course.id))

            assertEquals(course.id, awaitItem().selectedCourseId)
            assertTrue(awaitItem().isRouting)
            advanceUntilIdle()
            val routedState = awaitItem()
            assertEquals(routeResult, routedState.routeByCourse[course.id])
            assertFalse(routedState.isRouting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCourseClick ignores already selected course`() = runTest(testDispatcher) {
        val course = testCourse()
        val getRouteUseCase = mockk<GetRouteUseCase>()
        coEvery { getRouteUseCase(course) } returns Result.success(testRouteResult())
        val viewModel = createViewModel(courses = listOf(course), getRouteUseCase = getRouteUseCase)

        viewModel.onIntent(HomeIntent.OnCourseClick(course.id))
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.OnCourseClick(course.id))
        advanceUntilIdle()

        coVerify(exactly = 1) { getRouteUseCase(course) }
    }

    @Test
    fun `onCourseClick skips route request for parking item`() = runTest(testDispatcher) {
        val course = testCourse(itemType = RodiItemType.PARKING)
        val getRouteUseCase = mockk<GetRouteUseCase>()
        val viewModel = createViewModel(courses = listOf(course), getRouteUseCase = getRouteUseCase)

        viewModel.onIntent(HomeIntent.OnCourseClick(course.id))
        advanceUntilIdle()

        assertEquals(course.id, viewModel.state.value.selectedCourseId)
        coVerify(exactly = 0) { getRouteUseCase(any()) }
    }

    @Test
    fun `onCourseClick removes routing state when route request fails`() = runTest(testDispatcher) {
        val course = testCourse()
        val getRouteUseCase = mockk<GetRouteUseCase>()
        coEvery { getRouteUseCase(course) } returns Result.failure(RuntimeException("boom"))
        val viewModel = createViewModel(courses = listOf(course), getRouteUseCase = getRouteUseCase)

        viewModel.state.test {
            awaitItem()

            viewModel.onIntent(HomeIntent.OnCourseClick(course.id))

            awaitItem()
            assertTrue(awaitItem().isRouting)
            advanceUntilIdle()
            val failedState = awaitItem()
            assertFalse(failedState.isRouting)
            assertTrue(failedState.routeByCourse.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNavigateClick launches saved KakaoMap when installed`() = runTest(testDispatcher) {
        val course = testCourse()
        val naviPreferenceRepository = mockk<NaviPreferenceRepository>()
        every { naviPreferenceRepository.getAlways() } returns NaviApp.KAKAOMAP
        val viewModel = createViewModel(courses = listOf(course), naviPreferenceRepository = naviPreferenceRepository)

        viewModel.effect.test {
            viewModel.onIntent(
                HomeIntent.OnNavigateClick(
                    course = course,
                    kakaoMapInstalled = true,
                    kakaoNaviInstalled = true,
                ),
            )
            advanceUntilIdle()

            assertEquals(HomeEffect.LaunchKakaoMap(course), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNavigateClick shows picker when both apps installed and no preference exists`() = runTest(testDispatcher) {
        val course = testCourse()
        val naviPreferenceRepository = mockk<NaviPreferenceRepository>()
        every { naviPreferenceRepository.getAlways() } returns null
        val viewModel = createViewModel(courses = listOf(course), naviPreferenceRepository = naviPreferenceRepository)

        viewModel.effect.test {
            viewModel.onIntent(
                HomeIntent.OnNavigateClick(
                    course = course,
                    kakaoMapInstalled = true,
                    kakaoNaviInstalled = true,
                ),
            )
            advanceUntilIdle()

            assertEquals(HomeEffect.ShowNaviPicker(course), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNavigateClick shows install picker when no app is installed`() = runTest(testDispatcher) {
        val course = testCourse()
        val naviPreferenceRepository = mockk<NaviPreferenceRepository>()
        every { naviPreferenceRepository.getAlways() } returns null
        val viewModel = createViewModel(courses = listOf(course), naviPreferenceRepository = naviPreferenceRepository)

        viewModel.effect.test {
            viewModel.onIntent(
                HomeIntent.OnNavigateClick(
                    course = course,
                    kakaoMapInstalled = false,
                    kakaoNaviInstalled = false,
                ),
            )
            advanceUntilIdle()

            assertEquals(HomeEffect.ShowInstallNaviPicker(course), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNaviAppSelected stores always preference and launches selected app`() = runTest(testDispatcher) {
        val course = testCourse()
        val naviPreferenceRepository = mockk<NaviPreferenceRepository>()
        every { naviPreferenceRepository.setAlways(NaviApp.KAKAONAVI) } returns Unit
        val viewModel = createViewModel(courses = listOf(course), naviPreferenceRepository = naviPreferenceRepository)

        viewModel.effect.test {
            viewModel.onIntent(
                HomeIntent.OnNaviAppSelected(
                    app = NaviApp.KAKAONAVI,
                    course = course,
                    always = true,
                ),
            )
            advanceUntilIdle()

            assertEquals(HomeEffect.LaunchKakaoNavi(course), awaitItem())
            verify(exactly = 1) { naviPreferenceRepository.setAlways(NaviApp.KAKAONAVI) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        courses: List<Course> = listOf(testCourse()),
        getRouteUseCase: GetRouteUseCase = mockk(),
        naviPreferenceRepository: NaviPreferenceRepository = mockk(),
    ): HomeViewModel {
        val getCoursesUseCase = mockk<GetCoursesUseCase>()
        every { getCoursesUseCase() } returns courses
        return HomeViewModel(
            getCoursesUseCase = getCoursesUseCase,
            getRouteUseCase = getRouteUseCase,
            naviPreferenceRepository = naviPreferenceRepository,
        )
    }
}

private fun testCourse(id: Int = 1, itemType: RodiItemType = RodiItemType.COURSE) = Course(
    id = id,
    courseName = "테스트 코스",
    courseNickname = "테스트",
    areaName = "테스트동",
    region = "seoul",
    difficulty = 1,
    trafficDensity = null,
    source = "test",
    sourceUrl = "",
    crawledAt = "",
    waypoints = testWaypoints(),
    features = CourseFeatures(),
    recommendation = 1,
    caution = "",
    bestTime = "",
    enrichedDescription = "",
    itemType = itemType,
)

private fun testWaypoints() = listOf(
    Waypoint(
        order = 0,
        type = WaypointType.START,
        name = "출발",
        lat = 37.5665,
        lng = 126.9780,
        address = "서울",
        category = "test",
    ),
    Waypoint(
        order = 1,
        type = WaypointType.END,
        name = "도착",
        lat = 37.5651,
        lng = 126.9895,
        address = "서울",
        category = "test",
    ),
)

private fun testRouteResult() = RouteResult(
    points = listOf(GeoPoint(37.5665, 126.9780), GeoPoint(37.5651, 126.9895)),
    isRealRoute = true,
)
