package com.dororong.rodi.feature.entry.component

import com.dororong.rodi.feature.entry.OnboardingAnalysisState
import com.dororong.rodi.feature.entry.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun OnboardingAnalysisDialog(
    state: OnboardingAnalysisState,
    level: OnboardingLevel,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier.width(290.dp),
            shape = RoundedCornerShape(12.dp),
            color = RodiTheme.colors.white,
        ) {
            when (state) {
                OnboardingAnalysisState.ANALYZING -> AnalysisLoadingContent()
                OnboardingAnalysisState.RESULT -> AnalysisResultContent(level, onConfirm)
            }
        }
    }
}

@Composable
private fun AnalysisLoadingContent() {
    Column(
        modifier = Modifier
            .height(404.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "연습 유형 분석 중",
            style = RodiTheme.typography.heading2,
            color = RodiTheme.colors.black,
        )
        Spacer(Modifier.height(24.dp))
        AsyncImage(
            model = R.drawable.illust_practice_type_analysis,
            contentDescription = null,
            modifier = Modifier
                .width(150.dp)
                .height(80.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun AnalysisResultContent(
    level: OnboardingLevel,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.height(404.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 30.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "연습 유형 분석 완료!",
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(16.dp))
            Box(Modifier.height(100.dp))
            Text(
                text = level.displayName,
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = level.description,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "추천 연습 유형",
                style = RodiTheme.typography.body3SemiBold,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = level.recommendedPracticeTypes,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.primary800,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(RodiTheme.colors.primary20)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(12.dp))
            RodiButton(text = "확인", onClick = onConfirm)
        }
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
        OnboardingLevel.SEED -> "아직 도로에서 핸들을 잡는 게 낯설어요."
        OnboardingLevel.ROOKIE -> "아직 브레이크·엑셀 감각이 익숙하지 않아요."
        OnboardingLevel.OWNER -> "고속도로 합류·다차로 주행이 아직 어려워요."
        OnboardingLevel.EXPLORER -> "더 다양한 상황들을 연습하고 싶어요."
        OnboardingLevel.NAVIGATOR -> "길잡이로 함께해요."
    }

private val OnboardingLevel.recommendedPracticeTypes: String
    get() = when (this) {
        OnboardingLevel.SEED -> "직선주행  좌·우회전  차선변경"
        OnboardingLevel.ROOKIE -> "U턴  좌·우회전  직선주행"
        OnboardingLevel.OWNER -> "고속진입  합류  다차로 주행"
        OnboardingLevel.EXPLORER -> "비보호 좌회전  회전 교차로  좁은 도로 주행"
        OnboardingLevel.NAVIGATOR -> "코스 등록  리뷰 작성  추천 코스 공유"
    }

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun OnboardingAnalysisDialogPreview() {
    RodiTheme {
        OnboardingAnalysisDialog(
            state = OnboardingAnalysisState.RESULT,
            level = OnboardingLevel.ROOKIE,
            onConfirm = {},
        )
    }
}
