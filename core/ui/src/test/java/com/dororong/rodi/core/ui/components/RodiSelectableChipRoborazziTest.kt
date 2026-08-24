package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class RodiSelectableChipRoborazziTest {
    @Test
    fun `captures selected and unselected chips`() {
        captureRoboImage("RodiSelectableChipRoborazziTest/chips.png") {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RodiSelectableChip(text = "미선택", selected = false, onClick = {})
                        RodiSelectableChip(text = "선택", selected = true, onClick = {})
                    }
                }
            }
        }
    }
}
