package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.ReviewFormResponse
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption

fun ReviewFormResponse.toIssueForm(): ReviewIssueForm? {
    val questions = steps.flatMap { it.questions }.filter { it.code == "courseIssues" }
    if (questions.isEmpty()) return null
    val question = questions.single()
    val title = requireNotNull(question.title).also { require(it.isNotBlank()) }
    val options = requireNotNull(question.options).sortedBy { it.order }
    require(options.isNotEmpty() && options.map { it.code }.distinct().size == options.size)
    require(options.count { it.requiresTextInput } <= 1)
    return ReviewIssueForm(
        title = title,
        options = options.map { option ->
            require(option.code.isNotBlank() && option.label.isNotBlank())
            if (option.requiresTextInput) require(requireNotNull(option.textInputMaxLength) in 1..30)
            ReviewIssueOption(
                code = option.code,
                label = option.label,
                requiresTextInput = option.requiresTextInput,
                textInputPlaceholder = option.textInputPlaceholder,
                textInputMaxLength = option.textInputMaxLength,
            )
        },
    )
}
