package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun CourseEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "추천할 수 있는 연습 코스를 찾지 못했어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.gray800,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "지도를 축소시켜, 전체 지역의\n연습 코스를 둘러보세요.",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray800,
            textAlign = TextAlign.Center,
        )
    }
}
