package com.dororong.rodi.feature.mypage.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun SavedCoursesRow(count: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "저장한 코스 ($count)",
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
            contentDescription = "저장한 코스 보기",
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedCoursesRowEmptyPreview() {
    RodiTheme {
        SavedCoursesRow(count = 0, onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedCoursesRowFilledPreview() {
    RodiTheme {
        SavedCoursesRow(count = 12, onClick = {})
    }
}
