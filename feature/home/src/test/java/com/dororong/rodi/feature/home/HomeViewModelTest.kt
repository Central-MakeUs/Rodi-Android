package com.dororong.rodi.feature.home

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.LoginWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.navi.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.navi.SetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceCoordinatesUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceDetailUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlacesUseCase
import com.dororong.rodi.core.domain.usecase.place.SetPlaceBookmarkUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `first viewport loads a page and duplicate viewport is ignored`() = runTest(dispatcher) {
        val deps = Dependencies()
        val page = CursorPage(listOf(summary(1)), hasNext = false, nextCursor = null, totalCount = 1)
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(page)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.OnViewportSettled(query()))
        vm.onIntent(HomeIntent.OnViewportSettled(query()))
        advanceUntilIdle()

        assertEquals(page.items, vm.state.value.places)
        assertEquals(HomeListState.Content, vm.state.value.listState)
        coVerify(exactly = 1) { deps.getPlaces(query(), null, 20) }
    }

    @Test
    fun `next page removes duplicate ids`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(
            CursorPage(listOf(summary(1), summary(2)), true, "next", 3),
        )
        coEvery { deps.getPlaces(query(), "next", 20) } returns Result.success(
            CursorPage(listOf(summary(2), summary(3)), false, null, null),
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.OnViewportSettled(query()))
        advanceUntilIdle()
        vm.onIntent(HomeIntent.OnLoadNextPage)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), vm.state.value.places.map { it.id })
        assertFalse(vm.state.value.hasNextPage)
    }

    @Test
    fun `new viewport result wins over an older in flight request`() = runTest(dispatcher) {
        val deps = Dependencies()
        val older = CompletableDeferred<Result<CursorPage<PlaceSummary>>>()
        coEvery { deps.getPlaces(query(), null, 20) } coAnswers { older.await() }
        coEvery { deps.getPlaces(query(2.0), null, 20) } returns Result.success(
            CursorPage(listOf(summary(2)), false, null, 1),
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.OnViewportSettled(query()))
        vm.onIntent(HomeIntent.OnResearch(query(2.0)))
        advanceUntilIdle()
        older.complete(Result.success(CursorPage(listOf(summary(1)), false, null, 1)))
        advanceUntilIdle()

        assertEquals(listOf(2L), vm.state.value.places.map { it.id })
        assertEquals(query(2.0), vm.state.value.searchedQuery)
    }

    @Test
    fun `successful empty page is distinct from initial failure`() = runTest(dispatcher) {
        val emptyDeps = Dependencies()
        coEvery { emptyDeps.getPlaces(query(), null, 20) } returns Result.success(
            CursorPage(emptyList(), false, null, 0),
        )
        val emptyVm = emptyDeps.viewModel()
        emptyVm.onIntent(HomeIntent.OnViewportSettled(query()))
        advanceUntilIdle()
        assertEquals(HomeListState.Empty, emptyVm.state.value.listState)

        val failedDeps = Dependencies()
        coEvery { failedDeps.getPlaces(query(), null, 20) } returns Result.failure(IllegalStateException("failure"))
        val failedVm = failedDeps.viewModel()
        failedVm.onIntent(HomeIntent.OnViewportSettled(query()))
        advanceUntilIdle()
        assertEquals(HomeListState.InitialError, failedVm.state.value.listState)
    }

    @Test
    fun `failed refresh keeps previous success data`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(
            CursorPage(listOf(summary(1)), false, null, 1),
        )
        coEvery { deps.getPlaces(query(2.0), null, 20) } returns Result.failure(IllegalStateException("failure"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.OnViewportSettled(query()))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.OnResearch(query(2.0)))
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.state.value.places.map { it.id })
        assertEquals(HomeListState.Content, vm.state.value.listState)
    }

    @Test
    fun `surface transitions navigation partial full partial navigation`() {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.OnListOpen)
        assertEquals(HomeSurfaceState.PartialList, vm.state.value.surfaceState)
        vm.onIntent(HomeIntent.OnListExpand)
        assertEquals(HomeSurfaceState.FullList, vm.state.value.surfaceState)
        vm.onIntent(HomeIntent.OnListCollapse)
        assertEquals(HomeSurfaceState.PartialList, vm.state.value.surfaceState)
        vm.onIntent(HomeIntent.OnListCollapse)
        assertEquals(HomeSurfaceState.Navigation, vm.state.value.surfaceState)
    }

    @Test
    fun `guest detail action resumes exactly once after login`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(true)
        coEvery { deps.authSession() } returnsMany listOf(
            AuthSession(false, false),
            AuthSession(true, true),
        )
        coEvery { deps.getDetail(10L) } returns Result.success(HomePreviewData.courseDetail.copy(id = 10L))
        coEvery { deps.getRoute(any<PlaceDetail>()) } returns
            Result.failure(IllegalStateException("route unavailable"))
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.OnPlaceClick(10L, HomeDetailOrigin.Map))
        advanceUntilIdle()
        assertEquals(PendingHomeAction.OpenDetail(10L, HomeDetailOrigin.Map), vm.state.value.pendingAction)

        vm.onIntent(HomeIntent.OnKakaoLoginCredential("credential"))
        advanceUntilIdle()

        assertNull(vm.state.value.pendingAction)
        assertEquals(10L, vm.state.value.selectedPlaceId)
        coVerify(exactly = 1) { deps.getDetail(10L) }
    }

    @Test
    fun `bookmark state changes only after server success`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 20L, isBookmarked = false, bookmarkCount = 4)
        coEvery { deps.getDetail(20L) } returns Result.success(place)
        coEvery { deps.setBookmark(place, true) } returns Result.success(Unit)
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.OnPlaceClick(20L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.OnBookmarkClick)
        advanceUntilIdle()

        assertTrue(requireNotNull(vm.state.value.selectedPlace).isBookmarked)
        assertEquals(5, vm.state.value.selectedPlace?.bookmarkCount)
    }

    @Test
    fun `bookmark failure preserves ui state`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 20L, isBookmarked = false, bookmarkCount = 4)
        coEvery { deps.getDetail(20L) } returns Result.success(place)
        coEvery { deps.setBookmark(place, true) } returns Result.failure(IllegalStateException("failure"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.OnPlaceClick(20L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.OnBookmarkClick)
        advanceUntilIdle()

        assertFalse(requireNotNull(vm.state.value.selectedPlace).isBookmarked)
        assertEquals(4, vm.state.value.selectedPlace?.bookmarkCount)
    }
}

