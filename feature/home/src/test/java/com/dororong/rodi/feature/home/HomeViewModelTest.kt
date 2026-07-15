package com.dororong.rodi.feature.home

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.course.RodiItemType
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.navi.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.navi.SetNaviAlwaysUseCase
import com.dororong.rodi.feature.home.map.MapViewport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun `marks location permission request`() = runTest(testDispatcher) {
        val markLocationPermissionRequested = mockk<MarkLocationPermissionRequestedUseCase>(relaxed = true)
        val viewModel = createViewModel(
            markLocationPermissionRequested = markLocationPermissionRequested,
        )

        viewModel.markLocationPermissionRequested()
        advanceUntilIdle()

        coVerify(exactly = 1) { markLocationPermissionRequested() }
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
    fun `settings click emits navigation effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onIntent(HomeIntent.OnSettingsClick)
            advanceUntilIdle()

            assertEquals(HomeEffect.NavigateSettings, awaitItem())
        }
    }

    @Test
    fun `map search updates courses to the requested viewport`() {
        val nearbyCourse = testCourse(id = 1)
        val distantCourse = testCourse(id = 2).copy(
            waypoints = testWaypoints().mapIndexed { index, waypoint ->
                if (index == 0) waypoint.copy(lat = 35.1796, lng = 129.0756) else waypoint
            },
        )
        val viewModel = createViewModel(courses = listOf(nearbyCourse, distantCourse))

        viewModel.onIntent(
            HomeIntent.OnMapSearch(
                MapViewport(
                    northEast = GeoPoint(37.7, 127.1),
                    southWest = GeoPoint(37.4, 126.8),
                ),
            ),
        )

        assertEquals(listOf(nearbyCourse), viewModel.state.value.courses)
    }

    @Test
    fun `onNavigateClick launches saved KakaoMap when installed`() = runTest(testDispatcher) {
        val course = testCourse()
        val getNaviAlwaysUseCase = mockk<GetNaviAlwaysUseCase>()
        coEvery { getNaviAlwaysUseCase() } returns NaviApp.KAKAOMAP
        val viewModel = createViewModel(courses = listOf(course), getNaviAlwaysUseCase = getNaviAlwaysUseCase)

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
        val getNaviAlwaysUseCase = mockk<GetNaviAlwaysUseCase>()
        coEvery { getNaviAlwaysUseCase() } returns null
        val viewModel = createViewModel(courses = listOf(course), getNaviAlwaysUseCase = getNaviAlwaysUseCase)

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
        val getNaviAlwaysUseCase = mockk<GetNaviAlwaysUseCase>()
        coEvery { getNaviAlwaysUseCase() } returns null
        val viewModel = createViewModel(courses = listOf(course), getNaviAlwaysUseCase = getNaviAlwaysUseCase)

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
        val setNaviAlwaysUseCase = mockk<SetNaviAlwaysUseCase>()
        coEvery { setNaviAlwaysUseCase(NaviApp.KAKAONAVI) } returns Unit
        val viewModel = createViewModel(courses = listOf(course), setNaviAlwaysUseCase = setNaviAlwaysUseCase)

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
            coVerify(exactly = 1) { setNaviAlwaysUseCase(NaviApp.KAKAONAVI) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        courses: List<Course> = listOf(testCourse()),
        getRouteUseCase: GetRouteUseCase = mockk(),
        getNaviAlwaysUseCase: GetNaviAlwaysUseCase = mockk(),
        setNaviAlwaysUseCase: SetNaviAlwaysUseCase = mockk(),
        getLocationPermissionRequested: GetLocationPermissionRequestedUseCase = mockk(),
        markLocationPermissionRequested: MarkLocationPermissionRequestedUseCase = mockk(relaxed = true),
    ): HomeViewModel {
        val getCoursesUseCase = mockk<GetCoursesUseCase>()
        every { getCoursesUseCase() } returns courses
        every { getLocationPermissionRequested() } returns flowOf(false)
        return HomeViewModel(
            getCoursesUseCase = getCoursesUseCase,
            getRouteUseCase = getRouteUseCase,
            getNaviAlwaysUseCase = getNaviAlwaysUseCase,
            setNaviAlwaysUseCase = setNaviAlwaysUseCase,
            getLocationPermissionRequested = getLocationPermissionRequested,
            markLocationPermissionRequestedUseCase = markLocationPermissionRequested,
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
