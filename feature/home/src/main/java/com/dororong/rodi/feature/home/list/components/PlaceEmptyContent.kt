package com.dororong.rodi.feature.home.list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiIllustratedEmptyState
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.components.SheetHandle

@Composable
fun PlaceEmptyContent(
    isInitialError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    // 목록이 비었을 땐 헤더(ListSheetHeader) 대신 이 컴포넌트가 바로 오므로 전체 영역을
    // 드래그할 수 있게 한다. 재시도 버튼의 탭은 anchoredDraggable이 이동으로 인식하지 않는다.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .then(dragHandleModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetHandle(modifier = Modifier.height(24.dp))
        RodiIllustratedEmptyState(
            modifier = Modifier.fillMaxWidth(),
            // 바텀시트 안이라 화면 기본값(134dp)을 쓰면 시트를 넘친다. 핸들 아래 여백만 준다.
            topPadding = 68.dp,
            painter = painterResource(R.drawable.illust_course_empty),
            imageSize = 80.dp,
            title = if (isInitialError) "장소를 불러오지 못했어요." else "추천할 수 있는 연습 코스를 찾지 못했어요.",
            description = if (isInitialError) {
                "네트워크 연결을 확인하고\n잠시 후 다시 시도해주세요."
            } else {
                "지도를 축소시켜, 전체 지역의\n연습 코스를 둘러보세요."
            },
            footer = {
                if (isInitialError) {
                    Spacer(Modifier.height(16.dp))
                    RodiButton(
                        text = "다시 시도",
                        onClick = onRetry,
                        fillMaxWidth = false,
                    )
                }
            },
        )
    }
}

@Preview(name = "Place empty", showBackground = true, widthDp = 375, heightDp = 380)
@Composable
private fun PlaceEmptyPreview() {
    RodiTheme { PlaceEmptyContent(isInitialError = false, onRetry = {}) }
}

@Preview(name = "Place initial error", showBackground = true, widthDp = 375, heightDp = 380)
@Composable
private fun PlaceErrorPreview() {
    RodiTheme { PlaceEmptyContent(isInitialError = true, onRetry = {}) }
}
