package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewFormResponse
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption

fun ReviewFormResponse.toIssueForm(): ReviewIssueForm? {
    val questions = steps.flatMap { it.questions }.filter { it.code == "courseIssues" }
    if (questions.isEmpty()) return null
    val question = questions.single()
    val options = requireNotNull(question.options).sortedBy { it.order }.map { option ->
        ReviewIssueOption(
            code = option.code,
            label = option.label,
            requiresTextInput = option.requiresTextInput,
            textInputPlaceholder = option.textInputPlaceholder,
            textInputMaxLength = option.textInputMaxLength,
        )
    }
    return issueForm(requireNotNull(question.title), options)
}

/** 코스 신고 폼은 후기 '별로에요' 문제 항목과 같은 정의라 같은 화면 모델로 바꾼다. */
fun ReportFormResponse.toCourseIssueForm(): ReviewIssueForm = issueForm(
    title = title,
    options = options.sortedBy { it.order }.map { option ->
        ReviewIssueOption(
            code = option.code,
            label = option.label,
            requiresTextInput = option.requiresTextInput,
            textInputPlaceholder = option.textInputPlaceholder,
            textInputMaxLength = option.textInputMaxLength,
        )
    },
)

private fun issueForm(title: String, options: List<ReviewIssueOption>): ReviewIssueForm {
    require(title.isNotBlank())
    require(options.isNotEmpty() && options.map { it.code }.distinct().size == options.size)
    require(options.count { it.requiresTextInput } <= 1)
    options.forEach { option ->
        require(option.code.isNotBlank() && option.label.isNotBlank())
        if (option.requiresTextInput) require(requireNotNull(option.textInputMaxLength) in 1..30)
    }
    return ReviewIssueForm(title = title, options = options)
}
