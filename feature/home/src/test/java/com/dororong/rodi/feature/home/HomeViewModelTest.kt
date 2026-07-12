package com.dororong.rodi.feature.home

import app.cash.turbine.test
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseFeatures
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.RodiItemType
import com.dororong.rodi.core.domain.RouteResult
import com.dororong.rodi.core.domain.Waypoint
import com.dororong.rodi.core.domain.WaypointType
import com.dororong.rodi.core.domain.usecase.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.GetMapCoursesUseCase
import com.dororong.rodi.core.domain.usecase.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.SetNaviAlwaysUseCase
import com.dororong.rodi.feature.home.map.NationalGrid
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
    fun `viewport query replaces courses after debounce`() = runTest(testDispatcher) {
        val mapCourse = testCourse(id = 2)
        val query = testViewportQuery()
        val getMapCoursesUseCase = mockk<GetMapCoursesUseCase>()
        coEvery { getMapCoursesUseCase(query) } returns Result.success(listOf(mapCourse))
        val viewModel = createViewModel(getMapCoursesUseCase = getMapCoursesUseCase)

        viewModel.onIntent(HomeIntent.OnViewportChanged(query))
        advanceTimeBy(299)
        runCurrent()
        assertEquals(listOf(testCourse()), viewModel.state.value.courses)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf(mapCourse), viewModel.state.value.courses)
        assertFalse(viewModel.state.value.isLoadingMapCourses)
    }

    @Test
    fun `national grid request caches fixed national courses once`() = runTest(testDispatcher) {
        val nationalCourses = listOf(testCourse(id = 2), testCourse(id = 3))
        val getMapCoursesUseCase = mockk<GetMapCoursesUseCase>()
        coEvery { getMapCoursesUseCase(NationalGrid.query) } returns Result.success(nationalCourses)
        val viewModel = createViewModel(getMapCoursesUseCase = getMapCoursesUseCase)

        viewModel.onIntent(HomeIntent.OnNationalCoursesRequested)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.OnNationalCoursesRequested)
        advanceUntilIdle()

        assertEquals(nationalCourses, viewModel.state.value.nationalCourses)
        assertFalse(viewModel.state.value.isLoadingNationalCourses)
        coVerify(exactly = 1) { getMapCoursesUseCase(NationalGrid.query) }
    }

    @Test
    fun `identical viewport query is requested once`() = runTest(testDispatcher) {
        val query = testViewportQuery()
        val getMapCoursesUseCase = mockk<GetMapCoursesUseCase>()
        coEvery { getMapCoursesUseCase(query) } returns Result.success(emptyList())
        val viewModel = createViewModel(getMapCoursesUseCase = getMapCoursesUseCase)

        viewModel.onIntent(HomeIntent.OnViewportChanged(query))
        viewModel.onIntent(HomeIntent.OnViewportChanged(query))
        advanceUntilIdle()

        coVerify(exactly = 1) { getMapCoursesUseCase(query) }
    }

    @Test
    fun `viewport failure keeps last courses`() = runTest(testDispatcher) {
        val courses = listOf(testCourse())
        val query = testViewportQuery()
        val getMapCoursesUseCase = mockk<GetMapCoursesUseCase>()
        coEvery { getMapCoursesUseCase(query) } returns Result.failure(IllegalStateException("failed"))
        val viewModel = createViewModel(courses = courses, getMapCoursesUseCase = getMapCoursesUseCase)

        viewModel.onIntent(HomeIntent.OnViewportChanged(query))
        advanceUntilIdle()

        assertEquals(courses, viewModel.state.value.courses)
        assertTrue(viewModel.state.value.mapCourseLoadFailed)
    }

    @Test
    fun `new viewport cancels previous load and keeps latest courses`() = runTest(testDispatcher) {
        val firstQuery = testViewportQuery().copy(zoomLevel = 10)
        val latestQuery = testViewportQuery().copy(zoomLevel = 11)
        val staleCourse = testCourse(id = 2)
        val latestCourse = testCourse(id = 3)
        val getMapCoursesUseCase = mockk<GetMapCoursesUseCase>()
        coEvery { getMapCoursesUseCase(firstQuery) } coAnswers {
            delay(1_000)
            Result.success(listOf(staleCourse))
        }
        coEvery { getMapCoursesUseCase(latestQuery) } returns Result.success(listOf(latestCourse))
        val viewModel = createViewModel(getMapCoursesUseCase = getMapCoursesUseCase)

        viewModel.onIntent(HomeIntent.OnViewportChanged(firstQuery))
        advanceTimeBy(300)
        runCurrent()
        viewModel.onIntent(HomeIntent.OnViewportChanged(latestQuery))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(listOf(latestCourse), viewModel.state.value.courses)
        assertEquals(latestQuery, viewModel.state.value.viewportQuery)
        coVerify(exactly = 1) { getMapCoursesUseCase(firstQuery) }
        coVerify(exactly = 1) { getMapCoursesUseCase(latestQuery) }
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
        getMapCoursesUseCase: GetMapCoursesUseCase = mockk(),
        getRouteUseCase: GetRouteUseCase = mockk(),
        getNaviAlwaysUseCase: GetNaviAlwaysUseCase = mockk(),
        setNaviAlwaysUseCase: SetNaviAlwaysUseCase = mockk(),
    ): HomeViewModel {
        val getCoursesUseCase = mockk<GetCoursesUseCase>()
        every { getCoursesUseCase() } returns courses
        return HomeViewModel(
            getCoursesUseCase = getCoursesUseCase,
            getMapCoursesUseCase = getMapCoursesUseCase,
            getRouteUseCase = getRouteUseCase,
            getNaviAlwaysUseCase = getNaviAlwaysUseCase,
            setNaviAlwaysUseCase = setNaviAlwaysUseCase,
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

private fun testViewportQuery() = MapViewportQuery(
    northEast = GeoPoint(37.7, 127.2),
    southWest = GeoPoint(37.3, 126.7),
    zoomLevel = 13,
)
