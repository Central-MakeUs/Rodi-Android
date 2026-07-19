package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun PlaceEmptyContent(
    isInitialError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(375.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptySheetHandle()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.illust_course_empty),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isInitialError) "장소를 불러오지 못했어요." else "추천할 수 있는 연습 코스를 찾지 못했어요.",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isInitialError) {
                    "네트워크 연결을 확인하고\n잠시 후 다시 시도해주세요."
                } else {
                    "지도를 축소시켜, 전체 지역의\n연습 코스를 둘러보세요."
                },
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(287.dp),
            )
        }
    }
}

@Composable
private fun EmptySheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 4.dp)
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(3.dp)),
        )
    }
}

@Preview(name = "Place empty", showBackground = true, widthDp = 375, heightDp = 380)
@Composable
private fun PlaceEmptyPreview() {
    RodiTheme { PlaceEmptyContent(false) }
}

@Preview(name = "Place initial error", showBackground = true, widthDp = 375, heightDp = 380)
@Composable
private fun PlaceErrorPreview() {
    RodiTheme { PlaceEmptyContent(true) }
}
