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
import com.dororong.rodi.core.domain.model.course.CourseRegistrationSections
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
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
    fun `tutorial completion opens map and loads registration form`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial)
        advanceUntilIdle()

        assertEquals(CourseRegistrationPage.Map, viewModel.state.value.page)
        assertTrue(viewModel.state.value.tutorialCompleted)
        assertEquals(CourseRegistrationFormLoadState.Ready, viewModel.state.value.formLoadState)
    }

    @Test
    fun `tutorial completion failure keeps page three and re-enables retry`() = runTest(dispatcher) {
        coEvery { member.completeCourseTutorial() } throws IllegalStateException("network")
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<CourseRegistrationEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(CourseRegistrationIntent.TutorialPageChanged(2))
        coEvery { member.completeCourseTutorial() } throws IllegalStateException("network")
        viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial)
        advanceUntilIdle()

        assertEquals(CourseRegistrationPage.Tutorial, viewModel.state.value.page)
        assertEquals(2, viewModel.state.value.tutorialPage)
        assertEquals(CourseTutorialLoadState.Ready, viewModel.state.value.tutorialLoadState)
        assertEquals(listOf(CourseRegistrationEffect.ShowSnackbar("network")), effects)
        collector.cancel()
    }

    @Test
    fun `back from form returns to map while preserving the draft`() = runTest(dispatcher) {
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
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.ContinueToForm)
        assertEquals(CourseRegistrationPage.Form, viewModel.state.value.page)
        viewModel.onIntent(CourseRegistrationIntent.Back)

        assertEquals(CourseRegistrationPage.Map, viewModel.state.value.page)
        assertNull(viewModel.state.value.dialog)
        assertEquals(
            listOf(RegistrationWaypointType.START, RegistrationWaypointType.DESTINATION),
            viewModel.state.value.waypoints.map(RegistrationWaypoint::type),
        )
    }

    @Test
    fun `server maxWaypoints is the via limit in the sequential map flow`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "주소", null))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.6, 127.0), "도착", "주소", null))
        repeat(5) { index ->
            viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Via))
            viewModel.onIntent(
                CourseRegistrationIntent.SelectWaypoint(
                    point = GeoPoint(37.51 + index * 0.01, 126.91 + index * 0.01),
                    name = "경유$index",
                    address = "주소$index",
                    jibunAddress = null,
                ),
            )
        }

        assertEquals(4, viewModel.state.value.vias.size)
        assertEquals(6, viewModel.state.value.waypoints.size)
    }

    @Test
    fun `selecting points keeps start via destination order and rejects straight fallback`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.MapReady(true))

        coEvery { route.getStrictRoute(any(), any(), any()) } returns RouteResult(
            points = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
            isRealRoute = true,
            totalDistanceMeters = 3200,
            snappedPoints = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
        )
        stubReverseGeocode(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        assertEquals(listOf(RegistrationWaypointType.START, RegistrationWaypointType.DESTINATION), viewModel.state.value.waypoints.map(RegistrationWaypoint::type))
        assertTrue(viewModel.state.value.canFinishMap)
    }

    @Test
    fun `strict route snapped points replace submitted waypoint coordinates`() = runTest(dispatcher) {
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

        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "서울 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.6, 127.0), "도착", "서울 주소", null))
        advanceUntilIdle()

        assertEquals(snappedStart.lat, viewModel.state.value.waypoints.first().lat)
        assertEquals(snappedDestination.lng, viewModel.state.value.waypoints.last().lng)
    }

    @Test
    fun `search waits for 300ms and selecting result moves map without changing waypoint`() = runTest(dispatcher) {
        val suggestion = CourseLocationSuggestion("place-1", "강남역", "서울 강남구", GeoPoint(37.5, 127.0), CourseLocationKind.PLACE)
        coEvery { location.search("강남") } returns CourseLocationSearchResult(places = listOf(suggestion))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.SearchKeywordChanged("강남"))
        advanceTimeBy(299)
        coVerify(exactly = 0) { location.search(any()) }
        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(listOf(suggestion), viewModel.state.value.searchResult.places)

        viewModel.onIntent(CourseRegistrationIntent.SearchSuggestionSelected("place-1"))
        advanceUntilIdle()
        assertEquals(suggestion.point, viewModel.state.value.mapCenter)
        assertTrue(viewModel.state.value.waypoints.isEmpty())
    }

    @Test
    fun `search selection resolves a region before moving map and saving history`() = runTest(dispatcher) {
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

        assertFalse(viewModel.state.value.isSearchVisible)
        assertEquals(resolved.point, viewModel.state.value.mapCenter)
        coVerify(exactly = 1) { location.saveRecent(resolved) }
        assertTrue(viewModel.state.value.waypoints.isEmpty())
    }

    @Test
    fun `failed search selection preserves search and map state without saving history`() = runTest(dispatcher) {
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

        assertTrue(viewModel.state.value.isSearchVisible)
        assertEquals(initialCenter, viewModel.state.value.mapCenter)
        coVerify(exactly = 0) { location.saveRecent(any()) }
    }

    @Test
    fun `stale search selection cannot overwrite a newer selection`() = runTest(dispatcher) {
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

        assertEquals(resolvedSecond.point, viewModel.state.value.mapCenter)
        coVerify(exactly = 1) { location.saveRecent(resolvedSecond) }
        coVerify(exactly = 0) { location.saveRecent(resolvedFirst) }
    }

    @Test
    fun `closing search invalidates an in-flight selection resolution`() = runTest(dispatcher) {
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

        assertFalse(viewModel.state.value.isSearchVisible)
        assertEquals(initialCenter, viewModel.state.value.mapCenter)
        coVerify(exactly = 0) { location.saveRecent(any()) }
    }

    @Test
    fun `tapping map reverse geocodes and confirms the selected waypoint role`() = runTest(dispatcher) {
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

        assertEquals(RegistrationWaypointType.START, viewModel.state.value.waypoints.single().type)
        assertEquals(point, GeoPoint(viewModel.state.value.waypoints.single().lat, viewModel.state.value.waypoints.single().lng))
        assertFalse(viewModel.state.value.isMapPointLoading)
        assertFalse(viewModel.state.value.isPendingAddressLoading)
    }

    @Test
    fun `pin edit reset and commit preserve original address`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "원래 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.BeginPinEdit(0))
        viewModel.onIntent(CourseRegistrationIntent.MoveTemporaryPin(GeoPoint(37.51, 126.91)))
        viewModel.onIntent(CourseRegistrationIntent.ResetPinEdit)
        viewModel.onIntent(CourseRegistrationIntent.CommitPinEdit)
        advanceUntilIdle()

        assertEquals(37.5, viewModel.state.value.waypoints.single().lat)
        assertEquals("원래 주소", viewModel.state.value.waypoints.single().address)
        coVerify { draft.save(match { it.waypoints.single().lat == 37.5 }) }
    }

    @Test
    fun `camera movement does not select a temporary pin while editing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val original = GeoPoint(37.5, 126.9)
        val moved = GeoPoint(37.51, 126.91)
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(original, "출발", "원래 주소", null))
        viewModel.onIntent(CourseRegistrationIntent.BeginPinEdit(0))
        viewModel.onIntent(CourseRegistrationIntent.MapCenterChanged(moved))

        assertEquals(moved, viewModel.state.value.mapCenter)
        assertNull(viewModel.state.value.temporaryPin)
        assertEquals(original.lat, viewModel.state.value.waypoints.single().lat)
        assertEquals(original.lng, viewModel.state.value.waypoints.single().lng)
    }

    @Test
    fun `current location only moves map and does not confirm a waypoint`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val point = GeoPoint(37.55, 126.98)

        viewModel.onIntent(CourseRegistrationIntent.CurrentLocationSelected(point))

        assertEquals(point, viewModel.state.value.mapCenter)
        assertTrue(viewModel.state.value.waypoints.isEmpty())
    }

    @Test
    fun `practice type selection is capped by server form`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial)
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.TogglePracticeType("parking"))
        viewModel.onIntent(CourseRegistrationIntent.TogglePracticeType("turn"))
        viewModel.onIntent(CourseRegistrationIntent.TogglePracticeType("lane"))
        advanceUntilIdle()

        assertEquals(listOf("parking"), viewModel.state.value.selectedPracticeTypeCodes)
        assertFalse(viewModel.state.value.selectedPracticeTypeCodes.size > sampleForm().practiceTypeMaxSelect)
    }

    @Test
    fun `explicit exit clears draft before reporting exit`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Start))
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypoint(GeoPoint(37.5, 126.9), "출발", "주소", null))
        viewModel.onIntent(CourseRegistrationIntent.RequestExit)
        viewModel.onIntent(CourseRegistrationIntent.ConfirmExit)
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.dialog)
        coVerify { draft.clear() }
    }

    @Test
    fun `restoring draft with waypoints sets mapCenter and skips initial location request`() = runTest {
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

        assertEquals(startPoint, viewModel.state.value.mapCenter)
        assertEquals(InitialLocationState.Resolved, viewModel.state.value.initialLocationState)
    }

    @Test
    fun `tutorial completion triggers initial location request if no draft is present`() = runTest {
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = false)
        coEvery { draft.observe() } returns flowOf(null)
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        coEvery { member.completeCourseTutorial() } returns Unit
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial)
        advanceUntilIdle()

        assertEquals(CourseRegistrationPage.Map, viewModel.state.value.page)
        assertEquals(InitialLocationState.Requesting, viewModel.state.value.initialLocationState)
    }

    @Test
    fun `selecting destination at same coordinates as start is rejected with snackbar`() = runTest {
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
            CourseRegistrationIntent.SelectWaypoint(
                point = startPoint,
                name = "출발",
                address = "주소",
                jibunAddress = null,
            ),
        )
        // 2. 같은 좌표로 도착지 선택 시도
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(
            CourseRegistrationIntent.SelectWaypoint(
                point = startPoint,
                name = "도착(동일)",
                address = "주소",
                jibunAddress = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.waypoints.size)
        assertEquals(RegistrationWaypointType.START, viewModel.state.value.waypoints[0].type)
        assertEquals(
            listOf(CourseRegistrationEffect.ShowSnackbar("출발지와 다른 위치를 선택해주세요.")),
            effects,
        )
        collector.cancel()
    }

    @Test
    fun `selecting destination at different coordinates is accepted`() = runTest {
        val startPoint = GeoPoint(37.5, 126.9)
        val destPoint = GeoPoint(37.6, 127.0)
        coEvery { auth.invoke() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true, isCourseTutorialCompleted = true)
        coEvery { draft.observe() } returns flowOf(null)
        coEvery { registration.getRegistrationForm() } returns sampleForm()
        val viewModel = viewModel()
        advanceUntilIdle()

        // 1. 출발지 선택
        viewModel.onIntent(
            CourseRegistrationIntent.SelectWaypoint(
                point = startPoint,
                name = "출발",
                address = "주소",
                jibunAddress = null,
            ),
        )
        // 2. 다른 좌표로 도착지 선택
        viewModel.onIntent(CourseRegistrationIntent.SelectWaypointRole(CourseWaypointRole.Destination))
        viewModel.onIntent(
            CourseRegistrationIntent.SelectWaypoint(
                point = destPoint,
                name = "도착(다름)",
                address = "주소",
                jibunAddress = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.waypoints.size)
        assertEquals(RegistrationWaypointType.START, viewModel.state.value.waypoints[0].type)
        assertEquals(RegistrationWaypointType.DESTINATION, viewModel.state.value.waypoints[1].type)
    }

    private fun viewModel(): CourseRegistrationViewModel = CourseRegistrationViewModel(
        getAuthSession = auth,
        memberRepository = member,
        draftRepository = draft,
        locationRepository = location,
        registrationRepository = registration,
        routeRepository = route,
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
