package com.dororong.rodi.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.R
import com.dororong.rodi.ui.theme.RoutiRadius
import com.dororong.rodi.ui.theme.RoutiSpacing
import com.dororong.rodi.ui.theme.RoutiTheme

/**
 * 3단계: 운전 자격 및 주의 사항. 확인 3항목 모두 체크 시 "다음" 활성 → 완료.
 */
@Composable
fun DrivingPrecautionsContent(
    license: Boolean,
    companion: Boolean,
    agree: Boolean,
    onLicenseToggle: () -> Unit,
    onCompanionToggle: () -> Unit,
    onAgreeToggle: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allChecked = license && companion && agree

    EntryScaffold(
        currentStep = 3,
        onBack = onBack,
        buttonText = "다음",
        buttonEnabled = allChecked,
        onButtonClick = onComplete,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.illust_driving_precautions),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                )
            }
            Spacer(Modifier.height(RoutiSpacing.xl))
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("운전 자격 및 주의 사항", style = RoutiTheme.typography.headline1, color = RoutiTheme.colors.black)
                Spacer(Modifier.height(RoutiSpacing.sm))
                Text(
                    "안전한 연습을 위해 아래 내용을 반드시 확인해 주세요.",
                    style = RoutiTheme.typography.body3Medium,
                    color = RoutiTheme.colors.black,
                )
            }
            Spacer(Modifier.height(RoutiSpacing.md))

            // 책임 고지 카드
            Surface(
                color = RoutiTheme.colors.primary50,
                shape = RoundedCornerShape(RoutiRadius.xl),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = warningText(),
                    style = RoutiTheme.typography.caption1Regular,
                    color = RoutiTheme.colors.black,
                    modifier = Modifier.padding(RoutiSpacing.md),
                )
            }
            Spacer(Modifier.height(RoutiSpacing.lg))

            CheckRow(
                checked = license,
                text = "본인은 유효한 자동차 운전면허(제1·2종 보통 이상)를 소지한 만 18세 이상임을 확인합니다.",
                onToggle = onLicenseToggle,
                textColor = RoutiTheme.colors.gray700,
                modifier = Modifier.padding(start = 16.dp),
            )
            CheckRow(
                checked = companion,
                text = "연습운전면허 소지자는 운전경력 2년 이상의 동승자와 함께 이용해야 함을 확인합니다.",
                onToggle = onCompanionToggle,
                textColor = RoutiTheme.colors.gray700,
                modifier = Modifier.padding(start = 16.dp),
            )
            CheckRow(
                checked = agree,
                text = "위 내용을 확인하고 동의합니다.",
                onToggle = onAgreeToggle,
                textColor = RoutiTheme.colors.gray700,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun warningText() = buildAnnotatedString {
    withStyle(SpanStyle(color = RoutiTheme.colors.primary600)) {
        append("루티는 사고가 나지 않는 장소를 보장하지 않습니다.\n")
    }
    append(" 연습에 적합한 장소를 추천 드리더라도, 도로 위에서 발생하는")
    withStyle(SpanStyle(color = RoutiTheme.colors.primary600)) {
        append("모든 사고의 책임은 운전자 본인에게 있습니다.\n")
    }
    append("항상 교통 법규를 준수하고, 안전을 최우선으로 생각해 주세요.")
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun DrivingPrecautionsPreview() {
    RoutiTheme {
        DrivingPrecautionsContent(
            license = false,
            companion = false,
            agree = false,
            onLicenseToggle = {},
            onCompanionToggle = {},
            onAgreeToggle = {},
            onBack = {},
            onComplete = {},
        )
    }
}
