package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.components.error.RodiInlineRetryError
import com.dororong.rodi.core.ui.components.error.RodiRetryError
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class RodiRetryErrorRoborazziTest {
    @Test
    fun `captures full screen retry error`() {
        captureRoboImage("RodiRetryErrorRoborazziTest/full_screen.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    RodiRetryError(message = "목록을 불러오지 못했어요.", onRetry = {})
                }
            }
        }
    }

    @Test
    fun `captures inline retry error`() {
        captureRoboImage("RodiRetryErrorRoborazziTest/inline.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    RodiInlineRetryError(message = "다음 목록을 불러오지 못했어요.", onRetry = {})
                }
            }
        }
    }
}
