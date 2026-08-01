package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceListContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstPageGenerationStartsFromTheNewFirstItem() {
        val generation = mutableLongStateOf(0L)
        val places = mutableStateOf((1L..6L).map(::summary))

        composeRule.setContent {
            RodiTheme {
                key(generation.longValue) {
                    PlaceListContent(
                        places = places.value,
                        onPlaceClick = {},
                        onLoadNextPage = {},
                        isNextPageLoading = false,
                        topContentPadding = 0.dp,
                        modifier = Modifier.height(300.dp),
                    )
                }
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("place-3"))
        composeRule.onNodeWithText("place-3").assertIsDisplayed()

        composeRule.runOnIdle {
            places.value = listOf(summary(10), summary(11), summary(3), summary(12))
            generation.longValue += 1
        }

        composeRule.onNodeWithText("place-10").assertIsDisplayed()
    }
}

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
