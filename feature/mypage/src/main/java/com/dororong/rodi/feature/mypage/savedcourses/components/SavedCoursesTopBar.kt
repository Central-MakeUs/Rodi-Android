package com.dororong.rodi.feature.mypage.savedcourses.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
internal fun SavedCoursesTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(
            text = "저장목록",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun SavedCoursesTopBarPreview() {
    RodiTheme {
        SavedCoursesTopBar(onBack = {})
    }
}
