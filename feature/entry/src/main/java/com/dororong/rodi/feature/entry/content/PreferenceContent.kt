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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import com.dororong.rodi.core.ui.components.RodiButton
import com.dororong.rodi.core.ui.components.RodiButtonVariant
import com.dororong.rodi.core.ui.components.RodiSelectableChip
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun PreferenceContent(
    practiceSituations: List<PracticeSituation>,
    vehicleType: VehicleType?,
    goal: String,
    nextEnabled: Boolean,
    onPracticeSituationToggle: (PracticeSituation) -> Unit,
    onVehicleTypeSelect: (VehicleType) -> Unit,
    onGoalChange: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryScaffold(
        currentStep = 3,
        onBack = onBack,
        modifier = modifier,
        showProgress = true,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = RodiSpacing.md, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RodiButton(
                    text = "건너뛰기",
                    onClick = onSkip,
                    variant = RodiButtonVariant.Secondary,
                    fillMaxWidth = false,
                    modifier = Modifier.width(136.dp),
                )
                RodiButton(
                    text = "다음",
                    onClick = onNext,
                    enabled = nextEnabled,
                    fillMaxWidth = false,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("추가 정보를 입력하면 더 정확해요.", style = RodiTheme.typography.heading2, color = RodiTheme.colors.black)
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(
                "딱 맞는 코스 추천을 위한 선택항목이에요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
            )
            Spacer(Modifier.height(RodiSpacing.xl))

            PracticeSituationQuestion(
                selected = practiceSituations,
                onToggle = onPracticeSituationToggle,
            )
            Spacer(Modifier.height(RodiSpacing.lg))
            VehicleTypeQuestion(
                selected = vehicleType,
                onSelect = onVehicleTypeSelect,
            )
            Spacer(Modifier.height(RodiSpacing.lg))
            GoalQuestion(goal = goal, onGoalChange = onGoalChange)
            Spacer(Modifier.height(RodiSpacing.xl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeSituationQuestion(
    selected: List<PracticeSituation>,
    onToggle: (PracticeSituation) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("더 연습해보고 싶은 상황이 있나요?", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
            Spacer(Modifier.width(RodiSpacing.sm))
            Text("최대 3개", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        }
        Spacer(Modifier.height(10.dp))
        Text("1순위부터 순서대로 선택해주세요.", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        Spacer(Modifier.height(RodiSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PracticeSituation.entries.forEach { situation ->
                val order = selected.indexOf(situation).takeIf { it >= 0 }?.plus(1)
                RodiSelectableChip(
                    text = situation.label,
                    selected = selected.contains(situation),
                    order = order,
                    onClick = { onToggle(situation) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleTypeQuestion(
    selected: VehicleType?,
    onSelect: (VehicleType) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("주로 타는 차종은 무엇인가요?", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(RodiSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VehicleType.entries.forEach { type ->
                RodiSelectableChip(
                    text = type.label,
                    selected = type == selected,
                    onClick = { onSelect(type) },
                )
            }
        }
    }
}

@Composable
private fun GoalQuestion(
    goal: String,
    onGoalChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("이루고 싶은 운전 목표를 입력해주세요.", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(RodiSpacing.sm))
        OutlinedTextField(
            value = goal,
            onValueChange = { onGoalChange(it.take(MAX_GOAL_LENGTH)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 2,
            placeholder = {
                Text(
                    "ex)강남 운전 자신있게 하기!",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray500,
                )
            },
            textStyle = RodiTheme.typography.body3Medium,
            shape = RoundedCornerShape(RodiRadius.sm),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RodiTheme.colors.gray900,
                unfocusedBorderColor = RodiTheme.colors.gray300,
                focusedTextColor = RodiTheme.colors.black,
                unfocusedTextColor = RodiTheme.colors.black,
                cursorColor = RodiTheme.colors.primary600,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${goal.length}/$MAX_GOAL_LENGTH",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray600,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

private const val MAX_GOAL_LENGTH = 30

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PreferenceContentPreview() {
    RodiTheme {
        PreferenceContent(
            practiceSituations = listOf(PracticeSituation.U_TURN, PracticeSituation.PARKING),
            vehicleType = VehicleType.SUV,
            goal = "",
            nextEnabled = true,
            onPracticeSituationToggle = {},
            onVehicleTypeSelect = {},
            onGoalChange = {},
            onBack = {},
            onSkip = {},
            onNext = {},
        )
    }
}
