package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class LevelReviewSectionRoborazziTest {
    @Test
    fun `captures empty review state`() {
        captureRoboImage("LevelReviewSectionRoborazziTest/empty.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    LevelReviewSection(
                        totalCount = 0,
                        recommendCount = 0,
                        selectedLevel = OnboardingLevel.SEED,
                        difficultyCounts = emptyMap(),
                        review = null,
                        onSelectLevel = {},
                        onAllClick = {},
                        onWriteReviewClick = {},
                        onEditReviewClick = {},
                        onDeleteReviewClick = {},
                        onReportReviewClick = {},
                        onBlockMemberClick = {},
                    )
                }
            }
        }
    }

    @Test
    fun `captures review summary without a selected level review`() {
        captureRoboImage("LevelReviewSectionRoborazziTest/summary.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    LevelReviewSection(
                        totalCount = 30,
                        recommendCount = 15,
                        selectedLevel = OnboardingLevel.SEED,
                        difficultyCounts = mapOf(ReviewDifficulty.NORMAL to 8L),
                        review = null,
                        onSelectLevel = {},
                        onAllClick = {},
                        onWriteReviewClick = {},
                        onEditReviewClick = {},
                        onDeleteReviewClick = {},
                        onReportReviewClick = {},
                        onBlockMemberClick = {},
                    )
                }
            }
        }
    }
}
