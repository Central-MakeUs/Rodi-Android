package com.dororong.rodi.feature.home.detail

import android.app.Activity
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeActivityScenarioOption
import com.github.takahirom.roborazzi.RoborazziComposeCaptureOption
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
@OptIn(ExperimentalRoborazziApi::class)
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

    @Test
    fun `captures expanded state`() {
        var collapsedHeightPx = 0
        val expandSheet = ExpandSheetBeforeCapture { collapsedHeightPx }
        val composeOptions = RoborazziComposeOptions {
            addOption(expandSheet)
        }

        captureRoboImage(
            filePath = "CourseDetailSheetRoborazziTest/expanded.png",
            roborazziComposeOptions = composeOptions,
        ) {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    CourseDetailSheet(
                        place = HomePreviewData.courseDetail,
                        isBookmarkUpdating = false,
                        onDismiss = {},
                        onBookmarkClick = {},
                        onNavigate = {},
                        onSheetHeightChanged = { height -> collapsedHeightPx = height },
                        reviewContent = {},
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalRoborazziApi::class)
private class ExpandSheetBeforeCapture(
    private val collapsedHeightPx: () -> Int,
) : RoborazziComposeActivityScenarioOption, RoborazziComposeCaptureOption {
    private var activity: Activity? = null

    override fun configureWithActivityScenario(scenario: ActivityScenario<out Activity>) {
        scenario.onActivity { activity = it }
    }

    override fun beforeCapture() {
        val currentActivity = checkNotNull(activity)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val composeView = currentActivity.window.decorView.findComposeView()
            ?: error("Roborazzi ComposeView was not found")
        val collapsedHeight = collapsedHeightPx()
        check(collapsedHeight > 0) { "CourseDetailSheet collapsed height was not measured" }

        val startY = (composeView.height - collapsedHeight + 12).toFloat()
        dispatchSwipe(
            view = composeView,
            x = composeView.width / 2f,
            startY = startY,
            endY = 12f,
        )
        ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    }

    override fun afterCapture() {
        activity = null
    }
}

private fun dispatchSwipe(
    view: View,
    x: Float,
    startY: Float,
    endY: Float,
) {
    val downTime = SystemClock.uptimeMillis()
    fun dispatch(action: Int, eventTime: Long, y: Float) {
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).also { event ->
            check(view.dispatchTouchEvent(event)) { "CourseDetailSheet did not receive the drag" }
            event.recycle()
        }
    }

    dispatch(MotionEvent.ACTION_DOWN, downTime, startY)
    repeat(12) { index ->
        val fraction = (index + 1) / 12f
        dispatch(
            MotionEvent.ACTION_MOVE,
            downTime + index + 1L,
            startY + (endY - startY) * fraction,
        )
    }
    dispatch(MotionEvent.ACTION_UP, downTime + 14L, endY)
}

private fun View.findComposeView(): ComposeView? = when (this) {
    is ComposeView -> this
    is ViewGroup -> (0 until childCount)
        .asSequence()
        .mapNotNull { getChildAt(it).findComposeView() }
        .firstOrNull()
    else -> null
}
