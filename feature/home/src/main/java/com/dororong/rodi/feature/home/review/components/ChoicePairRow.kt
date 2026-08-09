package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun ChoicePairRow(
    startLabel: String,
    endLabel: String,
    selected: Boolean?,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Choice(label = startLabel, selected = selected == false) { onSelect(false) }
        Choice(label = endLabel, selected = selected == true) { onSelect(true) }
    }
}

@Composable
private fun RowScope.Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .background(
                color = if (selected) RodiTheme.colors.primary600 else RodiTheme.colors.white,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = if (selected) RodiTheme.colors.primary600 else RodiTheme.colors.gray300,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RodiTheme.typography.body3Medium,
            color = if (selected) RodiTheme.colors.white else RodiTheme.colors.gray800,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "선택 쌍 - 미선택", showBackground = true, widthDp = 375)
@Composable
private fun ChoicePairEmptyPreview() = RodiTheme {
    ChoicePairRow("추천해요", "아쉬워요", null, {})
}

@Preview(name = "선택 쌍 - 왼쪽", showBackground = true, widthDp = 375)
@Composable
private fun ChoicePairStartPreview() = RodiTheme {
    ChoicePairRow("추천해요", "아쉬워요", false, {})
}

@Preview(name = "선택 쌍 - 오른쪽", showBackground = true, widthDp = 375)
@Composable
private fun ChoicePairEndPreview() = RodiTheme {
    ChoicePairRow("추천해요", "아쉬워요", true, {})
}
