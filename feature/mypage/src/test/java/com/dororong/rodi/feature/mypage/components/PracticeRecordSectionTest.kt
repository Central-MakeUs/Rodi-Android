package com.dororong.rodi.feature.mypage.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PracticeRecordSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `미리보기는 삭제된 방문 기록을 유지하고 후기와 주차장 여부에 관계없이 후기 작성을 막는다`() {
        val record = mutableStateOf(courseRecord().copy(isDeleted = true))
        var reviewClicks = 0
        composeRule.setContent {
            RodiTheme {
                Box(Modifier.fillMaxSize()) {
                    PracticeRecordSection(
                        records = listOf(record.value),
                        onAllClick = {},
                        onWriteReviewClick = { _, _ -> reviewClicks += 1 },
                        onRetry = {},
                    )
                }
            }
        }

        listOf(false, true).forEach { hasReview ->
            listOf(PracticeType.ROUNDABOUT, PracticeType.PARKING).forEach { practiceType ->
                composeRule.runOnIdle {
                    record.value = record.value.copy(hasReview = hasReview, practiceTypes = listOf(practiceType))
                }
                composeRule.onNodeWithText("방문한 코스").assertIsDisplayed()
                composeRule.onNodeWithText("삭제된 코스")
                    .assertIsDisplayed()
                    .assertIsNotEnabled()
                    .performTouchInput { click() }
            }
        }

        composeRule.runOnIdle { assertEquals(0, reviewClicks) }
    }

    @Test
    fun `미리보기의 삭제되지 않은 코스는 후기 작성으로 이동한다`() {
        var selected: Pair<Long, String>? = null
        composeRule.setContent {
            RodiTheme {
                Box(Modifier.fillMaxSize()) {
                    PracticeRecordSection(
                        records = listOf(courseRecord()),
                        onAllClick = {},
                        onWriteReviewClick = { id, name -> selected = id to name },
                        onRetry = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("후기 작성")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(10L to "방문한 코스", selected) }
    }

    private fun courseRecord() = PracticeRecord(
        practiceId = 1,
        placeId = 10,
        placeName = "방문한 코스",
        practiceTypes = listOf(PracticeType.ROUNDABOUT),
        visitCount = 3,
        visitedAt = Instant.parse("2026-10-22T03:00:00Z"),
        hasReview = false,
        status = PracticeStatus.VISITED,
    )
}
