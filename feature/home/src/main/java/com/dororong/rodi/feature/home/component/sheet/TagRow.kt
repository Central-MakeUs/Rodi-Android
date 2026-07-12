package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.course.Difficulty
import com.dororong.rodi.core.domain.model.course.PracticeTag
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun TagRow(difficulty: Difficulty, tags: Set<PracticeTag>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DifficultyTag(difficulty)
        tags.take(2).forEach { PracticeTagChip(it.label) }
    }
}

@Composable
private fun PracticeTagChip(label: String) {
    Surface(shape = RoundedCornerShape(2.dp), color = RodiTheme.colors.gray200) {
        Text(
            label,
            style = RodiTheme.typography.caption3Medium,
            color = RodiTheme.colors.gray700,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
