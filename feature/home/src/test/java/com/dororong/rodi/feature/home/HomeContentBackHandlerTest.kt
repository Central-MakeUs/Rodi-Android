package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.detail.CourseReviewUiState
import com.dororong.rodi.feature.home.map.MapScreenState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class HomeContentBackHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HomeIntent>()
    private var dismissDetailCount = 0
    private var outerBackCount = 0

    @Test
    fun `상세 시트에서 시스템 뒤로가기를 누르면 상세를 닫는다`() {
        setContent(HomeUiState(surfaceState = HomeSurfaceState.Detail, isDetailLoading = true))

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(1, dismissDetailCount)
        assertEquals(emptyList<HomeIntent>(), intents)
        assertEquals(0, outerBackCount)
    }

    @Test
    fun `목록에서 시스템 뒤로가기를 누르면 목록을 접는다`() {
        setContent(HomeUiState(surfaceState = HomeSurfaceState.PartialList))

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(listOf<HomeIntent>(HomeIntent.ListCollapseRequested), intents)
        assertEquals(0, dismissDetailCount)
        assertEquals(0, outerBackCount)
    }

    @Test
    fun `필터 시트가 열려 있을 때 시스템 뒤로가기를 누르면 필터를 닫는다`() {
        setContent(
            HomeUiState(surfaceState = HomeSurfaceState.PartialList, isFilterSheetVisible = true),
        )

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(listOf<HomeIntent>(HomeIntent.FilterDismissed), intents)
        assertEquals(0, outerBackCount)
    }

    /** 저장 중에는 시트를 닫지도, 바깥 핸들러로 넘기지도 않는다 — 뒤로가기를 그대로 삼킨다. */
    @Test
    fun `필터 저장 중 시스템 뒤로가기는 아무것도 바꾸지 않는다`() {
        setContent(
            HomeUiState(
                surfaceState = HomeSurfaceState.PartialList,
                isFilterSheetVisible = true,
                isFilterSaving = true,
            ),
        )

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(emptyList<HomeIntent>(), intents)
        assertEquals(0, dismissDetailCount)
        assertEquals(0, outerBackCount)
    }

    @Test
    fun `지도 화면의 시스템 뒤로가기는 바깥 화면에 맡긴다`() {
        setContent(HomeUiState(surfaceState = HomeSurfaceState.Navigation))

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(1, outerBackCount)
        assertEquals(emptyList<HomeIntent>(), intents)
        assertEquals(0, dismissDetailCount)
    }

    private fun setContent(state: HomeUiState) {
        composeRule.setContent {
            RodiTheme {
                BackHandler { outerBackCount++ }
                val listSheetState = remember { AnchoredDraggableState(ListSheetValue.Hidden) }
                HomeContent(
                    state = state,
                    reviewState = CourseReviewUiState(),
                    overlay = remember { HomeOverlayState() },
                    mapScreenState = MapScreenState.Ready,
                    isAtCurrentLocation = false,
                    snackbarHostState = remember { RodiSnackbarHostState() },
                    sheet = HomeContentSheetLayout(
                        isContainerMeasured = false,
                        listSheetDrag = Modifier,
                        listSheetOffsetPx = { listSheetState.offset },
                        listSheetProgress = { 0f },
                        listHeaderHeightPx = { 0f },
                        listViewportHeightPx = { 0f },
                        bottomControlOffsetPx = { 0f },
                    ),
                    actions = HomeContentActions(
                        onSearchClick = {},
                        onResearchClick = {},
                        onMyLocationClick = {},
                        onRetryPlaces = {},
                        onRetryMap = {},
                        onDismissDetail = { dismissDetailCount++ },
                        onDragDismissDetail = {},
                        onNavigate = {},
                        onLoadReviews = {},
                        onSelectReviewLevel = {},
                        onContainerSizeChanged = {},
                        onBottomNavigationHeightChanged = {},
                        onCourseDetailSheetHeightChanged = {},
                        onParkingSheetMeasured = { _, _ -> },
                    ),
                    onIntent = { intents += it },
                    mapView = {},
                    bottomNavigation = {},
                )
            }
        }
        composeRule.waitForIdle()
    }
}
