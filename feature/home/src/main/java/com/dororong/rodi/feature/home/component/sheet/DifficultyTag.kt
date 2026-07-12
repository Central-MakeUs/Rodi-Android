package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Difficulty
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun DifficultyTag(difficulty: Difficulty) {
    val background = when (difficulty) {
        Difficulty.LV1 -> Color(0xFFCDF2F6)
        Difficulty.LV2 -> Color(0xFFD0F7DF)
        Difficulty.LV3 -> Color(0xFFFFF6A4)
        Difficulty.LV4 -> Color(0xFFFFE6C0)
        Difficulty.LV5 -> Color(0xFFFFD6D6)
    }
    Surface(shape = RoundedCornerShape(2.dp), color = background) {
        Text(
            difficulty.label,
            style = RodiTheme.typography.caption3Medium,
            color = RodiTheme.colors.gray800,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
