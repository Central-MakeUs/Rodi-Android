package com.dororong.rodi.entry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.ui.theme.RoutiRadius
import com.dororong.rodi.ui.theme.RoutiSpacing
import com.dororong.rodi.ui.theme.RoutiTheme

/**
 * 2단계: 약관 동의. 필수 3항목을 모두 체크하면 "다음" 활성. "약관 전체 동의"로 일괄 토글.
 */
@Composable
fun TermsAgreementContent(
    service: Boolean,
    privacy: Boolean,
    location: Boolean,
    onAllToggle: (Boolean) -> Unit,
    onServiceToggle: () -> Unit,
    onPrivacyToggle: () -> Unit,
    onLocationToggle: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onTermsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allChecked = service && privacy && location

    EntryScaffold(
        currentStep = 2,
        onBack = onBack,
        buttonText = "다음",
        buttonEnabled = allChecked,
        onButtonClick = onNext,
        modifier = modifier,
    ) {
        Text("루티 서비스 시작하기", style = RoutiTheme.typography.headline1, color = RoutiTheme.colors.black)
        Spacer(Modifier.height(RoutiSpacing.sm))
        Text("아래 약관에 동의해주세요", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Spacer(Modifier.height(RoutiSpacing.lg))

        // 약관 전체 동의 (선택 시 보더 gray900)
        CheckRow(
            checked = allChecked,
            text = "약관 전체 동의",
            onToggle = { onAllToggle(!allChecked) },
            modifier = Modifier
                .height(50.dp)
                .border(
                    width = 1.dp,
                    color = if (allChecked) RoutiTheme.colors.gray900 else RoutiTheme.colors.gray300,
                    shape = RoundedCornerShape(RoutiRadius.sm),
                )
                .padding(horizontal = RoutiSpacing.md),
        )
        Spacer(Modifier.height(RoutiSpacing.md))

        CheckRow(
            checked = service,
            text = "서비스 이용약관(필수)",
            onToggle = onServiceToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick("https://sites.google.com/view/dororong/홈") },
            modifier = Modifier.padding(start = 16.dp),
        )
        CheckRow(
            checked = privacy,
            text = "개인정보 수집·이용 동의(필수)",
            onToggle = onPrivacyToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick("https://sites.google.com/view/dorororongg/홈") },
            modifier = Modifier.padding(start = 16.dp),
        )
        CheckRow(
            checked = location,
            text = "위치기반 서비스 이용약관(필수)",
            onToggle = onLocationToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick("https://sites.google.com/view/dororonggg/홈") },
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun TermsAgreementPreview() {
    RoutiTheme {
        TermsAgreementContent(
            service = false,
            privacy = false,
            location = false,
            onAllToggle = {},
            onServiceToggle = {},
            onPrivacyToggle = {},
            onLocationToggle = {},
            onBack = {},
            onNext = {},
            onTermsClick = {},
        )
    }
}
