package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingFormResponse
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.OnboardingFormStep
import com.dororong.rodi.core.domain.model.onboarding.OnboardingOption
import com.dororong.rodi.core.domain.model.onboarding.OnboardingQuestion
import com.dororong.rodi.core.domain.model.onboarding.OnboardingQuestionType
import com.dororong.rodi.core.domain.model.onboarding.OnboardingShowWhen
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm

fun OnboardingFormResponse.toDomain(): OnboardingForm {
    val supported = localOnboardingForm().steps.flatMap { it.questions }.associateBy { it.code }
    require(steps.size == 2 && steps.map { it.order }.distinct().size == steps.size) { "Unsupported onboarding steps" }
    val codes = steps.flatMap { step -> step.questions.map { it.code } }
    require(codes.distinct().size == codes.size && "drivingPeriod" in codes) { "Invalid onboarding questions" }
    val form = OnboardingForm(steps.sortedBy { it.order }.map { step ->
        OnboardingFormStep(step.order, step.skippable, step.title, step.description,
            step.questions.sortedBy { it.order }.map { question ->
                val known = requireNotNull(supported[question.code]) { "Unsupported onboarding question" }
                val type = OnboardingQuestionType.valueOf(question.type)
                require(type == known.type) { "Unsupported onboarding question type" }
                require(question.maxSelect == null || question.maxSelect in 1..3) { "Invalid maximum selection" }
                require(question.maxLength == null || question.maxLength in 1..30) { "Invalid maximum text length" }
                require(type != OnboardingQuestionType.TEXT || question.maxLength != null) { "Missing maximum text length" }
                val options = question.options.orEmpty().sortedBy { it.order }.map { option ->
                    require(known.options.any { it.code == option.code }) { "Unsupported onboarding option" }
                    OnboardingOption(option.code, option.label, option.order, option.exclusive == true, option.skipsFollowUps == true)
                }
                require(options.map { it.code }.distinct().size == options.size) { "Duplicate onboarding options" }
                require(type == OnboardingQuestionType.TEXT || options.isNotEmpty()) { "Missing onboarding options" }
                val showWhen = question.showWhen?.let { condition ->
                    val conditionQuestion = requireNotNull(supported[condition.question]) { "Unsupported onboarding condition" }
                    require(condition.question in codes && condition.anyOf.isNotEmpty() &&
                        condition.anyOf.all { code -> conditionQuestion.options.any { it.code == code } }) { "Invalid onboarding condition" }
                    OnboardingShowWhen(condition.question, condition.anyOf)
                }
                OnboardingQuestion(question.code, question.order, type, question.required, question.title,
                    question.description, question.placeholder, question.maxSelect, question.maxLength, showWhen, options)
            },
        )
    })
    require(form.steps.first().questions.any { it.code == "drivingPeriod" && it.required }) { "Missing required driving period" }
    return form
}
