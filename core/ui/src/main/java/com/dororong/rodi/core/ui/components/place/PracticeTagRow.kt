package com.dororong.rodi.core.ui.components.place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun PracticeTagRow(
    types: List<PracticeType>,
    modifier: Modifier = Modifier,
    maxTags: Int = 3,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        types.distinct().take(maxTags).forEach { type ->
            Text(
                text = type.label,
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
                modifier = Modifier
                    .background(RodiTheme.colors.gray200, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Preview(name = "Practice tags - short", showBackground = true, widthDp = 360)
@Composable
private fun PracticeTagRowShortPreview() {
    RodiTheme { PracticeTagRow(listOf(PracticeType.PARKING)) }
}

@Preview(name = "Practice tags - overflow", showBackground = true, widthDp = 360)
@Composable
private fun PracticeTagRowOverflowPreview() {
    RodiTheme { PracticeTagRow(PracticeType.entries.toList()) }
}
