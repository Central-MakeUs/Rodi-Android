package com.dororong.rodi.feature.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.DrivingPeriod
import com.dororong.rodi.core.domain.RecentDrivingFrequency
import com.dororong.rodi.core.domain.RoadExperience
import com.dororong.rodi.core.domain.SoloDrivingRange
import com.dororong.rodi.core.domain.SoloParkingLevel
import com.dororong.rodi.core.ui.components.RodiSelectableChip
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun CareerContent(
    drivingPeriod: DrivingPeriod?,
    recentFrequency: RecentDrivingFrequency?,
    roadExperience: RoadExperience?,
    soloDrivingRange: SoloDrivingRange?,
    soloParkingLevel: SoloParkingLevel?,
    nextEnabled: Boolean,
    onDrivingPeriodSelect: (DrivingPeriod) -> Unit,
    onRecentFrequencySelect: (RecentDrivingFrequency) -> Unit,
    onRoadExperienceSelect: (RoadExperience) -> Unit,
    onSoloDrivingRangeSelect: (SoloDrivingRange) -> Unit,
    onSoloParkingLevelSelect: (SoloParkingLevel) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryScaffold(
        currentStep = 2,
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
            Spacer(Modifier.height(RodiSpacing.lg))
            SingleChoiceQuestion(
                title = "가장 최근, 운전을 언제 하셨나요?",
                values = RecentDrivingFrequency.entries,
                selected = recentFrequency,
                label = RecentDrivingFrequency::label,
                onSelect = onRecentFrequencySelect,
            )
            Spacer(Modifier.height(RodiSpacing.lg))
            SingleChoiceQuestion(
                title = "면허 취득 후 도로 주행을 해본 적이 있나요?",
                values = RoadExperience.entries,
                selected = roadExperience,
                label = RoadExperience::label,
                onSelect = onRoadExperienceSelect,
            )
            if (roadExperience == RoadExperience.SOLO) {
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

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun CareerContentPreview() {
    RodiTheme {
        CareerContent(
            drivingPeriod = DrivingPeriod.MONTH_1_TO_3,
            recentFrequency = RecentDrivingFrequency.WEEKLY_1,
            roadExperience = RoadExperience.SOLO,
            soloDrivingRange = SoloDrivingRange.FAMILIAR_ROAD,
            soloParkingLevel = null,
            nextEnabled = false,
            onDrivingPeriodSelect = {},
            onRecentFrequencySelect = {},
            onRoadExperienceSelect = {},
            onSoloDrivingRangeSelect = {},
            onSoloParkingLevelSelect = {},
            onBack = {},
            onNext = {},
        )
    }
}
