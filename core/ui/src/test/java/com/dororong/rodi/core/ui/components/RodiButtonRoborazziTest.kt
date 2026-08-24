package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class RodiButtonRoborazziTest {
    @Test
    fun `captures primary and secondary button states`() {
        captureRoboImage("RodiButtonRoborazziTest/buttons.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    ButtonStates()
                }
            }
        }
    }
}

@Composable
private fun ButtonStates() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RodiButton(text = "확인", onClick = {})
        RodiButton(
            text = "취소",
            onClick = {},
            variant = RodiButtonVariant.Secondary,
        )
        RodiButton(text = "비활성", onClick = {}, enabled = false)
    }
}
