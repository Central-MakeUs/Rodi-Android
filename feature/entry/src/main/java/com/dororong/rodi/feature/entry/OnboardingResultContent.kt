package com.dororong.rodi.feature.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.OnboardingLevel
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun OnboardingAnalysisContent(
    isFailed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .padding(horizontal = RodiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isFailed) {
            Text(
                text = "온보딩 정보를 제출하지 못했어요.",
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(
                text = "네트워크 연결을 확인한 뒤 다시 시도해주세요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RodiSpacing.xl))
            RodiButton(text = "다시 시도", onClick = onRetry)
        } else {
            CircularProgressIndicator(color = RodiTheme.colors.primary600)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "운전 경험을 분석하고 있어요.",
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(
                text = "딱 맞는 연습 코스를 준비할게요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OnboardingResultContent(
    level: OnboardingLevel,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .padding(horizontal = RodiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "당신의 운전 레벨은",
            style = RodiTheme.typography.heading2,
            color = RodiTheme.colors.black,
        )
        Spacer(Modifier.height(RodiSpacing.sm))
        Text(
            text = level.displayName,
            style = RodiTheme.typography.heading1,
            color = RodiTheme.colors.primary600,
        )
        Spacer(Modifier.height(RodiSpacing.md))
        Text(
            text = level.description,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        RodiButton(
            text = "로디 시작하기",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val OnboardingLevel.displayName: String
    get() = when (this) {
        OnboardingLevel.SEED -> "Seed"
        OnboardingLevel.ROOKIE -> "Rookie"
        OnboardingLevel.OWNER -> "Owner"
        OnboardingLevel.EXPLORER -> "Explorer"
        OnboardingLevel.NAVIGATOR -> "Navigator"
    }

private val OnboardingLevel.description: String
    get() = when (this) {
        OnboardingLevel.SEED -> "도로에서 직접 핸들 잡는 게 아직 낯설어요."
        OnboardingLevel.ROOKIE -> "교차로·유턴이 아직 긴장돼요."
        OnboardingLevel.OWNER -> "고속도로 합류·다차로 주행이 아직 어려워요."
        OnboardingLevel.EXPLORER -> "더 다양한 상황들을 연습하고 싶어요."
        OnboardingLevel.NAVIGATOR -> "길잡이로 함께해요. 익숙한 운전 경험을 바탕으로 다른 운전자에게 도움이 되는 코스를 남겨보세요."
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun OnboardingAnalysisContentPreview() {
    RodiTheme { OnboardingAnalysisContent(isFailed = false, onRetry = {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun OnboardingResultContentPreview() {
    RodiTheme { OnboardingResultContent(level = OnboardingLevel.ROOKIE, onStart = {}) }
}
