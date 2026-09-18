package com.dororong.rodi.feature.home

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.practice.Practice
import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import com.dororong.rodi.core.domain.model.practice.PracticeVisitResult
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.LoginWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.auth.RestoreWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.usecase.driving.ObserveDrivingSessionUseCase
import com.dororong.rodi.core.domain.usecase.navi.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.navi.SetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateFilterTagsUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceCoordinatesUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceDetailUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlacesUseCase
import com.dororong.rodi.core.domain.usecase.place.RefreshPlaceCoordinatesUseCase
import com.dororong.rodi.core.domain.usecase.place.RefreshPlacesUseCase
import com.dororong.rodi.core.domain.usecase.place.SetPlaceBookmarkUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetNotificationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkNotificationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.practice.ClearActivePracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.GetActivePracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.RecordPracticeVisitUseCase
import com.dororong.rodi.core.domain.usecase.practice.RegisterPracticeUseCase
import com.dororong.rodi.core.domain.usecase.practice.SaveActivePracticeSessionUseCase
import com.dororong.rodi.feature.home.search.RegionOfficeLocationResolver
import com.dororong.rodi.feature.home.filter.FilterCategory
import com.dororong.rodi.feature.home.filter.FilterPracticeOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow

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
        coEvery { deps.refreshPlaces(query(), null, 20) } returns Result.success(page)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()

        assertEquals(page.items, vm.uiState.value.places)
        assertEquals(HomeListState.Content, vm.uiState.value.listState)
        coVerify(exactly = 1) { deps.getPlaces(query(), null, 20) }
    }

    @Test
    fun `region search opens partial list and retains the selected region`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()
        val region = requireNotNull(RegionOfficeLocationResolver.find("서울 중구"))

        vm.effect.test {
            vm.onIntent(HomeIntent.RegionSearchRequested(region, listOf(summary(1))))

            assertEquals(HomeEffect.MoveToRegion(region), awaitItem())
        }

        assertEquals(HomeSurfaceState.PartialList, vm.uiState.value.surfaceState)
        assertEquals("서울 중구", vm.uiState.value.searchKeyword)
        assertEquals(region, vm.uiState.value.regionSearch)
        assertEquals(HomeListState.Content, vm.uiState.value.listState)
        assertEquals(listOf(1L), vm.uiState.value.places.map(PlaceSummary::id))
    }

    @Test
    fun `map detail keeps the search bar empty`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 41L)
        coEvery { deps.getDetail(41L) } returns Result.success(place)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.PlaceClicked(41L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        assertNull(vm.uiState.value.searchKeyword)
    }

    @Test
    fun `map detail clears an existing search keyword before loading`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 43L)
        coEvery { deps.getDetail(43L) } returns Result.success(place)
        val vm = deps.viewModel()
        val region = requireNotNull(RegionOfficeLocationResolver.find("서울 중구"))
        vm.onIntent(HomeIntent.RegionSearchRequested(region, listOf(summary(1))))

        vm.onIntent(HomeIntent.PlaceClicked(43L, HomeDetailOrigin.Map))
        runCurrent()

        assertNull(vm.uiState.value.searchKeyword)
    }

    @Test
    fun `list detail keeps the selected place name in the search bar`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 42L)
        coEvery { deps.getDetail(42L) } returns Result.success(place)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.PlaceClicked(42L, HomeDetailOrigin.List))
        advanceUntilIdle()

        assertEquals(place.name, vm.uiState.value.searchKeyword)
    }

    @Test
    fun `next page removes duplicate ids`() = runTest(dispatcher) {
        val deps = Dependencies()
        val firstPage = CursorPage(listOf(summary(1), summary(2)), true, "next", 3)
        val nextPage = CursorPage(listOf(summary(2), summary(3)), false, null, null)
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(firstPage)
        coEvery { deps.refreshPlaces(query(), null, 20) } returns Result.success(firstPage)
        coEvery { deps.refreshPlaces(query(), "next", 20) } returns Result.success(nextPage)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        vm.onIntent(HomeIntent.ListEndReached)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.places.map { it.id })
        assertFalse(vm.uiState.value.hasNextPage)
    }

    @Test
    fun `first page replacement advances the list generation but requesting and paging do not`() = runTest(dispatcher) {
        val deps = Dependencies()
        val initialPage = CursorPage(listOf(summary(1), summary(2), summary(3)), false, null, 3)
        val cachedResult = CompletableDeferred<Result<CursorPage<PlaceSummary>>>()
        val refreshedResult = CompletableDeferred<Result<CursorPage<PlaceSummary>>>()
        val cachedPage = CursorPage(listOf(summary(1), summary(2), summary(3)), false, null, null)
        val refreshedPage = CursorPage(listOf(summary(10), summary(11), summary(2)), true, "next", 4)
        val nextPage = CursorPage(listOf(summary(12)), false, null, null)
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(initialPage)
        coEvery { deps.refreshPlaces(query(), null, 20) } returns Result.success(initialPage)
        coEvery { deps.getPlaces(query(2.0), null, 20) } coAnswers { cachedResult.await() }
        coEvery { deps.refreshPlaces(query(2.0), null, 20) } coAnswers { refreshedResult.await() }
        coEvery { deps.refreshPlaces(query(2.0), "next", 20) } returns Result.success(nextPage)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        val initialGeneration = vm.uiState.value.placeListGeneration

        vm.onIntent(HomeIntent.ResearchClicked(query(2.0)))
        runCurrent()

        assertEquals(initialGeneration, vm.uiState.value.placeListGeneration)

        cachedResult.complete(Result.success(cachedPage))
        runCurrent()

        assertEquals(initialGeneration + 1, vm.uiState.value.placeListGeneration)
        assertEquals(cachedPage.items, vm.uiState.value.places)

        refreshedResult.complete(Result.success(refreshedPage))
        advanceUntilIdle()

        assertEquals(initialGeneration + 2, vm.uiState.value.placeListGeneration)
        assertEquals(refreshedPage.items, vm.uiState.value.places)

        vm.onIntent(HomeIntent.ListEndReached)
        advanceUntilIdle()

        assertEquals(initialGeneration + 2, vm.uiState.value.placeListGeneration)
        assertEquals(listOf(10L, 11L, 2L, 12L), vm.uiState.value.places.map(PlaceSummary::id))
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

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        vm.onIntent(HomeIntent.ResearchClicked(query(2.0)))
        advanceUntilIdle()
        older.complete(Result.success(CursorPage(listOf(summary(1)), false, null, 1)))
        advanceUntilIdle()

        assertEquals(listOf(2L), vm.uiState.value.places.map { it.id })
        assertEquals(query(2.0), vm.uiState.value.searchedQuery)
        assertEquals(1, vm.uiState.value.placeListGeneration)
    }

    @Test
    fun `successful empty page is distinct from initial failure`() = runTest(dispatcher) {
        val emptyDeps = Dependencies()
        val emptyPage = CursorPage<PlaceSummary>(emptyList(), false, null, 0)
        coEvery { emptyDeps.getPlaces(query(), null, 20) } returns Result.success(emptyPage)
        coEvery { emptyDeps.refreshPlaces(query(), null, 20) } returns Result.success(emptyPage)
        val emptyVm = emptyDeps.viewModel()
        emptyVm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        assertEquals(HomeListState.Empty, emptyVm.uiState.value.listState)

        val failedDeps = Dependencies()
        coEvery { failedDeps.getPlaces(query(), null, 20) } returns Result.failure(IllegalStateException("failure"))
        val failedVm = failedDeps.viewModel()
        failedVm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        assertEquals(HomeListState.InitialError, failedVm.uiState.value.listState)
        assertEquals(0, failedVm.uiState.value.placeListGeneration)
    }

    @Test
    fun `programmatic search retries the same failed viewport`() = runTest(dispatcher) {
        val deps = Dependencies()
        val page = CursorPage(listOf(summary(1)), false, null, 1)
        coEvery { deps.getPlaces(query(), null, 20) } returnsMany listOf(
            Result.failure(IllegalStateException("failure")),
            Result.success(page),
        )
        coEvery { deps.refreshPlaces(query(), null, 20) } returns Result.success(page)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        assertEquals(HomeListState.InitialError, vm.uiState.value.listState)

        vm.onIntent(HomeIntent.ProgrammaticSearchRequested(query()))
        advanceUntilIdle()

        assertEquals(HomeListState.Content, vm.uiState.value.listState)
        assertEquals(page.items, vm.uiState.value.places)
        coVerify(exactly = 2) { deps.getPlaces(query(), null, 20) }
    }

    @Test
    fun `failed refresh keeps previous success data`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(
            CursorPage(listOf(summary(1)), false, null, 1),
        )
        coEvery { deps.getPlaces(query(2.0), null, 20) } returns Result.failure(IllegalStateException("failure"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.ResearchClicked(query(2.0)))
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.uiState.value.places.map { it.id })
        assertEquals(HomeListState.Content, vm.uiState.value.listState)
    }

    @Test
    fun `map gesture stays dirty until explicit research refresh succeeds`() = runTest(dispatcher) {
        val deps = Dependencies()
        val page = CursorPage(listOf(summary(1)), false, null, 1)
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(page)
        coEvery { deps.refreshPlaces(query(), null, 20) } returnsMany listOf(
            Result.failure(IllegalStateException("offline")),
            Result.success(page),
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.MapGestured)
        vm.onIntent(HomeIntent.ResearchClicked(query()))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isMapSearchDirty)

        vm.onIntent(HomeIntent.ResearchClicked(query()))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isMapSearchDirty)
    }

    @Test
    fun `research opens the partial list from navigation`() = runTest(dispatcher) {
        val deps = Dependencies()
        val page = CursorPage(listOf(summary(1)), false, null, 1)
        coEvery { deps.getPlaces(query(), null, 20) } returns Result.success(page)
        coEvery { deps.refreshPlaces(query(), null, 20) } returns Result.success(page)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ResearchClicked(query()))

        assertEquals(HomeSurfaceState.PartialList, vm.uiState.value.surfaceState)
        advanceUntilIdle()
        assertEquals(HomeListState.Content, vm.uiState.value.listState)
    }

    @Test
    fun `surface transitions navigation partial full partial navigation`() {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.ListOpenClicked)
        assertEquals(HomeSurfaceState.PartialList, vm.uiState.value.surfaceState)
        vm.onIntent(HomeIntent.ListSheetSettled(HomeSurfaceState.FullList))
        assertEquals(HomeSurfaceState.FullList, vm.uiState.value.surfaceState)
        vm.onIntent(HomeIntent.ListCollapseRequested)
        assertEquals(HomeSurfaceState.PartialList, vm.uiState.value.surfaceState)
        vm.onIntent(HomeIntent.ListCollapseRequested)
        assertEquals(HomeSurfaceState.Navigation, vm.uiState.value.surfaceState)
    }

    @Test
    fun `review update sends a refresh effect each time`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()

        vm.effect.test {
            vm.onIntent(HomeIntent.ReviewUpdated)
            vm.onIntent(HomeIntent.ReviewUpdated)

            assertEquals(HomeEffect.RefreshReviews, awaitItem())
            assertEquals(HomeEffect.RefreshReviews, awaitItem())
        }
    }

    @Test
    fun `sheet dragged from full straight to hidden lands on navigation`() {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.ListSheetSettled(HomeSurfaceState.FullList))
        vm.onIntent(HomeIntent.ListSheetSettled(HomeSurfaceState.Navigation))

        assertEquals(HomeSurfaceState.Navigation, vm.uiState.value.surfaceState)
    }

    @Test
    fun `late list sheet settle event does not override an open detail`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 40L)
        coEvery { deps.getDetail(40L) } returns Result.success(place)
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(40L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.ListSheetSettled(HomeSurfaceState.Navigation))

        assertEquals(HomeSurfaceState.Detail, vm.uiState.value.surfaceState)
        assertEquals(40L, vm.uiState.value.selectedPlaceId)
        assertEquals(place, vm.uiState.value.selectedPlace)
        assertFalse(vm.uiState.value.isDetailLoading)
    }

    @Test
    fun `guest detail action resumes exactly once after login`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(LoginResult.Success(false, "로디"))
        coEvery { deps.authSession() } returnsMany listOf(
            AuthSession(false, false),
            AuthSession(true, true),
        )
        coEvery { deps.getDetail(10L) } returns Result.success(HomePreviewData.courseDetail.copy(id = 10L))
        coEvery { deps.getRoute(any<PlaceDetail>()) } returns
            Result.failure(IllegalStateException("route unavailable"))
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.PlaceClicked(10L, HomeDetailOrigin.Map))
        advanceUntilIdle()
        assertEquals(PendingHomeAction.OpenDetail(10L, HomeDetailOrigin.Map), vm.uiState.value.pendingAction)

        vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingAction)
        assertEquals(10L, vm.uiState.value.selectedPlaceId)
        coVerify(exactly = 1) { deps.getDetail(10L) }
    }

    @Test
    fun `search opens login flow and navigates after existing member login`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(LoginResult.Success(false, "로디"))
        val vm = deps.viewModel()
        val origin = GeoPoint(37.5, 126.9)

        vm.onIntent(HomeIntent.SearchClicked(origin))
        advanceUntilIdle()
        assertEquals(PendingHomeAction.OpenSearch(origin), vm.uiState.value.pendingAction)

        vm.effect.test {
            vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
            advanceUntilIdle()

            assertEquals(HomeEffect.NavigateSearch(origin), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `guest registration action uses the existing login gate and resumes after login`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(LoginResult.Success(false, "로디"))
        coEvery { deps.authSession() } returnsMany listOf(
            AuthSession(false, false),
            AuthSession(true, true),
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.RegisterClicked)
        advanceUntilIdle()

        assertEquals(PendingHomeAction.OpenCourseRegistration, vm.uiState.value.pendingAction)
        vm.effect.test {
            vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
            advanceUntilIdle()

            assertEquals(HomeEffect.NavigateCourseRegistration, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `guest new member navigates to sign up without resuming pending action`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns
            Result.success(LoginResult.Success(true, "로디"))
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.MyPageClicked)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
            advanceUntilIdle()

            assertEquals(HomeEffect.NavigateGuestSignUp, awaitItem())
            assertNull(vm.uiState.value.pendingAction)
            expectNoEvents()
        }
    }

    @Test
    fun `withdrawal pending credential stays private until account restore succeeds`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(
            LoginResult.WithdrawalPending(
                withdrawalRequestedAt = Instant.parse("2026-07-13T00:00:00Z"),
                recoverableUntil = Instant.parse("2026-07-16T00:00:00Z"),
            ),
        )
        coEvery { deps.restoreWithKakao("credential") } returns Result.success(
            AccountRestoreResult.Restored(isNewMember = false, nickname = "로디"),
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.MyPageClicked)
        advanceUntilIdle()
        vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasPendingRestore)
        vm.onIntent(HomeIntent.AccountRestoreClicked)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasPendingRestore)
        assertNull(vm.uiState.value.pendingAction)
        coVerify(exactly = 1) { deps.restoreWithKakao("credential") }
    }

    @Test
    fun `drag dismiss from list clears course detail and always returns to navigation`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.courseDetail.copy(id = 30L)
        val route = RouteResult(
            points = listOf(GeoPoint(37.5, 126.9), GeoPoint(37.6, 127.0)),
            isRealRoute = true,
        )
        coEvery { deps.getDetail(30L) } returns Result.success(place)
        coEvery { deps.getRoute(place) } returns Result.success(route)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.PlaceClicked(30L, HomeDetailOrigin.List))
        advanceUntilIdle()
        assertEquals(route, vm.uiState.value.selectedRoute)

        vm.onIntent(HomeIntent.DetailDragDismissed)

        assertEquals(HomeSurfaceState.Navigation, vm.uiState.value.surfaceState)
        assertNull(vm.uiState.value.selectedPlaceId)
        assertNull(vm.uiState.value.selectedPlace)
        assertNull(vm.uiState.value.selectedRoute)
        assertNull(vm.uiState.value.detailOrigin)
        assertFalse(vm.uiState.value.isDetailLoading)
        assertFalse(vm.uiState.value.isRouting)
        assertFalse(vm.uiState.value.isBookmarkUpdating)
    }

    @Test
    fun `drag dismiss cancels parking detail loading and clears selection`() = runTest(dispatcher) {
        val deps = Dependencies()
        val detailResult = CompletableDeferred<Result<PlaceDetail>>()
        var detailRequestCancelled = false
        coEvery { deps.getDetail(31L) } coAnswers {
            try {
                detailResult.await()
            } catch (error: CancellationException) {
                detailRequestCancelled = true
                throw error
            }
        }
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.PlaceClicked(31L, HomeDetailOrigin.List))
        runCurrent()
        assertTrue(vm.uiState.value.isDetailLoading)

        vm.onIntent(HomeIntent.DetailDragDismissed)
        runCurrent()

        assertEquals(HomeSurfaceState.Navigation, vm.uiState.value.surfaceState)
        assertNull(vm.uiState.value.selectedPlaceId)
        assertNull(vm.uiState.value.selectedPlace)
        assertNull(vm.uiState.value.detailOrigin)
        assertFalse(vm.uiState.value.isDetailLoading)
        assertTrue(detailRequestCancelled)
    }

    @Test
    fun `drag dismiss clears parking bookmark update state`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 32L, isBookmarked = false)
        val bookmarkResult = CompletableDeferred<Result<Unit>>()
        coEvery { deps.getDetail(32L) } returns Result.success(place)
        coEvery { deps.setBookmark(place, true) } coAnswers { bookmarkResult.await() }
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(32L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.BookmarkClicked)
        runCurrent()
        assertTrue(vm.uiState.value.isBookmarkUpdating)

        vm.onIntent(HomeIntent.DetailDragDismissed)

        assertEquals(HomeSurfaceState.Navigation, vm.uiState.value.surfaceState)
        assertNull(vm.uiState.value.selectedPlaceId)
        assertNull(vm.uiState.value.selectedPlace)
        assertFalse(vm.uiState.value.isBookmarkUpdating)

        bookmarkResult.complete(Result.success(Unit))
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedPlace)
    }

    @Test
    fun `regular dismiss from list keeps partial list destination`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 33L)
        coEvery { deps.getDetail(33L) } returns Result.success(place)
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(33L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.DetailDismissed)

        assertEquals(HomeSurfaceState.PartialList, vm.uiState.value.surfaceState)
        assertNull(vm.uiState.value.selectedPlaceId)
        assertNull(vm.uiState.value.selectedPlace)
        assertNull(vm.uiState.value.detailOrigin)
    }

    @Test
    fun `bookmark state changes only after server success`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 20L, isBookmarked = false, bookmarkCount = 4)
        coEvery { deps.getDetail(20L) } returns Result.success(place)
        coEvery { deps.setBookmark(place, true) } returns Result.success(Unit)
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(20L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.BookmarkClicked)
        advanceUntilIdle()

        assertTrue(requireNotNull(vm.uiState.value.selectedPlace).isBookmarked)
        assertEquals(5, vm.uiState.value.selectedPlace?.bookmarkCount)
    }

    @Test
    fun `bookmark failure preserves ui state`() = runTest(dispatcher) {
        val deps = Dependencies()
        val place = HomePreviewData.parkingDetail.copy(id = 20L, isBookmarked = false, bookmarkCount = 4)
        coEvery { deps.getDetail(20L) } returns Result.success(place)
        coEvery { deps.setBookmark(place, true) } returns Result.failure(IllegalStateException("failure"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(20L, HomeDetailOrigin.List))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.BookmarkClicked)
        advanceUntilIdle()

        assertFalse(requireNotNull(vm.uiState.value.selectedPlace).isBookmarked)
        assertEquals(4, vm.uiState.value.selectedPlace?.bookmarkCount)
    }

    @Test
    fun `filter keeps mixed tags across categories and saves them together`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.updateFilterTags(any()) } returns Result.success(Unit)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.FilterOpened)
        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.ALL))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.URBAN_BASICS))
        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.INTERSECTION))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.PARKING))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.URBAN_BASICS))
        vm.onIntent(HomeIntent.FilterApplyClicked)
        advanceUntilIdle()

        assertEquals(
            setOf(
                PracticeType.STRAIGHT,
                PracticeType.LEFT_RIGHT_TURN,
                PracticeType.LANE_CHANGE,
                PracticeType.INTERSECTION,
                PracticeType.PARKING,
            ),
            vm.uiState.value.selectedFilterPracticeTypes,
        )
        assertFalse(vm.uiState.value.isFilterSheetVisible)
        coVerify {
            deps.updateFilterTags(
                setOf(
                    PracticeType.STRAIGHT,
                    PracticeType.LEFT_RIGHT_TURN,
                    PracticeType.LANE_CHANGE,
                    PracticeType.INTERSECTION,
                    PracticeType.PARKING,
                ),
            )
        }
    }

    @Test
    fun `tapping an active category clears it without clearing selected practice types`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.STRAIGHT))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.BASIC_DRIVING))

        assertNull(vm.uiState.value.activeFilterCategory)
        assertEquals(setOf(PracticeType.STRAIGHT), vm.uiState.value.selectedFilterPracticeTypes)
    }

    @Test
    fun `tapping parking twice removes its tag and clears the active category`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.PARKING))
        assertEquals(FilterCategory.PARKING, vm.uiState.value.activeFilterCategory)
        assertEquals(setOf(PracticeType.PARKING), vm.uiState.value.selectedFilterPracticeTypes)

        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.PARKING))

        assertNull(vm.uiState.value.activeFilterCategory)
        assertTrue(vm.uiState.value.selectedFilterPracticeTypes.isEmpty())
    }

    @Test
    fun `moving from parking keeps its tag but activates only the new category`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.PARKING))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.ROAD_FLOW))
        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.MERGING))

        assertEquals(FilterCategory.ROAD_FLOW, vm.uiState.value.activeFilterCategory)
        assertEquals(
            setOf(PracticeType.PARKING, PracticeType.MERGING),
            vm.uiState.value.selectedFilterPracticeTypes,
        )
    }

    @Test
    fun `reset activates basic driving and clears all filter tags`() = runTest(dispatcher) {
        val vm = Dependencies().viewModel()

        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.PARKING))
        vm.onIntent(HomeIntent.FilterCategorySelected(FilterCategory.ROAD_FLOW))
        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.MERGING))
        vm.onIntent(HomeIntent.FilterResetClicked)

        assertEquals(FilterCategory.BASIC_DRIVING, vm.uiState.value.activeFilterCategory)
        assertTrue(vm.uiState.value.selectedFilterPracticeTypes.isEmpty())
    }

    @Test
    fun `reset is ignored while filter tags are saving`() = runTest(dispatcher) {
        val deps = Dependencies()
        val saveResult = CompletableDeferred<Result<Unit>>()
        coEvery { deps.updateFilterTags(setOf(PracticeType.STRAIGHT)) } coAnswers { saveResult.await() }
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.STRAIGHT))
        vm.onIntent(HomeIntent.FilterApplyClicked)
        runCurrent()
        vm.onIntent(HomeIntent.FilterResetClicked)

        assertTrue(vm.uiState.value.isFilterSaving)
        assertEquals(setOf(PracticeType.STRAIGHT), vm.uiState.value.selectedFilterPracticeTypes)
    }

    @Test
    fun `successful filter save reloads the current home viewport`() = runTest(dispatcher) {
        val deps = Dependencies()
        val initialPage = CursorPage(listOf(summary(1)), false, null, 1)
        val filteredPage = CursorPage(listOf(summary(2)), false, null, 1)
        coEvery { deps.getPlaces(query(), null, 20) } returnsMany listOf(
            Result.success(initialPage),
            Result.success(filteredPage),
        )
        coEvery { deps.updateFilterTags(setOf(PracticeType.STRAIGHT)) } returns Result.success(Unit)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.ViewportSettled(query()))
        advanceUntilIdle()
        vm.onIntent(HomeIntent.FilterOpened)
        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.STRAIGHT))
        vm.onIntent(HomeIntent.FilterApplyClicked)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isFilterSheetVisible)
        assertEquals(listOf(2L), vm.uiState.value.places.map { it.id })
        coVerify(exactly = 2) { deps.getPlaces(query(), null, 20) }
    }

    @Test
    fun `guest filter save resumes after existing member login`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        coEvery { deps.loginWithKakao("credential") } returns Result.success(LoginResult.Success(false, "로디"))
        coEvery { deps.updateFilterTags(any()) } returns Result.success(Unit)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.FilterPracticeOptionToggled(FilterPracticeOption.STRAIGHT))
        vm.onIntent(HomeIntent.FilterApplyClicked)
        advanceUntilIdle()
        assertEquals(
            PendingHomeAction.SaveFilterTags(setOf(PracticeType.STRAIGHT)),
            vm.uiState.value.pendingAction,
        )

        vm.onIntent(HomeIntent.KakaoLoginSucceeded("credential"))
        advanceUntilIdle()

        coVerify { deps.updateFilterTags(setOf(PracticeType.STRAIGHT)) }
    }

    @Test
    fun `resuming before ten minutes shows continue dialog from local session`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:09:59Z"))
        coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isPracticeContinueDialogVisible)
        assertNull(vm.uiState.value.practicePrompt)
        coVerify(exactly = 1) { deps.getActiveSession() }
    }

    @Test
    fun `GPS-confirmed arrival shows the visit prompt even before ten minutes`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:03:00Z"))
        val session = activeSession(
            startedAt = Instant.parse("2026-08-15T00:00:00Z"),
            isArrivalConfirmed = true,
        )
        coEvery { deps.getActiveSession() } returns session
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertEquals(session.placeId, vm.uiState.value.practicePrompt?.placeId)
    }

    @Test
    fun `unmeasured session shows nothing before ten minutes`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:05:00Z"))
        coEvery { deps.getActiveSession() } returns activeSession(
            startedAt = Instant.parse("2026-08-15T00:00:00Z"),
            isMeasured = false,
        )
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        // 알림 미허용으로 측정 자체가 없었던 세션은 "계속 측정할까요?"를 물을 이유가 없다.
        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertNull(vm.uiState.value.practicePrompt)
    }

    @Test
    fun `unmeasured session shows the visit prompt after ten minutes`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(
            startedAt = Instant.parse("2026-08-15T00:00:00Z"),
            isMeasured = false,
        )
        coEvery { deps.getActiveSession() } returns session
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertEquals(session.placeId, vm.uiState.value.practicePrompt?.placeId)
    }

    @Test
    fun `visited flow sends the recognized driving distance only when arrival was GPS-confirmed`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(
            startedAt = Instant.parse("2026-08-15T00:00:00Z"),
            practiceId = 108L,
            isArrivalConfirmed = true,
        )
        coEvery { deps.getActiveSession() } returns session
        every { deps.observeDrivingSession() } returns flowOf(
            DrivingSession(
                id = "driving-1",
                placeId = session.placeId,
                placeName = session.placeName,
                destination = GeoPoint(37.5, 127.0),
                plannedDistanceMeters = 1_000,
                startedAtEpochMillis = 0L,
                arrivedAtEpochMillis = 1_000L,
                traveledDistanceMeters = 412.0,
                status = DrivingSessionStatus.ARRIVED,
            ),
        )
        coEvery { deps.recordPracticeVisit(108L, 412) } returns Result.success(visitResult())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.recordPracticeVisit(108L, 412) }
    }

    @Test
    fun `exactly ten minutes shows visited prompt`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertEquals(session.placeId, vm.uiState.value.practicePrompt?.placeId)
        assertEquals(session.placeName, vm.uiState.value.practicePrompt?.placeName)
    }

    @Test
    fun `continue hides early dialog but keeps session for the next resume`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:05:00Z"))
        coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()
        vm.onIntent(HomeIntent.PracticeContinueClicked)
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isPracticeContinueDialogVisible)
        coVerify(exactly = 2) { deps.getActiveSession() }
        coVerify(exactly = 0) { deps.clearActiveSession() }
    }

    @Test
    fun `stop clears only local session and never calls practice APIs`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:05:00Z"))
        coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.PracticeStopClicked)
            advanceUntilIdle()
            assertEquals(HomeEffect.StopDrivingTracking, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.uiState.value.activePracticeSession)
        assertNull(vm.uiState.value.practicePrompt)
        coVerify(exactly = 1) { deps.clearActiveSession() }
        coVerify(exactly = 0) { deps.registerPractice(any()) }
        coVerify(exactly = 0) { deps.recordPracticeVisit(any(), any()) }
    }

    @Test
    fun `navigating to a different place while a session is active shows continue dialog instead of switching`() =
        runTest(dispatcher) {
            val deps = Dependencies(clockAt("2026-08-15T00:05:00Z"))
            val otherPlace = navigationPlace().copy(id = 20L, name = "다른 코스")
            coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
            coEvery { deps.getDetail(20L) } returns Result.success(otherPlace)
            val vm = deps.viewModel()
            vm.onIntent(HomeIntent.AppResumed)
            advanceUntilIdle()
            vm.onIntent(HomeIntent.PracticeContinueClicked)
            vm.onIntent(HomeIntent.PlaceClicked(20L, HomeDetailOrigin.Map))
            advanceUntilIdle()

            vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = true))
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPracticeContinueDialogVisible)
            assertEquals(27L, vm.uiState.value.activePracticeSession?.placeId)
            coVerify(exactly = 0) { deps.saveActiveSession(any()) }
            coVerify(exactly = 0) { deps.clearActiveSession() }
        }

    @Test
    fun `ending measurement to switch places stops the old session and starts the new one`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:05:00Z"))
        val otherPlace = navigationPlace().copy(id = 20L, name = "다른 코스")
        coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getDetail(20L) } returns Result.success(otherPlace)
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()
        vm.onIntent(HomeIntent.PracticeContinueClicked)
        vm.onIntent(HomeIntent.PlaceClicked(20L, HomeDetailOrigin.Map))
        advanceUntilIdle()
        vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = true))
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.PracticeStopClicked)
            advanceUntilIdle()
            assertEquals(HomeEffect.StopDrivingTracking, awaitItem())
            assertEquals(HomeEffect.LaunchKakaoMap(otherPlace), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertEquals(20L, vm.uiState.value.activePracticeSession?.placeId)
        coVerify(exactly = 1) { deps.clearActiveSession() }
        coVerify(exactly = 1) { deps.saveActiveSession(any()) }
    }

    @Test
    fun `dismissed practice prompt does not come back on the next resume`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        coEvery { deps.getActiveSession() } returns activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()
        assertEquals(27L, vm.uiState.value.practicePrompt?.placeId)

        vm.onIntent(HomeIntent.PracticePromptDismissed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.practicePrompt)
        coVerify(exactly = 1) { deps.clearActiveSession() }

        coEvery { deps.getActiveSession() } returns null
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.practicePrompt)
        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
    }

    @Test
    fun `first navigation shows rationale before system permission and does not register`() = runTest(dispatcher) {
        val deps = Dependencies()
        every { deps.notificationRequested() } returns flowOf(false)
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.onIntent(
            HomeIntent.NavigateClicked(
                kakaoMapInstalled = true,
                kakaoNaviInstalled = false,
                notificationPermissionGranted = false,
            ),
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isNotificationPermissionRationaleVisible)
        assertEquals(NaviApp.KAKAOMAP, vm.uiState.value.pendingPracticeNavigation?.app)
        coVerify(exactly = 0) { deps.registerPractice(any()) }
        coVerify(exactly = 0) { deps.saveActiveSession(any()) }
    }

    @Test
    fun `route only decision launches navigation and saves an unmeasured session for reentry`() = runTest(dispatcher) {
        val deps = Dependencies()
        val notificationRequested = MutableStateFlow(false)
        every { deps.notificationRequested() } returns notificationRequested
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = false))
            advanceUntilIdle()
            vm.onIntent(HomeIntent.NotificationPermissionRouteOnlyClicked)
            advanceUntilIdle()
            assertEquals(
                HomeEffect.LaunchKakaoMap(navigationPlace(), startDriving = false),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        // 측정은 안 하지만, 10분 뒤 재진입 시 RV-01을 띄우려면 isMeasured=false 세션은 남아있어야 한다.
        assertFalse(vm.uiState.value.activePracticeSession?.isMeasured ?: true)
        assertFalse(vm.uiState.value.isPracticeContinueDialogVisible)
        assertFalse(vm.uiState.value.isNotificationPermissionRationaleVisible)
        coVerify(exactly = 1) { deps.saveActiveSession(any()) }
        coVerify(exactly = 0) { deps.markNotificationRequested() }

        vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = false))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isNotificationPermissionRationaleVisible)
        assertEquals(NaviApp.KAKAOMAP, vm.uiState.value.pendingPracticeNavigation?.app)
        coVerify(exactly = 0) { deps.markNotificationRequested() }
    }

    @Test
    fun `denying notification permission routes and still saves an unmeasured session`() = runTest(dispatcher) {
        val deps = Dependencies()
        val notificationRequested = MutableStateFlow(false)
        every { deps.notificationRequested() } returns notificationRequested
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = false))
            advanceUntilIdle()
            vm.onIntent(HomeIntent.NotificationPermissionAllowClicked)
            advanceUntilIdle()
            vm.onIntent(HomeIntent.NotificationPermissionResultReceived(granted = false))
            advanceUntilIdle()

            assertEquals(
                HomeEffect.LaunchKakaoMap(navigationPlace(), startDriving = false),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(vm.uiState.value.activePracticeSession?.isMeasured ?: true)
        assertFalse(vm.uiState.value.isNotificationPermissionRationaleVisible)
        assertNull(vm.uiState.value.pendingPracticeNavigation)
        assertFalse(vm.uiState.value.isPracticeLaunchInProgress)
        coVerify(exactly = 1) { deps.saveActiveSession(any()) }
        coVerify(exactly = 1) { deps.markNotificationRequested() }
    }

    @Test
    fun `allowing notification permission starts local session after permission callback`() = runTest(dispatcher) {
        val start = Instant.parse("2026-08-15T00:00:00Z")
        val deps = Dependencies(Clock.fixed(start, ZoneOffset.UTC))
        val notificationRequested = MutableStateFlow(false)
        every { deps.notificationRequested() } returns notificationRequested
        coEvery { deps.markNotificationRequested() } coAnswers {
            notificationRequested.value = true
        }
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()
        vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = false))
        advanceUntilIdle()

        vm.permissionEffect.test {
            vm.onIntent(HomeIntent.NotificationPermissionAllowClicked)
            advanceUntilIdle()
            assertEquals(HomePermissionEffect.RequestNotificationPermission, awaitItem())
            coVerify(exactly = 0) { deps.saveActiveSession(any()) }
            vm.onIntent(HomeIntent.NotificationPermissionResultReceived(granted = true))
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(start, vm.uiState.value.activePracticeSession?.startedAt)
        coVerify(exactly = 1) { deps.saveActiveSession(any()) }
        coVerify(exactly = 1) { deps.markNotificationRequested() }

        vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = true))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isNotificationPermissionRationaleVisible)
        assertNull(vm.uiState.value.pendingPracticeNavigation)
        coVerify(exactly = 1) { deps.markNotificationRequested() }
    }

    @Test
    fun `session save failure routes without starting a local measurement`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        coEvery { deps.saveActiveSession(any()) } throws IllegalStateException("저장 실패")
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = true, kakaoNaviInstalled = false, notificationPermissionGranted = true))
            advanceUntilIdle()

            assertEquals(
                HomeEffect.ShowSnackbar("연습 측정을 시작하지 못해 경로만 안내합니다."),
                awaitItem(),
            )
            assertEquals(
                HomeEffect.LaunchKakaoMap(navigationPlace(), startDriving = false),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.uiState.value.activePracticeSession)
        assertNull(vm.uiState.value.pendingPracticeNavigation)
        coVerify(exactly = 3) { deps.saveActiveSession(any()) }
    }

    @Test
    fun `successful visit registers then visits and opens review for a course`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(101L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.recordPracticeVisit(101L, any()) } returns Result.success(visitResult())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.PracticeVisitedAnswered)
            advanceUntilIdle()
            assertEquals(HomeEffect.StopDrivingTracking, awaitItem())
            assertEquals(HomeEffect.OpenPracticeReview(session.placeId, session.placeName), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.uiState.value.activePracticeSession)
        coVerifyOrder {
            deps.registerPractice(session.placeId)
            deps.saveActiveSession(any())
            deps.recordPracticeVisit(101L, any())
            deps.saveActiveSession(any())
            deps.clearActiveSession()
        }
    }

    @Test
    fun `session persistence failure after register does not abandon the immediate visit`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(104L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.saveActiveSession(any()) } throws IllegalStateException("저장 실패")
        coEvery { deps.recordPracticeVisit(104L, any()) } returns Result.success(visitResult())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.registerPractice(session.placeId) }
        coVerify(exactly = 1) { deps.recordPracticeVisit(104L, any()) }
        assertNull(vm.uiState.value.activePracticeSession)
    }

    @Test
    fun `registered id stays in memory when persistence and visit both fail`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(106L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.saveActiveSession(any()) } throws IllegalStateException("저장 실패")
        coEvery { deps.recordPracticeVisit(106L, any()) } returnsMany listOf(
            Result.failure(IllegalStateException("방문 기록 실패")),
            Result.success(visitResult()),
        )
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        assertEquals(106L, vm.uiState.value.activePracticeSession?.practiceId)
        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.registerPractice(session.placeId) }
        coVerify(exactly = 2) { deps.recordPracticeVisit(106L, any()) }
        assertNull(vm.uiState.value.activePracticeSession)
    }

    @Test
    fun `transient clear failure is retried after a successful visit`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        var clearCalls = 0
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(105L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.recordPracticeVisit(105L, any()) } returns Result.success(visitResult())
        coEvery { deps.clearActiveSession() } coAnswers {
            clearCalls += 1
            if (clearCalls == 1) throw IllegalStateException("일시적인 저장 실패")
        }
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        assertEquals(2, clearCalls)
        assertNull(vm.uiState.value.activePracticeSession)
    }

    @Test
    fun `completed marker prevents duplicate visit when local clear keeps failing`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val originalSession = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns originalSession
        coEvery { deps.registerPractice(originalSession.placeId) } returns Result.success(
            Practice(107L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.saveActiveSession(any()) } returns Unit
        coEvery { deps.recordPracticeVisit(107L, any()) } returns Result.success(visitResult())
        coEvery { deps.clearActiveSession() } throws IllegalStateException("정리 실패")
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        val completedSession = originalSession.copy(practiceId = 107L, isCompleted = true)
        coVerify(exactly = 1) { deps.saveActiveSession(completedSession) }
        assertNull(vm.uiState.value.activePracticeSession)
        coEvery { deps.getActiveSession() } returns completedSession
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.activePracticeSession)
        assertNull(vm.uiState.value.practicePrompt)
        coVerify(exactly = 1) { deps.recordPracticeVisit(107L, any()) }
    }

    @Test
    fun `failed visit keeps registered id and retries without registering again`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(101L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.recordPracticeVisit(101L, any()) } returnsMany listOf(
            Result.failure(IllegalStateException("실패")),
            Result.success(visitResult()),
        )
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()
        assertEquals(101L, vm.uiState.value.activePracticeSession?.practiceId)
        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        advanceUntilIdle()

        coVerify(exactly = 1) { deps.registerPractice(session.placeId) }
        coVerify(exactly = 2) { deps.recordPracticeVisit(101L, any()) }
        assertNull(vm.uiState.value.activePracticeSession)
    }

    @Test
    fun `parking visit clears session without opening review`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(
            startedAt = Instant.parse("2026-08-15T00:00:00Z"),
            placeType = PlaceType.PARKING,
        )
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(102L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.recordPracticeVisit(102L, any()) } returns Result.success(visitResult())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(HomeIntent.PracticeVisitedAnswered)
            advanceUntilIdle()

            assertEquals(HomeEffect.StopDrivingTracking, awaitItem())
            assertEquals(
                HomeEffect.ShowSnackbar("연습 기록에 추가되었습니다"),
                awaitItem(),
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.uiState.value.activePracticeSession)
    }

    @Test
    fun `not visited registers the practice and opens skip reason`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(108L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()
        assertEquals(session.placeId, vm.uiState.value.practicePrompt?.placeId)

        vm.effect.test {
            vm.onIntent(HomeIntent.PracticeNotVisitedAnswered)
            advanceUntilIdle()

            assertEquals(HomeEffect.StopDrivingTracking, awaitItem())
            assertEquals(HomeEffect.OpenPracticeSkipReason(108L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.uiState.value.activePracticeSession)
        assertNull(vm.uiState.value.practicePrompt)
        coVerify(exactly = 1) { deps.registerPractice(session.placeId) }
        coVerify(exactly = 1) { deps.clearActiveSession() }
    }

    @Test
    fun `duplicate visited taps while request is in flight perform one request`() = runTest(dispatcher) {
        val deps = Dependencies(clockAt("2026-08-15T00:10:00Z"))
        val session = activeSession(startedAt = Instant.parse("2026-08-15T00:00:00Z"))
        val pendingVisit = CompletableDeferred<Result<PracticeVisitResult>>()
        coEvery { deps.getActiveSession() } returns session
        coEvery { deps.registerPractice(session.placeId) } returns Result.success(
            Practice(103L, com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED, 0, 0),
        )
        coEvery { deps.recordPracticeVisit(103L, any()) } coAnswers { pendingVisit.await() }
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        vm.onIntent(HomeIntent.PracticeVisitedAnswered)
        runCurrent()
        coVerify(exactly = 1) { deps.recordPracticeVisit(103L, any()) }
        pendingVisit.complete(Result.success(visitResult()))
        advanceUntilIdle()
    }

    @Test
    fun `guest resume does not read an active session`() = runTest(dispatcher) {
        val deps = Dependencies(loggedIn = false)
        val vm = deps.viewModel()

        vm.onIntent(HomeIntent.AppResumed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.activePracticeSession)
        coVerify(exactly = 0) { deps.getActiveSession() }
    }

    @Test
    fun `install picker does not start a local practice session`() = runTest(dispatcher) {
        val deps = Dependencies()
        coEvery { deps.getDetail(19L) } returns Result.success(navigationPlace())
        val vm = deps.viewModel()
        vm.onIntent(HomeIntent.PlaceClicked(19L, HomeDetailOrigin.Map))
        advanceUntilIdle()

        vm.onIntent(HomeIntent.NavigateClicked(kakaoMapInstalled = false, kakaoNaviInstalled = false, notificationPermissionGranted = false))
        advanceUntilIdle()

        coVerify(exactly = 0) { deps.saveActiveSession(any()) }
    }

}

private class Dependencies(
    val clock: Clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC),
    loggedIn: Boolean = true,
) {
    val coordinates = mockk<GetPlaceCoordinatesUseCase>()
    val refreshCoordinates = mockk<RefreshPlaceCoordinatesUseCase>()
    val getPlaces = mockk<GetPlacesUseCase>()
    val refreshPlaces = mockk<RefreshPlacesUseCase>()
    val getDetail = mockk<GetPlaceDetailUseCase>()
    val setBookmark = mockk<SetPlaceBookmarkUseCase>()
    val getRoute = mockk<GetRouteUseCase>()
    val authSession = mockk<GetAuthSessionUseCase>()
    val loginWithKakao = mockk<LoginWithKakaoUseCase>()
    val restoreWithKakao = mockk<RestoreWithKakaoUseCase>()
    val getNaviAlways = mockk<GetNaviAlwaysUseCase>()
    val setNaviAlways = mockk<SetNaviAlwaysUseCase>()
    val updateFilterTags = mockk<UpdateFilterTagsUseCase>()
    val registerPractice = mockk<RegisterPracticeUseCase>()
    val recordPracticeVisit = mockk<RecordPracticeVisitUseCase>()
    val getActiveSession = mockk<GetActivePracticeSessionUseCase>()
    val saveActiveSession = mockk<SaveActivePracticeSessionUseCase>()
    val clearActiveSession = mockk<ClearActivePracticeSessionUseCase>()
    val observeDrivingSession = mockk<ObserveDrivingSessionUseCase>()
    val notificationRequested = mockk<GetNotificationPermissionRequestedUseCase>()
    val markNotificationRequested = mockk<MarkNotificationPermissionRequestedUseCase>()

    init {
        coEvery { coordinates() } returns Result.success(emptyList())
        coEvery { refreshCoordinates() } returns Result.success(emptyList())
        coEvery { refreshPlaces(any(), any(), any()) } returns Result.failure(IllegalStateException("offline"))
        coEvery { authSession() } returns AuthSession(loggedIn, false)
        coEvery { getNaviAlways() } returns null
        coEvery { setNaviAlways(any()) } returns Unit
        coEvery { registerPractice(any()) } returns Result.success(
            Practice(
                practiceId = 19L,
                status = com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED,
                visitCount = 0,
                requiredDistanceMeters = 0,
            ),
        )
        coEvery { recordPracticeVisit(any(), any()) } returns Result.success(visitResult())
        coEvery { getActiveSession() } returns null
        coEvery { saveActiveSession(any()) } returns Unit
        coEvery { clearActiveSession() } returns Unit
        every { observeDrivingSession() } returns flowOf(null)
        every { notificationRequested() } returns flowOf(true)
        coEvery { markNotificationRequested() } returns Unit
    }

    fun viewModel() = HomeViewModel(
        getPlaceCoordinatesUseCase = coordinates,
        refreshPlaceCoordinatesUseCase = refreshCoordinates,
        getPlacesUseCase = getPlaces,
        refreshPlacesUseCase = refreshPlaces,
        getPlaceDetailUseCase = getDetail,
        getRouteUseCase = getRoute,
        setPlaceBookmarkUseCase = setBookmark,
        getAuthSessionUseCase = authSession,
        loginWithKakaoUseCase = loginWithKakao,
        restoreWithKakaoUseCase = restoreWithKakao,
        getNaviAlwaysUseCase = getNaviAlways,
        setNaviAlwaysUseCase = setNaviAlways,
        updateFilterTagsUseCase = updateFilterTags,
        registerPracticeUseCase = registerPractice,
        recordPracticeVisitUseCase = recordPracticeVisit,
        getActivePracticeSessionUseCase = getActiveSession,
        saveActivePracticeSessionUseCase = saveActiveSession,
        clearActivePracticeSessionUseCase = clearActiveSession,
        observeDrivingSessionUseCase = observeDrivingSession,
        markNotificationPermissionRequestedUseCase = markNotificationRequested,
        clock = clock,
    )
}

