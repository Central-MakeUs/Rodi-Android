package com.dororong.rodi.feature.course.registration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationSearchContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseRegistrationSearchContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchBackButtonUsesAccessibleChevronHitTarget() {
        var backPressed = false
        composeRule.setContent {
            RodiTheme {
                CourseRegistrationSearchContent(
                    keyword = "중구",
                    isLoading = false,
                    result = CourseLocationSearchResult(),
                    error = null,
                    onBack = { backPressed = true },
                    onKeywordChanged = {},
                    onSubmit = {},
                    onClear = {},
                    onSelect = {},
                    onDeleteRecent = {},
                    onDeleteAll = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("검색 닫기")
            .assertIsDisplayed()
            .performClick()

        assertTrue(backPressed)
    }

    @Test
    fun searchRowsPreservePlaceAddressInAccessibleDescription() {
        composeRule.setContent {
            RodiTheme {
                CourseRegistrationSearchContent(
                    keyword = "길음",
                    isLoading = false,
                    result = CourseLocationSearchResult(
                        places = listOf(
                            CourseLocationSuggestion(
                                id = "place-1",
                                title = "길음역",
                                address = "서울 성북구 동소문로",
                                point = GeoPoint(37.603, 127.025),
                                kind = CourseLocationKind.PLACE,
                            ),
                        ),
                    ),
                    error = null,
                    onBack = {},
                    onKeywordChanged = {},
                    onSubmit = {},
                    onClear = {},
                    onSelect = {},
                    onDeleteRecent = {},
                    onDeleteAll = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("길음역, 서울 성북구 동소문로")
            .assertIsDisplayed()
    }
}
