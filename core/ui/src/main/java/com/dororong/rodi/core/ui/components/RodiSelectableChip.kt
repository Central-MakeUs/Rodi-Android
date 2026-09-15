package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiSelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = RodiTheme.typography.body3Medium,
        color = if (selected) RodiTheme.colors.primary800 else RodiTheme.colors.gray600,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) RodiTheme.colors.primary100 else RodiTheme.colors.white)
            .border(
                width = 1.dp,
                color = if (selected) RodiTheme.colors.primary600 else RodiTheme.colors.primary200,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiSelectableChipPreview() {
    RodiTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RodiSelectableChip(text = "미선택", selected = false, onClick = {})
            RodiSelectableChip(text = "선택", selected = true, onClick = {})
            RodiSelectableChip(text = "순서", selected = true, onClick = {})
        }
    }
}