private fun query(offset: Double = 0.0) = PlaceViewportQuery(
    southWest = GeoPoint(37.0 + offset, 126.0 + offset),
    northEast = GeoPoint(38.0 + offset, 127.0 + offset),
    origin = GeoPoint(37.5 + offset, 126.5 + offset),
)

private fun activeSession(
    startedAt: Instant,
    placeType: PlaceType = PlaceType.COURSE,
    practiceId: Long? = null,
    isArrivalConfirmed: Boolean = false,
    isMeasured: Boolean = true,
) = ActivePracticeSession(
    placeId = 27L,
    placeName = "강남역 주변 코스",
    placeType = placeType,
    startedAt = startedAt,
    practiceId = practiceId,
    isArrivalConfirmed = isArrivalConfirmed,
    isMeasured = isMeasured,
)

private fun clockAt(value: String): Clock = Clock.fixed(Instant.parse(value), ZoneOffset.UTC)

private fun visitResult(
    levelUp: Boolean = false,
    newLevel: OnboardingLevel? = null,
) = PracticeVisitResult(
    visitCount = 1,
    addedCertifiedDistanceMeters = 0,
    requiredDistanceMeters = 1000,
    isCertifiedNow = false,
    isVerified = false,
    totalDistanceKm = 0.0,
    levelUp = levelUp,
    newLevel = newLevel,
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

private fun navigationPlace() = PlaceDetail(
    id = 19L,
    type = PlaceType.COURSE,
    name = "테스트 연습장",
    address = "서울",
    point = GeoPoint(37.5, 126.5),
    practiceTypes = listOf(PracticeType.STRAIGHT),
    bookmarkCount = 0,
    isBookmarked = false,
    course = null,
    parking = null,
)

private fun parkingNavigationPlace() = navigationPlace().copy(
    type = PlaceType.PARKING,
    practiceTypes = listOf(PracticeType.PARKING),
)
