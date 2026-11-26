package com.dororong.rodi.feature.entry.content

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.components.EntryScaffold

@Composable
fun OnboardingFormUnavailableContent(
    isLoading: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryScaffold(
        currentStep = 2,
        onBack = onBack,
        buttonText = "다시 불러오기",
        buttonEnabled = !isLoading,
        onButtonClick = onRetry,
        modifier = modifier,
    ) {
        Text(if (isLoading) "설문을 불러오고 있어요." else "설문을 불러오지 못했어요. 다시 시도해주세요.",
            style = RodiTheme.typography.heading2, color = RodiTheme.colors.black)
    }
}
