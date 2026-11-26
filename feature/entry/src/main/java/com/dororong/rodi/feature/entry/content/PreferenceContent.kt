package com.dororong.rodi.feature.entry.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.components.EntryScaffold
import com.dororong.rodi.feature.entry.components.OnboardingQuestionsContent

@Composable
fun PreferenceContent(
    form: OnboardingForm,
    profile: OnboardingProfile,
    nextEnabled: Boolean,
    onOptionSelect: (String, String) -> Unit,
    onGoalChange: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = form.steps.last()
    EntryScaffold(
        currentStep = 3,
        onBack = onBack,
        modifier = modifier,
        showProgress = true,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (step.skippable) {
                    RodiButton("건너뛰기", onSkip, variant = RodiButtonVariant.Secondary,
                        fillMaxWidth = false, modifier = Modifier.width(136.dp))
                }
                RodiButton("다음", onNext, enabled = nextEnabled, fillMaxWidth = false, modifier = Modifier.weight(1f))
            }
        },
    ) {
        OnboardingQuestionsContent(form, step, profile, onOptionSelect, onGoalChange)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PreferenceContentPreview() {
    RodiTheme {
        PreferenceContent(localOnboardingForm(), OnboardingProfile(practiceSituations = listOf(PracticeSituation.PARKING)),
            true, { _, _ -> }, {}, {}, {}, {})
    }
}
