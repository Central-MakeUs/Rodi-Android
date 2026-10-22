package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

    @Test
    fun `빈 목록과 조회 오류에서도 필터를 다시 열고 목록을 닫을 수 있다`() {
        val state = mutableStateOf(HomeUiState(surfaceState = HomeSurfaceState.PartialList))
        setContent(state = { state.value }, isContainerMeasured = true)

        listOf(HomeListState.Empty, HomeListState.InitialError).forEach { listState ->
            composeRule.runOnIdle {
                intents.clear()
                state.value = state.value.copy(listState = listState)
            }

            composeRule.onNodeWithText(
                if (listState == HomeListState.Empty) "추천할 수 있는 연습 코스를 찾지 못했어요."
                else "장소를 불러오지 못했어요.",
            ).assertIsDisplayed()
            composeRule.onNodeWithContentDescription("필터")
                .assertIsDisplayed()
                .assertIsEnabled()
                .performTouchInput { click() }
            composeRule.onNodeWithContentDescription("뒤로가기")
                .assertIsDisplayed()
                .assertIsEnabled()
                .performTouchInput { click() }

            composeRule.runOnIdle {
                assertEquals(listOf(HomeIntent.FilterOpened, HomeIntent.ListCollapseRequested), intents)
            }
        }
    }

    private fun setContent(state: HomeUiState) = setContent(state = { state })

    private fun setContent(state: () -> HomeUiState, isContainerMeasured: Boolean = false) {
        composeRule.setContent {
            RodiTheme {
                BackHandler { outerBackCount++ }
                val listSheetState = remember { AnchoredDraggableState(ListSheetValue.Hidden) }
                HomeContent(
                    state = state(),
                    reviewState = CourseReviewUiState(),
                    overlay = remember { HomeOverlayState() },
                    mapScreenState = MapScreenState.Ready,
                    isAtCurrentLocation = false,
                    snackbarHostState = remember { RodiSnackbarHostState() },
                    sheet = HomeContentSheetLayout(
                        isContainerMeasured = isContainerMeasured,
                        listSheetDrag = Modifier,
                        listSheetOffsetPx = { if (isContainerMeasured) 0f else listSheetState.offset },
                        listSheetProgress = { 0f },
                        listHeaderHeightPx = { PARTIAL_LIST_HEADER_HEIGHT.toPx() },
                        listViewportHeightPx = { 380.dp.toPx() },
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
