package com.dororong.rodi.feature.entry.component

import com.dororong.rodi.feature.entry.OnboardingAnalysisState
import com.dororong.rodi.feature.entry.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.R as CoreUiR

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
                .fillMaxWidth()
                .height(258.dp)
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "연습 유형 분석 완료!",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                level.characterImageRes?.let { imageRes ->
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = level.displayName,
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = level.description,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = RodiTheme.colors.gray100)
        Box(
            modifier = Modifier
                .height(145.dp)
                .fillMaxWidth(),
        ) {
            level.recommendedPracticeTypes?.let { practiceTypes ->
                Column(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                ) {
                    Text(
                        text = "추천 연습 유형",
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.black,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        practiceTypes.forEach { practiceType ->
                            RecommendedPracticeTypeChip(practiceType)
                        }
                    }
                }
            }
            RodiButton(
                text = "확인",
                onClick = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun RecommendedPracticeTypeChip(text: String) {
    Text(
        text = text,
        style = RodiTheme.typography.caption1Medium,
        color = RodiTheme.colors.gray800,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(RodiTheme.colors.primary20)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
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
        OnboardingLevel.NAVIGATOR -> "길잡이로 함께해요.\n지금까지의 경험을 바탕으로 다른 운전자에게 도움이 되는 코스를 남겨보세요."
    }

private val OnboardingLevel.characterImageRes: Int?
    get() = when (this) {
        OnboardingLevel.SEED -> CoreUiR.drawable.illust_level_seed
        OnboardingLevel.ROOKIE -> CoreUiR.drawable.illust_level_rookie
        OnboardingLevel.OWNER -> CoreUiR.drawable.illust_level_owner
        OnboardingLevel.EXPLORER -> CoreUiR.drawable.illust_level_explorer
        OnboardingLevel.NAVIGATOR -> null
    }

private val OnboardingLevel.recommendedPracticeTypes: List<String>?
    get() = when (this) {
        OnboardingLevel.SEED -> listOf("직선주행", "좌·우회전", "차선변경")
        OnboardingLevel.ROOKIE -> listOf("U턴", "좌·우회전", "직선주행")
        OnboardingLevel.OWNER -> listOf("고속진입", "합류", "다차로 주행")
        OnboardingLevel.EXPLORER -> listOf("비보호 좌회전", "회전 교차로", "좁은 도로 주행")
        OnboardingLevel.NAVIGATOR -> null
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
