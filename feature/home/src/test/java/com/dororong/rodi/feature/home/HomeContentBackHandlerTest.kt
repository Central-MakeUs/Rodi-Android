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
    fun `system back on a detail sheet dismisses the detail`() {
        setContent(HomeUiState(surfaceState = HomeSurfaceState.Detail, isDetailLoading = true))

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(1, dismissDetailCount)
        assertEquals(emptyList<HomeIntent>(), intents)
        assertEquals(0, outerBackCount)
    }

    @Test
    fun `system back on the list collapses the list`() {
        setContent(HomeUiState(surfaceState = HomeSurfaceState.PartialList))

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        assertEquals(listOf<HomeIntent>(HomeIntent.OnListCollapse), intents)
        assertEquals(0, dismissDetailCount)
        assertEquals(0, outerBackCount)
    }

    @Test
    fun `system back on the map surface is left to the enclosing screen`() {
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
