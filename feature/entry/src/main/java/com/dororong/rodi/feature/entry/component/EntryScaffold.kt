package com.dororong.rodi.feature.entry.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

private val APP_BAR_HEIGHT = 56.dp

/**
 * 진입 플로우 공통 골격: 앱바(56dp, 뒤로) + 진행 인디케이터 + 콘텐츠 + 하단 고정 버튼.
 *
 * @param currentStep 1..3 (진행 인디케이터 채움 개수)
 * @param onBack null 이면 뒤로 버튼 숨김(1단계)
 */
@Composable
fun EntryScaffold(
    currentStep: Int,
    onBack: (() -> Unit)?,
    buttonText: String = "",
    buttonEnabled: Boolean = false,
    onButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    showProgress: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        // 앱바 56dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(APP_BAR_HEIGHT)
                .padding(horizontal = RodiSpacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (onBack != null) {
                // 좌향 화살표 에셋 부재 → chevron_right 180° 미러로 임시 사용
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                    contentDescription = "뒤로",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .rotate(180f),
                )
            }
        }
        if (showProgress) {
            StepProgressIndicator(
                currentStep = currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RodiSpacing.md)
                    .padding(top = RodiSpacing.xs),
            )
            Spacer(Modifier.height(RodiSpacing.xl))
        }

        // 콘텐츠 영역
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RodiSpacing.md),
            content = content,
        )

        if (bottomBar != null) {
            bottomBar()
        } else {
            // 하단 고정 버튼 48dp + 상하 10dp
            RodiButton(
                text = buttonText,
                onClick = onButtonClick,
                enabled = buttonEnabled,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
            )
        }
    }
}
