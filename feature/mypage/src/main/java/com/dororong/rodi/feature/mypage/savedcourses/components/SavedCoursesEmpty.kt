package com.dororong.rodi.feature.mypage.savedcourses.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R

@Composable
internal fun SavedCoursesEmpty(modifier: Modifier = Modifier) {
    RodiIllustratedEmptyState(
        modifier = modifier.fillMaxWidth(),
        painter = painterResource(R.drawable.illust_saved_course_empty),
        imageSize = 60.dp,
        title = "저장목록이 없어요.",
        description = "홈에서 나에게 맞는 연습 장소를 찾아\n저장해보세요.",
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 700)
@Composable
private fun SavedCoursesEmptyPreview() {
    RodiTheme {
        SavedCoursesEmpty(modifier = Modifier.fillMaxSize())
    }
}