private class Dependencies(loggedIn: Boolean = true) {
    val coordinates = mockk<GetPlaceCoordinatesUseCase>()
    val getPlaces = mockk<GetPlacesUseCase>()
    val getDetail = mockk<GetPlaceDetailUseCase>()
    val setBookmark = mockk<SetPlaceBookmarkUseCase>()
    val getRoute = mockk<GetRouteUseCase>()
    val authSession = mockk<GetAuthSessionUseCase>()
    val loginWithKakao = mockk<LoginWithKakaoUseCase>()
    val getNaviAlways = mockk<GetNaviAlwaysUseCase>()
    val setNaviAlways = mockk<SetNaviAlwaysUseCase>()

    init {
        coEvery { coordinates() } returns Result.success(emptyList())
        coEvery { authSession() } returns AuthSession(loggedIn, false)
        coEvery { getNaviAlways() } returns null
        coEvery { setNaviAlways(any()) } returns Unit
    }

    fun viewModel() = HomeViewModel(
        getPlaceCoordinatesUseCase = coordinates,
        getPlacesUseCase = getPlaces,
        getPlaceDetailUseCase = getDetail,
        getRouteUseCase = getRoute,
        setPlaceBookmarkUseCase = setBookmark,
        getAuthSessionUseCase = authSession,
        loginWithKakaoUseCase = loginWithKakao,
        getNaviAlwaysUseCase = getNaviAlways,
        setNaviAlwaysUseCase = setNaviAlways,
    )
}

private fun query(offset: Double = 0.0) = PlaceViewportQuery(
    southWest = GeoPoint(37.0 + offset, 126.0 + offset),
    northEast = GeoPoint(38.0 + offset, 127.0 + offset),
    origin = GeoPoint(37.5 + offset, 126.5 + offset),
)

private fun summary(id: Long) = PlaceSummary(
    id = id,
    type = PlaceType.COURSE,
    name = "place-$id",
    address = "서울",
    point = GeoPoint(37.5, 126.5),
    distanceFromMeMeters = 100,
    practiceTypes = listOf(PracticeType.STRAIGHT),
    description = "description",
    distanceMeters = 1_000,
    capacity = null,
    openTime = null,
)
