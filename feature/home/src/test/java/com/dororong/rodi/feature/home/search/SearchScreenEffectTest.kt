package com.dororong.rodi.feature.home.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import com.dororong.rodi.core.domain.usecase.search.DeleteAllRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteRecentSearchUseCase
import com.dororong.rodi.core.domain.usecase.search.GetRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.RegisterRecentSearchUseCase
import com.dororong.rodi.core.domain.usecase.place.GetRelatedSearchUseCase
import com.dororong.rodi.core.domain.usecase.place.SearchPlacesUseCase
import com.dororong.rodi.core.ui.theme.RodiTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class SearchScreenEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `place tapped while an error snackbar is showing opens without waiting for the snackbar`() {
        val recentRepository = mockk<RecentSearchRepository>(relaxed = true)
        coEvery { recentRepository.getRecentSearches() } returns listOf(
            RecentSearch(id = 1, keyword = UNKNOWN_REGION, type = SearchTargetType.REGION),
            RecentSearch(id = 2, keyword = PLACE_KEYWORD, type = SearchTargetType.PLACE, placeId = PLACE_ID),
        )
        val placeRepository = mockk<PlaceRepository>(relaxed = true)
        val viewModel = SearchViewModel(
            getRecentSearchesUseCase = GetRecentSearchesUseCase(recentRepository),
            deleteAllRecentSearchesUseCase = DeleteAllRecentSearchesUseCase(recentRepository),
            deleteRecentSearchUseCase = DeleteRecentSearchUseCase(recentRepository),
            getRelatedSearchUseCase = GetRelatedSearchUseCase(placeRepository),
            searchPlacesUseCase = SearchPlacesUseCase(placeRepository),
            registerRecentSearchUseCase = RegisterRecentSearchUseCase(recentRepository),
        )
        val openedPlaces = mutableListOf<Long>()
        composeRule.setContent {
            RodiTheme {
                SearchScreen(
                    origin = GeoPoint(37.5, 127.0),
                    onBack = {},
                    onPlaceClick = { openedPlaces += it },
                    onRegionClick = { _, _ -> },
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitUntil { composeRule.onAllNodesWithText(PLACE_KEYWORD).fetchSemanticsNodes().isNotEmpty() }
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText(UNKNOWN_REGION).performClick()
        composeRule.mainClock.advanceTimeBy(FRAME_BUDGET_MS)
        composeRule.onNodeWithText(PLACE_KEYWORD).performClick()
        composeRule.mainClock.advanceTimeBy(FRAME_BUDGET_MS)

        assertEquals(listOf(PLACE_ID), openedPlaces)
    }

    private companion object {
        const val UNKNOWN_REGION = "없는지역이름"
        const val PLACE_KEYWORD = "연습 코스"
        const val PLACE_ID = 9L
        const val FRAME_BUDGET_MS = 500L
    }
}
