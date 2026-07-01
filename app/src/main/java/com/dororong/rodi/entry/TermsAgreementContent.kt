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
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

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
        Text("루티 서비스 시작하기", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black)
        Spacer(Modifier.height(RodiSpacing.sm))
        Text("아래 약관에 동의해주세요", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray800)
        Spacer(Modifier.height(RodiSpacing.lg))

        // 약관 전체 동의 (선택 시 보더 gray900)
        CheckRow(
            checked = allChecked,
            text = "약관 전체 동의",
            onToggle = { onAllToggle(!allChecked) },
            modifier = Modifier
                .height(50.dp)
                .border(
                    width = 1.dp,
                    color = if (allChecked) RodiTheme.colors.gray900 else RodiTheme.colors.gray300,
                    shape = RoundedCornerShape(RodiRadius.sm),
                )
                .padding(horizontal = RodiSpacing.md),
        )
        Spacer(Modifier.height(RodiSpacing.md))

        CheckRow(
            checked = service,
            text = TermsDocuments.SERVICE.requiredTitle,
            onToggle = onServiceToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick(TermsDocuments.SERVICE.url) },
            modifier = Modifier.padding(start = 16.dp),
        )
        CheckRow(
            checked = privacy,
            text = TermsDocuments.PRIVACY.requiredTitle,
            onToggle = onPrivacyToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick(TermsDocuments.PRIVACY.url) },
            modifier = Modifier.padding(start = 16.dp),
        )
        CheckRow(
            checked = location,
            text = TermsDocuments.LOCATION.requiredTitle,
            onToggle = onLocationToggle,
            trailingChevron = true,
            onChevronClick = { onTermsClick(TermsDocuments.LOCATION.url) },
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun TermsAgreementPreview() {
    RodiTheme {
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
