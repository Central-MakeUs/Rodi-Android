package com.dororong.rodi.feature.entry.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.components.EntryScaffold
import com.dororong.rodi.feature.entry.components.OnboardingQuestionsContent

@Composable
fun CareerContent(
    form: OnboardingForm,
    profile: OnboardingProfile,
    nextEnabled: Boolean,
    onOptionSelect: (String, String) -> Unit,
    onGoalChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = form.steps.first()
    EntryScaffold(
        currentStep = if (form.skipsFollowUps(step, profile)) 3 else 2,
        onBack = onBack,
        buttonText = "다음",
        buttonEnabled = nextEnabled,
        onButtonClick = onNext,
        modifier = modifier,
        showProgress = true,
    ) {
        OnboardingQuestionsContent(form, step, profile, onOptionSelect, onGoalChange)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun CareerContentPreview() {
    RodiTheme {
        CareerContent(localOnboardingForm(), OnboardingProfile(drivingPeriod = DrivingPeriod.MONTHS_1_2),
            false, { _, _ -> }, {}, {}, {})
    }
}
