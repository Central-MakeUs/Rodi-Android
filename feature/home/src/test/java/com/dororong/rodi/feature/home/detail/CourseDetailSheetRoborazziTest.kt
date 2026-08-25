package com.dororong.rodi.feature.home.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class CourseDetailSheetRoborazziTest {
    @Test
    fun `captures collapsed state`() {
        captureRoboImage("CourseDetailSheetRoborazziTest/collapsed.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    CourseDetailSheet(
                        place = HomePreviewData.courseDetail,
                        isBookmarkUpdating = false,
                        onDismiss = {},
                        onBookmarkClick = {},
                        onNavigate = {},
                        onSheetHeightChanged = {},
                        reviewContent = {},
                    )
                }
            }
        }
    }

    @Test
    fun `captures bookmarked collapsed state`() {
        captureRoboImage("CourseDetailSheetRoborazziTest/bookmarked.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    CourseDetailSheet(
                        place = HomePreviewData.courseDetail.copy(isBookmarked = true),
                        isBookmarkUpdating = false,
                        onDismiss = {},
                        onBookmarkClick = {},
                        onNavigate = {},
                        onSheetHeightChanged = {},
                        reviewContent = {},
                    )
                }
            }
        }
    }
}
