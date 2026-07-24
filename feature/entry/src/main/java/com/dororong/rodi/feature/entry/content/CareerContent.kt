package com.dororong.rodi.feature.entry.content

import com.dororong.rodi.feature.entry.component.EntryScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.isNavigatorLevel
import com.dororong.rodi.core.ui.components.RodiSelectableChip
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun CareerContent(
    drivingPeriod: DrivingPeriod?,
    recentFrequency: RecentDrivingFrequency?,
    roadExperiences: List<RoadExperience>,
    soloDrivingRange: SoloDrivingRange?,
    soloParkingLevel: SoloParkingLevel?,
    nextEnabled: Boolean,
    onDrivingPeriodSelect: (DrivingPeriod) -> Unit,
    onRecentFrequencySelect: (RecentDrivingFrequency) -> Unit,
    onRoadExperienceToggle: (RoadExperience) -> Unit,
    onSoloDrivingRangeSelect: (SoloDrivingRange) -> Unit,
    onSoloParkingLevelSelect: (SoloParkingLevel) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryScaffold(
        currentStep = if (drivingPeriod?.isNavigatorLevel == true) 3 else 2,
        onBack = onBack,
        buttonText = "다음",
        buttonEnabled = nextEnabled,
        onButtonClick = onNext,
        modifier = modifier,
        showProgress = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("운전 경험에 대해 알려주세요.", style = RodiTheme.typography.heading2, color = RodiTheme.colors.black)
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(
                "자세히 입력할수록 더 잘 맞는 연습 장소를 추천해요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
            )
            Spacer(Modifier.height(RodiSpacing.xl))

            SingleChoiceQuestion(
                title = "면허 취득 후 실제 운전한 기간을 알려주세요",
                values = DrivingPeriod.entries,
                selected = drivingPeriod,
                label = DrivingPeriod::label,
                onSelect = onDrivingPeriodSelect,
            )

            if (drivingPeriod.requiresDetailedCareerQuestions) {
                Spacer(Modifier.height(RodiSpacing.lg))
                SingleChoiceQuestion(
                    title = "가장 최근, 운전을 언제 하셨나요?",
                    values = RecentDrivingFrequency.entries,
                    selected = recentFrequency,
                    label = RecentDrivingFrequency::label,
                    onSelect = onRecentFrequencySelect,
                )
                Spacer(Modifier.height(RodiSpacing.lg))
                MultiChoiceQuestion(
                    title = "면허 취득후 도로주행을 해본 적이 있나요?",
                    assistiveText = "복수선택",
                    values = RoadExperience.entries,
                    selected = roadExperiences,
                    label = RoadExperience::label,
                    onToggle = onRoadExperienceToggle,
                )
            }

            if (roadExperiences.contains(RoadExperience.SOLO)) {
                Spacer(Modifier.height(RodiSpacing.lg))
                SingleChoiceQuestion(
                    title = "혼자 운전, 어디까지 해봤나요?",
                    values = SoloDrivingRange.entries,
                    selected = soloDrivingRange,
                    label = SoloDrivingRange::label,
                    onSelect = onSoloDrivingRangeSelect,
                )
                Spacer(Modifier.height(RodiSpacing.lg))
                SingleChoiceQuestion(
                    title = "혼자 주차는 어느 정도 해봤나요?",
                    values = SoloParkingLevel.entries,
                    selected = soloParkingLevel,
                    label = SoloParkingLevel::label,
                    onSelect = onSoloParkingLevelSelect,
                )
            }
            Spacer(Modifier.height(RodiSpacing.xl))
        }
    }
}

private val DrivingPeriod?.requiresDetailedCareerQuestions: Boolean
    get() = this?.isNavigatorLevel == false

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SingleChoiceQuestion(
    title: String,
    values: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(RodiSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { value ->
                RodiSelectableChip(
                    text = label(value),
                    selected = value == selected,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChoiceQuestion(
    title: String,
    assistiveText: String,
    values: List<T>,
    selected: List<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
            Spacer(Modifier.width(4.dp))
            Text(assistiveText, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        }
        Spacer(Modifier.height(RodiSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { value ->
                RodiSelectableChip(
                    text = label(value),
                    selected = selected.contains(value),
                    onClick = { onToggle(value) },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun CareerContentPreview() {
    RodiTheme {
        CareerContent(
            drivingPeriod = DrivingPeriod.MONTHS_1_2,
            recentFrequency = RecentDrivingFrequency.WEEKLY_1,
            roadExperiences = listOf(RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.FAMILIAR_ROAD,
            soloParkingLevel = null,
            nextEnabled = false,
            onDrivingPeriodSelect = {},
            onRecentFrequencySelect = {},
            onRoadExperienceToggle = {},
            onSoloDrivingRangeSelect = {},
            onSoloParkingLevelSelect = {},
            onBack = {},
            onNext = {},
        )
    }
}
