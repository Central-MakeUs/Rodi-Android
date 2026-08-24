package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbar
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class RodiSnackbarRoborazziTest {
    @Test
    fun `captures snackbar with an action`() {
        captureRoboImage("RodiSnackbarRoborazziTest/action.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    RodiSnackbar(
                        data = RodiSnackbarData(
                            message = "네트워크 연결이 원활하지 않아요.",
                            actionLabel = "새로고침",
                            onAction = {},
                        ),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
