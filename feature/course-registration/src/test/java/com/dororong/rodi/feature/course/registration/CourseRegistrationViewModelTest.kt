package com.dororong.rodi.feature.course.registration

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.model.course.CourseInputSpec
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CoursePracticeCategory
import com.dororong.rodi.core.domain.model.course.CoursePracticeType
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationResult
import com.dororong.rodi.core.domain.model.course.CourseRegistrationSections
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import com.dororong.rodi.core.domain.repository.CourseRegistrationRouteRepository
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.course.ClearCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.ClearCourseSearchHistoryUseCase
import com.dororong.rodi.core.domain.usecase.course.DeleteCourseSearchHistoryUseCase
import com.dororong.rodi.core.domain.usecase.course.GetCourseRegistrationFormUseCase
import com.dororong.rodi.core.domain.usecase.course.GetStrictCourseRouteUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveCourseSearchHistoryUseCase
import com.dororong.rodi.core.domain.usecase.course.RegisterCourseUseCase
import com.dororong.rodi.core.domain.usecase.course.ResolveCourseLocationSelectionUseCase
import com.dororong.rodi.core.domain.usecase.course.ReverseGeocodeCourseLocationUseCase
import com.dororong.rodi.core.domain.usecase.course.SaveCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.SaveCourseSearchHistoryUseCase
import com.dororong.rodi.core.domain.usecase.course.SearchCourseLocationsUseCase
import com.dororong.rodi.core.domain.usecase.member.CompleteCourseTutorialUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRegistrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<GetAuthSessionUseCase>()
    private val member = mockk<MemberRepository>()
    private val draft = mockk<CourseDraftRepository>()
    private val location = mockk<CourseLocationRepository>()
    private val registration = mockk<CourseRegistrationRepository>()
    private val route = mockk<CourseRegistrationRouteRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { auth() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = false)
        coEvery { member.completeCourseTutorial() } returns Unit
        every { draft.observe() } returns flowOf(null)
        every { location.observeRecent() } returns flowOf(emptyList())
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        coEvery { draft.save(any()) } returns Unit
        coEvery { draft.clear() } returns Unit
        coEvery { location.saveRecent(any()) } returns Unit
        coEvery { location.resolveSelection(any()) } answers { arg(0) }
        coEvery { location.deleteRecent(any()) } returns Unit
        coEvery { location.clearRecent() } returns Unit
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `튜토리얼을 마치면 지도를 열고 등록 폼을 불러온다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        assertEquals(CourseRegistrationPage.Map, viewModel.uiState.value.page)
        assertTrue(viewModel.uiState.value.tutorialCompleted)
        assertEquals(CourseRegistrationFormLoadState.Ready, viewModel.uiState.value.formLoadState)
    }

    @Test
    fun `튜토리얼 완료 저장이 실패해도 사용자를 가두지 않고 지도로 넘어간다`() = runTest(dispatcher) {
        coEvery { member.completeCourseTutorial() } throws IllegalStateException("network")
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<CourseRegistrationEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(CourseRegistrationIntent.TutorialPageChanged(2))
        coEvery { member.completeCourseTutorial() } throws IllegalStateException("network")
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        // 완료 상태를 서버에 남기는 호출이 실패해도 튜토리얼에 가둬두지 않고 지도로 넘어간다 —
        // 다음에 다시 들어오면 서버가 여전히 미완료로 보고 있을 테니 그때 다시 시도된다.
        assertEquals(CourseRegistrationPage.Map, viewModel.uiState.value.page)
        assertEquals(CourseTutorialLoadState.Ready, viewModel.uiState.value.tutorialLoadState)
        assertEquals(emptyList<CourseRegistrationEffect>(), effects)
        collector.cancel()
    }

    @Test
    fun `폼에서 뒤로 가면 임시 저장을 유지한 채 지도로 돌아간다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapReady(true))
        coEvery { route.getStrictRoute(any(), any(), any()) } returns RouteResult(
            points = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
            isRealRoute = true,
            totalDistanceMeters = 3200,
            snappedPoints = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
        )
        stubReverseGeocode(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.ContinueToFormClicked)
        assertEquals(CourseRegistrationPage.Form, viewModel.uiState.value.page)
        viewModel.onIntent(CourseRegistrationIntent.BackPressed)

        assertEquals(CourseRegistrationPage.Map, viewModel.uiState.value.page)
        assertNull(viewModel.uiState.value.dialog)
        assertEquals(
            listOf(RegistrationWaypointType.START, RegistrationWaypointType.DESTINATION),
            viewModel.uiState.value.waypoints.map(RegistrationWaypoint::type),
        )
    }

    @Test
    fun `서버의 maxWaypoints는 순차 지도 흐름의 경유지 수 제한이다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "주소", null))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "주소", null))
        repeat(5) { index ->
            viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Via))
            viewModel.onIntent(
                CourseRegistrationIntent.WaypointSelected(
                    point = GeoPoint(37.51 + index * 0.01, 126.91 + index * 0.01),
                    name = "경유$index",
                    address = "주소$index",
                    jibunAddress = null,
                ),
            )
        }

        assertEquals(4, viewModel.uiState.value.vias.size)
        assertEquals(6, viewModel.uiState.value.waypoints.size)
    }

    @Test
    fun `지점을 고르면 출발 경유 도착 순서를 지키고 직선 대체 경로는 거부한다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapReady(true))

        coEvery { route.getStrictRoute(any(), any(), any()) } returns RouteResult(
            points = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
            isRealRoute = true,
            totalDistanceMeters = 3200,
            snappedPoints = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
        )
        stubReverseGeocode(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        assertEquals(listOf(RegistrationWaypointType.START, RegistrationWaypointType.DESTINATION), viewModel.uiState.value.waypoints.map(RegistrationWaypoint::type))
        assertTrue(viewModel.uiState.value.canFinishMap)
    }

    @Test
    fun `확정 경로의 보정 좌표로 제출할 경유지 좌표를 바꾼다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapReady(true))
        val snappedStart = GeoPoint(37.5001, 126.9001)
        val snappedDestination = GeoPoint(37.6001, 127.0001)
        coEvery { route.getStrictRoute(any(), any(), any()) } returns RouteResult(
            points = listOf(snappedStart, snappedDestination),
            isRealRoute = true,
            totalDistanceMeters = 3200,
            snappedPoints = listOf(snappedStart, snappedDestination),
        )
        stubReverseGeocode(snappedStart, snappedDestination)

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        assertEquals(snappedStart.lat, viewModel.uiState.value.waypoints.first().lat)
        assertEquals(snappedDestination.lng, viewModel.uiState.value.waypoints.last().lng)
    }

    @Test
    fun `검색은 300ms를 기다린 뒤 실행하고 결과를 고르면 경유지는 그대로 두고 지도만 옮긴다`() = runTest(dispatcher) {
        val suggestion = CourseLocationSuggestion("place-1", "강남역", "서울 강남구", GeoPoint(37.5, 127.0), CourseLocationKind.PLACE)
        coEvery { location.search("강남") } returns CourseLocationSearchResult(places = listOf(suggestion))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("강남"))
        advanceTimeBy(299)
        coVerify(exactly = 0) { location.search(any()) }
        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(listOf(suggestion), viewModel.uiState.value.searchResult.places)

        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected("place-1"))
        advanceUntilIdle()
        assertEquals(suggestion.point, viewModel.uiState.value.mapCenter)
        assertTrue(viewModel.uiState.value.waypoints.isEmpty())
    }

    @Test
    fun `지역 검색 결과를 고르면 위치를 확정한 뒤 지도를 옮기고 기록을 저장한다`() = runTest(dispatcher) {
        val region = CourseLocationSuggestion(
            id = "region-1",
            title = "성북구",
            address = "서울 성북구",
            point = null,
            kind = CourseLocationKind.REGION,
        )
        val resolved = region.copy(point = GeoPoint(37.59, 127.02))
        coEvery { location.search("성북") } returns CourseLocationSearchResult(regions = listOf(region))
        coEvery { location.resolveSelection(region) } returns resolved
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("성북"))
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected(region.id))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearchVisible)
        assertEquals(resolved.point, viewModel.uiState.value.mapCenter)
        coVerify(exactly = 1) { location.saveRecent(resolved) }
        assertTrue(viewModel.uiState.value.waypoints.isEmpty())
    }

    @Test
    fun `검색 결과 선택이 실패하면 검색과 지도 상태를 유지하고 기록을 저장하지 않는다`() = runTest(dispatcher) {
        val initialCenter = GeoPoint(37.5, 126.9)
        val suggestion = CourseLocationSuggestion(
            id = "place-unresolved",
            title = "장소",
            address = "서울",
            point = null,
            kind = CourseLocationKind.PLACE,
        )
        coEvery { location.search("장소") } returns CourseLocationSearchResult(places = listOf(suggestion))
        coEvery { location.resolveSelection(suggestion) } returns null
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapCenterChanged(initialCenter))
        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("장소"))
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected(suggestion.id))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSearchVisible)
        assertEquals(initialCenter, viewModel.uiState.value.mapCenter)
        coVerify(exactly = 0) { location.saveRecent(any()) }
    }

    @Test
    fun `이전 검색 결과 선택은 더 최근 선택을 덮어쓰지 않는다`() = runTest(dispatcher) {
        val first = CourseLocationSuggestion("first", "첫 장소", "서울", null, CourseLocationKind.PLACE)
        val second = CourseLocationSuggestion("second", "둘째 장소", "서울", null, CourseLocationKind.PLACE)
        val resolvedFirst = first.copy(point = GeoPoint(37.5, 126.9))
        val resolvedSecond = second.copy(point = GeoPoint(37.6, 127.0))
        val firstResolution = CompletableDeferred<CourseLocationSuggestion?>()
        coEvery { location.search("장소") } returns CourseLocationSearchResult(places = listOf(first, second))
        coEvery { location.resolveSelection(first) } coAnswers { firstResolution.await() }
        coEvery { location.resolveSelection(second) } returns resolvedSecond
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("장소"))
        advanceTimeBy(300)
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected(first.id))
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected(second.id))
        advanceUntilIdle()
        firstResolution.complete(resolvedFirst)
        advanceUntilIdle()

        assertEquals(resolvedSecond.point, viewModel.uiState.value.mapCenter)
        coVerify(exactly = 1) { location.saveRecent(resolvedSecond) }
        coVerify(exactly = 0) { location.saveRecent(resolvedFirst) }
    }

    @Test
    fun `검색을 닫으면 진행 중인 결과 확정을 무효로 한다`() = runTest(dispatcher) {
        val initialCenter = GeoPoint(37.5, 126.9)
        val suggestion = CourseLocationSuggestion("pending", "대기 장소", "서울", null, CourseLocationKind.PLACE)
        val resolved = suggestion.copy(point = GeoPoint(37.6, 127.0))
        val resolution = CompletableDeferred<CourseLocationSuggestion?>()
        coEvery { location.search("대기") } returns CourseLocationSearchResult(places = listOf(suggestion))
        coEvery { location.resolveSelection(suggestion) } coAnswers { resolution.await() }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapCenterChanged(initialCenter))
        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("대기"))
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected(suggestion.id))
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchVisibilityChanged(false))
        resolution.complete(resolved)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearchVisible)
        assertEquals(initialCenter, viewModel.uiState.value.mapCenter)
        coVerify(exactly = 0) { location.saveRecent(any()) }
    }

    @Test
    fun `지도를 누르면 주소를 찾고 선택한 경유지 역할로 확정한다`() = runTest(dispatcher) {
        val point = GeoPoint(37.51, 127.01)
        coEvery { location.reverseGeocode(point) } returns CourseLocationSuggestion(
            id = "map-point",
            title = "선택한 장소",
            address = "서울 강남구",
            point = point,
            kind = CourseLocationKind.PLACE,
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.MapCenterChanged(point))
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapPointSelected(point))
        advanceUntilIdle()

        assertEquals(RegistrationWaypointType.START, viewModel.uiState.value.waypoints.single().type)
        assertEquals(point, GeoPoint(viewModel.uiState.value.waypoints.single().lat, viewModel.uiState.value.waypoints.single().lng))
        assertFalse(viewModel.uiState.value.isMapPointLoading)
        assertFalse(viewModel.uiState.value.isPendingAddressLoading)
    }

    @Test
    fun `핀 수정을 초기화하고 확정해도 원래 주소를 유지한다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "원래 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.PinEditStarted(0))
        viewModel.onIntent(CourseRegistrationIntent.TemporaryPinMoved(GeoPoint(37.51, 126.91)))
        viewModel.onIntent(CourseRegistrationIntent.PinEditReset)
        viewModel.onIntent(CourseRegistrationIntent.PinEditCommitted)
        advanceUntilIdle()

        assertEquals(37.5, viewModel.uiState.value.waypoints.single().lat)
        assertEquals("원래 주소", viewModel.uiState.value.waypoints.single().address)
        coVerify { draft.save(match { it.waypoints.single().lat == 37.5 }) }
    }

    @Test
    fun `핀 수정 중 카메라가 움직여도 임시 핀을 고르지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val original = GeoPoint(37.5, 126.9)
        val moved = GeoPoint(37.51, 126.91)
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(original, "출발", "원래 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.PinEditStarted(0))
        viewModel.onIntent(CourseRegistrationIntent.MapCenterChanged(moved))

        assertEquals(moved, viewModel.uiState.value.mapCenter)
        assertNull(viewModel.uiState.value.temporaryPin)
        assertEquals(original.lat, viewModel.uiState.value.waypoints.single().lat)
        assertEquals(original.lng, viewModel.uiState.value.waypoints.single().lng)
    }

    @Test
    fun `현재 위치는 지도만 옮기고 경유지를 확정하지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val point = GeoPoint(37.55, 126.98)

        viewModel.onIntent(CourseRegistrationIntent.CurrentLocationSelected(point))

        assertEquals(point, viewModel.uiState.value.mapCenter)
        assertTrue(viewModel.uiState.value.waypoints.isEmpty())
    }

    @Test
    fun `연습 유형 선택 수는 서버 폼 제한을 넘지 않는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.PracticeTypeToggled("parking"))
        viewModel.onIntent(CourseRegistrationIntent.PracticeTypeToggled("turn"))
        viewModel.onIntent(CourseRegistrationIntent.PracticeTypeToggled("lane"))
        advanceUntilIdle()

        assertEquals(listOf("parking"), viewModel.uiState.value.selectedPracticeTypeCodes)
        assertFalse(viewModel.uiState.value.selectedPracticeTypeCodes.size > sampleForm().practiceTypeMaxSelect)
    }

    @Test
    fun `나가기를 확정하면 임시 저장을 지우고 다이얼로그를 닫는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "주소", null))
        viewModel.onIntent(CourseRegistrationIntent.ExitRequested)
        viewModel.onIntent(CourseRegistrationIntent.ExitConfirmed)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.dialog)
        coVerify { draft.clear() }
    }

    @Test
    fun `코스 등록은 임시 저장 삭제가 끝난 뒤에 성공을 보여준다`() = runTest(dispatcher) {
        val startPoint = GeoPoint(37.5, 126.9)
        val destinationPoint = GeoPoint(37.6, 127.0)
        val routeResult = RouteResult(
            points = listOf(startPoint, destinationPoint),
            isRealRoute = true,
            totalDistanceMeters = 3200,
            snappedPoints = listOf(startPoint, destinationPoint),
        )
        val clearStarted = CompletableDeferred<Unit>()
        val releaseClear = CompletableDeferred<Unit>()
        val saveStarted = CompletableDeferred<Unit>()
        val saveCancelled = CompletableDeferred<Unit>()
        coEvery { auth.invoke() } returns AuthSession(
            isLoggedIn = true,
            hasRecentKakaoLogin = true,
            isCourseTutorialCompleted = true,
        )
        coEvery { route.getStrictRoute(any(), any(), any()) } returns routeResult
        stubReverseGeocode(startPoint, destinationPoint)
        coEvery { registration.registerCourse(any()) } returns CourseRegistrationResult(1L, CourseApprovalStatus.PENDING)
        coEvery { draft.save(match { it.description == "연습 코스" }) } coAnswers {
            saveStarted.complete(Unit)
            try {
                awaitCancellation()
            } catch (error: CancellationException) {
                saveCancelled.complete(Unit)
                throw error
            }
        }
        coEvery { draft.clear() } coAnswers {
            clearStarted.complete(Unit)
            releaseClear.await()
        }

        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapReady(true))
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = startPoint,
                name = "출발",
                address = "주소",
                jibunAddress = null,
            ),
        )
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = destinationPoint,
                name = "도착",
                address = "주소",
                jibunAddress = null,
            ),
        )
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.PracticeTypeToggled("parking"))
        viewModel.onIntent(CourseRegistrationIntent.ContinueToFormClicked)
        viewModel.onIntent(CourseRegistrationIntent.DescriptionChanged("연습 코스"))
        runCurrent()
        assertTrue(saveStarted.isCompleted)

        viewModel.onIntent(CourseRegistrationIntent.SubmitClicked)
        runCurrent()

        assertTrue(saveCancelled.isCompleted)
        assertTrue(clearStarted.isCompleted)
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.registrationResult)
        assertNull(viewModel.uiState.value.dialog)

        viewModel.onIntent(CourseRegistrationIntent.DescriptionChanged("제출 중 수정"))
        runCurrent()
        coVerify(exactly = 0) { draft.save(match { it.description == "제출 중 수정" }) }

        releaseClear.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(CourseRegistrationDialog.Success, viewModel.uiState.value.dialog)
        coVerify(exactly = 1) { draft.clear() }
    }

    @Test
    fun `경유지가 있는 임시 저장을 복원해도 지도를 보여주기 전에 현재 위치를 요청한다`() = runTest {
        val startPoint = GeoPoint(37.5, 126.9)
        val sampleDraft = CourseDraft(
            waypoints = listOf(RegistrationWaypoint(RegistrationWaypointType.START, "출발", "주소", lat = startPoint.lat, lng = startPoint.lng)),
            selectedPracticeTypeCodes = emptyList(),
            caution = "",
            description = "",
        )
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = true)
        coEvery { draft.observe() } returns flowOf(sampleDraft)
        coEvery { registration.getRegistrationForm() } returns sampleForm()

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.mapCenter)
        assertEquals(InitialLocationState.Requesting, viewModel.uiState.value.initialLocationState)
    }

    @Test
    fun `임시 저장이 없으면 튜토리얼 완료 후 초기 위치를 요청한다`() = runTest {
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = false)
        coEvery { draft.observe() } returns flowOf(null)
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        coEvery { member.completeCourseTutorial() } returns Unit
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        assertEquals(CourseRegistrationPage.Map, viewModel.uiState.value.page)
        assertEquals(InitialLocationState.Requesting, viewModel.uiState.value.initialLocationState)
    }

    @Test
    fun `출발지와 같은 좌표를 도착지로 고르면 스낵바로 거부한다`() = runTest {
        val startPoint = GeoPoint(37.5, 126.9)
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = true)
        coEvery { draft.observe() } returns flowOf(null)
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<CourseRegistrationEffect>()
        val collector = launch { viewModel.effect.toList(effects) }
        // selectWaypoint()의 tryEmit은 코루틴 dispatch 없이 즉시 실행되는 동기 호출이라,
        // collector가 실제로 구독을 시작하기 전에 onIntent를 부르면 replay=0 SharedFlow가
        // 구독자 없는 emit을 그냥 흘려보낸다. runCurrent()로 collector를 먼저 진짜 돌려둔다.
        runCurrent()

        // 1. 출발지 선택
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = startPoint,
                name = "출발",
                address = "주소",
                jibunAddress = null,
            ),
        )
        // 2. 같은 좌표로 도착지 선택 시도
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = startPoint,
                name = "도착(동일)",
                address = "주소",
                jibunAddress = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.waypoints.size)
        assertEquals(RegistrationWaypointType.START, viewModel.uiState.value.waypoints[0].type)
        assertEquals(
            listOf(CourseRegistrationEffect.ShowSnackbar("출발지와 다른 위치를 선택해주세요.")),
            effects,
        )
        collector.cancel()
    }

    @Test
    fun `출발지와 다른 좌표를 도착지로 고르면 받아들인다`() = runTest {
        val startPoint = GeoPoint(37.5, 126.9)
        val destPoint = GeoPoint(37.6, 127.0)
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = true)
        coEvery { draft.observe() } returns flowOf(null)
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        val viewModel = viewModel()
        advanceUntilIdle()

        // 1. 출발지 선택
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = startPoint,
                name = "출발",
                address = "주소",
                jibunAddress = null,
            ),
        )
        // 2. 다른 좌표로 도착지 선택
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(
            CourseRegistrationIntent.WaypointSelected(
                point = destPoint,
                name = "도착(다름)",
                address = "주소",
                jibunAddress = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.waypoints.size)
        assertEquals(RegistrationWaypointType.START, viewModel.uiState.value.waypoints[0].type)
        assertEquals(RegistrationWaypointType.DESTINATION, viewModel.uiState.value.waypoints[1].type)
    }

    @Test
    fun `출발지 없이 도착지를 고르면 선택 역할을 출발지로 되돌린다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "주소", null))
        advanceUntilIdle()

        assertEquals(RegistrationWaypointType.DESTINATION, viewModel.uiState.value.waypoints.single().type)
        assertEquals(CourseWaypointRole.Start, viewModel.uiState.value.selectedWaypointRole)
    }

    @Test
    fun `출발지 다음 도착지를 고르면 선택 역할이 도착지로 남는다`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "주소", null))
        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.6, 127.0), "도착", "주소", null))
        advanceUntilIdle()

        assertEquals(CourseWaypointRole.Destination, viewModel.uiState.value.selectedWaypointRole)
    }

    @Test
    fun `경유지를 확정하면 줌을 유지하고 검색 결과를 고르면 줌을 초기화한다`() = runTest(dispatcher) {
        val suggestion = CourseLocationSuggestion("place-1", "강남역", "서울 강남구", GeoPoint(37.5, 127.0), CourseLocationKind.PLACE)
        coEvery { location.search("강남") } returns CourseLocationSearchResult(places = listOf(suggestion))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "주소", null))
        assertTrue(viewModel.uiState.value.mapCenterKeepsZoom)

        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("강남"))
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected("place-1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.mapCenterKeepsZoom)
    }

    @Test
    fun `핀 수정에 들어가면 줌을 유지한다`() = runTest(dispatcher) {
        val suggestion = CourseLocationSuggestion("place-1", "강남역", "서울 강남구", GeoPoint(37.5, 127.0), CourseLocationKind.PLACE)
        coEvery { location.search("강남") } returns CourseLocationSearchResult(places = listOf(suggestion))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.WaypointRoleSelected(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.WaypointSelected(GeoPoint(37.5, 126.9), "출발", "주소", null))
        // 검색 결과 선택으로 줌 유지 플래그를 false로 만들어둔 뒤, beginPinEdit가 같은 자리
        // 재중심으로 인식해 스스로 true로 되돌리는지 확인한다.
        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("강남"))
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected("place-1"))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.mapCenterKeepsZoom)

        viewModel.onIntent(CourseRegistrationIntent.PinEditStarted(0))

        assertTrue(viewModel.uiState.value.mapCenterKeepsZoom)
    }

    @Test
    fun `카테고리를 바꿔도 이전에 고른 연습 유형을 유지한다`() = runTest(dispatcher) {
        coEvery { registration.getRegistrationForm() } returns twoCategoryForm()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.CategorySelected("basic"))
        viewModel.onIntent(CourseRegistrationIntent.PracticeTypeToggled("straight"))
        viewModel.onIntent(CourseRegistrationIntent.CategorySelected("parking"))
        advanceUntilIdle()

        assertEquals("parking", viewModel.uiState.value.selectedCategoryCode)
        assertEquals(listOf("straight"), viewModel.uiState.value.selectedPracticeTypeCodes)
    }

    @Test
    fun `같은 카테고리를 다시 골라도 선택을 해제하지 않는다`() = runTest(dispatcher) {
        coEvery { registration.getRegistrationForm() } returns twoCategoryForm()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.CategorySelected("basic"))
        viewModel.onIntent(CourseRegistrationIntent.CategorySelected("basic"))
        advanceUntilIdle()

        assertEquals("basic", viewModel.uiState.value.selectedCategoryCode)
    }

    @Test
    fun `폼을 불러오면 항상 카테고리 하나를 선택한다`() = runTest(dispatcher) {
        coEvery { registration.getRegistrationForm() } returns twoCategoryForm()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TutorialCompleted)
        advanceUntilIdle()

        assertEquals("basic", viewModel.uiState.value.selectedCategoryCode)
    }

    private fun twoCategoryForm() = CourseRegistrationForm(
        maxWaypoints = 4,
        sections = CourseRegistrationSections("코스 정보", "연습 카테고리", "연습 유형", "주의사항", "설명"),
        practiceTypeMaxSelect = 3,
        practiceTypeMaxSelectExceededMessage = "최대 3개까지 선택할 수 있어요.",
        categories = listOf(
            CoursePracticeCategory(
                code = "basic",
                label = "기초 주행",
                order = 1,
                practiceTypes = listOf(CoursePracticeType("straight", "직선주행", 1)),
            ),
            CoursePracticeCategory(
                code = "parking",
                label = "주차",
                order = 2,
                practiceTypes = listOf(CoursePracticeType("parallel", "평행주차", 1)),
            ),
        ),
        cautionInput = CourseInputSpec(false, maxLength = 100, placeholder = "주의사항"),
        descriptionInput = CourseInputSpec(true, minLength = 1, maxLength = 200, placeholder = "설명"),
    )

    private fun viewModel(): CourseRegistrationViewModel = CourseRegistrationViewModel(
        getAuthSession = auth,
        completeCourseTutorial = CompleteCourseTutorialUseCase(member),
        observeCourseDraft = ObserveCourseDraftUseCase(draft),
        saveCourseDraft = SaveCourseDraftUseCase(draft),
        clearCourseDraft = ClearCourseDraftUseCase(draft),
        observeSearchHistory = ObserveCourseSearchHistoryUseCase(location),
        saveSearchHistory = SaveCourseSearchHistoryUseCase(location),
        deleteSearchHistory = DeleteCourseSearchHistoryUseCase(location),
        clearSearchHistory = ClearCourseSearchHistoryUseCase(location),
        searchLocations = SearchCourseLocationsUseCase(location),
        resolveLocationSelection = ResolveCourseLocationSelectionUseCase(location),
        reverseGeocode = ReverseGeocodeCourseLocationUseCase(location),
        getRegistrationForm = GetCourseRegistrationFormUseCase(registration),
        registerCourse = RegisterCourseUseCase(registration),
        getStrictCourseRoute = GetStrictCourseRouteUseCase(route),
    )

    private fun stubReverseGeocode(vararg points: GeoPoint) {
        points.forEach { point ->
            coEvery { location.reverseGeocode(point) } returns CourseLocationSuggestion(
                id = "route-${point.lat}-${point.lng}",
                title = "도로 위 위치",
                address = "서울 도로명 주소",
                point = point,
                kind = CourseLocationKind.PLACE,
            )
        }
    }

    private fun sampleForm() = CourseRegistrationForm(
        maxWaypoints = 4,
        sections = CourseRegistrationSections("코스 정보", "연습 카테고리", "연습 유형", "주의사항", "설명"),
        practiceTypeMaxSelect = 1,
        practiceTypeMaxSelectExceededMessage = "하나만 선택해 주세요.",
        categories = listOf(
            CoursePracticeCategory(
                code = "basic",
                label = "기본",
                order = 1,
                practiceTypes = listOf(
                    CoursePracticeType("parking", "주차", 1),
                    CoursePracticeType("turn", "회전", 2),
                    CoursePracticeType("lane", "차선 변경", 3),
                ),
            ),
        ),
        cautionInput = CourseInputSpec(false, maxLength = 100, placeholder = "주의사항"),
        descriptionInput = CourseInputSpec(true, minLength = 1, maxLength = 200, placeholder = "설명"),
    )
}
