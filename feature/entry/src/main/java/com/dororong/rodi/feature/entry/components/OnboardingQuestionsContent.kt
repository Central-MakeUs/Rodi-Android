package com.dororong.rodi.feature.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.OnboardingFormStep
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingQuestion
import com.dororong.rodi.core.domain.model.onboarding.OnboardingQuestionType
import com.dororong.rodi.core.domain.model.onboarding.answers
import com.dororong.rodi.core.ui.components.RodiOrderBadge
import com.dororong.rodi.core.ui.components.RodiSelectableChip
import com.dororong.rodi.core.ui.components.input.RodiFieldLabel
import com.dororong.rodi.core.ui.components.input.rememberGraphemeTextFieldState
import com.dororong.rodi.core.ui.components.input.rodiInputBorderColor
import com.dororong.rodi.core.ui.text.graphemeLength
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun OnboardingQuestionsContent(
    form: OnboardingForm,
    step: OnboardingFormStep,
    profile: OnboardingProfile,
    onOptionSelect: (String, String) -> Unit,
    onGoalChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(step.title, style = RodiTheme.typography.heading2, color = RodiTheme.colors.black)
        Spacer(Modifier.height(RodiSpacing.sm))
        Text(step.description, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        Spacer(Modifier.height(RodiSpacing.xl))
        form.visibleQuestions(step, profile).forEach { question ->
            key(question.code) {
                if (question.type == OnboardingQuestionType.TEXT) {
                    GoalQuestion(question, profile.goal, onGoalChange)
                } else {
                    ChoiceQuestion(question, profile.answers(question.code)) { code -> onOptionSelect(question.code, code) }
                }
                Spacer(Modifier.height(RodiSpacing.lg))
            }
        }
        Spacer(Modifier.height(RodiSpacing.sm))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceQuestion(question: OnboardingQuestion, selected: List<String>, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        RodiFieldLabel(
            question.title,
            required = question.required,
            assistiveText = when (question.type) {
                OnboardingQuestionType.RANKED_SELECT -> question.maxSelect?.let { "최대 ${it}개" }.orEmpty()
                OnboardingQuestionType.MULTI_SELECT -> "복수선택"
                else -> ""
            },
        )
        question.description?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        }
        Spacer(Modifier.height(RodiSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            question.options.forEach { option ->
                key(option.code) {
                    Box {
                        RodiSelectableChip(option.label, option.code in selected, { onSelect(option.code) })
                        if (question.type == OnboardingQuestionType.RANKED_SELECT) {
                            selected.indexOf(option.code).takeIf { it >= 0 }?.let { index ->
                                RodiOrderBadge(index + 1,
                                    modifier = Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).zIndex(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalQuestion(question: OnboardingQuestion, goal: String, onGoalChange: (String) -> Unit) {
    val maxLength = requireNotNull(question.maxLength)
    val textFieldState = rememberGraphemeTextFieldState(goal, maxLength, onGoalChange)
    Column(Modifier.fillMaxWidth()) {
        RodiFieldLabel(question.title, required = question.required)
        question.description?.let {
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(it, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
        }
        Spacer(Modifier.height(RodiSpacing.sm))
        OutlinedTextField(
            value = textFieldState.value,
            onValueChange = textFieldState.onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 2,
            placeholder = { question.placeholder?.let {
                Text(it, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray500)
            } },
            textStyle = RodiTheme.typography.body3Medium,
            shape = RoundedCornerShape(RodiRadius.sm),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = rodiInputBorderColor(focused = true),
                unfocusedBorderColor = rodiInputBorderColor(focused = false),
                focusedTextColor = RodiTheme.colors.black,
                unfocusedTextColor = RodiTheme.colors.black,
                cursorColor = RodiTheme.colors.primary600,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text("${textFieldState.value.text.graphemeLength()}/$maxLength",
            style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600,
            modifier = Modifier.align(Alignment.End))
    }
}
