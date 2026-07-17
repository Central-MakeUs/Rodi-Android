package com.dororong.rodi.feature.mypage.savedcourses.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R

@Composable
internal fun SavedCoursesEmpty(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-122).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.illust_saved_course_empty),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = "저장한 코스가 없어요.",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
            )
            Text(
                text = "홈에서 나에게 맞는 연습 코스를 찾아\n저장해보세요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 700)
@Composable
private fun SavedCoursesEmptyPreview() {
    RodiTheme {
        SavedCoursesEmpty(modifier = Modifier.fillMaxSize())
    }
}
